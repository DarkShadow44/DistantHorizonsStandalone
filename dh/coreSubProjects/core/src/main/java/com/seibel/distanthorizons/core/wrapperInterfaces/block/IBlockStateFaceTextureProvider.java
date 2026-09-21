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

package com.seibel.distanthorizons.core.wrapperInterfaces.block;

import com.seibel.distanthorizons.core.dataObjects.render.textures.BlockFaceTexture;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the baked {@link BlockFaceTexture} for a given block state and face,
 * used when building textured LODs.
 */
public interface IBlockStateFaceTextureProvider extends IBindable
{
	/**
	 * May be slow the first time a given block state is requested
	 * since the texture needs to be baked.
	 *
	 * @param direction which face of the block to get,
	 *                  only cardinal directions and up/down are valid
	 * @return null if no texture could be baked for this block state
	 */
	@Nullable
	BlockFaceTexture getFaceTexture(IBlockStateWrapper blockState, EDhDirection direction);

	/** Should be called whenever MC's textures change, IE when resource packs are swapped. */
	void clear();

}
