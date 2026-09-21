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

package com.seibel.distanthorizons.common.render.openGl.util.vertexFormat;

import com.google.common.collect.ImmutableList;

/**
 * A (almost) exact copy of MC's
 * DefaultVertexFormats class.
 */
public class GlVertexFormats
{
	public static final GlLodVertexFormatElement ELEMENT_POSITION = new GlLodVertexFormatElement(3, GlLodVertexFormatElement.DataType.USHORT, 3, false);
	public static final GlLodVertexFormatElement ELEMENT_COLOR = new GlLodVertexFormatElement(0, GlLodVertexFormatElement.DataType.UBYTE, 4, false);
	public static final GlLodVertexFormatElement ELEMENT_BYTE_PADDING = new GlLodVertexFormatElement(0, GlLodVertexFormatElement.DataType.BYTE, 1, true);
	
	public static final GlLodVertexFormatElement ELEMENT_LIGHT = new GlLodVertexFormatElement(0, GlLodVertexFormatElement.DataType.UBYTE, 1, false);
	public static final GlLodVertexFormatElement ELEMENT_IRIS_MATERIAL_INDEX = new GlLodVertexFormatElement(0, GlLodVertexFormatElement.DataType.BYTE, 1, false);
	public static final GlLodVertexFormatElement ELEMENT_IRIS_NORMAL_INDEX = new GlLodVertexFormatElement(0, GlLodVertexFormatElement.DataType.BYTE, 1, false);
	
	
	public static final GlLodVertexFormat POSITION_COLOR_BLOCK_LIGHT_SKY_LIGHT_MATERIAL_ID_NORMAL_INDEX = new GlLodVertexFormat(ImmutableList.<GlLodVertexFormatElement>builder()
			.add(ELEMENT_POSITION)
			.add(ELEMENT_BYTE_PADDING)
			.add(ELEMENT_LIGHT)
			.add(ELEMENT_COLOR)
			.add(ELEMENT_IRIS_MATERIAL_INDEX)
			.add(ELEMENT_IRIS_NORMAL_INDEX)
			.add(ELEMENT_BYTE_PADDING)
			.add(ELEMENT_BYTE_PADDING) // padding is to make sure the format is a multiple of 4
			.build());
	
}
