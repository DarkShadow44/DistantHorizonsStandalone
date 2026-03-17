package com.seibel.distanthorizons.common.render.openGl;

import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShaderProgram;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.*;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3f;
import com.seibel.distanthorizons.common.render.openGl.glObject.GLProxy;
import com.seibel.distanthorizons.common.render.openGl.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.common.render.openGl.glObject.buffer.GlQuadIndexBuffer;
import com.seibel.distanthorizons.common.render.openGl.glObject.shader.GlShaderProgram;
import com.seibel.distanthorizons.common.render.openGl.glObject.vertexAttribute.GlAbstractVertexAttribute;
import com.seibel.distanthorizons.common.render.openGl.glObject.vertexAttribute.GlVertexAttributePostGL43;
import com.seibel.distanthorizons.common.render.openGl.glObject.vertexAttribute.GlVertexAttributePreGL43;
import com.seibel.distanthorizons.common.render.openGl.glObject.vertexAttribute.GlVertexPointer;
import com.seibel.distanthorizons.common.render.openGl.util.vertexFormat.GlLodVertexFormat;
import com.seibel.distanthorizons.common.wrappers.minecraft.LightMapWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftGLWrapper;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodBufferContainer;
import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodQuadBuilder;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import com.seibel.distanthorizons.core.util.math.Vec3f;
import com.seibel.distanthorizons.core.util.objects.SortedArraySet;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.IVertexBufferWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.renderPass.IDhTerrainRenderer;
import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import org.lwjgl.opengl.GL32;

/**
 * Handles rendering the normal LOD terrain.
 * @see LodQuadBuilder
 */
public class GlDhTerrainShaderProgram extends GlShaderProgram implements IDhApiShaderProgram, IDhTerrainRenderer
{
    public static final DhLogger LOGGER = new DhLoggerBuilder()
        .fileLevelConfig(Config.Common.Logging.logRendererEventToFile)
        .build();

    public static final GlDhTerrainShaderProgram INSTANCE = new GlDhTerrainShaderProgram();

    private static final MinecraftGLWrapper GLMC = MinecraftGLWrapper.INSTANCE;
    private static final IIrisAccessor IRIS_ACCESSOR = ModAccessorInjector.INSTANCE.get(IIrisAccessor.class);


    private boolean init = false;

    public GlAbstractVertexAttribute vao;

    // uniforms //
    //region

    public int uCombinedMatrix = -1;
    public int uModelOffset = -1;
    public int uWorldYOffset = -1;

    public int uMircoOffset = -1;
    public int uEarthRadius = -1;
    public int uLightMap = -1;

    // fragment shader uniforms
    public int uClipDistance = -1;
    public int uDitherDhRendering = -1;

    // Noise Uniforms
    public int uNoiseEnabled = -1;
    public int uNoiseSteps = -1;
    public int uNoiseIntensity = -1;
    public int uNoiseDropoff = -1;

    // Debug Uniform
    public int uIsWhiteWorld = -1;

    //endregion



    //=============//
    // constructor //
    //=============//
    //region

    private GlDhTerrainShaderProgram()
    {
        super(
            "assets/distanthorizons/shaders/shared/gl/standard.vert",
            "assets/distanthorizons/shaders/shared/gl/flat_shaded.frag",
            new String[]{"vPosition", "color"}
        );
    }

    public void init()
    {
        if (this.init)
        {
            return;
        }


        this.uCombinedMatrix = this.getUniformLocation("uCombinedMatrix");
        this.uModelOffset = this.getUniformLocation("uModelOffset");
        this.uWorldYOffset = this.getUniformLocation("uWorldYOffset");
        this.uDitherDhRendering = this.getUniformLocation("uDitherDhRendering");
        this.uMircoOffset = this.getUniformLocation("uMircoOffset");
        this.uEarthRadius = this.getUniformLocation("uEarthRadius");

        this.uLightMap = this.getUniformLocation("uLightMap");

        // Fog/Clip Uniforms
        this.uClipDistance = this.getUniformLocation("uClipDistance");

        // Noise Uniforms
        this.uNoiseEnabled = this.getUniformLocation("uNoiseEnabled");
        this.uNoiseSteps = this.getUniformLocation("uNoiseSteps");
        this.uNoiseIntensity = this.getUniformLocation("uNoiseIntensity");
        this.uNoiseDropoff = this.getUniformLocation("uNoiseDropoff");

        // Debug Uniform
        this.uIsWhiteWorld = this.getUniformLocation("uIsWhiteWorld");


        if (GLProxy.getInstance().vertexAttributeBufferBindingSupported)
        {
            this.vao = new GlVertexAttributePostGL43(); // also binds AbstractVertexAttribute
        }
        else
        {
            this.vao = new GlVertexAttributePreGL43(); // also binds AbstractVertexAttribute
        }
        this.vao.bind();

        // short: x, y, z, meta
        //      meta: byte skylight, byte blocklight, byte microOffset
        this.vao.setVertexAttribute(0, 0, GlVertexPointer.addUnsignedShortsPointer(4, false, true));
        // byte: r, g, b, a
        this.vao.setVertexAttribute(0, 1, GlVertexPointer.addUnsignedBytesPointer(4, true, false));
        // byte: iris material ID, normal index, 2 spacers
        this.vao.setVertexAttribute(0, 2, GlVertexPointer.addUnsignedBytesPointer(4, true, true));

        try
        {
            int vertexByteCount = GlLodVertexFormat.DH_VERTEX_FORMAT.getByteSize();
            this.vao.completeAndCheck(vertexByteCount);
        }
        catch (RuntimeException e)
        {
            System.out.println(GlLodVertexFormat.DH_VERTEX_FORMAT);
            throw e;
        }

        // unbinding here is necessary to fix an issue when running on Legacy GL
        this.vao.unbind();

        this.init = true;
    }

    //endregion



    //=============//
    // API methods //
    //=============//
    //region

    @Override
    public void bind()
    {
        this.init();
        super.bind();
        this.vao.bind();
    }
    @Override
    public void unbind()
    {
        super.unbind();
        this.vao.unbind();
    }

    @Override
    public void free()
    {
        this.vao.free();
        super.free();
    }

    @Override
    public void bindVertexBuffer(int vbo) { this.vao.bindBufferToAllBindingPoints(vbo); }

    @Override
    public void fillUniformData(DhApiRenderParam renderParameters)
    {
        Mat4f combinedMatrix = new Mat4f(renderParameters.dhProjectionMatrix);
        combinedMatrix.multiply(renderParameters.dhModelViewMatrix);

        super.bind();

        // uniforms
        this.setUniform(this.uCombinedMatrix, combinedMatrix);
        this.setUniform(this.uMircoOffset, 0.01f); // 0.01 block offset

        this.setUniform(this.uLightMap, LightMapWrapper.GL_BOUND_INDEX);

        this.setUniform(this.uWorldYOffset, (float) renderParameters.worldYOffset);

        this.setUniform(this.uDitherDhRendering, Config.Client.Advanced.Graphics.Quality.ditherDhFade.get());

        float curveRatio = Config.Client.Advanced.Graphics.Experimental.earthCurveRatio.get();
        if (curveRatio < -1.0f || curveRatio > 1.0f)
        {
            curveRatio = /*6371KM*/ 6371000.0f / curveRatio;
        }
        else
        {
            // disable curvature if the config value is between -1 and 1
            curveRatio = 0.0f;
        }
        this.setUniform(this.uEarthRadius, curveRatio);

        // Noise Uniforms
        this.setUniform(this.uNoiseEnabled, Config.Client.Advanced.Graphics.NoiseTexture.enableNoiseTexture.get());
        this.setUniform(this.uNoiseSteps, Config.Client.Advanced.Graphics.NoiseTexture.noiseSteps.get());
        this.setUniform(this.uNoiseIntensity, Config.Client.Advanced.Graphics.NoiseTexture.noiseIntensity.get());
        this.setUniform(this.uNoiseDropoff, Config.Client.Advanced.Graphics.NoiseTexture.noiseDropoff.get());

        // Debug
        this.setUniform(this.uIsWhiteWorld, Config.Client.Advanced.Debugging.enableWhiteWorld.get());

        // Clip Uniform
        float dhNearClipDistance = RenderUtil.getNearClipPlaneInBlocks();
        if (!Config.Client.Advanced.Debugging.lodOnlyMode.get())
        {
            // this added value prevents the near clip plane and discard circle from touching, which looks bad
            dhNearClipDistance += 16f;
        }
        this.setUniform(this.uClipDistance, dhNearClipDistance);
    }

    @Override
    public void setModelOffsetPos(DhApiVec3f modelOffsetPos) { this.setUniform(this.uModelOffset, new Vec3f(modelOffsetPos)); }

    @Override
    public int getId() { return this.id; }

    /** The base DH render program should always render */
    @Override
    public boolean overrideThisFrame() { return true; }

    //endregion



    //===========//
    // rendering //
    //===========//
    //region

    @Override
    public void render(RenderParams renderEventParam, boolean opaquePass, SortedArraySet<LodBufferContainer> bufferContainers, IProfilerWrapper profiler)
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
            // which causes Sodium to render some water chunks with their normals inverted
            // https://github.com/IrisShaders/Iris/issues/2582
            // https://github.com/IrisShaders/Iris/blob/1.21.9/common/src/main/java/net/irisshaders/iris/compat/dh/LodRendererEvents.java#L346
            GLMC.enableFaceCulling();
        }


        if (bufferContainers != null)
        {
            for (int lodIndex = 0; lodIndex < bufferContainers.size(); lodIndex++)
            {
                LodBufferContainer bufferContainer = bufferContainers.get(lodIndex);

                // set uniforms and fire events
                {
                    Vec3d camPos = renderEventParam.exactCameraPosition;
                    Vec3f modelPos = new Vec3f(
                        (float) (bufferContainer.minCornerBlockPos.getX() - camPos.x),
                        (float) (bufferContainer.minCornerBlockPos.getY() - camPos.y),
                        (float) (bufferContainer.minCornerBlockPos.getZ() - camPos.z));

                    GlDhMetaRenderer.INSTANCE.shaderProgramForThisFrame.bind();
                    GlDhMetaRenderer.INSTANCE.shaderProgramForThisFrame.setModelOffsetPos(modelPos);

                    ApiEventInjector.INSTANCE.fireAllEvents(DhApiBeforeBufferRenderEvent.class, new DhApiBeforeBufferRenderEvent.EventParam(renderEventParam, modelPos));
                }

                IVertexBufferWrapper[] vertexBuffers = (opaquePass ? bufferContainer.vbos : bufferContainer.vbosTransparent);
                for (int vboIndex = 0; vboIndex < vertexBuffers.length; vboIndex++)
                {
                    GLVertexBuffer vbo = (GLVertexBuffer) vertexBuffers[vboIndex];
                    if (vbo == null)
                    {
                        continue;
                    }

                    if (vbo.getVertexCount() == 0)
                    {
                        continue;
                    }

                    // 4 vertices per face, but 6 indices (IE 2 triangles) per face, aka need to multiply by 1.5
                    int indexCount = (int)(vbo.getVertexCount() * 1.5);

                    vbo.bind();
                    vbo.quadIBO.bind();

                    GlDhMetaRenderer.INSTANCE.shaderProgramForThisFrame.bindVertexBuffer(vbo.getId());
                    GL32.glDrawElements(
                        GL32.GL_TRIANGLES,
                        indexCount,
                        vbo.quadIBO.getType(), 0);

                    vbo.unbind();
                    vbo.quadIBO.unbind();
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

    //endregion



}
