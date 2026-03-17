package com.seibel.distanthorizons.core.util.objects.pooling;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.lang.ref.PhantomReference;

/**
 * Any object that needs pooled arrays should extend this object.
 * This handles setting up and tracking the necessary {@link PhantomReference}'s
 * needed to make sure none of the arrays are leaked. 
 * However, if possible, the implementing object should be closed
 * instead via a try-resource block as that will reduce the number of
 * unnecessary arrays created.
 * 
 * @see PhantomArrayListCheckout
 * @see PhantomArrayListPool
 */
public abstract class AbstractPhantomArrayList implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	private final PhantomArrayListPool phantomArrayListPool;
	private final PhantomReference<AbstractPhantomArrayList> phantomReference;

	/** 
	 * It's recommended to set this as null after the child's constructor 
	 * finishes to show the pooled arrays have all been accessed 
	 */
	protected final PhantomArrayListCheckout pooledArraysCheckout; 
	
	
	
	//=============//
	// constructor //
	//=============//
	
	/** The Array counts can be 0 or greater. */
	public AbstractPhantomArrayList(PhantomArrayListPool phantomArrayListPool, int byteArrayCount, int shortArrayCount, int longArrayCount, int charArrayCount) 
	{
		if (byteArrayCount < 0 
			|| shortArrayCount < 0 
			|| longArrayCount < 0
			|| charArrayCount < 0)
		{
			throw new IllegalArgumentException("Can't get a negative number of pooled arrays.");
		}
		
		this.phantomArrayListPool = phantomArrayListPool;
		this.phantomReference = new PhantomReference<>(this, this.phantomArrayListPool.phantomRefQueue);
		this.pooledArraysCheckout = this.phantomArrayListPool.checkoutArrays(byteArrayCount, shortArrayCount, longArrayCount, charArrayCount);
		this.phantomArrayListPool.phantomRefToCheckout.put(this.phantomReference, this.pooledArraysCheckout);
	}
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override 
	public void close() { this.phantomArrayListPool.returnParentPhantomRef(this.phantomReference); }
	
	
}
