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

package com.seibel.distanthorizons.api.methods.events.sharedParameterObjects;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiFogFalloff;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiHeightFogDirection;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiHeightFogMixMode;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiFarFogConfig;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiFogConfig;
import com.seibel.distanthorizons.api.interfaces.config.client.IDhApiHeightFogConfig;

import java.awt.*;

/**
 * A mutable version of {@link DhApiFogRenderParam} to allow
 * API modification of DH's fog rendering.
 *
 * @see IDhApiFogConfig
 * @see IDhApiFarFogConfig
 * @see IDhApiHeightFogConfig
 * @see DhApiFogRenderParam
 * 
 * @author James Seibel
 * @version 2026-05-20
 * @since API 7.0.0
 */
public class DhApiMutableFogRenderParam extends DhApiFogRenderParam
{
	public void setFogColor(Color fogColor) { this.fogColor = fogColor; }
	
	// far fog //
	//region
	
	/** @see IDhApiFarFogConfig#farFogFalloff() */
	public void setFarFogFalloff(EDhApiFogFalloff farFogFalloff) { this.farFogFalloff = farFogFalloff; }
	/** @see IDhApiFarFogConfig#farFogStartDistance() */
	public void setFarFogStartPercent(float farFogStartPercent) { this.farFogStartPercent = farFogStartPercent; }
	/** @see IDhApiFarFogConfig#farFogEndDistance() */
	public void setFarFogEndPercent(float farFogEndPercent) { this.farFogEndPercent = farFogEndPercent; }
	/** @see IDhApiFarFogConfig#farFogMinThickness() */
	public void setFarFogMinThickness(float farFogMinThickness) { this.farFogMinThickness = farFogMinThickness; }
	/** @see IDhApiFarFogConfig#farFogMaxThickness() */
	public void setFarFogMaxThickness(float farFogMaxThickness) { this.farFogMaxThickness = farFogMaxThickness; }
	/** @see IDhApiFarFogConfig#farFogDensity() */
	public void setFarFogDensity(float farFogDensity) { this.farFogDensity = farFogDensity; }
	
	//endregion
	
	// height fog //
	//region
	
	/** @see IDhApiHeightFogConfig#heightFogFalloff() */
	public void setHeightFogFalloff(EDhApiFogFalloff heightFogFalloff) { this.heightFogFalloff = heightFogFalloff; }
	/** @see IDhApiHeightFogConfig#heightFogMixMode() */
	public void setHeightFogMixingMode(EDhApiHeightFogMixMode heightFogMixingMode) { this.heightFogMixingMode = heightFogMixingMode; }
	/** @see IDhApiHeightFogConfig#heightFogDirection() */
	public void setHeightFogDirection(EDhApiHeightFogDirection heightFogDirection) { this.heightFogDirection = heightFogDirection; }
	/** @see IDhApiHeightFogConfig#heightFogBaseHeight() */
	public void setHeightFogBaseHeight(float heightFogBaseHeight) { this.heightFogBaseHeight = heightFogBaseHeight; }
	/** @see IDhApiHeightFogConfig#heightFogStartingHeightPercent() */
	public void setHeightFogStartPercent(float heightFogStartPercent) { this.heightFogStartPercent = heightFogStartPercent; }
	/** @see IDhApiHeightFogConfig#heightFogEndingHeightPercent() */
	public void setHeightFogEndPercent(float heightFogEnd) { this.heightFogEndPercent = heightFogEnd; }
	/** @see IDhApiHeightFogConfig#heightFogMinThickness() */
	public void setHeightFogMinThickness(float heightFogMinThickness) { this.heightFogMinThickness = heightFogMinThickness; }
	/** @see IDhApiHeightFogConfig#heightFogMaxThickness() */
	public void setHeightFogMaxThickness(float heightFogMaxThickness) { this.heightFogMaxThickness = heightFogMaxThickness; }
	/** @see IDhApiHeightFogConfig#heightFogDensity() */
	public void setHeightFogDensity(float heightFogDensity) { this.heightFogDensity = heightFogDensity; }
	
	//endregion
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public DhApiMutableFogRenderParam(DhApiFogRenderParam parent)
	{ super(parent); }
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public DhApiMutableFogRenderParam copy() { return new DhApiMutableFogRenderParam(this); }
	
	//endregion
	
	
	
	
}
