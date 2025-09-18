package com.seibel.distanthorizons.core.util.objects.dataStreams;

//import com.github.luben.zstd.BufferPool;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/** 
 * LZMA requires a custom object to cache it's backend arrays. 
 */
public class ZstdArrayCache //implements BufferPool
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/**
	 * In James' testing the byte and int caches only ever had to store 2 and 4 arrays respectively.
	 * With the in mind we could take a few shortcuts, but if that changes then we need to be 
	 * notified as it might cause issues with the current logic.
	 */
	public static final int WARN_CACHE_LENGTH_EXCEEDED = 10;
	
	public static final AtomicInteger MAX_BYTE_CACHE_LENGTH_REF = new AtomicInteger(WARN_CACHE_LENGTH_EXCEEDED);
	
	public final IntUnaryOperator maxByteCacheSizeUnaryOperator = (x) -> Math.max(this.bufferCache.size(), x);
	
	
	/** 
	 * generally only 2 items long <br>
	 * {@link Int2ReferenceArrayMap} can be used since the cache should only be a few items long.
	 * If the array ends up being longer then this design will need to be changed.
	 */
	public final Int2ReferenceArrayMap<ArrayList<ByteBuffer>> bufferCache = new Int2ReferenceArrayMap<>();
	
	
	
	//=============//
	// byte arrays //
	//=============//
	
	//@Override
	public ByteBuffer get(int size)
	{
		ArrayList<ByteBuffer> cacheList = this.bufferCache.computeIfAbsent(size, (newSize) -> new ArrayList<>(4));
		if (cacheList.isEmpty())
		{
			return ByteBuffer.allocate(size);
		}
		
		ByteBuffer array = cacheList.remove(cacheList.size()-1);
		if (array == null)
		{
			return ByteBuffer.allocate(size);
		}
		
		return array;
	}
	
	//@Override
	public void release(ByteBuffer buffer)
	{
		int size = buffer.array().length;
		this.bufferCache.computeIfAbsent(size, (newSize) -> new ArrayList<>());
		this.bufferCache.get(size).add(buffer);
		
		
		if (this.bufferCache.size() > WARN_CACHE_LENGTH_EXCEEDED)
		{
			int previousMax = MAX_BYTE_CACHE_LENGTH_REF.getAndUpdate(this.maxByteCacheSizeUnaryOperator);
			int newMax = MAX_BYTE_CACHE_LENGTH_REF.get();
			if (newMax > previousMax)
			{
				LOGGER.warn("LZMA byte array cache expected size exceeded. Expected max length ["+WARN_CACHE_LENGTH_EXCEEDED+"], actual length ["+this.bufferCache.size()+"].");
			}
		}
	}
	
	
	
}
