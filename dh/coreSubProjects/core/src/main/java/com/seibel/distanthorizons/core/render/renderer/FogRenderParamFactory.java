package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiFogColorMode;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiFogFalloff;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiHeightFogDirection;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiHeightFogMixMode;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiFogRenderParam;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.render.RenderParams;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;

import java.awt.*;

/**
 * @see DhApiFogRenderParam
 * @see DhApiBeforeFogRenderEvent.EventParam
 */
public class FogRenderParamFactory
{
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	
	/** cached object to reduce GC pressure */
	private static final DhApiBeforeFogRenderEvent.EventParam EVENT_PARAM = new DhApiBeforeFogRenderEvent.EventParam();
	
	
	
	//=========//
	// methods //
	//=========//
	//region
	
	/** returns a cached object to reduce GC pressure */
	public static DhApiBeforeFogRenderEvent.EventParam getRenderParam(RenderParams renderParams)
	{
		Color fogColor = getFogColor(renderParams.partialTicks);
		
		// far fog
		EDhApiFogFalloff farFogFalloff = Config.Client.Advanced.Graphics.Fog.farFogFalloff.get();
		float farFogStart = Config.Client.Advanced.Graphics.Fog.farFogStart.get();
		float farFogEnd = Config.Client.Advanced.Graphics.Fog.farFogEnd.get();
		float farFogMin = Config.Client.Advanced.Graphics.Fog.farFogMin.get();
		float farFogMax = Config.Client.Advanced.Graphics.Fog.farFogMax.get();
		float farFogDensity = Config.Client.Advanced.Graphics.Fog.farFogDensity.get();
		
		
		// height fog
		EDhApiFogFalloff heightFogFalloff = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogFalloff.get();
		EDhApiHeightFogMixMode heightFogMixingMode = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogMixMode.get();
		EDhApiHeightFogDirection heightFogCameraDirection = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogDirection.get();
		
		float heightFogBaseHeight = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogBaseHeight.get();
		float heightFogStart = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogStart.get();
		float heightFogEnd = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogEnd.get();
		float heightFogMin = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogMin.get();
		float heightFogMax = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogMax.get();
		float heightFogDensity = Config.Client.Advanced.Graphics.Fog.HeightFog.heightFogDensity.get();
		
		// override fog if underwater
		if (MC_RENDER.isFogStateSpecial())
		{
			// hide everything behind fog
			farFogStart = 0.0f;
			farFogEnd = 1.0f;
			farFogMin = 1.0f; // minimum fog thickness is 1 so everything renders in fog
			farFogMax = 1.0f;
			farFogDensity = 1.0f; // always render max density
		}
		
		DhApiFogRenderParam fogRenderParam = new DhApiFogRenderParam(
			fogColor,
			
			// far fog
			farFogFalloff,
			farFogStart, farFogEnd,
			farFogMin, farFogMax,
			farFogDensity,
			
			// height fog
			heightFogFalloff,
			heightFogMixingMode, heightFogCameraDirection,
			heightFogBaseHeight,
			heightFogStart, heightFogEnd,
			heightFogMin, heightFogMax,
			heightFogDensity
		);
		
		EVENT_PARAM.update(renderParams, fogRenderParam);
		return EVENT_PARAM;
	}
	
	private static Color getFogColor(float partialTicks)
	{
		Color fogColor;
		
		if (Config.Client.Advanced.Graphics.Fog.colorMode.get() == EDhApiFogColorMode.USE_SKY_COLOR
			// when underwater or special fogs are being used, don't use the sky color
			&& !MC_RENDER.isFogStateSpecial())
		{
			fogColor = MC_RENDER.getSkyColor();
		}
		else
		{
			fogColor = MC_RENDER.getFogColor(partialTicks);
		}
		
		return fogColor;
	}
	
	//endregion
	
	
	
}
