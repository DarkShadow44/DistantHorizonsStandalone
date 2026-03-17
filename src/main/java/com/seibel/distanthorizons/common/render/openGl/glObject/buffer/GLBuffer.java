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

package com.seibel.distanthorizons.common.render.openGl.glObject.buffer;

import com.seibel.distanthorizons.api.enums.config.EDhApiGpuUploadMethod;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLProxy;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLState;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class GLBuffer implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logRendererGLEventToFile)
			.chatLevelConfig(Config.Common.Logging.logRendererGLEventToChat)
			.build();
	
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
	
	
	public static final double BUFFER_EXPANSION_MULTIPLIER = 1.3;
	public static final double BUFFER_SHRINK_TRIGGER = BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER;
	/** the number of active buffers, can be used for debugging */
	public static AtomicInteger bufferCount = new AtomicInteger(0);
	
	private static final int PHANTOM_REF_CHECK_TIME_IN_MS = 5 * 1000;
	private static final ConcurrentHashMap<PhantomReference<? extends GLBuffer>, Integer> PHANTOM_TO_BUFFER_ID = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Integer, PhantomReference<? extends GLBuffer>> BUFFER_ID_TO_PHANTOM = new ConcurrentHashMap<>();
	private static final ReferenceQueue<GLBuffer> PHANTOM_REFERENCE_QUEUE = new ReferenceQueue<>();
	private static final ThreadPoolExecutor CLEANUP_THREAD = ThreadUtil.makeSingleDaemonThreadPool("GLBuffer Cleanup");
	
	
	protected int id;
	public final int getId() { return this.id; }
	protected int size = 0;
	public int getSize() { return this.size; }
	protected boolean bufferStorage;
	public final boolean isBufferStorage() { return this.bufferStorage; }
	protected boolean isMapped = false;
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	static { CLEANUP_THREAD.execute(() -> runPhantomReferenceCleanupLoop()); }
	
	public GLBuffer(boolean isBufferStorage) { this.destroyOldAndCreate(isBufferStorage); }
	
	//endregion
	
	
	
	//=========//
	// methods //
	//=========//
	//region
	
	// Should be override by subclasses
	public int getBufferBindingTarget() { return GL32.GL_COPY_READ_BUFFER; }
	
	public void bind() { GL32.glBindBuffer(this.getBufferBindingTarget(), this.id); }
	public void unbind() { GL32.glBindBuffer(this.getBufferBindingTarget(), 0); }
	
	//endregion
	
	
	
	//====================//
	// create and destroy //
	//====================//
	//region
	
	protected void destroyOldAndCreate(boolean asBufferStorage)
	{
		if (!GLProxy.runningOnRenderThread())
		{
			LodUtil.assertNotReach("Thread ["+Thread.currentThread()+"] tried to create a GLBuffer outside the MC render thread.");
		}
		
		// destroy the old buffer if one is present
		if (this.id != 0)
		{
			destroyBufferIdNow(this.id);
		}
		
		this.id = GLMC.glGenBuffers();
		this.bufferStorage = asBufferStorage;
		bufferCount.getAndIncrement();
		
		PhantomReference<GLBuffer> phantom = new PhantomReference<>(this, PHANTOM_REFERENCE_QUEUE);
		PHANTOM_TO_BUFFER_ID.put(phantom, this.id);
		BUFFER_ID_TO_PHANTOM.put(this.id, phantom);
		
	}
	
	protected void destroyAsync()
	{
		if (this.id == 0)
		{
			// the buffer has already been closed
			return;
		}
		
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("GLBuffer destroyAsync", () -> { destroyBufferIdNow(this.id); });
		
		this.id = 0;
		this.size = 0;
	}
	private static void destroyBufferIdNow(int id)
	{
		// only delete valid buffers
		if (id == 0)
		{
			return;
		}
		
		// remove and clear the phantom reference if present
		if (BUFFER_ID_TO_PHANTOM.containsKey(id))
		{
			Reference<? extends GLBuffer> phantom = BUFFER_ID_TO_PHANTOM.get(id);
			
			// if we are manually closing this buffer, we don't want the phantom reference to accidentally close it again
			// this can cause a race condition were we accidentally delete an in-use buffer and cause NVIDIA
			// to throw an EXCEPTION_ACCESS_VIOLATION when we attempt to render it
			phantom.clear();
			
			PHANTOM_TO_BUFFER_ID.remove(phantom);
			BUFFER_ID_TO_PHANTOM.remove(id);
		}
		
		// destroy the buffer if it exists,
		// the buffer may not exist if the destroy method is called twice
		if (GL32.glIsBuffer(id))
		{
			GLMC.glDeleteBuffers(id);
			bufferCount.decrementAndGet();
			
			if (Config.Client.Advanced.Debugging.logBufferGarbageCollection.get())
			{
				LOGGER.info("destroyed buffer [" + id + "], remaining: [" + BUFFER_ID_TO_PHANTOM.size() + "]");
			}
		}
	}
	
	//endregion
	
	
	
	//==================//
	// buffer uploading //
	//==================//
	//region
	
	/** 
	 * Assumes the GL Context is already bound. <br> 
	 * Will create the VBO if one exist.
	 */
	public void uploadBuffer(ByteBuffer bb, EDhApiGpuUploadMethod uploadMethod, int maxExpansionSize, int bufferHint)
	{
		LodUtil.assertTrue(!uploadMethod.useEarlyMapping, "UploadMethod signal that this should use Mapping instead of uploadBuffer!");
		int bbSize = bb.limit() - bb.position();
		if (bbSize > maxExpansionSize) 
		{ 
			LodUtil.assertNotReach("maxExpansionSize is [" + maxExpansionSize + "] but buffer size is [" + bbSize + "]!"); 
		}
		
		// Don't upload an empty buffer
		if (bbSize == 0)
		{
			return;
		}
		
		
		
		// GLState to prevent issues with corrupting the global GL state.
		// This can happen especially on legacy GL for MC 1.16.5, creating a black screen.
		// Maybe this will also fix random crashing on Mac?
		try(GLState state = new GLState())
		{
			// make sure the buffer is ready for uploading
			this.createOrChangeBufferTypeForUpload(uploadMethod);
			
			switch (uploadMethod)
			{
				case AUTO:
					LodUtil.assertNotReach("GpuUploadMethod AUTO must be resolved before call to uploadBuffer()!");
				case BUFFER_STORAGE:
					this.uploadBufferStorage(bb);
					break;
				case DATA:
					this.uploadBufferData(bb, bufferHint);
					break;
				case SUB_DATA:
					this.uploadSubData(bb, maxExpansionSize, bufferHint);
					break;
				default:
					LodUtil.assertNotReach("Unknown GpuUploadMethod!");
			}
		}
	}
	/** Requires the buffer to be bound */
	protected void uploadBufferStorage(ByteBuffer bb)
	{
		LodUtil.assertTrue(this.bufferStorage, "Buffer is not bufferStorage but its trying to use bufferStorage upload method!");
		
		int bbSize = bb.limit() - bb.position();
		this.destroyOldAndCreate(true);
		this.bind();
		GL44.glBufferStorage(this.getBufferBindingTarget(), bb, 0);
		this.size = bbSize;
	}
	/** Requires the buffer to be bound */
	protected void uploadBufferData(ByteBuffer bb, int bufferDataHint)
	{
		LodUtil.assertTrue(!this.bufferStorage, "Buffer is bufferStorage but its trying to use bufferData upload method!");
		
		int bbSize = bb.limit() - bb.position();
		GL32.glBufferData(this.getBufferBindingTarget(), bb, bufferDataHint);
		this.size = bbSize;
	}
	/** Requires the buffer to be bound */
	protected void uploadSubData(ByteBuffer bb, int maxExpansionSize, int bufferDataHint)
	{
		LodUtil.assertTrue(!this.bufferStorage, "Buffer is bufferStorage but its trying to use subData upload method!");
		
		int bbSize = bb.limit() - bb.position();
		if (this.size < bbSize || this.size > bbSize * BUFFER_SHRINK_TRIGGER)
		{
			int newSize = (int) (bbSize * BUFFER_EXPANSION_MULTIPLIER);
			if (newSize > maxExpansionSize) newSize = maxExpansionSize;
			GL32.glBufferData(this.getBufferBindingTarget(), newSize, bufferDataHint);
			this.size = newSize;
		}
		GL32.glBufferSubData(this.getBufferBindingTarget(), 0, bb);
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public void close() { this.destroyAsync(); }
	
	@Override
	public String toString()
	{
		return (this.bufferStorage ? "" : "Static-") + this.getClass().getSimpleName() +
				"[id:" + this.id + ",size:" + this.size + (this.isMapped ? ",MAPPED" : "") + "]";
	}
	
	//endregion
	
	
	
	//================//
	// helper methods //
	//================//
	//region
	
	/** 
	 * Makes sure the buffer exists and is of the correct format
	 * before uploading.
	 */
	private void createOrChangeBufferTypeForUpload(EDhApiGpuUploadMethod uploadMethod)
	{
		// create/change the buffer type if necessary
		if (uploadMethod.useBufferStorage != this.bufferStorage)
		{
			// recreate if the buffer storage type changed
			this.bind();
			destroyBufferIdNow(this.id);
			this.destroyOldAndCreate(uploadMethod.useBufferStorage);
			this.bind();
		}
		else
		{
			// Prevent uploading to the null buffer (ID 0).
			// This can happen if the buffer was deleted previously.
			if (this.id == 0)
			{
				this.destroyOldAndCreate(this.bufferStorage);
			}
			
			this.bind();
		}
	}
	
	//endregion
	
	
	
	//================//
	// static cleanup //
	//================//
	//region
	
	private static void runPhantomReferenceCleanupLoop()
	{
		while (true)
		{
			try
			{
				try
				{
					Thread.sleep(PHANTOM_REF_CHECK_TIME_IN_MS);
				}
				catch (InterruptedException ignore) { }
				
				
				Reference<? extends GLBuffer> phantomRef = PHANTOM_REFERENCE_QUEUE.poll();
				while (phantomRef != null)
				{
					// destroy the buffer if it hasn't been cleared yet
					if (PHANTOM_TO_BUFFER_ID.containsKey(phantomRef))
					{
						int id = PHANTOM_TO_BUFFER_ID.get(phantomRef);
						RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("GLBuffer phantom destroy", () -> { destroyBufferIdNow(id); });
						//LOGGER.warn("Buffer Phantom collected, ID: ["+id+"]");
					}
					
					phantomRef = PHANTOM_REFERENCE_QUEUE.poll();
				}
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected error in buffer cleanup thread: [" + e.getMessage() + "].", e);
			}
		}
	}
	
	//endregion
	
	
	
}
