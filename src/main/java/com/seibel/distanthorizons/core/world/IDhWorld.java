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

package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * Shared base between all DH world objects. <br><br>
 * 
 * Independent from {@link AbstractDhServerWorld} so we don't
 * want to deal with generics floating around and so we can
 * have a shared interface between {@link IDhClientWorld}
 * and {@link IDhServerWorld}.
 * 
 * @see AbstractDhServerWorld
 * @see IDhClientWorld
 * @see IDhServerWorld
 */
public interface IDhWorld extends Closeable
{
	
	@Nullable
	IDhLevel getOrLoadLevel(@NotNull ILevelWrapper levelWrapper);
	@Nullable
	IDhLevel getLevel(@NotNull ILevelWrapper wrapper);
	Iterable<? extends IDhLevel> getAllLoadedLevels();
	int getLoadedLevelCount();
	
	void unloadLevel(@NotNull ILevelWrapper levelWrapper);
	
}
