package com.seibel.distanthorizons.common.render.blaze.util;

#if MC_VER <= MC_1_21_10
public class BlazeDhVertexFormatUtil {}

#else

import com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding.LodQuadBuilder;
import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockTextureRegistry;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import org.jetbrains.annotations.NotNull;

#if MC_VER <= MC_26_1_2
import com.mojang.blaze3d.vertex.VertexFormatElement;
#elif MC_VER <= MC_26_2_0
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.GpuFormat;
#else
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.mojang.renderpearl.api.GpuFormat;
#endif


/**
 * The following site includes information about supported Vulkan formats:
 * https://vulkan.gpuinfo.org/listbufferformats.php?age=recent&apiversion=1.2
 * 
 * @see LodQuadBuilder
 */
public class BlazeDhVertexFormatUtil
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	@NotNull public static final VertexFormatElement SCREEN_POS;
	@NotNull public static final VertexFormatElement RGBA_FLOAT_COLOR;
	
	/** 
	 * first 3 unsigned shorts (XYZ) are position, 
	 * last short (W) is for metadata (light, micro-offset, etc). <br>
	 * Using a 4 element format is necessary for GPUs that only support 16 byte wide formats.
	 * (Vs the 12 bytes that 3 shorts alone would use.)
	 */
	@NotNull public static final VertexFormatElement SHORT_XYZ_POS_AND_W_META;
	@NotNull public static final VertexFormatElement BYTE_PAD;
	/** contains light and micro-offset */
	@NotNull public static final VertexFormatElement META;
	@NotNull public static final VertexFormatElement RGBA_UBYTE_COLOR;
	@NotNull public static final VertexFormatElement IRIS_MATERIAL;
	@NotNull public static final VertexFormatElement IRIS_NORMAL;
	/** {@link BlockTextureRegistry} tile id, 0 = flat color */
	@NotNull public static final VertexFormatElement TEXTURE_TILE;
	
	@NotNull public static final VertexFormatElement FLOAT_XYZ_POS;
	
	
	
	static
	{
		LOGGER.debug("Attempting to register ["+VertexFormatElement.class.getSimpleName()+"]...");
		
		try
		{
			#if MC_VER <= MC_1_21_11
			SCREEN_POS = VertexFormatElement.register(/*id*/22, /*index*/0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, /*count*/ 2);
			RGBA_FLOAT_COLOR = VertexFormatElement.register(/*id*/23, /*index*/0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.COLOR, /*count*/ 4);
			
			SHORT_XYZ_POS_AND_W_META = VertexFormatElement.register(/*id*/24, /*index*/0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.POSITION, /*count*/ 4);
			BYTE_PAD = VertexFormatElement.register(/*id*/25, /*index*/0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, /*count*/ 1);
			
			META = VertexFormatElement.register(/*id*/26, /*index*/0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.GENERIC, /*count*/ 1);
			RGBA_UBYTE_COLOR = VertexFormatElement.register(/*id*/27, /*index*/0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, /*count*/ 4);
			IRIS_MATERIAL = VertexFormatElement.register(/*id*/28, /*index*/0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, /*count*/ 1);
			IRIS_NORMAL = VertexFormatElement.register(/*id*/29, /*index*/0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, /*count*/ 1);
			
			FLOAT_XYZ_POS = VertexFormatElement.register(/*id*/30, /*index*/0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, /*count*/ 3);
			
			TEXTURE_TILE = VertexFormatElement.register(/*id*/31, /*index*/0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.GENERIC, /*count*/ 1);
			#elif MC_VER <= MC_26_1_2
			SCREEN_POS = VertexFormatElement.register(/*id*/22, /*index*/0, VertexFormatElement.Type.FLOAT, false, /*count*/ 2);
			RGBA_FLOAT_COLOR = VertexFormatElement.register(/*id*/23, /*index*/0, VertexFormatElement.Type.FLOAT, false, /*count*/ 4);
			
			SHORT_XYZ_POS_AND_W_META = VertexFormatElement.register(/*id*/24, /*index*/0, VertexFormatElement.Type.USHORT, false, /*count*/ 4);
			BYTE_PAD = VertexFormatElement.register(/*id*/25, /*index*/0, VertexFormatElement.Type.BYTE, false, /*count*/ 1);
			
			META = VertexFormatElement.register(/*id*/26, /*index*/0, VertexFormatElement.Type.USHORT, false, /*count*/ 1);
			RGBA_UBYTE_COLOR = VertexFormatElement.register(/*id*/27, /*index*/0, VertexFormatElement.Type.UBYTE, true, /*count*/ 4);
			IRIS_MATERIAL = VertexFormatElement.register(/*id*/28, /*index*/0, VertexFormatElement.Type.BYTE, false, /*count*/ 1);
			IRIS_NORMAL = VertexFormatElement.register(/*id*/29, /*index*/0, VertexFormatElement.Type.BYTE, false, /*count*/ 1);
			
			FLOAT_XYZ_POS = VertexFormatElement.register(/*id*/30, /*index*/0, VertexFormatElement.Type.FLOAT, false, /*count*/ 3);
			
			TEXTURE_TILE = VertexFormatElement.register(/*id*/31, /*index*/0, VertexFormatElement.Type.USHORT, false, /*count*/ 1);
			#elif MC_VER <= MC_26_1_2
			
			SCREEN_POS = VertexFormatElement.register(/*id*/22, /*index*/0, GpuFormat.RG32_FLOAT); // 2 floats
			RGBA_FLOAT_COLOR = VertexFormatElement.register(/*id*/23, /*index*/0, GpuFormat.RGBA32_FLOAT); // 4 floats
			
			SHORT_XYZ_POS_AND_W_META = VertexFormatElement.register(/*id*/24, /*index*/0, GpuFormat.RGBA16_UINT); // 4 ushorts
			BYTE_PAD = VertexFormatElement.register(/*id*/25, /*index*/0, GpuFormat.R8_UINT); // 1 byte
			
			META = VertexFormatElement.register(/*id*/26, /*index*/0, GpuFormat.R16_UINT); // 1 ushort
			RGBA_UBYTE_COLOR = VertexFormatElement.register(/*id*/27, /*index*/0, GpuFormat.RGBA8_UNORM); // 4 ubytes
			IRIS_MATERIAL = VertexFormatElement.register(/*id*/28, /*index*/0, GpuFormat.R8_UINT); // 1 byte
			IRIS_NORMAL = VertexFormatElement.register(/*id*/29, /*index*/0, GpuFormat.R8_UINT); // 1 byte
			
			FLOAT_XYZ_POS = VertexFormatElement.register(/*id*/30, /*index*/0, GpuFormat.RGB32_FLOAT); // 3 floats
			
			TEXTURE_TILE = VertexFormatElement.register(/*id*/31, /*index*/0, GpuFormat.R16_UINT); // 1 ushort
			
			#else
			
			SCREEN_POS = new VertexFormatElement("Screen Pos", Float.BYTES * 2, GpuFormat.RG32_FLOAT);
			RGBA_FLOAT_COLOR = new VertexFormatElement("RGBA Float Color", Float.BYTES * 4, GpuFormat.RGBA32_FLOAT);
			
			SHORT_XYZ_POS_AND_W_META = new VertexFormatElement("Short XYZ Pos and W Meta", Short.BYTES * 4, GpuFormat.RGBA16_UINT);
			BYTE_PAD = new VertexFormatElement("Byte Pad", 1, GpuFormat.R8_UINT);
			
			META = new VertexFormatElement("DH Meta", Short.BYTES, GpuFormat.R16_UINT);
			RGBA_UBYTE_COLOR = new VertexFormatElement("Rgba Ubyte Color", 4, GpuFormat.RGBA8_UNORM);
			IRIS_MATERIAL = new VertexFormatElement("Iris Material", 1, GpuFormat.R8_UINT);
			IRIS_NORMAL = new VertexFormatElement("Iris Normal", 1, GpuFormat.R8_UINT);
			
			FLOAT_XYZ_POS = new VertexFormatElement("Float XYZ Pos", Float.BYTES * 3, GpuFormat.RGB32_FLOAT);
			
			TEXTURE_TILE = new VertexFormatElement("DH Texture Tile", Short.BYTES, GpuFormat.R16_UINT);
			#endif
		}
		catch (Exception e)
		{
			String message = "Unable to register one or more ["+VertexFormatElement.class.getSimpleName()+"] this is likely caused by another mod registering their own custom ["+VertexFormatElement.class.getSimpleName()+"]'s. This should be fixed in the next major Minecraft version.";
			
			IMinecraftClientWrapper mc = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
			mc.crashMinecraft(message, new Exception(message, e));
			
			// here to make the compiler happy, the process should shut down before this
			throw new RuntimeException(e);
		}
		
		LOGGER.debug("Successfully registered ["+VertexFormatElement.class.getSimpleName()+"].");
	}
	
	
	
	
	
}
#endif