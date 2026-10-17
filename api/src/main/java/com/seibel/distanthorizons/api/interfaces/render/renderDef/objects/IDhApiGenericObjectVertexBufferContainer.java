package com.seibel.distanthorizons.api.interfaces.render.renderDef.objects;

import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;

import java.util.List;

/**
 * Holds all the necessary information data Distant Horizons
 * needs to render a generic object.
 *
 * @since API 7.3.0
 * @version 2026-10-02
 */
public interface IDhApiGenericObjectVertexBufferContainer extends AutoCloseable
{
	/**
	 * Will be called on the render thread
	 * so you can upload any data necessary to the GPU.
	 */
	void uploadDataToGpu();
	
	/** 
	 * Will be called on a background Distant Horizons thread
	 * (not the render thread) so you can prepare any necessary
	 * buffers for uploading to the GPU without
	 * stalling the render thread.
	 */
	void updateCpuSideVertexData(List<DhApiRenderableBox> uploadBoxList);
	
	/** When first created should return {@link IDhApiGenericObjectVertexBufferContainer.EState#NEW} */
	EState getState();
	void setState(EState state);
	
	@Override
	void close();
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	/**
	 * These states are used by DH to handle
	 * automatic uploading/rendering of your container. <br><br>
	 * 
	 * You shouldn't need to set any states yourself,
	 * these states are all handled by DH while it
	 * handles the lifecycle of your object.
	 */
	enum EState
	{
		/** this container was just created */
		NEW,
		/** this container is in the process of having new data uploaded */
		UPDATING_DATA,
		/** this container has updated its CPU side data and can now upload to the GPU */
		READY_FOR_GPU_UPLOAD,
		/** this container has been uploaded to the GPU and is ready for rendering */
		RENDER,
		
		ERROR,
	}
	
	//endregion
	
	
	
}
