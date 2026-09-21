package com.seibel.distanthorizons.common.wrappers.worldGeneration.chunkFileHandling;

#if MC_VER > MC_1_12_2
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

/**
 * these tag helpers are usedd to simplify tag accessing between MC versions
 */
public class CompoundTagUtil
{
	
	/** defaults to "false" if the tag isn't present */
	public static boolean getBoolean(CompoundTag tag, String key)
	{
		#if MC_VER < MC_1_21_5
		return tag.getBoolean(key);
		#else
		return tag.getBoolean(key).orElse(false);
		#endif
	}
	
	/** defaults to "0" if the tag isn't present */
	public static byte getByte(CompoundTag tag, String key)
	{
		#if MC_VER < MC_1_21_5
		return tag.getByte(key);
		#else
		return tag.getByte(key).orElse((byte)0);
		#endif
	}
	
	/** defaults to "0" if the tag isn't present */
	public static short getShort(ListTag tag, int index)
	{
		#if MC_VER < MC_1_21_5
		return tag.getShort(index);
		#else
		return tag.getShort(index).orElse((short)0);
		#endif
	}
	
	/** defaults to "0" if the tag isn't present */
	public static int getInt(CompoundTag tag, String key)
	{
		#if MC_VER < MC_1_21_5
		return tag.getInt(key);
		#else
		return tag.getInt(key).orElse(0);
		#endif
	}
	
	/** defaults to "0" if the tag isn't present */
	public static long getLong(CompoundTag tag, String key)
	{
		#if MC_VER < MC_1_21_5
		return tag.getInt(key);
		#else
		return tag.getLong(key).orElse(0L);
		#endif
	}
	
	
	
	/** defaults to null if the tag isn't present */
	@Nullable
	public static String getString(CompoundTag tag, String key)
	{
		#if MC_VER < MC_1_21_5
		return tag.getString(key);
		#else
		return tag.getString(key).orElse(null);
		#endif
	}
	
	/** defaults to null if the tag isn't present */
	@Nullable
	public static byte[] getByteArray(CompoundTag tag, String key)
	{
		#if MC_VER < MC_1_21_5
		return tag.getByteArray(key);
		#else
		return tag.getByteArray(key).orElse(null);
		#endif
	}
	
	
	
	/** defaults to null if the tag isn't present */
	@Nullable
	public static CompoundTag getCompoundTag(CompoundTag tag, String key)
	{
		#if MC_VER < MC_1_21_5
		return tag.getCompound(key);
		#else
		return tag.getCompound(key).orElse(null);
		#endif
	}
	/** defaults to null if the tag isn't present */
	@Nullable
	public static CompoundTag getCompoundTag(ListTag tag, int index)
	{
		#if MC_VER < MC_1_21_5
		return tag.getCompound(index);
		#else
		return tag.getCompound(index).orElse(null);
		#endif
	}
	
	/**
	 * defaults to null if the tag isn't present
	 * @param elementType unused after MC 1.21.5
	 */
	@Nullable
	public static ListTag getListTag(CompoundTag tag, String key, int elementType)
	{
		#if MC_VER < MC_1_21_5
		return tag.getList(key, elementType);
		#else
		return tag.getList(key).orElse(null);
		#endif
	}
	
	/** defaults to null if the tag isn't present */
	@Nullable
	public static ListTag getListTag(ListTag tag, int index)
	{
		#if MC_VER < MC_1_21_5
		return tag.getList(index);
		#else
		return tag.getList(index).orElse(null);
		#endif
	}
	
	
	
	public static boolean contains(CompoundTag tag, String key, int index)
	{
		#if MC_VER < MC_1_21_5
		return tag.contains(key, index);
		#else
		return tag.contains(key);
		#endif
	}
	
	
	
}
#endif