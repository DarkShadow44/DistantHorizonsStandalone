package com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayList;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Tracks information related to the {@link PhantomArrayListCheckout}'s
 * held by a {@link PhantomArrayListPool}
 */
public final class PhantomArrayListPoolStatTracker<T>
{
	/** display label used in logs and debug screens (e.g. "byte", "short") */
	public final String typeName;
	/** size of one element in bytes, used for memory-usage estimates */
	public final long elementSizeInBytes;
	private final Supplier<T> emptyListCreatorFunc;
	
	/** total number of backing lists ever created by this pool */
	public final AtomicInteger totalArrayCountRef = new AtomicInteger(0);
	
	
	/** used for debugging: estimated byte-size of all lists currently in the pool */
	public long lastPoolSizeInBytes = -1;
	/** used for debugging: number of lists currently in the pool */
	public int lastPoolCount = 0;
	
	// temporary counts used when determining the size/array counts for
	// the debug screen
	private long pendingPoolByteSize = 0;
	private int pendingPoolCount = 0;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public PhantomArrayListPoolStatTracker(String typeName, long elementSizeInBytes, Supplier<T> emptyListCreatorFunc)
	{
		this.typeName = typeName;
		this.elementSizeInBytes = elementSizeInBytes;
		this.emptyListCreatorFunc = emptyListCreatorFunc;
	}
	
	//endregion
	
	
	
	//===================//
	// checkout tracking //
	//===================//
	//region
	
	/**
	 * Ensures {@link PhantomArrayListCheckout} contains at least {@code requestedArrayCount} lists,
	 * creating new ones as needed.
	 */
	public void fillCheckout(
		int requestedArrayCount,
		IntSupplier getCheckoutExistingArrayCountFunc,
		Consumer<T> addArrayToCheckoutFunc)
	{
		int alreadyCreatedArrayCount = getCheckoutExistingArrayCountFunc.getAsInt();
		for (int i = alreadyCreatedArrayCount; i < requestedArrayCount; i++)
		{
			T newList = this.emptyListCreatorFunc.get();
			addArrayToCheckoutFunc.accept(newList);
		}
	}
	
	//endregion
	
	
	
	//=====================//
	// debug size tracking //
	//=====================//
	//region
	
	public void debugAddPoolByteSize(Iterable<T> lists)
	{
		for (T list : lists)
		{
			long elementCount = getBackingElementCount(list);
			this.pendingPoolByteSize += (elementCount * this.elementSizeInBytes);
			this.pendingPoolCount++;
		}
	}
	private long getBackingElementCount(@NotNull T list)
	{
		if (list instanceof ByteArrayList) return ((ByteArrayList) list).elements().length;
		else if (list instanceof ShortArrayList) return ((ShortArrayList) list).elements().length;
		else if (list instanceof LongArrayList) return ((LongArrayList) list).elements().length;
		else if (list instanceof CharArrayList) return ((CharArrayList) list).elements().length;
		else if (list instanceof ByteBufferCheckoutWrapper) return ((ByteBufferCheckoutWrapper) list).size;
		
		else throw new UnsupportedOperationException("getBackingElementCount not implemented for type [" + list.getClass().getSimpleName() + "].");
	}
	
	public void updateDebugValues(boolean clearLastPoolSize)
	{
		if (clearLastPoolSize)
		{
			this.lastPoolSizeInBytes = 0;
		}
		
		// math.max is used since the pool should only grow until a soft reference is freed, 
		// and it's easier to understand if this constantly grows instead of jumping around
		this.lastPoolSizeInBytes = Math.max(this.pendingPoolByteSize, this.lastPoolSizeInBytes);
		this.lastPoolCount = this.pendingPoolCount;
		
		this.pendingPoolByteSize = 0;
		this.pendingPoolCount = 0;
	}
	
	//endregion
	
	
	
}
