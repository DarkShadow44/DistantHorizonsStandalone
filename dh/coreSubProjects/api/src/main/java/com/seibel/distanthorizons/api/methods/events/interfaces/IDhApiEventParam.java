package com.seibel.distanthorizons.api.methods.events.interfaces;

import com.seibel.distanthorizons.api.interfaces.util.IDhApiCopyable;

/**
 * @author James Seibel
 * @version 2024-7-12
 * @since API 3.0.0
 */
public interface IDhApiEventParam extends IDhApiCopyable
{
	/** 
	 * Internal DH use. <br> <br>
	 * 
	 * Most API events will clone their parameters
	 * before firing to prevent API implementors
	 * from modifying the properties causing
	 * any subsequent listeners to see the wrong data. <br><br>
	 * 
	 * However, this can be overridden for API events that shouldn't 
	 * be cloned before firing.
	 * Generally that would be done for performance reasons
	 * where an event may fire hundreds or thousands of times 
	 * in quick succession or where the event parameter is needed
	 * internally by DH after firing.
	 * 
	 * @since API 4.1.0 
	 */
	default boolean getCopyBeforeFire() { return true; }
	
}
