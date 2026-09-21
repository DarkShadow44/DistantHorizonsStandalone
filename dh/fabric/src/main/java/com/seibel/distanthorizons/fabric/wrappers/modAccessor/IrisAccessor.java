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

package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

#if MC_VER <= MC_1_18_2
#else // MC_1_19_4 and newer

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IIrisAccessor;

#if MC_VER <= MC_1_20_4
import net.coderbot.iris.Iris;
#else
import net.irisshaders.iris.Iris;
#endif
import net.irisshaders.iris.api.v0.IrisApi;

public class IrisAccessor implements IIrisAccessor
{
	@Override
	public String getModName() { return Iris.MODID; }
	
	@Override
	public boolean isShaderPackInUse() { return IrisApi.getInstance().isShaderPackInUse(); }
	
	@Override
	public boolean isRenderingShadowPass() { return IrisApi.getInstance().isRenderingShadowPass(); }
	
	@Override
	public boolean isReverseZDuringShaders()
	{
		#if MC_VER <= MC_1_21_11
		return false;
		#else
		// only supported on Iris for MC 26.2 and newer
		return IrisApi.getInstance().isReverseZDuringShaders();
		#endif
	}
	
}

#endif
