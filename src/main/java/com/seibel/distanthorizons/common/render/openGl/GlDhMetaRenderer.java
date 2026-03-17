package com.seibel.distanthorizons.common.render.openGl;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiFramebuffer;
import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShaderProgram;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.*;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiTextureCreatedParam;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLProxy;
import com.seibel.distanthorizons.common.render.openGl.glObject.GlDhFramebuffer;
import com.seibel.distanthorizons.common.render.openGl.glObject.buffer.GlQuadIndexBuffer;
import com.seibel.distanthorizons.common.render.openGl.glObject.texture.*;
import com.seibel.distanthorizons.common.render.openGl.postProcessing.apply.GlDhApplyShader;
import com.seibel.distanthorizons.common.wrappers.minecraft.LightMapWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodQuadBuilder;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.DhApiRenderProxy;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.AbstractOptifineAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhMetaRenderer;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import com.seibel.distanthorizons.coreapi.DependencyInjection.OverrideInjector;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;

public class GlDhMetaRenderer implements IDhMetaRenderer
{
	public static final DhLogger LOGGER = new DhLoggerBuilder()
		.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
		.build();

	public static final DhLogger RATE_LIMITED_LOGGER = new DhLoggerBuilder()
		.fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
		.maxCountPerSecond(4)
		.build();

	public static final GlDhMetaRenderer INSTANCE = new GlDhMetaRenderer();


	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;


	private int activeFramebufferId = -1;
	private int activeColorTextureId = -1;
	private int activeDepthTextureId = -1;
	private int textureWidth;
	private int textureHeight;


	// framebuffer and texture ID's for this renderer
	private IDhApiFramebuffer framebuffer;
	/** will be null if MC's framebuffer is being used since MC already has a color texture */
	@Nullable
	private GlDhColorTexture nullableColorTexture;
	private GlDhDepthTexture depthTexture;
	/**
	 * If true the {@link GlDhMetaRenderer#framebuffer} is the same as MC's.
	 * This should only be true in the case of Optifine so LODs won't be overwritten when shaders are enabled.
	 */
	private boolean usingMcFramebuffer = false;

	private boolean renderObjectsCreated = false;
	/** used in case there's an API override */
	public IDhApiShaderProgram shaderProgramForThisFrame;



	//============//
	// pre render //
	//============//
	//region

	@Override
	public void runRenderPassSetup(RenderParams renderParams)
	{
		boolean firstPass =
			(renderParams.renderPass == EDhApiRenderPass.OPAQUE
			|| renderParams.renderPass == EDhApiRenderPass.OPAQUE_AND_TRANSPARENT);

		if (!this.renderObjectsCreated)
		{
			boolean setupSuccess = this.createRenderObjects();
			if (!setupSuccess)
			{
				// shouldn't normally happen, but just in case
				return;
			}

			this.renderObjectsCreated = true;
		}

		this.shaderProgramForThisFrame = GlDhTerrainShaderProgram.INSTANCE;
		IDhApiShaderProgram lodShaderProgramOverride = OverrideInjector.INSTANCE.get(IDhApiShaderProgram.class);
		if (lodShaderProgramOverride != null && this.shaderProgramForThisFrame.overrideThisFrame())
		{
			this.shaderProgramForThisFrame = lodShaderProgramOverride;
		}


		this.setGLState(renderParams, firstPass);

		this.bindLightmap(renderParams.lightmap);
	}
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
		this.setActiveFramebufferId(framebuffer.getId());
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

		this.shaderProgramForThisFrame.bind();



		//==========//
		// uniforms //
		//==========//

		IDhApiShaderProgram shaderProgramOverride = OverrideInjector.INSTANCE.get(IDhApiShaderProgram.class);
		if (shaderProgramOverride != null)
		{
			shaderProgramOverride.fillUniformData(renderEventParam);
		}

		this.shaderProgramForThisFrame.fillUniformData(renderEventParam);



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
		int depthTextureId = this.depthTexture.getTextureId();
		this.setActiveDepthTextureId(depthTextureId);

		if (this.nullableColorTexture != null)
		{
			int colorTextureId = this.nullableColorTexture.getTextureId();
			this.setActiveColorTextureId(colorTextureId);
		}
		else
		{
			// get MC's color texture
			int colorTextureId = GL32.glGetFramebufferAttachmentParameteri(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
			this.setActiveColorTextureId(colorTextureId);
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
				framebuffer.addDepthAttachment(this.depthTexture.getTextureId(), EGlDhDepthBufferFormat.DEPTH32F.isCombinedStencil());


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


		// create or get the frame buffer
		if (AbstractOptifineAccessor.optifinePresent())
		{
			// use MC/Optifine's default Framebuffer so shaders won't remove the LODs
			int currentFramebufferId = MC_RENDER.getTargetFramebuffer();
			this.framebuffer = new GlDhFramebuffer(currentFramebufferId);
			this.usingMcFramebuffer = true;
		}
		else
		{
			// normal use case
			this.framebuffer = new GlDhFramebuffer();
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

	@SuppressWarnings( "deprecation" ) // done to ignore DhApiColorDepthTextureCreatedEvent
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


		this.depthTexture = new GlDhDepthTexture(this.textureWidth, this.textureHeight, EGlDhDepthBufferFormat.DEPTH32F);
		this.framebuffer.addDepthAttachment(this.depthTexture.getTextureId(), EGlDhDepthBufferFormat.DEPTH32F.isCombinedStencil());
		if (framebufferOverride != null)
		{
			framebufferOverride.addDepthAttachment(this.depthTexture.getTextureId(), EGlDhDepthBufferFormat.DEPTH32F.isCombinedStencil());
		}


		// if we are using MC's frame buffer, a color texture is already present and shouldn't need to be bound
		if (!this.usingMcFramebuffer)
		{
			this.nullableColorTexture = GlDhColorTexture.builder()
				.setDimensions(this.textureWidth, this.textureHeight)
				.setInternalFormat(EGlDhInternalTextureFormat.RGBA8)
				.setPixelType(EGlDhPixelType.UNSIGNED_BYTE)
				.setPixelFormat(EGlDhPixelFormat.RGBA)
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

	//endregion



	//=============//
	// post render //
	//=============//
	//region

	@Override
	public void runRenderPassCleanup(RenderParams renderParams)
	{
		boolean runningDeferredPass = (renderParams.renderPass == EDhApiRenderPass.TRANSPARENT);
		if (!runningDeferredPass)
		{
			//===================//
			// optifine clean up //
			//===================//

			if (this.usingMcFramebuffer)
			{
				// If MC's framebuffer is being used the depth needs to be cleared to prevent rendering on top of MC.
				// This should only happen when Optifine shaders are being used.
				GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);
			}
		}


		this.unbindLightmap();
		this.shaderProgramForThisFrame.unbind();
	}

	@Override
	public void applyToMcTexture(RenderParams renderParams) { GlDhApplyShader.INSTANCE.render(renderParams); }

	//endregion



	//================//
	// clear textures //
	//================//
	//region

	@Override
	public void clearDhDepthAndColorTextures(RenderParams renderParams)
	{
		IDhApiFramebuffer framebufferOverride = OverrideInjector.INSTANCE.get(IDhApiFramebuffer.class);

		boolean firstPass =
			(renderParams.renderPass == EDhApiRenderPass.OPAQUE
			|| renderParams.renderPass == EDhApiRenderPass.OPAQUE_AND_TRANSPARENT);



		// needs to be fired after all the textures have been created/bound
		boolean clearTextures = !ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeTextureClearEvent.class, renderParams);
		if (clearTextures)
		{
			GL32.glClearDepth(1.0);

			float[] clearColorValues = new float[4];
			GL32.glGetFloatv(GL32.GL_COLOR_CLEAR_VALUE, clearColorValues);
			GL32.glClearColor(clearColorValues[0], clearColorValues[1], clearColorValues[2], 1.0f);

			if (this.usingMcFramebuffer
				&& framebufferOverride == null)
			{
				//// Due to using MC/Optifine's framebuffer we need to re-bind the depth texture,
				//// otherwise we'll be writing to MC/Optifine's depth texture which causes rendering issues
				//this.framebuffer.addDepthAttachment(this.depthTexture.getTextureId(), EDhDepthBufferFormat.DEPTH32F.isCombinedStencil());


				// don't clear the color texture, that removes the sky
				GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);
			}
			else if (firstPass)
			{
				GL32.glClear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT);
			}
		}

	}

	//endregion



	//===============//
	// API functions //
	//===============//
	//region

	public void setActiveFramebufferId(int id) { this.activeFramebufferId = id; }
	/** @return -1 if no frame buffer has been bound yet */
	public int getActiveFramebufferId() { return this.activeFramebufferId; }

	public void setActiveColorTextureId(int id)
	{
		this.activeColorTextureId = id;
		DhApiRenderProxy.activeOpenGlDhColorTextureId = id;
	}
	/** @return -1 if no texture has been bound yet */
	public int getActiveColorTextureId() { return this.activeColorTextureId; }

	public void setActiveDepthTextureId(int id)
	{
		this.activeDepthTextureId = id;
		DhApiRenderProxy.activeOpenGlDhDepthTextureId = id;
	}
	/** @return -1 if no texture has been bound yet */
	public int getActiveDepthTextureId() { return this.activeDepthTextureId; }

	//endregion


	//================//
	// helper methods //
	//================//
	//region

	public void bindLightmap(ILightMapWrapper lightMapWrapper)
	{
		LightMapWrapper lightMap = (LightMapWrapper)lightMapWrapper;
		GLMC.glActiveTexture(GL32.GL_TEXTURE0 + LightMapWrapper.GL_BOUND_INDEX);
		GLMC.glBindTexture(lightMap.getOpenGlId());
	}

	public void unbindLightmap()
	{
		// strange that we don't call "glActiveTexture" here but since it's working James isn't going to change it right now (2026-03-10)
		GLMC.glBindTexture(0);
	}

	//endregion


}
