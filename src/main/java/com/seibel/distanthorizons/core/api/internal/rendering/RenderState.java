package com.seibel.distanthorizons.core.api.internal.rendering;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.util.math.Mat4f;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;

/**
 * Used to track the rendering state for the current frame.
 * 
 * @see ClientApi
 */
public class RenderState
{
	public Mat4f mcModelViewMatrix = null;
	public Mat4f mcProjectionMatrix = null;
	public float frameTime = -1;
	public IClientLevelWrapper clientLevelWrapper = null;
	
	
	
	//========//
	// checks //
	//========//
	
	public String unableToRenderBecause()
	{
		String errorReasons = "";
		
		// the matrix may be the identity matrix or and old/incorrect matrix
		// but we did set it at least once before this
		if (this.mcModelViewMatrix == null)
		{
			errorReasons += "no MVM Matrix, ";
		}
		
		if (this.mcProjectionMatrix == null)
		{
			errorReasons += "no Projection Matrix, ";
		}
		
		if (this.frameTime == -1)
		{
			errorReasons += "no Frame Time, ";
		}
		
		if (this.clientLevelWrapper == null)
		{
			errorReasons += "no Level Wrapper, ";
		}
		
		return errorReasons;
	}
	
	public boolean canRender()
	{
		// separated variable to allow for easy checking with the debugger
		String errorReasons = this.unableToRenderBecause();
		return errorReasons.isEmpty();
	}
	
	public void canRenderOrThrow() throws IllegalStateException
	{
		String errorReasons = this.unableToRenderBecause();
		if (!errorReasons.isEmpty())
		{
			throw new IllegalStateException(errorReasons);
		}
	}
	
	
	
}
