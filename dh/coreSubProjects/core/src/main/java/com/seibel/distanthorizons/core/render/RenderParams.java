package com.seibel.distanthorizons.core.render;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.api.internal.rendering.DhRenderState;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import com.seibel.distanthorizons.core.util.math.DhVec3d;
import com.seibel.distanthorizons.core.world.IDhClientWorld;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IOptifineAccessor;
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
	
	private static final IOptifineAccessor OPTIFINE_ACCESSOR = ModAccessorInjector.INSTANCE.get(IOptifineAccessor.class);
	
	/** 
	 * Copy used for API events. <br>
	 * A separate copy is used to prevent API users from accidentally setting values
	 * that screw up DH's copy of the render parameters.
	 */
	public final DhApiRenderParam apiCopy = new DhApiRenderParam();
	
	
	public IDhClientWorld dhClientWorld;
	public IDhClientLevel dhClientLevel;
	/** more specific override of the API value {@link DhApiRenderParam#clientLevelWrapper} */
	public IClientLevelWrapper clientLevelWrapper;
	public ILightMapWrapper lightmap;
	public RenderBufferHandler renderBufferHandler;
	public IDhGenericRenderer genericRenderer;
	public DhVec3d exactCameraPosition;
	/** @see DhRenderState#vanillaFogEnabled */
	public boolean vanillaFogEnabled;
	
	public boolean hasBeenValidated = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public void update(EDhApiRenderPass renderPass, DhRenderState renderState)
	{
		RenderUtil.setDhProjectionMatrix(this.dhProjectionMatrix, renderState.mcProjectionMatrix);
		this.dhModelViewMatrix.set(renderState.mcModelViewMatrix); // DH and MC MVM matrix are the same 
		
		super.update(renderPass,
			renderState.partialTickTime,
			RenderUtil.getNearClipPlaneInBlocks(), RenderUtil.getFarClipPlaneDistanceInBlocks(),
			renderState.mcProjectionMatrix, renderState.mcModelViewMatrix,
			this.dhProjectionMatrix, this.dhModelViewMatrix,
			renderState.clientLevelWrapper.getMinHeight(),
			renderState.clientLevelWrapper);
		
		
		
		this.clientLevelWrapper = renderState.clientLevelWrapper;
		
		this.dhClientWorld = SharedApi.tryGetDhClientWorld();
		if (this.dhClientWorld != null)
		{
			this.dhClientLevel = this.dhClientWorld.getOrLoadClientLevel(clientLevelWrapper);
			if (this.dhClientLevel != null)
			{
				this.renderBufferHandler = this.dhClientLevel.getRenderBufferHandler();
				this.genericRenderer = this.dhClientLevel.getGenericRenderer();
			}
		}
		
		this.lightmap = MC_RENDER.getLightmapWrapper(this.clientLevelWrapper);
		
		if (MC_CLIENT.playerExists())
		{
			this.exactCameraPosition = MC_RENDER.getCameraExactPosition();
		}
		
		this.vanillaFogEnabled = renderState.vanillaFogEnabled;
		
		this.apiCopy.update(this);
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
	public String getValidationErrorMessage()
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
		
		if (this.dhModelViewMatrix.equals(DhMat4f.IDENTITY) 
			|| this.dhModelViewMatrix.equals(DhMat4f.EMPTY))
		{
			return "No DH MVM Matrix Given";
		}
		
		if (this.mcModelViewMatrix.equals(DhMat4f.IDENTITY) 
			|| this.mcModelViewMatrix.equals(DhMat4f.EMPTY))
		{
			return "No MC MVM Matrix Given";
		}
		
		// projection matrix not checked since there are some MC versions where
		// the MVM and projection matrices are pre-multiplied together
		
		if (OPTIFINE_ACCESSOR != null
			&& MC_RENDER.getTargetFramebuffer() == -1)
		{
			// wait for MC to finish setting up their renderer
			return "Optifine Target Frame Buffer not set";
		}
		
		
		return null;
	}
	
	//endregion
	
	
	
}
