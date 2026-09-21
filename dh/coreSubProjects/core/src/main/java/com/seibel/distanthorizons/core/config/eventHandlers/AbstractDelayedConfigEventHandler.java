package com.seibel.distanthorizons.core.config.eventHandlers;

import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.util.TimerUtil;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractDelayedConfigEventHandler implements IConfigListener
{
	public static final long DEFAULT_TIMEOUT_IN_MS = 2_000L;
	
	/** how long to wait in milliseconds before applying the config changes */
	private final long timeoutInMs;
	private Timer timer;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public AbstractDelayedConfigEventHandler(long timeoutInMs) { this.timeoutInMs = timeoutInMs; }
	
	//endregion
	
	
	
	//==================//
	// abstract methods //
	//==================//
	//region
	
	public abstract void onConfigTimeout();
	
	//endregion
	
	
	
	//========//
	// events //
	//========//
	//region
	
	@Override
	public void onConfigValueSet()
	{
		if (this.timeoutInMs > 0)
		{
			this.refreshRenderDataAfterTimeout();
		}
		else
		{
			this.onConfigTimeout();
		}
	}
	
	/** Calling this method multiple times will reset the timer */
	private synchronized void refreshRenderDataAfterTimeout() // synchronized to prevent potential threading issues when adding/removing the timer
	{
		// stop the previous timer if one exists
		if (this.timer != null)
		{
			this.timer.cancel();
		}
		
		// create a new timer task
		TimerTask timerTask = new TimerTask()
		{
			public void run()
			{
				AbstractDelayedConfigEventHandler.this.onConfigTimeout();
			}
		};
		this.timer = TimerUtil.CreateTimer("AbstractDelayedConfigTimer");
		this.timer.schedule(timerTask, this.timeoutInMs);
	}
	
	//endregion
	
	
	
}
