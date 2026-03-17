package com.seibel.distanthorizons.core.pooling;

import com.seibel.distanthorizons.core.util.ListUtil;
import com.seibel.distanthorizons.coreapi.util.StringUtil;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This keeps track of all the poolable
 * arrays that can be retrieved via the {@link PhantomArrayListPool}.
 * 
 * @see AbstractPhantomArrayList
 * @see PhantomArrayListPool
 */
public class PhantomArrayListCheckout implements AutoCloseable
{
	/** defines which pool the arrays should be returned too */
	@NotNull
	private final PhantomArrayListPool owningPool;

	/** 
	 * soft reference used by the {@link PhantomArrayListPool} so this checkout can be
	 * freed if there isn't enough memory.
	 */
	@NotNull
	public final SoftReference<PhantomArrayListCheckout> ownerSoftReference;
	
	/** Will be null if the parent pool doesn't want leak stack tracing */
	@Nullable
	public String allocationStackTrace = null;
	
	private final ArrayList<ByteArrayList> byteArrayLists = new ArrayList<>();
	private final ArrayList<ShortArrayList> shortArrayLists = new ArrayList<>();
	private final ArrayList<LongArrayList> longArrayLists = new ArrayList<>();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public PhantomArrayListCheckout(@NotNull PhantomArrayListPool owningPool)
	{
		this.owningPool = owningPool;
		this.ownerSoftReference = new SoftReference<>(this);
	}
	
	public void onCheckout()
	{
		if (this.owningPool.logGarbageCollectedStacks)
		{
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			StackTraceElement[] trimmedElements = Arrays.copyOfRange(stackTraceElements, 4, stackTraceElements.length);
			this.allocationStackTrace = StringUtil.join("\n", trimmedElements).intern();
		}
	}
	
	
	
	//=========//
	// setters //
	//=========//
	
	public void addByteArrayList(ByteArrayList list) { this.byteArrayLists.add(list); }
	public void addShortArrayList(ShortArrayList list) { this.shortArrayLists.add(list); }
	public void addLongArrayListRef(LongArrayList list) { this.longArrayLists.add(list); }
	
	
	
	//=========//
	// getters //
	//=========//
	
	public int getByteArrayCount() { return this.byteArrayLists.size(); }
	public int getShortArrayCount() { return this.shortArrayLists.size(); }
	public int getLongArrayCount() { return this.longArrayLists.size(); }
	
	
	
	public ByteArrayList getByteArray(int index, int size)
	{
		ByteArrayList list = this.byteArrayLists.get(index);
		ListUtil.clearAndSetSize(list, size);
		return list;
	}
	public ShortArrayList getShortArray(int index, int size)
	{
		ShortArrayList list = this.shortArrayLists.get(index);
		ListUtil.clearAndSetSize(list, size);
		return list;
	}
	public LongArrayList getLongArray(int index, int size)
	{
		LongArrayList list = this.longArrayLists.get(index);
		ListUtil.clearAndSetSize(list, size);
		return list;
	}
	
	public ArrayList<ByteArrayList> getAllByteArrays() { return this.byteArrayLists; }
	public ArrayList<ShortArrayList> getAllShortArrays() { return this.shortArrayLists; }
	public ArrayList<LongArrayList> getAllLongArrays() { return this.longArrayLists; }
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override 
	public void close() { this.owningPool.returnCheckout(this); }
	
	
	
}
