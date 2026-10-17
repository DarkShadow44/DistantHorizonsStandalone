package com.seibel.distanthorizons.api.enums.config;

/**
 * AUTO, <br>
 * OPEN_GL, <br>
 * BLAZE_3D, <br>
 * API, <br><br>
 *
 * @see EDhApiRenderingApi
 * 
 * @since API 7.0.0
 * @version 2026-10-02
 */
public enum EDhApiRenderingEngine
{
	AUTO,
	OPEN_GL,
	BLAZE_3D,
	
	/** @since API 7.3.0 */
	@DisallowSelectingViaConfigGui
	API;
	
}
