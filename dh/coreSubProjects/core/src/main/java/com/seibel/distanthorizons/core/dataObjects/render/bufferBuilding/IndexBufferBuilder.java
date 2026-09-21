package com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding;

import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList.PhantomArrayListCheckout;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IndexBufferBuilder
{
	
	
	//==========//
	// building //
	//==========//
	//region
	
	public static ByteBuffer populateBuffer(PhantomArrayListCheckout checkout, int checkoutIndex, int quadCount)
	{
		int indexCount = quadCount * 6; // 2 triangles per quad
		int byteSize = indexCount * Integer.BYTES;
		
		ByteBuffer buffer = checkout.getByteBuffer(checkoutIndex, byteSize);
		buildBufferInt(quadCount, buffer);
		
		return buffer;
	}
	private static void buildBufferByte(int quadCount, ByteBuffer buffer)
	{
		for (int i = 0; i < quadCount; i++)
		{
			int vIndex = i * 4;
			// First triangle
			buffer.put((byte) (vIndex));
			buffer.put((byte) (vIndex + 1));
			buffer.put((byte) (vIndex + 2));
			// Second triangle
			buffer.put((byte) (vIndex + 2));
			buffer.put((byte) (vIndex + 3));
			buffer.put((byte) (vIndex));
		}
		if (buffer.hasRemaining())
		{
			throw new IllegalStateException("QuadElementBuffer is not full somehow after building");
		}
		buffer.rewind();
	}
	private static void buildBufferShort(int quadCount, ByteBuffer buffer)
	{
		for (int i = 0; i < quadCount; i++)
		{
			int vIndex = i * 4;
			// First triangle
			buffer.putShort((short) (vIndex));
			buffer.putShort((short) (vIndex + 1));
			buffer.putShort((short) (vIndex + 2));
			// Second triangle
			buffer.putShort((short) (vIndex + 2));
			buffer.putShort((short) (vIndex + 3));
			buffer.putShort((short) (vIndex));
		}
		if (buffer.hasRemaining())
		{
			throw new IllegalStateException("QuadElementBuffer is not full somehow after building");
		}
		buffer.rewind();
	}
	private static void buildBufferInt(int quadCount, ByteBuffer buffer)
	{
		for (int i = 0; i < quadCount; i++)
		{
			int vIndex = i * 4;
			// First triangle
			buffer.putInt(vIndex);
			buffer.putInt(vIndex + 1);
			buffer.putInt(vIndex + 2);
			// Second triangle
			buffer.putInt(vIndex + 2);
			buffer.putInt(vIndex + 3);
			buffer.putInt(vIndex);
		}
		if (buffer.hasRemaining())
		{
			throw new IllegalStateException("QuadElementBuffer is not full somehow after building");
		}
		buffer.rewind();
	}
	
	//endregion
	
	
	
}
