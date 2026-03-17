package com.seibel.distanthorizons.api.interfaces.data;

/**
 * Can be used to drastically speed up repeat read operations in {@link IDhApiTerrainDataRepo}. <br><br>
 * 
 * Once you are done with this cache, closing it will free up any objects
 * the cache is holding. This can reduce Garbage Collector overhead and reduce stuttering.
 * 
 * @see IDhApiTerrainDataRepo
 * 
 * @author James Seibel
 * @version 2026-1-29
 * @since API 3.0.0
 */
public interface IDhApiTerrainDataCache extends AutoCloseable
{
	/**
	 * Removes any data that's currently stored in this cache.
	 * This cane be done to free up memory or invalidate 
	 * the cache so fresh data can be pulled in.
	 * <br><br>
	 * This should be called before de-referencing this object
	 * so DH can handle any necessary cleanup for internal objects.
	 */
	void clear();
	
}
