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

import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiWorldLoadEvent;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEventParam;
import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;

/**
 * Contains information relevant to Distant Horizons and Minecraft rendering.
 *
 * @author James Seibel
 * @version 2025-12-23
 * @since API 1.0.0
 */
public class DhApiRenderParam implements IDhApiEventParam
{
	/** Indicates what render pass DH is currently rendering */
	public EDhApiRenderPass renderPass;
	
	/** Indicates how far into this tick the frame is. */
	public float partialTicks;
	
	/**
	 * Indicates DH's near clip plane, measured in blocks. 
	 * Note: this may change based on time, player speed, and other factors. 
	 */
	public float nearClipPlane;
	/**
	 * Indicates DH's far clip plane, measured in blocks. 
	 * Note: this may change based on time, player speed, and other factors. 
	 */
	public float farClipPlane;
	
	/** The projection matrix Minecraft is using to render this frame. */
	public final DhApiMat4f mcProjectionMatrix = new DhApiMat4f();
	/** The model view matrix Minecraft is using to render this frame. */
	public final DhApiMat4f mcModelViewMatrix = new DhApiMat4f();
	public final DhApiMat4f mcInverseMvmProjectionMatrix = new DhApiMat4f();
	
	/** The projection matrix Distant Horizons is using to render this frame. */
	public final DhApiMat4f dhProjectionMatrix = new DhApiMat4f();
	/** The model view matrix Distant Horizons is using to render this frame. */
	public final DhApiMat4f dhModelViewMatrix = new DhApiMat4f();
	/** combination of the MVM and projection matrices */
	public final DhApiMat4f dhMvmProjMatrix = new DhApiMat4f();
	public final DhApiMat4f dhInverseMvmProjectionMatrix = new DhApiMat4f();
	
	public int worldYOffset;
	
	/**
	 * The level currently being rendered.
	 * 
	 * @since API 5.1.0 
	 */
	public IDhApiLevelWrapper clientLevelWrapper;
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public DhApiRenderParam() {}
	
	/** Internal DH method */
	public void update(DhApiRenderParam param) 
	{
		this.update(
			param.renderPass,
			param.partialTicks,
			param.nearClipPlane, param.farClipPlane,
			param.mcProjectionMatrix, param.mcModelViewMatrix,
			param.dhProjectionMatrix, param.mcModelViewMatrix, 
			param.worldYOffset,
			param.clientLevelWrapper
		);
	}
	/** Internal DH method */
	public void update(
			EDhApiRenderPass renderPass,
			float newPartialTicks,
			float nearClipPlane, float farClipPlane,
			DhApiMat4f newMcProjectionMatrix, DhApiMat4f newMcModelViewMatrix,
			DhApiMat4f newDhProjectionMatrix, DhApiMat4f newDhModelViewMatrix,
			int worldYOffset,
			IDhApiLevelWrapper clientLevelWrapper
		)
	{
		this.renderPass = renderPass;
		
		this.partialTicks = newPartialTicks;
		
		this.farClipPlane = farClipPlane;
		this.nearClipPlane = nearClipPlane;
		
		// mc matricies
		{
			this.mcProjectionMatrix.set(newMcProjectionMatrix);
			this.mcModelViewMatrix.set(newMcModelViewMatrix);
			
			// inverse mvm Proj
			this.mcInverseMvmProjectionMatrix.set(newMcProjectionMatrix);
			this.mcInverseMvmProjectionMatrix.invert();
		}
		
		// dh matricies
		{
			this.dhProjectionMatrix.set(newDhProjectionMatrix);
			this.dhModelViewMatrix.set(newDhModelViewMatrix);
			
			// proj
			this.dhMvmProjMatrix.set(this.dhProjectionMatrix);
			this.dhMvmProjMatrix.multiply(this.dhModelViewMatrix);
			
			// inverse mvm Proj
			this.dhInverseMvmProjectionMatrix.set(this.dhMvmProjMatrix);
			this.dhInverseMvmProjectionMatrix.invert();
		}
		
		this.worldYOffset = worldYOffset;
		this.clientLevelWrapper = clientLevelWrapper;
		
	}
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public boolean getCopyBeforeFire() { return false; }
	
	@Override
	public DhApiRenderParam copy() { return this; }
	
	//endregion
	
	
	
}
