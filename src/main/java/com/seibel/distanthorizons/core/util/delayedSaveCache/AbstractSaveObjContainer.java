package com.seibel.distanthorizons.core.util.delayedSaveCache;

import org.jetbrains.annotations.Nullable;

/**
 * used to keep track of when data sources
 * were written to so we can flush them once
 * enough time has passed.
 * 
 * @see AbstractDelayedSaveCache
 */
public abstract class AbstractSaveObjContainer<T>
{
	/** the last unix millisecond time this data source was written to */
	public long lastWrittenDateTimeMs;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public AbstractSaveObjContainer() { this.lastWrittenDateTimeMs = System.currentTimeMillis(); }
	
	//endregion
	
	
	
	//==================//
	// abstract methods //
	//==================//
	//region
	
	/** 
	 * Can be used to merge multiple objects together if the implementation requires it.
	 * 
	 * @param newObj whether or not this can be null will depend on the specific implementation of the parent {@link AbstractDelayedSaveCache}. 
	 */
	public abstract void update(@Nullable T newObj);
	
	//endregion
	
	
	
	//=========//
	// timeout //
	//=========//
	//region
	
	public void updateLastWrittenTimestamp() { this.lastWrittenDateTimeMs = System.currentTimeMillis(); }
	
	public boolean hasTimedOut(long msTillTimeout)
	{
		long currentTime = System.currentTimeMillis();
		long timeSinceUpdate = currentTime - this.lastWrittenDateTimeMs;
		return (timeSinceUpdate > msTillTimeout);
	}
	
	//endregion
	
	
	
}
