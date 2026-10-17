package com.seibel.distanthorizons.api.interfaces.render.renderDef;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeApplyShaderRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeTextureClearEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

/**
 * Contains anything that's shared between
 * render passes or doesn't cleanly fit into another render pass interface.
 *
 * @since API 7.3.0
 * @version 2026-10-02
 */
public interface IDhApiMetaRenderer
{
	/** 
	 * Called before any other render passes start. <Br>
	 * Can be used to create a shared texture or render state used by the rest of DH's render passes. 
	 */
	void runRenderPassSetup(DhApiRenderParam renderParams);
	/**
	 * Called after all other render passes end. <Br>
	 * Should be used to clean up your render state so vanilla Minecraft renders properly.
	 */
	void runRenderPassCleanup(DhApiRenderParam renderParams);
	
	/** 
	 * Ran after DH has finished its render passes
	 * so the DH texture can be merged with vanilla Minecraft's.
	 * @see DhApiBeforeApplyShaderRenderEvent
	 */
	void copyToMcTexture(DhApiRenderParam renderParams);
	/**
	 * Fired before rendering when DH wants to clear its shared
	 * color/depth textures.
	 * @see DhApiBeforeTextureClearEvent
	 */
	void clearDhDepthAndColorTextures(DhApiRenderParam renderParams);
	
	
	
}
