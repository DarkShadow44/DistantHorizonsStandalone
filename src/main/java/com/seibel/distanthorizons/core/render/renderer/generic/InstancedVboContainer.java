package com.seibel.distanthorizons.core.render.renderer.generic;

import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import org.lwjgl.opengl.GL32;

import java.awt.*;
import java.util.List;

/**
 * For use by {@link RenderableBoxGroup} 
 * 
 * @see RenderableBoxGroup
 */
public class InstancedVboContainer implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftGLWrapper GLMC = SingletonInjector.INSTANCE.get(IMinecraftGLWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	
	
	public int chunkPos = 0;
	public int subChunkPos = 0;
	public int scale = 0;
	public int color = 0;
	public int material = 0;
	
	public int[] chunkPosData = new int[0];
	public float[] subChunkPosData = new float[0];
	public float[] scalingData = new float[0];
	public float[] colorData = new float[0];
	public int[] materialData = new int[0];
	
	public int uploadedBoxCount = 0;
	
	public EState state = EState.NEW;
	
	
	
	//===========================//
	// render building/uploading //
	//===========================//
	//region
	
	/** needs to be done on the render thread */
	public void tryRunRenderThreadSetup()
	{
		if (this.chunkPos == 0)
		{
			this.chunkPos = GLMC.glGenBuffers();
			this.subChunkPos = GLMC.glGenBuffers();
			this.scale = GLMC.glGenBuffers();
			this.color = GLMC.glGenBuffers();
			this.material = GLMC.glGenBuffers();
		}
	}
	
	public void updateVertexData(List<DhApiRenderableBox> uploadBoxList)
	{
		int boxCount = uploadBoxList.size();
		
		
		// recreate the data arrays if their size is different
		if (this.uploadedBoxCount != boxCount)
		{
			this.uploadedBoxCount = boxCount;
			
			this.chunkPosData = new int[boxCount * 3]; // 3 elements XYZ
			this.subChunkPosData = new float[boxCount * 3]; // 3 elements XYZ
			this.scalingData = new float[boxCount * 3]; // 3 elements XYZ
			
			this.colorData = new float[boxCount * 4]; // 4 elements, RGBA
			this.materialData = new int[boxCount];
		}
		
		
		// transformation / scaling //
		for (int i = 0; i < boxCount; i++)
		{
			DhApiRenderableBox box = uploadBoxList.get(i);
			
			int dataIndex = i * 3;
			
			this.chunkPosData[dataIndex] = LodUtil.getChunkPosFromDouble(box.minPos.x);
			this.chunkPosData[dataIndex + 1] = LodUtil.getChunkPosFromDouble(box.minPos.y);
			this.chunkPosData[dataIndex + 2] = LodUtil.getChunkPosFromDouble(box.minPos.z);
			
			this.subChunkPosData[dataIndex] = LodUtil.getSubChunkPosFromDouble(box.minPos.x);
			this.subChunkPosData[dataIndex + 1] = LodUtil.getSubChunkPosFromDouble(box.minPos.y);
			this.subChunkPosData[dataIndex + 2] = LodUtil.getSubChunkPosFromDouble(box.minPos.z);
			
			this.scalingData[dataIndex] = (float) (box.maxPos.x - box.minPos.x);
			this.scalingData[dataIndex + 1] = (float) (box.maxPos.y - box.minPos.y);
			this.scalingData[dataIndex + 2] = (float) (box.maxPos.z - box.minPos.z);
		}
		
		
		// colors/materials //
		for (int i = 0; i < boxCount; i++)
		{
			DhApiRenderableBox box = uploadBoxList.get(i);
			Color color = box.color;
			int colorIndex = i * 4;
			this.colorData[colorIndex] = color.getRed() / 255.0f;
			this.colorData[colorIndex + 1] = color.getGreen() / 255.0f;
			this.colorData[colorIndex + 2] = color.getBlue() / 255.0f;
			this.colorData[colorIndex + 3] = color.getAlpha() / 255.0f;
			
			this.materialData[i] = box.material;
		}
		
		this.state = InstancedVboContainer.EState.READY_TO_UPLOAD;
	}
	
	public void uploadDataToGpu()
	{
		// Upload transformation matrices
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, this.chunkPos);
		GL32.glBufferData(GL32.GL_ARRAY_BUFFER, this.chunkPosData, GL32.GL_DYNAMIC_DRAW);
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, this.subChunkPos);
		GL32.glBufferData(GL32.GL_ARRAY_BUFFER, this.subChunkPosData, GL32.GL_DYNAMIC_DRAW);
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, this.scale);
		GL32.glBufferData(GL32.GL_ARRAY_BUFFER, this.scalingData, GL32.GL_DYNAMIC_DRAW);
		
		// Upload colors
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, this.color);
		GL32.glBufferData(GL32.GL_ARRAY_BUFFER, this.colorData, GL32.GL_DYNAMIC_DRAW);
		
		// Upload materials
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, this.material);
		GL32.glBufferData(GL32.GL_ARRAY_BUFFER, this.materialData, GL32.GL_DYNAMIC_DRAW);
		
		this.state = EState.RENDER;
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public void close()
	{
		tryDeleteBuffer(this.chunkPos);
		tryDeleteBuffer(this.subChunkPos);
		tryDeleteBuffer(this.scale);
		tryDeleteBuffer(this.color);
		tryDeleteBuffer(this.material);
	}
	private static void tryDeleteBuffer(int bufferId)
	{
		// usually unnecessary, but just in case
		if (bufferId != 0)
		{
			GLMC.glDeleteBuffers(bufferId);
		}
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	public enum EState
	{
		NEW,
		UPDATING_DATA,
		READY_TO_UPLOAD,
		RENDER,
		
		ERROR,
	}
	
	//endregion
	
	
	
}
