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

package com.seibel.distanthorizons.core.api.external.methods.config.client;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiFogFalloff;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiFarFogConfig;
import com.seibel.distanthorizons.core.config.api.DhApiConfigValue;
import com.seibel.distanthorizons.core.config.Config;

public class DhApiFarFogConfig implements IDhApiFarFogConfig
{
	public static DhApiFarFogConfig INSTANCE = new DhApiFarFogConfig();
	
	private DhApiFarFogConfig() { }
	
	
	
	@Override
	public IDhApiConfigValue<Float> farFogStartDistance()
	{ return new DhApiConfigValue<Float, Float>(Config.Client.Advanced.Graphics.Fog.farFogStart); }
	
	@Override
	public IDhApiConfigValue<Float> farFogEndDistance()
	{ return new DhApiConfigValue<Float, Float>(Config.Client.Advanced.Graphics.Fog.farFogEnd); }
	
	@Override
	public IDhApiConfigValue<Float> farFogMinThickness()
	{ return new DhApiConfigValue<Float, Float>(Config.Client.Advanced.Graphics.Fog.farFogMin); }
	
	@Override
	public IDhApiConfigValue<Float> farFogMaxThickness()
	{ return new DhApiConfigValue<Float, Float>(Config.Client.Advanced.Graphics.Fog.farFogMax); }
	
	@Override
	public IDhApiConfigValue<EDhApiFogFalloff> farFogFalloff()
	{ return new DhApiConfigValue<EDhApiFogFalloff, EDhApiFogFalloff>(Config.Client.Advanced.Graphics.Fog.farFogFalloff); }
	
	@Override
	public IDhApiConfigValue<Float> farFogDensity()
	{ return new DhApiConfigValue<Float, Float>(Config.Client.Advanced.Graphics.Fog.farFogDensity); }
	
}
