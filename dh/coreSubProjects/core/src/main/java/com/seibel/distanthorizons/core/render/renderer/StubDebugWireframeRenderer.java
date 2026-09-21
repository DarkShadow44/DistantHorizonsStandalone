package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.render.RenderParams;

/**
 * Used to prevent attempts to render debug stuff on the server side.
 */
public class StubDebugWireframeRenderer extends AbstractDebugWireframeRenderer
{
	@Override
	public void render(RenderParams renderParams) { }
	
	@Override
	public void renderBox(Box box) { }
	
	@Override
	public void makeParticle(BoxParticle particle) { }
	
	@Override
	public void register(IDebugRenderable renderable, ConfigEntry<Boolean> config) { }
	
	@Override
	public void addRenderer(IDebugRenderable renderable, ConfigEntry<Boolean> config) { }
	
	@Override
	public void unregister(IDebugRenderable renderable, ConfigEntry<Boolean> config) { }
	
	@Override
	public void removeRenderer(IDebugRenderable renderable, ConfigEntry<Boolean> config) { }
	
	@Override
	public void clearRenderables() { }
	
}
