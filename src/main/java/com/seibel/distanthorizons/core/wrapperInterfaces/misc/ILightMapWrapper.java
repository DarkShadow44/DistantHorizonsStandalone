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

package com.seibel.distanthorizons.core.wrapperInterfaces.misc;

import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;
import org.lwjgl.opengl.GL32;

/**
 * @author James Seibel
 * @version 3-5-2022
 */
public interface ILightMapWrapper extends IBindable
{
	/** 
	 * which texture index IE 0,1,2... the lightmap will be bound to. <Br> 
	 * Related to but different from {@link GL32#GL_TEXTURE0}.
	 */
	int BOUND_INDEX = 0;
	
	void bind();
	void unbind();
	
}
