package com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteBufferCheckoutWrapper
{
	public ByteBuffer buffer = null;
	/** in bytes */
	public int size = -1;
	
	/**
	 * a buffer slice is used so the backing buffer
	 * can be bigger than the requested size.
	 */
	public ByteBuffer bufferSlice = null;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	/** waits to create a buffer until requested */
	public ByteBufferCheckoutWrapper() { }
	public ByteBufferCheckoutWrapper(int byteBufferSize)
	{
		this.clearAndSetSize(byteBufferSize);
	}
	
	//endregion
	
	
	
	//=========//
	// methods //
	//=========//
	//region
	
	public void clearAndSetSize(int newSize)
	{
		if (this.size != newSize)
		{
			if (this.size < newSize)
			{
				// the old buffer will automatically be garbage collected when no longer in use
				// (hopefully at a relatively quick time to prevent too much native memory floating around)
				this.buffer = ByteBuffer.allocateDirect(newSize);
				this.buffer.order(ByteOrder.nativeOrder()); // we need native ordering for GL/Vulkan
			}
			
			this.bufferSlice = this.buffer.duplicate();
			this.bufferSlice.limit(this.buffer.capacity());
			this.bufferSlice.position(0);
			this.bufferSlice.limit(newSize);
			this.bufferSlice.order(ByteOrder.nativeOrder());
			
			this.size = newSize;
		}
		
		buffer.rewind();
		buffer.limit(this.size);
		
		this.bufferSlice.rewind();
		this.bufferSlice.limit(this.size);
	}
	
	//endregion
	
	
	
}
