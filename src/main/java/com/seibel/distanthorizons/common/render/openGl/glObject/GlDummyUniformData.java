package com.seibel.distanthorizons.common.render.openGl.glObject;

import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.ILodContainerUniformBufferWrapper;

/**
 * With OpenGL all uniform data is uploaded during the rendering phase
 * so nothing is needed here.
 */
public class GlDummyUniformData implements ILodContainerUniformBufferWrapper
{
	@Override public void createUniformData(LodBufferContainer bufferContainer) { }
	@Override public void tryUpload() { }
	@Override public void upload() { }
	@Override public void close() { }
	
}
