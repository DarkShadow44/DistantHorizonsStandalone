package com.seibel.distanthorizons.api.interfaces.render.renderDef.objects;

/**
 * Holds all the necessary information data Distant Horizons
 * needs to render a single LOD.
 * 
 * @since API 7.3.0
 * @version 2026-10-02
 */
public interface IDhApiTerrainBufferContainer
{
	IDhApiVertexBufferWrapper[] getVboOpaqueWrappers();
	IDhApiVertexBufferWrapper[] getVboTransparentWrappers();
	
	IDhApiTerrainContainerUniformBufferWrapper getUniformContainer();
	
	
	
}
