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

package com.seibel.distanthorizons.common.wrappers.block;

import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockFaceTexture;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateFaceTextureProvider;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes {@link ClientBlockStateTextureCache} to DH core.
 *
 * @see IBlockStateFaceTextureProvider
 */
public class BlockStateTextureProvider implements IBlockStateFaceTextureProvider
{
	public static final BlockStateTextureProvider INSTANCE = new BlockStateTextureProvider();
	
	
	
	@Nullable
	@Override
	public BlockFaceTexture getFaceTexture(IBlockStateWrapper blockState, EDhDirection direction)
	{
		if (!(blockState instanceof BlockStateWrapper))
		{
			// shouldn't happen, but just in case
			throw new UnsupportedOperationException("blockState must be a ["+BlockStateWrapper.class.getSimpleName()+"]");
		}
		return ClientBlockStateTextureCache.getFaceTexture((BlockStateWrapper) blockState, direction);
	}
	
	@Override
	public void clear() 
	{
		ClientBlockStateTextureCache.clearCache();
		ClientBlockStateColorCache.clearCachedTints();
	}
	
}
