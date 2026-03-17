package com.seibel.distanthorizons.core.wrapperInterfaces.render.objects;

import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;

import java.util.List;

public interface IDhGenericObjectVertexBufferContainer extends AutoCloseable
{
	void uploadDataToGpu();
	
	void updateVertexData(List<DhApiRenderableBox> uploadBoxList);
	
	EState getState();
	void setState(EState state);
	
	@Override
	void close();
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	enum EState
	{
		NEW,
		UPDATING_DATA,
		READY_TO_UPLOAD,
		RENDER,
		
		ERROR,
	}
	
	//endregion
	
	
	
}
