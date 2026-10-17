package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.interfaces.render.renderDef.IDhApiDebugWireframeRenderer;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderParam;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import com.seibel.distanthorizons.core.util.math.DhVec3f;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

public abstract class AbstractDebugWireframeRenderer implements IDhApiDebugWireframeRenderer
{
	protected static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder()
		.maxCountPerSecond(1)
		.build();
	
	protected static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	
	/** A box from 0,0,0 to 1,1,1 */
	protected static final float[] BOX_VERTICES = {
		//region
		// Pos x y z
		0, 0, 0,
		1, 0, 0,
		1, 1, 0,
		0, 1, 0,
		0, 0, 1,
		1, 0, 1,
		1, 1, 1,
		0, 1, 1,
		//endregion
	};
	
	protected static final int[] BOX_OUTLINE_INDICES = {
		//region
		0, 1,
		1, 2,
		2, 3,
		3, 0,
		
		4, 5,
		5, 6,
		6, 7,
		7, 4,
		
		0, 4,
		1, 5,
		2, 6,
		3, 7,
		//endregion
	};
	
	protected static final int VERTICES_PER_BOX = 8; // BOX_VERTICES.length / 3
	protected static final int INDICES_PER_BOX = BOX_OUTLINE_INDICES.length; // 24
	
	
	protected RenderParam renderParams;
	protected DhMat4f dhMvmProjMatrixThisFrame;
	protected DhVec3f camPosFloatThisFrame;
	
	
	
	//===========//
	// rendering //
	//===========//
	//region
	
	@Override 
	public void beginRenderBatch(DhApiRenderParam apiRenderParams)
	{
		this.renderParams = (RenderParam) apiRenderParams;
		
		this.dhMvmProjMatrixThisFrame = new DhMat4f(this.renderParams.dhMvmProjMatrix);
		this.camPosFloatThisFrame = new DhVec3f(this.renderParams.exactCameraPosition);
	}
	
	//endregion
	
	
	
}
