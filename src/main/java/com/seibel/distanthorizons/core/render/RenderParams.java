package com.seibel.distanthorizons.core.render;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.api.internal.rendering.DhRenderState;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.EPlatform;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.world.IDhClientWorld;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractOptifineAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhGenericRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;

/**
 * An extension of {@link DhApiRenderParam}
 * that allows additional validation and putting all
 * rendering variables in a single place.
 */
public class RenderParams extends DhApiRenderParam
{
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	private static final long TIME_FOR_MAC_TO_FINISH_COMPILING_IN_MS = 10_000;
	private static boolean initialLoadingComplete = false;
	
	
	public IDhClientWorld dhClientWorld;
	public IDhClientLevel dhClientLevel;
	/** more specific override of the API value {@link DhApiRenderParam#clientLevelWrapper} */
	public IClientLevelWrapper clientLevelWrapper;
	public ILightMapWrapper lightmap;
	public RenderBufferHandler renderBufferHandler;
	public IDhGenericRenderer genericRenderer;
	public Vec3d exactCameraPosition;
	/** @see DhRenderState#vanillaFogEnabled */
	public boolean vanillaFogEnabled;
	
	public boolean hasBeenValidated = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public RenderParams(EDhApiRenderPass renderPass, DhRenderState renderState)
	{
		this(renderPass,
			renderState.partialTickTime,
			renderState.mcProjectionMatrix, renderState.mcModelViewMatrix,
			renderState.clientLevelWrapper,
			renderState.vanillaFogEnabled
		);
	}
	private RenderParams(
			EDhApiRenderPass renderPass,
			float newPartialTicks,
			Mat4f newMcProjectionMatrix, Mat4f newMcModelViewMatrix,
			IClientLevelWrapper clientLevelWrapper,
			boolean vanillaFogEnabled
		)
	{
		super(renderPass,
			newPartialTicks,
			RenderUtil.getNearClipPlaneInBlocks(), RenderUtil.getFarClipPlaneDistanceInBlocks(),
			newMcProjectionMatrix, newMcModelViewMatrix,
			RenderUtil.createLodProjectionMatrix(newMcProjectionMatrix), RenderUtil.createLodModelViewMatrix(newMcModelViewMatrix),
			clientLevelWrapper.getMinHeight(),
			clientLevelWrapper);
		
		
		this.dhClientWorld = SharedApi.tryGetDhClientWorld();
		if (this.dhClientWorld != null)
		{
			this.dhClientLevel = (IDhClientLevel) this.dhClientWorld.getLevel(clientLevelWrapper);
			if (this.dhClientLevel != null)
			{
				this.renderBufferHandler = this.dhClientLevel.getRenderBufferHandler();
				this.genericRenderer = this.dhClientLevel.getGenericRenderer();
			}
		}
		
		this.clientLevelWrapper = clientLevelWrapper;
		this.lightmap = MC_RENDER.getLightmapWrapper(this.clientLevelWrapper);
		
		if (MC_CLIENT.playerExists())
		{
			this.exactCameraPosition = MC_RENDER.getCameraExactPosition();
		}
		
		this.vanillaFogEnabled = vanillaFogEnabled;
		
	}
	
	//endregion
	
	
	
	//======================//
	// parameter validation //
	//======================//
	//region
	
	/** 
	 * Should be called before rendering is done.
	 * @return a message if LODs shouldn't be rendered, null if the LODs can render 
	 */
	public String getValidationErrorMessage(long firstRenderTimeMs)
	{
		// Note: all strings here should be constants to prevent String allocations
		
		this.hasBeenValidated = true;
		
		
		if (!MC_CLIENT.playerExists())
		{
			return "No Player Exists";
		}
		
		if (this.dhClientWorld == null)
		{
			return "No DH Client World Loaded";
		}
		
		if (this.dhClientLevel == null)
		{
			return "No DH Client Level Loaded";
		}
		
		if (this.clientLevelWrapper == null)
		{
			return "No Client Level Wrapper Loaded";
		}
		
		if (this.lightmap == null)
		{
			return "No Lightmap Loaded";
		}
		
		if (this.renderBufferHandler == null)
		{
			return "No RenderBufferHandler Present";
		}
		
		if (this.genericRenderer == null)
		{
			return "No Generic Renderer Present";
		}
		
		if (this.dhModelViewMatrix == null
			|| this.mcModelViewMatrix == null)
		{
			return "No MVM or Proj Matrix Given";
		}
		
		if (AbstractOptifineAccessor.optifinePresent()
			&& MC_RENDER.getTargetFramebuffer() == -1)
		{
			// wait for MC to finish setting up their renderer
			return "Optifine Target Frame Buffer not set";
		}
		
		
		// potential fix for a segfault when
		// Sodium and DH are running together
		if (EPlatform.get() == EPlatform.MACOS
			&& !initialLoadingComplete)
		{
			// Once MC starts rendering, wait a few seconds so
			// MC/Sodium can finish their shader compiling before DH does its own.
			// This will allow DH to compile its own shaders after Sodium finishes
			// compiling its own.
			long nowMs = System.currentTimeMillis();
			long firstAllowedRenderTimeMs = firstRenderTimeMs + TIME_FOR_MAC_TO_FINISH_COMPILING_IN_MS;
			if (nowMs < firstAllowedRenderTimeMs)
			{
				return "Waiting for initial MC compile...";
			}
			
			
			// null shouldn't happen, but just in case
			PriorityTaskPicker.Executor renderLoadExecutor = ThreadPoolUtil.getRenderLoadingExecutor();
			if (renderLoadExecutor == null)
			{
				return "Waiting for DH Threadpool...";
			}
			
			// wait for DH to finish loading, by the time that's done
			// java should have finished all of DH's JIT compiling,
			// which will hopefully mean less concurrency and thus a lower
			// chance of breaking
			// (plus this gives Sodium/vanill a bit longer to finish their setup)
			int taskCount = renderLoadExecutor.getQueueSize();
			if (taskCount > 0)
			{
				return "Waiting for DH JIT compiling...";
			}
			
			initialLoadingComplete = true;
		}
		
		
		return null;
	}
	
	//endregion
	
	
	
}
