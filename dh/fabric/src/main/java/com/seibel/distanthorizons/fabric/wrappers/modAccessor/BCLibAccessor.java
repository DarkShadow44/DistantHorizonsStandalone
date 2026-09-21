package com.seibel.distanthorizons.fabric.wrappers.modAccessor;

import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.IBCLibAccessor;
#if MC_VER == MC_1_16_5 || MC_VER == MC_1_17_1 || MC_VER == MC_1_20_4 || MC_VER == MC_1_20_6  // These versions either don't have BCLib, or the implementation is different
#elif MC_VER == MC_1_18_2
import ru.bclib.config.ClientConfig;
import ru.bclib.config.Configs;
#elif MC_VER < MC_1_21_1
import org.betterx.bclib.config.ClientConfig;
import org.betterx.bclib.config.Configs;
#endif

public class BCLibAccessor implements IBCLibAccessor
{
	@Override
	public String getModName() { return "BCLib"; }
	
	public void setRenderCustomFog(boolean newValue)
	{
		// only some MC versions have BCLib and require this fix
		#if (MC_VER > MC_1_17_1 && MC_VER < MC_1_20_4)
		
		// Change the value of CUSTOM_FOG_RENDERING in the bclib client config
		// This disabled fog from rendering within bclib
		Configs.CLIENT_CONFIG.set(ClientConfig.CUSTOM_FOG_RENDERING, newValue);
		#endif
	}
	
}