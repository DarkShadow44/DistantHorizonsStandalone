package com.seibel.distanthorizons.forge112.modCompat.thermaldynamics;

import cofh.thermaldynamics.duct.TDDucts;
import com.seibel.distanthorizons.common.wrappers.block.ClientBlockStateColorCache;
import net.minecraft.block.state.IBlockState;

import static com.seibel.distanthorizons.common.wrappers.block.ClientBlockStateColorCache.calculateColorFromTexture;

public class ThermalDynamics
{
	public static int getThermalDynamicDuctColor(IBlockState blockState)
	{
		int meta = blockState.getBlock().getMetaFromState(blockState);
		int idOffset = 0;
		
		String name = blockState.getBlock().getRegistryName().toString();
		
		if (name.contains("thermaldynamics:duct_32"))
		{
			idOffset = TDDucts.OFFSET_ITEM;
			return calculateColorFromTexture(TDDucts.getType(meta + idOffset).iconBaseTexture, ClientBlockStateColorCache.EColorMode.Default);
		}
		else if (name.contains("thermaldynamics:duct_64"))
		{
			idOffset = TDDucts.OFFSET_TRANSPORT;
			return calculateColorFromTexture(TDDucts.getType(meta + idOffset).iconBaseTexture, ClientBlockStateColorCache.EColorMode.Default);
		}
		else if (name.contains("thermaldynamics:duct_16"))
		{
			idOffset = TDDucts.OFFSET_FLUID;
			return calculateColorFromTexture(TDDucts.getType(meta + idOffset).iconBaseTexture, ClientBlockStateColorCache.EColorMode.Default);
		}
		else if (name.contains("thermaldynamics:duct_80"))
		{
			idOffset = TDDucts.OFFSET_ENDER;
			return calculateColorFromTexture(TDDucts.getType(meta + idOffset).iconBaseTexture, ClientBlockStateColorCache.EColorMode.Default);
		}
		else
		{
			return calculateColorFromTexture(TDDucts.getType(meta + idOffset).iconBaseTexture, ClientBlockStateColorCache.EColorMode.Default);
		}
	}
	
}