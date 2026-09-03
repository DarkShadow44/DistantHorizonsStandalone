package com.seibel.distanthorizons.core.render;

/**
 * ZERO_TO_POS_ONE, <br>
 * NEG_ONE_TO_POS_ONE, <br>
 */
public enum EDhDepthRange
{
	/**
	 * [0, 1] <br>
	 * 0 can be close or far based on {@link EDhRenderDepth}
	 */
	ZERO_TO_POS_ONE,
	/**
	 * [-1, 1] <br>
	 * -1 can be close or far based on {@link EDhRenderDepth}
	 */
	NEG_ONE_TO_POS_ONE;
	
	
	
}
