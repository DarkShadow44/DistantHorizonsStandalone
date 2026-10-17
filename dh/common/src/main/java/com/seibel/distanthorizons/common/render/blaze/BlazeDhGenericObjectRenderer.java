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

package com.seibel.distanthorizons.common.render.blaze;

#if MC_VER <= MC_1_21_10
public class BlazeDhGenericObjectRenderer {}

#else

#if MC_VER <= MC_26_2_0
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
#else
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.vertex.VertexFormat;
#endif

import com.seibel.distanthorizons.api.enums.config.EDhApiDepthDirection;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeGenericObjectRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeGenericRenderCleanupEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeGenericRenderSetupEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import com.seibel.distanthorizons.common.render.blaze.objects.BlazeGenericObjectVertexContainer;
import com.seibel.distanthorizons.common.render.blaze.util.BlazeDhVertexFormatUtil;
import com.seibel.distanthorizons.common.render.blaze.wrappers.BlazeVertexFormatBuilder;
import com.seibel.distanthorizons.common.render.blaze.wrappers.RenderPassWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.RenderPipelineBuilderWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.texture.BlazeTextureViewWrapper;
import com.seibel.distanthorizons.common.render.blaze.wrappers.uniform.BlazeUniformBufferWrapper;
import com.seibel.distanthorizons.common.wrappers.misc.LightMapWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderParam;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiGenericObjectVertexBufferContainer;
import com.seibel.distanthorizons.core.render.renderer.RenderableBoxGroup;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.math.DhMat4f;
import com.seibel.distanthorizons.core.util.math.DhVec3d;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.IDhApiGenericRenderer;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;

import java.util.Collection;

/**
 * Handles rendering generic groups of {@link DhApiRenderableBox}.
 * 
 * @see IDhApiCustomRenderRegister
 * @see DhApiRenderableBox
 */
public class BlazeDhGenericObjectRenderer implements IDhApiGenericRenderer
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	public static final BlazeDhGenericObjectRenderer INSTANCE = new BlazeDhGenericObjectRenderer();
	
	
	private static final GpuDevice GPU_DEVICE = RenderSystem.getDevice();
	private static final CommandEncoder COMMAND_ENCODER = GPU_DEVICE.createCommandEncoder();
	
	private static final DhApiRenderableBoxGroupShading DEFAULT_SHADING = DhApiRenderableBoxGroupShading.getUnshaded();
	
	private static final DhApiBeforeGenericObjectRenderEvent.EventParam EVENT_PARAM = new DhApiBeforeGenericObjectRenderEvent.EventParam();
	
	
	
	
	// rendering setup
	private boolean init = false;
	
	private RenderPipeline pipeline;
	
	private final BlazeUniformBufferWrapper vertUniformBufferWrapper = new BlazeUniformBufferWrapper("vertUniformBlock");
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private BlazeDhGenericObjectRenderer() { }
	
	public void init(DhApiRenderParam renderEventParam)
	{
		if (this.init)
		{
			return;
		}
		this.init = true;
		
		this.createPipelines(renderEventParam);
	}
	private void createPipelines(DhApiRenderParam renderEventParam)
	{
		RenderPipelineBuilderWrapper pipelineBuilder = new RenderPipelineBuilderWrapper();
		{
			pipelineBuilder.withFaceCulling(true);
			pipelineBuilder.withDepthWrite(true);
			if (renderEventParam.renderDefinition.getDepthDirection() == EDhApiDepthDirection.FORWARD_Z)
			{
				pipelineBuilder.withDepthTest(RenderPipelineBuilderWrapper.EDhDepthTest.LESS);
			}
			else
			{
				pipelineBuilder.withDepthTest(RenderPipelineBuilderWrapper.EDhDepthTest.GREATER);
			}
			pipelineBuilder.withBlend(BlendFunction.TRANSLUCENT); // TRANSLUCENT = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
			pipelineBuilder.withColorWrite(true);
			pipelineBuilder.withPolygonMode(RenderPipelineBuilderWrapper.EDhPolygonMode.FILL);
			pipelineBuilder.withName("generic_objects");
			
			pipelineBuilder.withVertexShader("generic/blaze/vert");
			pipelineBuilder.withFragmentShader("generic/blaze/frag");
			
			pipelineBuilder.withSampler("uLightMap");
			
			pipelineBuilder.withUniformBuffer("vertUniformBlock");
			
			VertexFormat vertexFormat = new BlazeVertexFormatBuilder()
				.add("vPosition", BlazeDhVertexFormatUtil.FLOAT_XYZ_POS)
				.add("aColor", BlazeDhVertexFormatUtil.RGBA_UBYTE_COLOR)
				.add("aMaterial", BlazeDhVertexFormatUtil.IRIS_MATERIAL)
				
				.add("paddingOne", BlazeDhVertexFormatUtil.BYTE_PAD)
				.add("paddingTwo", BlazeDhVertexFormatUtil.BYTE_PAD)
				.add("paddingThree", BlazeDhVertexFormatUtil.BYTE_PAD)
				.build();
			pipelineBuilder.withVertexFormat(vertexFormat);
			pipelineBuilder.withVertexMode(RenderPipelineBuilderWrapper.EDhVertexMode.TRIANGLES);
		}
		this.pipeline = pipelineBuilder.build();
	}
	
	//endregion
	
	
	
	//===========//
	// rendering //
	//===========//
	//region
	
	/**
	 * @param renderingWithSsao 
	 *      if true that means this render call is happening before the SSAO pass
     *      and any objects rendered in this pass will have SSAO applied to them.
	 */
	@Override
	public void render(
		DhApiRenderParam apiRenderEventParam, 
		IDhApiCustomRenderRegister renderRegister, boolean renderingWithSsao)
	{
		RenderParam renderEventParam = (RenderParam)apiRenderEventParam;
		IProfilerWrapper profiler = renderEventParam.profiler;
		
		try (IProfilerWrapper.IProfileBlock generic_profile = profiler.push("setup"))
		{
			
			
			//==============//
			// render setup //
			//==============//
			//#region
			
			this.init(apiRenderEventParam);
			
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeGenericRenderSetupEvent.class, renderEventParam.apiCopy);
			
			DhVec3d camPos = MC_RENDER.getCameraExactPosition();
			
			//#endregion
			
			if (BlazeDhMetaRenderer.INSTANCE.dhColorTextureWrapper.isEmpty()
				|| BlazeDhMetaRenderer.INSTANCE.dhDepthTextureWrapper.isEmpty())
			{
				return;
			}
			
			
			
			//===========//
			// rendering //
			//===========//
			//#region
			
			Collection<? extends IDhApiRenderableBoxGroup> boxList = renderRegister.getRenderBoxList();
			for (IDhApiRenderableBoxGroup apiBoxGroup : boxList)
			{
				RenderableBoxGroup boxGroup = (RenderableBoxGroup)apiBoxGroup;
				
				// validation //
				
				// shouldn't happen, but just in case
				if (boxGroup == null)
				{
					continue;
				}
				
				// skip boxes that shouldn't render this pass
				if (boxGroup.ssaoEnabled != renderingWithSsao)
				{
					continue;
				}
				
				profiler.popPush("render prep");
				boxGroup.preRender(renderEventParam); // called even if the group is inactive, so the group can be activate if desired
				
				// ignore inactive groups
				if (!boxGroup.active)
				{
					continue;
				}
				
				// allow API users to cancel this object's rendering
				EVENT_PARAM.update(renderEventParam, boxGroup);
				boolean cancelRendering = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeGenericObjectRenderEvent.class, EVENT_PARAM);
				if (cancelRendering)
				{
					continue;
				}
				
				// update instanced data if needed
				{
					boxGroup.tryUpdateInstancedDataAsync(apiRenderEventParam);
					
					// skip groups that haven't been uploaded yet
					if (boxGroup.vertexBufferContainer.getState() != IDhApiGenericObjectVertexBufferContainer.EState.RENDER)
					{
						continue;
					}
				}
				
				
				DhApiRenderableBoxGroupShading shading = boxGroup.shading;
				if (shading == null)
				{
					shading = DEFAULT_SHADING;
				}
				
				// uniforms
				{
					// create data //
					
					DhMat4f projectionMvmMatrix = new DhMat4f(renderEventParam.dhProjectionMatrix);
					projectionMvmMatrix.multiply(renderEventParam.dhModelViewMatrix);
					
					
					// upload data //
					
					this.vertUniformBufferWrapper
						.putVec3i(
							LodUtil.getChunkPosFromDouble(boxGroup.getOriginBlockPos().x),
							LodUtil.getChunkPosFromDouble(boxGroup.getOriginBlockPos().y),
							LodUtil.getChunkPosFromDouble(boxGroup.getOriginBlockPos().z)
						) // uOffsetChunk
						.putVec3f(
							LodUtil.getSubChunkPosFromDouble(boxGroup.getOriginBlockPos().x),
							LodUtil.getSubChunkPosFromDouble(boxGroup.getOriginBlockPos().y),
							LodUtil.getSubChunkPosFromDouble(boxGroup.getOriginBlockPos().z)
						) // uOffsetSubChunk
						.putVec3i(
							LodUtil.getChunkPosFromDouble(camPos.x),
							LodUtil.getChunkPosFromDouble(camPos.y),
							LodUtil.getChunkPosFromDouble(camPos.z)
						) // uCameraPosChunk
						.putVec3f(
							LodUtil.getSubChunkPosFromDouble(camPos.x),
							LodUtil.getSubChunkPosFromDouble(camPos.y),
							LodUtil.getSubChunkPosFromDouble(camPos.z)
						) // uCameraPosSubChunk
						
						.putMat4f(projectionMvmMatrix) // uProjectionMvm
						.putInt(boxGroup.getSkyLight()) // uSkyLight
						.putInt(boxGroup.getBlockLight()) // uBlockLight
						
						.putFloat(shading.north)
						.putFloat(shading.south)
						.putFloat(shading.east)
						.putFloat(shading.west)
						.putFloat(shading.top)
						.putFloat(shading.bottom)
						
						.finishAndUpload()
					;
				}
				
				
				
				// render //
				
				profiler.popPush("rendering");
				try (IProfilerWrapper.IProfileBlock namespace_profile = profiler.push(boxGroup.getResourceLocationNamespace());
					IProfilerWrapper.IProfileBlock location_profile = profiler.push(boxGroup.getResourceLocationPath()))
				{
					this.renderBoxGroupInstanced(renderEventParam, boxGroup);
				}
				
				boxGroup.postRender(renderEventParam);
			}
			
			//#endregion
			
			
			
			//==========//
			// clean up //
			//==========//
			//region
			
			profiler.popPush("cleanup");
			
			ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeGenericRenderCleanupEvent.class, renderEventParam);
			
			//endregion
		}
	}
	private String getRenderPassName() { return "distantHorizons:GenericObjectRenderer"; }
	
	//endregion
	
	
	
	//=====================//
	// instanced rendering //
	//=====================//
	//region
	
	private void renderBoxGroupInstanced(
		RenderParam renderEventParam, 
		RenderableBoxGroup boxGroup)
	{
		try (RenderPassWrapper renderPassWrapper = new RenderPassWrapper(
			this::getRenderPassName,
			BlazeDhMetaRenderer.INSTANCE.dhColorTextureWrapper,
			BlazeDhMetaRenderer.INSTANCE.dhDepthTextureWrapper))
		{
			
			// update instance data //
			
			BlazeGenericObjectVertexContainer container = (BlazeGenericObjectVertexContainer) boxGroup.vertexBufferContainer;
			
			LightMapWrapper lightMapWrapper = (LightMapWrapper) renderEventParam.lightmap;
			BlazeTextureViewWrapper lightmapTextureViewWrapper = lightMapWrapper.getTextureViewWrapper();
			renderPassWrapper.bindTexture("uLightMap", lightmapTextureViewWrapper);
			
			
			
			// Bind instance data //
			
			
			renderPassWrapper.setUniform("vertUniformBlock", this.vertUniformBufferWrapper);
			
			// set pipeline
			renderPassWrapper.setPipeline(this.pipeline);
			renderPassWrapper.setIndexBuffer(container.indexGpuBuffer);
			
			renderPassWrapper.setVertexBuffer(container.vboGpuBuffer);
			
			// Draw instanced
			if (container.uploadedBoxCount > 0)
			{
				// 36 = 6 faces * 6 verticies per face
				renderPassWrapper.drawIndexed(container.uploadedBoxCount * 36);
			}
		}
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public void close()
	{
		// close is called outside the render thread and buffer closing must be done on the render thread
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("Generic Obj Cleanup", () ->
		{
			if (this.vertUniformBufferWrapper != null)
			{
				this.vertUniformBufferWrapper.close();
			}
		});
	}
	
	//endregion
	
	
	
}
#endif