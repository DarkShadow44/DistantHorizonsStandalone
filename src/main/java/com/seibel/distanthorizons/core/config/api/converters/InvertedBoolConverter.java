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

package com.seibel.distanthorizons.core.config.api.converters;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiRendererMode;
import com.seibel.distanthorizons.coreapi.interfaces.config.IConverter;

/**
 * Used to support deprecated config options that may be identical
 * in implementation but with the On/Off values flipped.
 *
 * @author James Seibel
 * @version 2025-12-22
 */
public class InvertedBoolConverter implements IConverter<Boolean, Boolean>
{
	
	@Override 
	public Boolean convertToCoreType(Boolean core)
	{ return !core; }
	
	@Override 
	public Boolean convertToApiType(Boolean api)
	{ return !api; }
	
}
