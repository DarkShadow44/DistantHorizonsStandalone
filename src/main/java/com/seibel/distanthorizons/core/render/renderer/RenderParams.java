package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.renderer.generic.GenericObjectRenderer;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.world.IDhClientWorld;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractOptifineAccessor;
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
	
	
	public IDhClientWorld dhClientWorld;
	public IDhClientLevel dhClientLevel;
	/** more specific override of the API value {@link DhApiRenderParam#clientLevelWrapper} */
	public IClientLevelWrapper clientLevelWrapper;
	public ILightMapWrapper lightmap;
	public RenderBufferHandler renderBufferHandler;
	public GenericObjectRenderer genericRenderer;
	public Vec3d exactCameraPosition;
	
	public boolean validationRun = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public RenderParams(
			EDhApiRenderPass renderPass,
			float newPartialTicks,
			Mat4f newMcProjectionMatrix, Mat4f newMcModelViewMatrix,
			IClientLevelWrapper clientLevelWrapper
		)
	{
		super(renderPass,
			newPartialTicks,
			RenderUtil.getNearClipPlaneDistanceInBlocks(newPartialTicks), RenderUtil.getFarClipPlaneDistanceInBlocks(),
			newMcProjectionMatrix, newMcModelViewMatrix,
			RenderUtil.createLodProjectionMatrix(newMcProjectionMatrix, newPartialTicks), RenderUtil.createLodModelViewMatrix(newMcModelViewMatrix),
			clientLevelWrapper.getMinHeight(),
			clientLevelWrapper);
		
		
		this.dhClientWorld = SharedApi.tryGetDhClientWorld();
		if (this.dhClientWorld != null)
		{
			// TODO changing to getOrLoadClientLevel() fixes Immersive Portals only rendering the level the user starts in
			//  however this may break how other level handling is done so James doesn't want to change it.
			//  Special handling may be necessary when Immersive Portals is present, although additional testing is needed.
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
		
	}
	
	
	
	//======================//
	// parameter validation //
	//======================//
	
	/** 
	 * Should be called before rendering is done.
	 * @return a message if LODs shouldn't be rendered, null if the LODs can render 
	 */
	public String getValidationErrorMessage()
	{
		// Note: all strings here should be constants to prevent String allocations
		
		this.validationRun = true;
		
		
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
		
		
		return null;
	}
	
	
	
}
