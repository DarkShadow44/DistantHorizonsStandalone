package com.seibel.distanthorizons.api.objects.render;

import com.seibel.distanthorizons.api.interfaces.render.renderDef.IDhApiDebugWireframeRenderer;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3f;

import java.awt.*;

/**
 * @since API 7.3.0
 * @version 2026-10-02
 * @see IDhApiDebugWireframeRenderer
 */
public interface IDebugBox
{
	DhApiVec3f getMinPos();
	DhApiVec3f getMaxPos();
	Color getColor();
}
