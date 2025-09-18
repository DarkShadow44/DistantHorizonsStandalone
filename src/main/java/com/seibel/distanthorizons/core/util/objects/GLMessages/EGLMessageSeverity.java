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

import java.util.HashMap;

public enum EGLMessageSeverity
{
	HIGH,
	MEDIUM,
	LOW,
	NOTIFICATION;
	
	
	public final String name;
	
	static final HashMap<String, EGLMessageSeverity> ENUM_BY_NAME = new HashMap<>();
	
	
	static
	{
		for (EGLMessageSeverity severity : EGLMessageSeverity.values())
		{
			ENUM_BY_NAME.put(severity.name, severity);
		}
	}
	
	EGLMessageSeverity() { this.name = super.toString().toUpperCase(); }
	
	
	@Override
	public final String toString() { return this.name; }
	
	public static EGLMessageSeverity get(String name) { return ENUM_BY_NAME.get(name.toUpperCase()); }
	
}
	