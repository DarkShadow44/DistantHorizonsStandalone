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
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEventParam;

import java.awt.*;

/**
 * Contains all the information needed to render Distant Horizons' fog.
 * 
 * @see IDhApiFogConfig
 * @see IDhApiFarFogConfig
 * @see IDhApiHeightFogConfig
 * 
 * @author James Seibel
 * @version 2026-05-20
 * @since API 7.0.0
 */
public class DhApiFogRenderParam implements IDhApiEventParam
{
	protected Color fogColor;
	public Color getFogColor() { return this.fogColor; }
	
	// far fog //
	//region
	
	protected EDhApiFogFalloff farFogFalloff;
	/** @see IDhApiFarFogConfig#farFogFalloff() */
	public EDhApiFogFalloff getFarFogFalloff() { return this.farFogFalloff; }
	
	protected float farFogStartPercent;
	/** @see IDhApiFarFogConfig#farFogStartDistance() */
	public float getFarFogStartPercent() { return this.farFogStartPercent; }
	
	protected float farFogEndPercent;
	/** @see IDhApiFarFogConfig#farFogEndDistance() */
	public float getFarFogEndPercent() { return this.farFogEndPercent; }
	
	protected float farFogMinThickness;
	/** @see IDhApiFarFogConfig#farFogMinThickness() */
	public float getFarFogMinThickness() { return this.farFogMinThickness; }
	
	protected float farFogMaxThickness;
	/** @see IDhApiFarFogConfig#farFogMaxThickness() */
	public float getFarFogMaxThickness() { return this.farFogMaxThickness; }
	
	protected float farFogDensity;
	/** @see IDhApiFarFogConfig#farFogDensity() */
	public float getFarFogDensity() { return this.farFogDensity; }
	
	//endregion
	
	// height fog //
	//region
	
	protected EDhApiFogFalloff heightFogFalloff;
	/** @see IDhApiHeightFogConfig#heightFogFalloff() */
	public EDhApiFogFalloff getHeightFogFalloff() { return this.heightFogFalloff; }
	
	protected EDhApiHeightFogMixMode heightFogMixingMode;
	/** @see IDhApiHeightFogConfig#heightFogMixMode() */
	public EDhApiHeightFogMixMode getHeightFogMixingMode() { return this.heightFogMixingMode; }
	
	protected EDhApiHeightFogDirection heightFogDirection;
	/** @see IDhApiHeightFogConfig#heightFogDirection() */
	public EDhApiHeightFogDirection getHeightFogDirection() { return this.heightFogDirection; }
	
	protected float heightFogBaseHeight;
	/** @see IDhApiHeightFogConfig#heightFogBaseHeight() */
	public float getHeightFogBaseHeight() { return this.heightFogBaseHeight; }
	
	protected float heightFogStartPercent;
	/** @see IDhApiHeightFogConfig#heightFogStartingHeightPercent() */
	public float getHeightFogStartPercent() { return this.heightFogStartPercent; }
	
	protected float heightFogEndPercent;
	/** @see IDhApiHeightFogConfig#heightFogEndingHeightPercent() */
	public float getHeightFogEndPercent() { return this.heightFogEndPercent; }
	
	protected float heightFogMinThickness;
	/** @see IDhApiHeightFogConfig#heightFogMinThickness() */
	public float getHeightFogMinThickness() { return this.heightFogMinThickness; }
	
	protected float heightFogMaxThickness;
	/** @see IDhApiHeightFogConfig#heightFogMaxThickness() */
	public float getHeightFogMaxThickness() { return this.heightFogMaxThickness; }
	
	protected float heightFogDensity;
	/** @see IDhApiHeightFogConfig#heightFogDensity() */
	public float getHeightFogDensity() { return this.heightFogDensity; }
	
	//endregion
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public DhApiFogRenderParam(DhApiFogRenderParam parent)
	{
		this(
			parent.fogColor,
			
			// far fog
			parent.farFogFalloff,
			parent.farFogStartPercent, parent.farFogEndPercent,
			parent.farFogMinThickness, parent.farFogEndPercent,
			parent.farFogDensity,
			
			// height fog
			parent.heightFogFalloff,
			parent.heightFogMixingMode, parent.heightFogDirection,
			parent.heightFogBaseHeight,
			parent.heightFogStartPercent, parent.heightFogEndPercent,
			parent.heightFogMinThickness, parent.heightFogMaxThickness,
			parent.heightFogDensity
		);
	}
	public DhApiFogRenderParam(
		Color fogColor,
		
		// far fog
		EDhApiFogFalloff farFogFalloff,
		float farFogStartPercent, float farFogEndPercent,
		float farFogMinThickness, float farFogMaxThickness,
		float farFogDensity,
		
		// height fog
		EDhApiFogFalloff heightFogFalloff,
		EDhApiHeightFogMixMode heightFogMixingMode, EDhApiHeightFogDirection heightFogDirection,
		float heightFogBaseHeight,
		float heightFogStartPercent, float heightFogEndPercent,
		float heightFogMinThickness, float heightFogMaxThickness,
		float heightFogDensity
	)
	{
		this.fogColor = fogColor;
		
		// far fog
		this.farFogFalloff = farFogFalloff;
		this.farFogStartPercent = farFogStartPercent;
		this.farFogEndPercent = farFogEndPercent;
		this.farFogMinThickness = farFogMinThickness;
		this.farFogMaxThickness = farFogMaxThickness;
		this.farFogDensity = farFogDensity;
		
		// height fog
		this.heightFogFalloff = heightFogFalloff;
		this.heightFogMixingMode = heightFogMixingMode;
		this.heightFogDirection = heightFogDirection;
		this.heightFogBaseHeight = heightFogBaseHeight;
		this.heightFogStartPercent = heightFogStartPercent;
		this.heightFogEndPercent = heightFogEndPercent;
		this.heightFogMinThickness = heightFogMinThickness;
		this.heightFogMaxThickness = heightFogMaxThickness;
		this.heightFogDensity = heightFogDensity;
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public DhApiFogRenderParam copy() { return new DhApiFogRenderParam(this); }
	
	//endregion
	
	
	
	
}
