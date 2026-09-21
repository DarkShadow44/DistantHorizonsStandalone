package com.seibel.distanthorizons.common.render.openGl;

import com.seibel.distanthorizons.common.render.openGl.terrain.GlBlockTextureAtlas;
import com.seibel.distanthorizons.common.render.openGl.terrain.GlDhTerrainShaderProgram;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhTerrainRenderer;

public class GlDhTerrainRenderer implements IDhTerrainRenderer
{
	public static final GlDhTerrainRenderer INSTANCE = new GlDhTerrainRenderer();
	
	private GlDhTerrainShaderProgram terrainShaderProgram = null;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private GlDhTerrainRenderer() {}
	
	//endregion
	
	
	
	//=========//
	// getters //
	//=========//
	//region
	
	/** must be called on the render thread the first time so GL can run it's setup */
	public GlDhTerrainShaderProgram getTerrainShaderProgram()
	{
		if (this.terrainShaderProgram == null)
		{
			this.terrainShaderProgram = new GlDhTerrainShaderProgram();
		}
		
		return this.terrainShaderProgram;
	}
	
	//endregion
	
	
	
	//========//
	// render //
	//========//
	//region
	
	@Override 
	public void render(RenderParams renderEventParam, boolean opaquePass, SortedArraySet<LodBufferContainer> bufferContainers, IProfilerWrapper profiler)
	{
		this.getTerrainShaderProgram();
		
		this.terrainShaderProgram.tryInit();
		this.terrainShaderProgram.render(renderEventParam, opaquePass, bufferContainers, profiler);
	}
	
	//endregion
	
	
	
}
