package com.seibel.distanthorizons.fabric.testing;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeFogRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiFogRenderParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiMutableFogRenderParam;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;

public class TestFogRenderEvent extends DhApiBeforeFogRenderEvent
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final float INCREMENT_PER_SECOND = 0.25f;
	
	
	private float lastFar = 1;
	private long lastUpdateTime = System.nanoTime();
	
	
	
	@Override
	public void beforeRender(DhApiCancelableEventParam<EventParam> event)
	{
		EventParam eventParam = event.value;
		DhApiFogRenderParam originalParms = eventParam.getOriginalFogRenderParam();
		DhApiMutableFogRenderParam mutableParms = eventParam.getFogRenderParam();
		
		long currentTime = System.nanoTime();
		float deltaSeconds = (currentTime - this.lastUpdateTime) / 1_000_000_000f;
		this.lastUpdateTime = currentTime;
		
		this.lastFar += INCREMENT_PER_SECOND * deltaSeconds;
		if (this.lastFar >= originalParms.getFarFogEndPercent())
		{
			this.lastFar = 0.0f;
		}
		
		mutableParms.setFarFogStartPercent(this.lastFar);
		mutableParms.setFarFogEndPercent(this.lastFar + 0.5f);
	}
	
	
	
}
