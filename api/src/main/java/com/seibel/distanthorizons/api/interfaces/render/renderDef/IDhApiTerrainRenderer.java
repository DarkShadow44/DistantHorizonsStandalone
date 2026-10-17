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

package com.seibel.distanthorizons.api.interfaces.render.renderDef;

import com.seibel.distanthorizons.api.interfaces.render.renderDef.objects.IDhApiTerrainBufferContainer;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.util.IDhApiReadOnlyList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Handles rendering Distant Horizons LOD terrain.
 * 
 * @since API 7.3.0
 * @version 2026-10-02
 */
public interface IDhApiTerrainRenderer
{
	/**
	 * @param bufferContainers should be rendered in order (i.e. index 0 first).
	 *                      When you receive this list it will already have been culled
	 *                      as defined by the frustum culler.
	 */
	void render(
		DhApiRenderParam renderEventParam, boolean opaquePass,
		IDhApiReadOnlyList<? extends IDhApiTerrainBufferContainer> bufferContainers);
	
	
	
}
