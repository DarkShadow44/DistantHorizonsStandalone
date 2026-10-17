package com.seibel.distanthorizons.common.render.openGl;

import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiTerrainBufferContainer;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.util.IDhApiReadOnlyList;
import com.seibel.distanthorizons.common.render.openGl.terrain.GlDhTerrainShaderProgram;
import com.seibel.distanthorizons.core.render.RenderParam;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.IDhApiTerrainRenderer;

import java.util.List;

public class GlDhTerrainRenderer implements IDhApiTerrainRenderer
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
	public void render(
		DhApiRenderParam apiRenderEventParam, boolean opaquePass,
		IDhApiReadOnlyList<? extends IDhApiTerrainBufferContainer> bufferContainers)
	{
		RenderParam renderEventParam = (RenderParam) apiRenderEventParam;
		
		this.getTerrainShaderProgram();
		
		this.terrainShaderProgram.tryInit();
		this.terrainShaderProgram.render(renderEventParam, opaquePass, bufferContainers);
	}
	
	//endregion
	
	
	
}
