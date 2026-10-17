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

package com.seibel.distanthorizons.api.interfaces.render.renderDef.objects;

import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingApi;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;

import java.nio.ByteBuffer;

/**
 * Holds the vertex/index data used to render an LOD.
 * The number/size of these buffers may vary based on
 * OS and {@link EDhApiRenderingApi}.
 * 
 * @see IDhApiTerrainBufferContainer
 * @since API 7.3.0
 * @version 2026-10-02
 */
public interface IDhApiVertexBufferWrapper extends IBindable, AutoCloseable
{
	void uploadVertexBuffer(ByteBuffer buffer, int vertexCount);
	void uploadIndexBuffer(ByteBuffer buffer, int vertexCount);
	
	int getVertexCount();
	
	@Override
	void close();
	
	
	
}
