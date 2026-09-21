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

package com.seibel.distanthorizons.common.wrappers.minecraft;

import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;

#if MC_VER <= MC_1_12_2
import net.minecraft.profiler.Profiler;
#else
import net.minecraft.util.profiling.ProfilerFiller;
#endif

public class ProfilerWrapper implements IProfilerWrapper
{
	#if MC_VER <= MC_1_12_2
	public Profiler profiler;
	#else
	public ProfilerFiller profiler;
	#endif
	
	
	
	#if MC_VER <= MC_1_12_2
	public ProfilerWrapper(Profiler newProfiler)
	#else
	public ProfilerWrapper(ProfilerFiller newProfiler)
	#endif
	{ this.profiler = newProfiler; }
	
	
	
	@Override
	public IProfileBlock push(String newSection) 
	{
		#if MC_VER <= MC_1_12_2
		this.profiler.startSection(newSection);
		#else
		this.profiler.push(newSection);
		#endif
		return new ProfileBlock(this.profiler);
	}
	
	@Override
	public void popPush(String newSection) 
	{
		#if MC_VER <= MC_1_12_2
		this.profiler.endStartSection(newSection);
		#else
		this.profiler.popPush(newSection) ;
		#endif
	}
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	public static class ProfileBlock implements IProfileBlock
	{
		#if MC_VER <= MC_1_12_2
		public Profiler profiler;
		#else
		public ProfilerFiller profiler;
		#endif

		#if MC_VER <= MC_1_12_2
		public ProfileBlock(Profiler newProfiler)
		#else
		public ProfileBlock(ProfilerFiller newProfiler)
		#endif
		{ this.profiler = newProfiler; }
		
		@Override
		public void close()
		{
			#if MC_VER <= MC_1_12_2
			this.profiler.endSection();
			#else
			this.profiler.pop();
			#endif
		}
	}
	
	//endregion
	
	
	
}
