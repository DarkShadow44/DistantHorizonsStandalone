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

import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

/**
 * Can be used alongside {@link IDhApiMetaRenderer} as
 * a proof-of-concept renderer to make sure
 * DH's renderer is working properly. <Br><Br>
 * 
 * The "GL triangle" should render behind vanilla terrain
 * just like DH's normal rendering does.
 *
 * @since API 7.3.0
 * @version 2026-10-02
 */
public interface IDhApiTestTriangleRenderer
{
	void render(DhApiRenderParam renderParams);
	
}
