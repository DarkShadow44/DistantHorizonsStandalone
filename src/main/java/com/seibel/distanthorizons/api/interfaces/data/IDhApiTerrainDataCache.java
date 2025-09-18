package com.seibel.distanthorizons.api.interfaces.data;

/**
 * Can be used to drastically speed up repeat read operations in {@link IDhApiTerrainDataRepo}.
 *
 * @see IDhApiTerrainDataRepo
 * 
 * @author James Seibel
 * @version 2024-7-14
 * @since API 3.0.0
 */
public interface IDhApiTerrainDataCache // TODO should this be AutoClosable?
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
