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

package com.seibel.distanthorizons.core.util.objects.GLMessages;

public final class GLMessage
{
	static final String HEADER = "[LWJGL] OpenGL debug message";
	public final EGLMessageType type;
	public final EGLMessageSeverity severity;
	public final EGLMessageSource source;
	public final String id;
	public final String message;
	
	
	
	GLMessage(EGLMessageType type, EGLMessageSeverity severity, EGLMessageSource source, String id, String message)
	{
		this.type = type;
		this.source = source;
		this.severity = severity;
		this.id = id;
		this.message = message;
	}
	
	
	
	@Override
	public String toString() 
	{ 
		return "level: [" + this.severity + "], " +
				"type: [" + this.type + "], " +
				"source: [" + this.source + "], " +
				"id: [" + this.id + "], " +
				"msg: [" + this.message + "]"; 
	}
	
	
	
}