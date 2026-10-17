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
import com.seibel.distanthorizons.api.objects.math.DhApiVec3f;
import com.seibel.distanthorizons.api.objects.render.IDebugBox;

import java.awt.*;

/**
 * Handles rendering Distant Horizons debug wireframes. <Br>
 * These axis-aligned wireframes are used to show
 * the state of various internal systems.
 *
 * @since API 7.3.0
 * @version 2026-10-02
 * @see IDebugBox
 */
public interface IDhApiDebugWireframeRenderer
{
	/** Called once at the start of the render pass, before any {@link #renderBox(IDebugBox)} calls. Override to reset/prepare a batch. */
	void beginRenderBatch(DhApiRenderParam apiRenderParams);
	
	/** 
	 * Will be called potentially thousands of times. <br>
	 * Depending on how fast your renderer is at individual draw calls,
	 * it may be best to batch draw calls together in
	 * conjunction with {@link IDhApiDebugWireframeRenderer#beginRenderBatch(DhApiRenderParam)} 
	 * and {@link IDhApiDebugWireframeRenderer#endRenderBatch(DhApiRenderParam)}
	 */
	void renderBox(IDebugBox box);
	
	/** Called once at the end of the render pass, after all {@link #renderBox(IDebugBox)} calls for the frame. Override to flush a batch. */
	void endRenderBatch(DhApiRenderParam apiRenderParams);
	
	
	
}
