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

package com.seibel.distanthorizons.core.generation.tasks;

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import org.jetbrains.annotations.Nullable;

/**
 * @see DataSourceRetrievalTask
 */
public class DataSourceRetrievalResult
{
	public final ERetrievalResultState state;
	/** the position that was generated, will be null if nothing was generated */
	public final long pos;
	@Nullable
	public final FullDataSourceV2 dataSource;
	
	
	
	//==============//
 	// constructors //
	//==============//
	
	public static DataSourceRetrievalResult CreateSplit() { return new DataSourceRetrievalResult(ERetrievalResultState.REQUIRES_SPLITTING, 0, null); }
	public static DataSourceRetrievalResult CreateSuccess(long pos, FullDataSourceV2 generatedDataSource) { return new DataSourceRetrievalResult(ERetrievalResultState.SUCCESS, pos, generatedDataSource); }
	private DataSourceRetrievalResult(ERetrievalResultState state, long pos, @Nullable FullDataSourceV2 dataSource)
	{
		this.state = state;
		this.pos = pos;
		this.dataSource = dataSource;
	}
	
	
}
