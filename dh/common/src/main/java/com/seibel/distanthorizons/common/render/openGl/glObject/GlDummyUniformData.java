package com.seibel.distanthorizons.common.render.openGl.glObject;

import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiTerrainBufferContainer;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiTerrainContainerUniformBufferWrapper;

/**
 * With OpenGL all uniform data is uploaded during the rendering phase
 * so nothing is needed here.
 */
public class GlDummyUniformData implements IDhApiTerrainContainerUniformBufferWrapper
{
	@Override public void tryUpload(IDhApiTerrainBufferContainer bufferContainer) { }
	@Override public void close() { }
	
}
