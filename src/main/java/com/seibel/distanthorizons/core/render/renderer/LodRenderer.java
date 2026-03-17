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

package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiFramebuffer;
import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShaderProgram;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.*;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiTextureCreatedParam;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.render.DhApiRenderProxy;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import com.seibel.distanthorizons.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.core.render.glObject.buffer.QuadElementBuffer;
import com.seibel.distanthorizons.core.render.glObject.texture.*;
import com.seibel.distanthorizons.core.render.renderer.generic.GenericObjectRenderer;
import com.seibel.distanthorizons.core.render.renderer.shaders.*;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftGLWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractOptifineAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.coreapi.DependencyInjection.OverrideInjector;
import com.seibel.distanthorizons.core.util.math.Vec3f;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;

/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 */
public class LodRenderer
{
	public static final DhLogger LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
			.build();
	
	public static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
			.maxCountPerSecond(4)
			.build();
	
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final IMinecraftGLWrapper GLMC = SingletonInjector.INSTANCE.get(IMinecraftGLWrapper.class);
	private static final IIrisAccessor IRIS_ACCESSOR = ModAccessorInjector.INSTANCE.get(IIrisAccessor.class);
	
	public static final LodRenderer INSTANCE = new LodRenderer();
	
	
	
	// these ID's either what any render is currently using (since only one renderer can be active at a time), or just used previously
	private int activeFramebufferId = -1;
	private int activeColorTextureId = -1;
	private int activeDepthTextureId = -1;
	private int textureWidth;
	private int textureHeight;
	
	
	private IDhApiShaderProgram lodRenderProgram = null;
	public QuadElementBuffer quadIBO = null;
	private boolean renderObjectsCreated = false;
	
	// framebuffer and texture ID's for this renderer
	private IDhApiFramebuffer framebuffer;
	/** will be null if MC's framebuffer is being used since MC already has a color texture */
	@Nullable
	private DhColorTexture nullableColorTexture;
	private DHDepthTexture depthTexture;
	/** 
	 * If true the {@link LodRenderer#framebuffer} is the same as MC's.
	 * This should only be true in the case of Optifine so LODs won't be overwritten when shaders are enabled.
	 */
	private boolean usingMcFramebuffer = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	private LodRenderer() { }
	
	
	
	//===========//
	// rendering //
	//===========//
	
	/**
	 * This will draw both opaque and transparent LODs if 
	 * {@link DhApiRenderProxy#getDeferTransparentRendering()} is false,
	 * otherwise it will only render opaque LODs.
	 */
	public void render(RenderParams renderParams, IProfilerWrapper profiler)
	{  this.renderLodPass(renderParams, profiler, false);  }
	
	/**
	 * This method is designed for Iris to be able 
	 * to draw water in a deferred rendering context. 
	 * It needs to be updated with any major changes, 
	 * but shouldn't be activated as per deferWaterRendering.
	 */
	public void renderDeferred(RenderParams renderParams, IProfilerWrapper profiler)
	{ this.renderLodPass(renderParams, profiler, true); }
	
	private void renderLodPass(RenderParams renderParams, IProfilerWrapper profiler, boolean runningDeferredPass)
	{
		//====================//
		// validate rendering //
		//====================//
		
		boolean deferTransparentRendering = DhApiRenderProxy.INSTANCE.getDeferTransparentRendering();
		if (runningDeferredPass 
			&& !deferTransparentRendering)
		{
			return;
		}
		boolean firstPass = !runningDeferredPass;
		
		// RenderParams parameter validation should be done before this
		if (!renderParams.validationRun)
		{
			throw new IllegalArgumentException("Render parameters validation");
		}
		
		RenderBufferHandler renderBufferHandler = renderParams.renderBufferHandler;
		GenericObjectRenderer genericRenderer = renderParams.genericRenderer;
		ILightMapWrapper lightmap = renderParams.lightmap;
		
		
		
		//=================//
		// rendering setup //
		//=================//
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderSetupEvent.class, renderParams);
		profiler.push("LOD GL setup");
		
		if (!this.renderObjectsCreated)
		{
			boolean setupSuccess = this.createRenderObjects();
			if (!setupSuccess)
			{
				// shouldn't normally happen, but just in case
				return;
			}
			
			// only do this once, that way they can still be reverted if desired
			if (Config.Client.Advanced.Graphics.overrideVanillaGraphicsSettings.get())
			{
				MC.disableVanillaClouds();
				MC.disableVanillaChunkFadeIn();
			}
			
			this.renderObjectsCreated = true;
		}
		
		this.setGLState(renderParams, firstPass);
		
		lightmap.bind();
		this.quadIBO.bind();
		
		if (firstPass)
		{
			// we only need to sort/cull the LODs during the first frame 
			profiler.popPush("LOD build render list");
			renderBufferHandler.buildRenderList(renderParams);
		}
		
		IDhApiShaderProgram lodShaderProgram = this.lodRenderProgram;
		IDhApiShaderProgram lodShaderProgramOverride = OverrideInjector.INSTANCE.get(IDhApiShaderProgram.class);
		if (lodShaderProgramOverride != null && lodShaderProgram.overrideThisFrame())
		{
			lodShaderProgram = lodShaderProgramOverride;
		}
		
		
		
		//===========//
		// rendering //
		//===========//
		
		if (!runningDeferredPass)
		{
			//=========================//
			// opaque and non-deferred //
			// transparent rendering   //
			//=========================//
			
			// opaque LODs
			profiler.popPush("LOD Opaque");
			this.renderLodPass(lodShaderProgram, renderBufferHandler, renderParams, /*opaquePass*/ true);
			
			// custom objects with SSAO
			if (Config.Client.Advanced.Graphics.GenericRendering.enableGenericRendering.get())
			{
				profiler.popPush("Custom Objects");
				genericRenderer.render(renderParams, profiler, true);
			}
			
			// SSAO
			if (Config.Client.Advanced.Graphics.Ssao.enableSsao.get())
			{
				profiler.popPush("LOD SSAO");
				SSAORenderer.INSTANCE.render(new Mat4f(renderParams.dhProjectionMatrix), renderParams.partialTicks);
			}
			
			// custom objects without SSAO
			if (Config.Client.Advanced.Graphics.GenericRendering.enableGenericRendering.get())
			{
				profiler.popPush("Custom Objects");
				genericRenderer.render(renderParams, profiler, false);
			}
			
			// combined pass transparent rendering
			if (!deferTransparentRendering 
				&& Config.Client.Advanced.Graphics.Quality.transparency.get().transparencyEnabled)
			{
				profiler.popPush("LOD Transparent");
				this.renderLodPass(lodShaderProgram, renderBufferHandler, renderParams, /*opaquePass*/ false);
			}
			
			// far plane clip fading
			if (Config.Client.Advanced.Graphics.Quality.dhFadeFarClipPlane.get()
				// the fade shader messes with the GL state in a way Iris doesn't like,
				// so skip it if a shader is active
				&& (IRIS_ACCESSOR == null || !IRIS_ACCESSOR.isShaderPackInUse()))
			{
				profiler.popPush("Fade Far Clip Fade");
				DhFadeRenderer.INSTANCE.render(
						new Mat4f(renderParams.mcModelViewMatrix), new Mat4f(renderParams.mcProjectionMatrix),
						renderParams.partialTicks, profiler);
			}
			
			// fog
			if (Config.Client.Advanced.Graphics.Fog.enableDhFog.get())
			{
				profiler.popPush("LOD Fog");
				
				Mat4f combinedMatrix = new Mat4f(renderParams.dhProjectionMatrix);
				combinedMatrix.multiply(renderParams.dhModelViewMatrix);
				
				FogRenderer.INSTANCE.render(combinedMatrix, renderParams.partialTicks);
			}
			
			
			
			//=================//
			// debug rendering //
			//=================//
			
			if (Config.Client.Advanced.Debugging.DebugWireframe.enableRendering.get())
			{
				profiler.popPush("Debug wireframes");
				
				Mat4f combinedMatrix = new Mat4f(renderParams.dhProjectionMatrix);
				combinedMatrix.multiply(renderParams.dhModelViewMatrix);
				
				// Note: this can be very slow if a lot of boxes are being rendered 
				DebugRenderer.INSTANCE.render(combinedMatrix);
			}
			
			
			
			//===================//
			// optifine clean up //
			//===================//
			
			if (this.usingMcFramebuffer)
			{
				// If MC's framebuffer is being used the depth needs to be cleared to prevent rendering on top of MC.
				// This should only happen when Optifine shaders are being used.
				GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);
			}
			
			
			
			//=============================//
			// Apply to the MC Framebuffer //
			//=============================//
			
			boolean cancelApplyShader = ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeApplyShaderRenderEvent.class, renderParams);
			if (!cancelApplyShader)
			{
				profiler.popPush("LOD Apply");
				
				// Copy the LOD framebuffer to Minecraft's framebuffer
				DhApplyShader.INSTANCE.render(renderParams.partialTicks);
			}
		}
		else
		{
			//====================//
			// deferred rendering //
			//====================//
			
			if (Config.Client.Advanced.Graphics.Quality.transparency.get().transparencyEnabled)
			{
				profiler.popPush("LOD Transparent");
				this.renderLodPass(lodShaderProgram, renderBufferHandler, renderParams, /*opaquePass*/ false);
				
				
				if (Config.Client.Advanced.Graphics.Fog.enableDhFog.get())
				{
					profiler.popPush("LOD Fog");
					
					Mat4f combinedMatrix = new Mat4f(renderParams.dhProjectionMatrix);
					combinedMatrix.multiply(renderParams.dhModelViewMatrix);
					
					FogRenderer.INSTANCE.render(combinedMatrix, renderParams.partialTicks);
				}
			}
		}
		
		
		
		//================//
		// render cleanup //
		//================//
		
		profiler.popPush("LOD cleanup");
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderCleanupEvent.class, renderParams);
		
		lightmap.unbind();
		this.quadIBO.unbind();
		lodShaderProgram.unbind();
		
		
		// end of internal LOD profiling
		profiler.pop();
	}
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	
	private void setGLState(
			DhApiRenderParam renderEventParam,
			boolean firstPass)
	{
		//===================//
		// framebuffer setup //
		//===================//
		
		// get the active framebuffer
		IDhApiFramebuffer framebuffer = this.framebuffer;
		IDhApiFramebuffer framebufferOverride = OverrideInjector.INSTANCE.get(IDhApiFramebuffer.class);
		if (framebufferOverride != null && framebufferOverride.overrideThisFrame())
		{
			framebuffer = framebufferOverride;
		}
		this.activeFramebufferId = framebuffer.getId();
		framebuffer.bind();
		
		
		
		//==========//
		// bindings //
		//==========//
		
		// by default draw everything as triangles
		GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
		GLMC.enableFaceCulling();
		
		GLMC.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);
		GLMC.glBlendFuncSeparate(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA, GL32.GL_ONE, GL32.GL_ZERO);
		
		GL32.glDisable(GL32.GL_SCISSOR_TEST);
		
		// Enable depth test and depth mask
		GLMC.enableDepthTest();
		GLMC.glDepthFunc(GL32.GL_LESS);
		GLMC.enableDepthMask();
		
		// This is required for MC versions 1.21.5+
		// due to MC updating the lightmap by changing the viewport size
		GL32.glViewport(0, 0, this.textureWidth, this.textureHeight);
		
		this.lodRenderProgram.bind();
		
		
		
		//==========//
		// uniforms //
		//==========//
		
		IDhApiShaderProgram shaderProgramOverride = OverrideInjector.INSTANCE.get(IDhApiShaderProgram.class);
		if (shaderProgramOverride != null)
		{
			shaderProgramOverride.fillUniformData(renderEventParam);
		}
		
		this.lodRenderProgram.fillUniformData(renderEventParam);
		
		
		
		
		//===============//
		// texture setup //
		//===============//
		
		// resize the textures if needed
		if (MC_RENDER.getTargetFramebufferViewportWidth() != this.textureWidth
				|| MC_RENDER.getTargetFramebufferViewportHeight() != this.textureHeight)
		{
			// just resizing the textures doesn't work when Optifine is present,
			// so recreate the textures with the new size instead
			this.createAndBindTextures();
		}
		
		
		// set the active textures
		this.activeDepthTextureId = this.depthTexture.getTextureId();
		
		if (this.nullableColorTexture != null)
		{
			this.activeColorTextureId = this.nullableColorTexture.getTextureId();
		}
		else
		{
			// get MC's color texture 
			this.activeColorTextureId = GL32.glGetFramebufferAttachmentParameteri(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
		}
		
		
		// needs to be fired after all the textures have been created/bound
		boolean clearTextures = !ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeTextureClearEvent.class, renderEventParam);
		if (clearTextures)
		{
			GL32.glClearDepth(1.0);
			
			float[] clearColorValues = new float[4];
			GL32.glGetFloatv(GL32.GL_COLOR_CLEAR_VALUE, clearColorValues);
			GL32.glClearColor(clearColorValues[0], clearColorValues[1], clearColorValues[2], 1.0f);
			
			if (this.usingMcFramebuffer && framebufferOverride == null)
			{
				// Due to using MC/Optifine's framebuffer we need to re-bind the depth texture,
				// otherwise we'll be writing to MC/Optifine's depth texture which causes rendering issues
				framebuffer.addDepthAttachment(this.depthTexture.getTextureId(), EDhDepthBufferFormat.DEPTH32F.isCombinedStencil());
				
				
				// don't clear the color texture, that removes the sky 
				GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);
			}
			else if (firstPass)
			{
				GL32.glClear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT);
			}
		}
		
		
	}
	
	private boolean createRenderObjects()
	{
		if (this.renderObjectsCreated)
		{
			LOGGER.warn("Renderer setup called but it has already completed setup!");
			return false;
		}
		
		// GLProxy should have already been created by this point, but just in case create it now
		GLProxy.getInstance();
		
		
		
		LOGGER.info("Setting up renderer");
		this.lodRenderProgram = new DhTerrainShaderProgram();
		
		this.quadIBO = new QuadElementBuffer();
		this.quadIBO.reserve(LodBufferContainer.MAX_QUADS_PER_BUFFER);
		
		
		// create or get the frame buffer
		if (AbstractOptifineAccessor.optifinePresent())
		{
			// use MC/Optifine's default Framebuffer so shaders won't remove the LODs
			int currentFramebufferId = MC_RENDER.getTargetFramebuffer();
			this.framebuffer = new DhFramebuffer(currentFramebufferId);
			this.usingMcFramebuffer = true;
		}
		else 
		{
			// normal use case
			this.framebuffer = new DhFramebuffer();
			this.usingMcFramebuffer = false;
		}
		
		// create and bind the necessary textures
		this.createAndBindTextures();
		
		if(this.framebuffer.getStatus() != GL32.GL_FRAMEBUFFER_COMPLETE)
		{
			// This generally means something wasn't bound, IE missing either the color or depth texture
			LOGGER.warn("Framebuffer ["+this.framebuffer.getId()+"] isn't complete.");
			return false;
		}
		
		
		
		LOGGER.info("Renderer setup complete");
		return true;
	}
	
	@SuppressWarnings( "deprecation" )
	private void createAndBindTextures()
	{
		int oldWidth = this.textureWidth;
		int oldHeight = this.textureHeight;
		this.textureWidth = MC_RENDER.getTargetFramebufferViewportWidth();
		this.textureHeight = MC_RENDER.getTargetFramebufferViewportHeight();
		
		DhApiTextureCreatedParam textureCreatedParam = new DhApiTextureCreatedParam(
				oldWidth, oldHeight,
				this.textureWidth, this.textureHeight
		);
		
		
		// DhApiColorDepthTextureCreatedEvent needs to be kept around since old versions of Iris need it
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiColorDepthTextureCreatedEvent.class, new DhApiColorDepthTextureCreatedEvent.EventParam(textureCreatedParam));
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeColorDepthTextureCreatedEvent.class, textureCreatedParam);
		
		
		// also update the framebuffer override if present
		IDhApiFramebuffer framebufferOverride = OverrideInjector.INSTANCE.get(IDhApiFramebuffer.class);
		
		
		this.depthTexture = new DHDepthTexture(this.textureWidth, this.textureHeight, EDhDepthBufferFormat.DEPTH32F);
		this.framebuffer.addDepthAttachment(this.depthTexture.getTextureId(), EDhDepthBufferFormat.DEPTH32F.isCombinedStencil());
		if (framebufferOverride != null)
		{
			framebufferOverride.addDepthAttachment(this.depthTexture.getTextureId(), EDhDepthBufferFormat.DEPTH32F.isCombinedStencil());
		}
		
		// if we are using MC's frame buffer, a color texture is already present and shouldn't need to be bound
		if (!this.usingMcFramebuffer)
		{
			this.nullableColorTexture = DhColorTexture.builder().setDimensions(this.textureWidth, this.textureHeight)
					.setInternalFormat(EDhInternalTextureFormat.RGBA8)
					.setPixelType(EDhPixelType.UNSIGNED_BYTE)
					.setPixelFormat(EDhPixelFormat.RGBA)
					.build();
			
			this.framebuffer.addColorAttachment(0, this.nullableColorTexture.getTextureId());
			if (framebufferOverride != null)
			{
				framebufferOverride.addColorAttachment(0, this.nullableColorTexture.getTextureId());
			}
		}
		else
		{
			this.nullableColorTexture = null;
		}
		
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiAfterColorDepthTextureCreatedEvent.class, textureCreatedParam);
	}
	
	
	
	//===============//
	// LOD rendering //
	//===============//
	
	private void renderLodPass(IDhApiShaderProgram shaderProgram, RenderBufferHandler lodBufferHandler, RenderParams renderEventParam, boolean opaquePass)
	{
		//=======================//
		// debug wireframe setup //
		//=======================//
		
		boolean renderWireframe = Config.Client.Advanced.Debugging.renderWireframe.get();
		if (renderWireframe)
		{
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_LINE);
			GLMC.disableFaceCulling();
		}
		else
		{
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
			GLMC.enableFaceCulling();
		}
		
		if (!opaquePass)
		{
			GLMC.enableBlend();
			GLMC.enableDepthTest();
			GL32.glBlendEquation(GL32.GL_FUNC_ADD);
			GLMC.glBlendFuncSeparate(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA, GL32.GL_ONE, GL32.GL_ONE_MINUS_SRC_ALPHA);
		}
		else
		{
			GLMC.disableBlend();
		}
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeRenderPassEvent.class, renderEventParam);
		
		if (IRIS_ACCESSOR != null)
		{
			// done to fix a bug with Iris where face culling isn't properly set or reverted in the MC state manager
			// which causes Sodium to render some water chunks with their normal inverted
			// https://github.com/IrisShaders/Iris/issues/2582
			// https://github.com/IrisShaders/Iris/blob/1.21.9/common/src/main/java/net/irisshaders/iris/compat/dh/LodRendererEvents.java#L346
			GLMC.enableFaceCulling();
		}
		
		
		SortedArraySet<LodBufferContainer> lodBufferContainer = lodBufferHandler.getColumnRenderBuffers();
		if (lodBufferContainer != null)
		{
			for (int lodIndex = 0; lodIndex < lodBufferContainer.size(); lodIndex++)
			{
				LodBufferContainer bufferContainer = lodBufferContainer.get(lodIndex);
				this.setShaderProgramMvmOffset(bufferContainer.minCornerBlockPos, shaderProgram, renderEventParam);
				
				GLVertexBuffer[] vbos = opaquePass ? bufferContainer.vbos : bufferContainer.vbosTransparent;
				for (int vboIndex = 0; vboIndex < vbos.length; vboIndex++)
				{
					GLVertexBuffer vbo = vbos[vboIndex];
					if (vbo == null)
					{
						continue;
					}
					
					if (vbo.getVertexCount() == 0)
					{
						continue;
					}
					
					vbo.bind();
					shaderProgram.bindVertexBuffer(vbo.getId());
					GL32.glDrawElements(
							GL32.GL_TRIANGLES,
							(vbo.getVertexCount() / 4) * 6, // TODO what does the 4 and 6 here represent?
							this.quadIBO.getType(), 0);
					vbo.unbind();
				}
			}
		}
		
		
		
		//=========================//
		// debug wireframe cleanup //
		//=========================//
		
		if (renderWireframe)
		{
			// default back to GL_FILL since all other rendering uses it 
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
			GLMC.enableFaceCulling();
		}
		
	}
	
	/**
	 * the MVM offset is needed so LODs can be rendered anywhere in the MC world
	 * without running into floating point percision loss.
	 */
	private void setShaderProgramMvmOffset(DhBlockPos pos, IDhApiShaderProgram shaderProgram, RenderParams renderEventParam) throws IllegalStateException
	{
		Vec3d camPos = renderEventParam.exactCameraPosition;
		Vec3f modelPos = new Vec3f(
				(float) (pos.getX() - camPos.x),
				(float) (pos.getY() - camPos.y),
				(float) (pos.getZ() - camPos.z));
		
		shaderProgram.bind();
		shaderProgram.setModelOffsetPos(modelPos);
		
		ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeBufferRenderEvent.class, new DhApiBeforeBufferRenderEvent.EventParam(renderEventParam, modelPos));
	}
	
	
	
	//===============//
	// API functions //
	//===============//
	
	/** @return -1 if no frame buffer has been bound yet */
	public int getActiveFramebufferId() { return this.activeFramebufferId; }
	
	/** @return -1 if no texture has been bound yet */
	public int getActiveColorTextureId() { return this.activeColorTextureId; }
	
	/** @return -1 if no texture has been bound yet */
	public int getActiveDepthTextureId() { return this.activeDepthTextureId; }
	
	
	
}
