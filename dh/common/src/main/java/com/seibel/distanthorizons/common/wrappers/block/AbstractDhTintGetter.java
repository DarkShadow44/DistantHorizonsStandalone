package com.seibel.distanthorizons.common.wrappers.block;
#if MC_VER > MC_1_12_2
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dataObjects.BlockBiomeWrapperPair;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPosMutable;
import com.seibel.distanthorizons.coreapi.util.ColorUtil;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.client.Minecraft;
#if MC_VER <= MC_1_12_2
import net.minecraft.world.biome.Biome;
#else
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
#endif

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.seibel.distanthorizons.core.logging.DhLogger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

#if MC_VER >= MC_1_18_2
import net.minecraft.core.Holder;
#endif

#if MC_VER <= MC_1_21_11
import net.minecraft.world.level.BlockAndTintGetter;
#else
import net.minecraft.client.renderer.block.BlockAndTintGetter;
#endif

public abstract class AbstractDhTintGetter implements BlockAndTintGetter
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	#if MC_VER < MC_1_18_2
	private static final ConcurrentHashMap<String, Biome> BIOME_BY_RESOURCE_STRING = new ConcurrentHashMap<>();
	#else
	private static final ConcurrentHashMap<String, Holder<Biome>> BIOME_BY_RESOURCE_STRING = new ConcurrentHashMap<>();
    #endif
	
	private static final ConcurrentHashMap<BlockBiomeWrapperPair, Integer> COLOR_BY_BLOCK_BIOME_PAIR = new ConcurrentHashMap<>();
	
	
	protected BiomeWrapper biomeWrapper;
	protected BlockStateWrapper blockStateWrapper;
	protected FullDataSourceV2 fullDataSource;
	protected int smoothingRadiusInBlocks;
	protected IClientLevelWrapper clientLevelWrapper;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public AbstractDhTintGetter() { }
	
	/** 
	 * Mutates this getter so we can access the necessary
	 * variables for tint getting.
	 */
	public void update(BiomeWrapper biomeWrapper, BlockStateWrapper blockStateWrapper, FullDataSourceV2 fullDataSource, IClientLevelWrapper clientLevelWrapper)
	{
		this.biomeWrapper = biomeWrapper;
		this.blockStateWrapper = blockStateWrapper;
		this.fullDataSource = fullDataSource;
		this.clientLevelWrapper = clientLevelWrapper;
		this.smoothingRadiusInBlocks = Config.Client.Advanced.Graphics.Quality.lodBiomeBlending.get();
	}
	
	//endregion
	
	
	
	//===============//
	// color getters //
	//===============//
	//region
	
	/** Called by MC's tint getter */
	@Override
	public int getBlockTint(@NotNull BlockPos blockPos, @NotNull ColorResolver colorResolver)
	{
		DhBlockPosMutable mutableBlockPos = new DhBlockPosMutable(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		return this.tryGetBlockTint(mutableBlockPos, colorResolver);
	}
	
	/**
	 * Can be called by DH directly, skipping some of MC's logic
	 * to speed up tint getting slightly.
	 * 
	 * @return {@link ClientBlockStateColorCache#INVALID_COLOR} if any of the biomes needed for this position
	 *          were not cached. In that case calling {@link AbstractDhTintGetter#getBlockTint(BlockPos, ColorResolver)}
	 *          will need to be called by MC's ColorResolver so we can
	 *          populate the color cache.
	 */
	public int tryGetBlockTint(DhBlockPosMutable mutableBlockPos)
	{ return this.tryGetBlockTint(mutableBlockPos, null); }
	
	private int tryGetBlockTint(DhBlockPosMutable mutableBlockPos, @Nullable ColorResolver colorResolver)
	{
		// determine how wide this data source is so we can determine
		// if blending should be used
		byte dataSourceDetailLevel = DhSectionPos.getDetailLevel(this.fullDataSource.getPos());
		// convert from section detail level to absolute detail level
		dataSourceDetailLevel = (byte)(dataSourceDetailLevel - DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
		int dataSourceLodWidthInBlocks = DhSectionPos.getDetailLevelWidthInBlocks(dataSourceDetailLevel);
		
		// don't do any smoothing if smoothing is disabled or if the LOD
		// is to large for block-based smoothing to show up
		if (this.smoothingRadiusInBlocks == 0
			|| dataSourceLodWidthInBlocks > this.smoothingRadiusInBlocks)
		{
			return this.tryGetClientBiomeColor(colorResolver, this.biomeWrapper);
		}
		
		
		// use a rolling average to calculate the color
		int dataPointCount = 0;
		int rollingRed = 0;
		int rollingGreen = 0;
		int rollingBlue = 0;
		
		int xMin = mutableBlockPos.getX() - this.smoothingRadiusInBlocks;
		int xMax = mutableBlockPos.getX() + this.smoothingRadiusInBlocks + 1; // +1 to account for the center block
		
		int zMin = mutableBlockPos.getZ() - this.smoothingRadiusInBlocks;
		int zMax = mutableBlockPos.getZ() + this.smoothingRadiusInBlocks + 1;
		
		int levelMinY = this.clientLevelWrapper.getMinHeight();
		
		for (int x = xMin; x < xMax; x++)
		{
			for (int z = zMin; z < zMax; z++)
			{
				mutableBlockPos.setX(x);
				mutableBlockPos.setZ(z);
				
				// this can return the same position/datapoint for larger LODs duplicating work,
				// however for small smoothing ranges that isn't a big deal and for large LODs
				// we ignore smoothing anyway
				long dataPoint = this.fullDataSource.getDataPointAtBlockPos(mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ(), levelMinY);
				if (dataPoint == FullDataPointUtil.EMPTY_DATA_POINT)
				{
					continue;
				}
				
				
				// get the color for this nearby position
				int id = FullDataPointUtil.getId(dataPoint);
				BiomeWrapper biomeWrapper = (BiomeWrapper) this.fullDataSource.mapping.getBiomeWrapper(id);
				int color = this.tryGetClientBiomeColor(colorResolver, biomeWrapper);
				if (color == ClientBlockStateColorCache.INVALID_COLOR)
				{
					return ClientBlockStateColorCache.INVALID_COLOR;
				}
				
				
				// rolling average
				rollingRed += ColorUtil.getRed(color);
				rollingGreen += ColorUtil.getGreen(color);
				rollingBlue += ColorUtil.getBlue(color);
				
				dataPointCount++;
			}
		}
		
		
		// if no data was present (rarely possible)
		// just use the default center's color
		if (dataPointCount == 0)
		{
			return this.tryGetClientBiomeColor(colorResolver, this.biomeWrapper);
		}
		
		int colorInt = ColorUtil.argbToInt(
				255, // blending often ignores alpha, having it always 255 prevents multiplication issues later
				rollingRed / dataPointCount,
				rollingGreen / dataPointCount,
				rollingBlue / dataPointCount);
		return colorInt;
	}
	
	/** 
	 * If given a ColorResolver this will always succeed. <Br> 
	 * If not it will attempt to use the cached color.
	 */
	private int tryGetClientBiomeColor(@Nullable ColorResolver colorResolver, BiomeWrapper biomeWrapper)
	{
		BlockBiomeWrapperPair pair = BlockBiomeWrapperPair.get(this.blockStateWrapper, biomeWrapper);
		
		// use the cached color if possible
		Integer cachedColor = COLOR_BY_BLOCK_BIOME_PAIR.get(pair);
		if (cachedColor != null)
		{
			return cachedColor;
		}
		
		if (colorResolver == null)
		{
			// no color resolver is present,
			// the cache needs to be populated before 
			// we can use the fast path
			return ClientBlockStateColorCache.INVALID_COLOR;
		}
		
		
		int color = colorResolver.getColor(unwrapClientBiome(biomeWrapper), 0, 0);
		COLOR_BY_BLOCK_BIOME_PAIR.put(pair, color);
		return color;
	}
	
	protected static Biome unwrapClientBiome(BiomeWrapper biomeWrapper)
	{
		String biomeString = biomeWrapper.getSerialString();
		if (biomeString == null
			|| biomeString.isEmpty()
			|| biomeString.equals(BiomeWrapper.EMPTY_BIOME_STRING))
		{
			// default to "plains" for empty/invalid biomes
			biomeString = "minecraft:plains";
		}
		
		return unwrapBiome(getClientBiome(biomeString));
	}
	
	protected static Biome unwrapBiome(#if MC_VER >= MC_1_18_2 Holder<Biome> #else Biome #endif biome)
	{
		#if MC_VER >= MC_1_18_2
		return biome.value();
		#else
		return biome;
		#endif
	}
	
	/**
	 * <p>Previously, this class might have immediately unwrapped the Holder like this:</p>
	 * <pre>{@code
	 * // Inside constructor (OLD WAY - PROBLEMATIC):
	 * Holder<Biome> biomeHolder = getTheHolderFromSomewhere();
	 * this.biome = biomeHolder.value(); // <-- PROBLEM HERE
	 * }</pre>
	 *
	 * <p>This approach is problematic because the {@link net.minecraft.core.Holder} system,
	 * particularly {@code Holder.Reference}, is designed for <strong>late binding</strong>. Here's why storing
	 * the Holder itself is now necessary:</p>
	 * <ol>
	 *   <li>A {@code Holder.Reference<Biome>} might be created initially just with a
	 *       {@link net.minecraft.resources.ResourceKey} (like {@code minecraft:plains}), but its actual
	 *       {@link net.minecraft.core.Holder#value() value()} (the {@code Biome} object itself) might be {@code null}
	 *       at construction time.</li>
	 *   <li>Later, during game loading, registry population, or potentially due to modifications by other mods
	 *       (e.g., Polytone), the system calls internal binding methods (like {@code bindValue(Biome)})
	 *       on the {@code Holder} instance. This sets or <strong>updates</strong> the internal reference to the
	 *       actual {@code Biome} object.</li>
	 *   <li>Crucially, the binding process might assign a completely <strong>new</strong> {@code Biome} object
	 *       instance to the {@code Holder} reference, replacing any previous one.</li>
	 * </ol>
	 *
	 * <p>If we unwrapped the {@code Holder} using {@code .value()} within the constructor (the old way),
	 * our class's internal {@code biome} field would permanently store a reference to whatever {@code Biome}
	 * object the {@code Holder} pointed to *at that exact moment*. It would have no link back to the
	 * {@code Holder} and would be unaware if the {@code Holder} was later updated to point to a different
	 * (or the initially missing) {@code Biome} object. This would lead to using stale or even {@code null} data.</p>
	 *
	 * <p>By storing the {@code Holder<Biome>} itself, this class can call {@link net.minecraft.core.Holder#value()}
	 * whenever the biome information is needed, ensuring it always retrieves the most current {@code Biome}
	 * instance associated with the holder at that time.</p>
	 */
	private static #if MC_VER < MC_1_18_2 Biome #else Holder<Biome> #endif getClientBiome(String biomeResourceString)
	{
		#if MC_VER < MC_1_18_2 
		Biome biome;
		#else 
		Holder<Biome> biome; 
		#endif
		
		// calling get instead of compute is slightly faster for already
		// computed values
		biome = BIOME_BY_RESOURCE_STRING.get(biomeResourceString);
		if (biome != null)
		{
			return biome;
		}
		
		
		// cache the client biomes so we don't have to re-parse the resource location every time
		return BIOME_BY_RESOURCE_STRING.compute(biomeResourceString,
				(resourceString, existingBiome) ->
				{
					if (existingBiome != null)
					{
						return existingBiome;
					}
					
					ClientLevel clientLevel = Minecraft.getInstance().level;
					if (clientLevel == null)
					{
						// shouldn't happen, but just in case
						throw new IllegalStateException("Attempted to get client biome when no client level was loaded.");
					}
					
					BiomeWrapper.BiomeDeserializeResult result;
					try
					{
						result = BiomeWrapper.deserializeBiome(resourceString, clientLevel.registryAccess());
					}
					catch (Exception e)
					{
						LOGGER.warn("Unable to deserialize client biome ["+resourceString+"], using fallback...");
						
						try
						{
							result = BiomeWrapper.deserializeBiome(BiomeWrapper.PLAINS_RESOURCE_LOCATION_STRING, clientLevel.registryAccess());
						}
						catch (IOException ex)
						{
							// should never happen, if it does this log will explode, but just in case
							LOGGER.error("Unable to deserialize fallback client biome ["+BiomeWrapper.PLAINS_RESOURCE_LOCATION_STRING+"], returning NULL.");
							return null;
						}
					}
					
					if (result.success)
					{
						existingBiome = result.biome;
					}
					
					return existingBiome;
				});
	}
	
	//endregion
	
	
	
	//=================//
	// static handlers //
	//=================//
	//region
	
	/** 
	 * can be used in newer MC versions
	 * where the color getting logic is a bit more manual
	 */
	public static void setStaticColor(BlockStateWrapper blockStateWrapper, BiomeWrapper biomeWrapper, Integer colorInt)
	{
		BlockBiomeWrapperPair pair = BlockBiomeWrapperPair.get(blockStateWrapper, biomeWrapper);
		COLOR_BY_BLOCK_BIOME_PAIR.put(pair, colorInt);
	}
	
	public static void clear() { COLOR_BY_BLOCK_BIOME_PAIR.clear(); }
	
	//endregion
	
	
	
}
#endif