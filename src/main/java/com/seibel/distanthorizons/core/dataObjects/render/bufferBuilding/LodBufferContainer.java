/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding;

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.ILodContainerUniformBufferWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhTerrainRenderer;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.IVertexBufferWrapper;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Java representation of one or more OpenGL buffers for rendering.
 *
 * @see ColumnRenderBufferBuilder
 */
public class LodBufferContainer implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
	
	
	/** the position closest to minimum X/Z infinity and the level's lowest Y */
	public final DhBlockPos minCornerBlockPos;
	public final long pos;
	
	public boolean buffersUploaded = false;
	
	public IVertexBufferWrapper[] vbos;
	public IVertexBufferWrapper[] vbosTransparent;
	
	public ILodContainerUniformBufferWrapper uniformContainer = WRAPPER_FACTORY.createLodContainerUniformWrapper();
	
	private final AtomicReference<CompletableFuture<LodBufferContainer>> uploadFutureRef = new AtomicReference<>(null);
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public LodBufferContainer(long pos, DhBlockPos minCornerBlockPos)
	{
		this.pos = pos;
		this.minCornerBlockPos = minCornerBlockPos;
		this.vbos = new IVertexBufferWrapper[0];
		this.vbosTransparent = new IVertexBufferWrapper[0];
		
		this.uniformContainer.createUniformData(this);
	}
	
	//endregion
	
	
	
	//==================//
	// buffer uploading //
	//==================//
	//region
	
	/** Should be run on a DH thread. */
	public synchronized CompletableFuture<LodBufferContainer> makeAndUploadBuffersAsync(LodQuadBuilder builder)
	{
		// separate variable to prevent race condition when checking null
		CompletableFuture<LodBufferContainer> oldFuture = this.uploadFutureRef.get();
		if (oldFuture != null)
		{
			// upload already in process
			return oldFuture;
		}
		
		// new upload needed
		CompletableFuture<LodBufferContainer> future = new CompletableFuture<>();
		future.handle((lodBufferContainer, throwable) -> 
		{
			if (!this.uploadFutureRef.compareAndSet(future, null))
			{
				LOGGER.warn("upload future ref changed for pos ["+DhSectionPos.toString(this.pos)+"].");
			}
			
			return null;
		});
		
		if (!this.uploadFutureRef.compareAndSet(null, future))
		{
			oldFuture = this.uploadFutureRef.get();
			LodUtil.assertTrue(oldFuture != null, "Concurrency error");
			return oldFuture;
		}
		
		
		
		// make the buffers
		ArrayList<ByteBuffer> opaqueBuffers = builder.makeOpaqueVertexBuffers();
		ArrayList<ByteBuffer> transparentBuffers = builder.makeTransparentVertexBuffers();
		
		this.vbos = resizeBufferArray(this.vbos, opaqueBuffers.size());
		this.vbosTransparent = resizeBufferArray(this.vbosTransparent, transparentBuffers.size());
		
		
		// upload on MC's render thread
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("LodBufferContainer Upload", () ->
		{
			try
			{
				// skip this event if requested
				if (Thread.interrupted() 
					|| future.isCancelled())
				{
					throw new InterruptedException();
				}
				
				// upload on the render thread
				uploadBuffers(this.vbos, opaqueBuffers);
				uploadBuffers(this.vbosTransparent, transparentBuffers);
				this.buffersUploaded = true;
				
				// success
				future.complete(this);
			}
			catch (InterruptedException ignore) 
			{
				future.complete(this);
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected issue uploading buffer ["+this.minCornerBlockPos +"], error: ["+e.getMessage()+"].", e);
				
				future.completeExceptionally(e);
			}
			finally
			{
				// all the buffers must be manually freed to prevent memory leaks
				
				for (ByteBuffer buffer : opaqueBuffers)
				{
					MemoryUtil.memFree(buffer);
				}
				
				for (ByteBuffer buffer : transparentBuffers)
				{
					MemoryUtil.memFree(buffer);
				}
			}
		});
		
		return future;
	}
	private static IVertexBufferWrapper[] resizeBufferArray(IVertexBufferWrapper[] vbos, int newSize)
	{
		if (vbos.length == newSize)
		{
			return vbos;
		}
		
		IVertexBufferWrapper[] newVbos = new IVertexBufferWrapper[newSize];
		System.arraycopy(vbos, 0, newVbos, 0, Math.min(vbos.length, newSize));
		if (newSize < vbos.length)
		{
			for (int i = newSize; i < vbos.length; i++)
			{
				if (vbos[i] != null)
				{
					vbos[i].close();
				}
			}
		}
		return newVbos;
	}
	private static void uploadBuffers(IVertexBufferWrapper[] vbos, ArrayList<ByteBuffer> byteBuffers) throws InterruptedException
	{
		int vboIndex = 0;
		for (int i = 0; i < byteBuffers.size(); i++)
		{
			if (vboIndex >= vbos.length)
			{
				throw new RuntimeException("Too many vertex buffers!!");
			}
			
			
			// get or create the VBO
			if (vbos[vboIndex] == null)
			{
				vbos[vboIndex] = SingletonInjector.INSTANCE.get(IWrapperFactory.class).createVboWrapper("distantHorizons:McLodRenderer");
			}
			IVertexBufferWrapper vbo = vbos[vboIndex];
			
			ByteBuffer buffer = byteBuffers.get(i);
			int size = buffer.limit() - buffer.position();
			int vertexCount = size / LodQuadBuilder.BYTES_PER_VERTEX;
			
			try
			{
				vbo.upload(buffer, vertexCount);
			}
			catch (Exception e)
			{
				vbos[vboIndex] = null;
				vbo.close();
				LOGGER.error("Failed to upload buffer. Error: ["+e.getMessage()+"].", e);
			}
			
			vboIndex++;
		}
		
		if (vboIndex < vbos.length)
		{
			throw new RuntimeException("Too few vertex buffers!!");
		}
	}
	
	//endregion
	
	
	
	//================//
	// helper methods //
	//================//
	//region
	
	/** can be used when debugging */
	public boolean hasNonNullVbos() { return this.vbos != null || this.vbosTransparent != null; }
	
	/** can be used when debugging */
	public int vboBufferCount() 
	{
		int count = 0;
		
		if (this.vbos != null)
		{
			count += this.vbos.length;
		}
		
		if (this.vbosTransparent != null)
		{
			count += this.vbosTransparent.length;
		}
		
		return count;
	}
	
	public boolean uploadInProgress() { return this.uploadFutureRef.get() != null; }
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	/**
	 * This method is called when object is no longer in use.
	 * Called either after uploadBuffers() returned false (On buffer Upload
	 * thread), or by others when the object is not being used. (not in build,
	 * upload, or render state). 
	 */
	@Override
	public void close()
	{
		this.buffersUploaded = false;
		
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("LodBufferContainer Close", () -> 
		{
			for (IVertexBufferWrapper buffer : this.vbos)
			{
				if (buffer != null)
				{
					buffer.close();
				}
			}
			
			for (IVertexBufferWrapper buffer : this.vbosTransparent)
			{
				if (buffer != null)
				{
					buffer.close();
				}
			}
			
			this.uniformContainer.close();
		});
	}
	
	//endregion
	
	
	
}
