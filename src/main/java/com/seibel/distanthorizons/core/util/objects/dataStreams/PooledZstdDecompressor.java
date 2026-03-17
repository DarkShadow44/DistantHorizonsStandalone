package com.seibel.distanthorizons.core.util.objects.dataStreams;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDecompressCtx;
import com.github.luben.zstd.ZstdException;
import com.seibel.distanthorizons.core.util.objects.pooling.PhantomArrayListCheckout;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PooledZstdDecompressor
{
	/** reuse the ZStd native context for a minor speed improvement */
	private static final ConcurrentLinkedQueue<ZstdDecompressCtx> ZSTD_CONTEXT_CACHE = new ConcurrentLinkedQueue<>();
	
	
	
	/**
	 * Replaces {@link Zstd#decompress} so we can use a pooled byte array
	 * which significantly reduces GC pressure.
	 */
	public static ByteArrayList decompressFrame(byte[] src, PhantomArrayListCheckout checkout) throws ZstdException
	{
		int compressedSize = (int) Zstd.findFrameCompressedSize(src, 0);
		int contentSize = (int) Zstd.getFrameContentSize(src, 0, compressedSize);
		if (Zstd.isError(contentSize))
		{
			// known error at the moment, but not for getErrorName
			if (contentSize == -1)
			{
				throw new ZstdException(contentSize, "Content size is unknown");
			}
			// otherwise let ZstdException get error message itself
			throw new ZstdException(contentSize);
		}
		
		ByteArrayList destination = checkout.getByteArray(0, contentSize);
		return decompress(src, compressedSize, contentSize, destination);
	}
	private static ByteArrayList decompress(byte[] src, int srcSize, int originalSize, ByteArrayList destination) throws ZstdException
	{
		if (originalSize < 0)
		{
			throw new ZstdException(Zstd.errGeneric(), "Original size should not be negative");
		}
		
		// reuse the ZStd native context for a minor speed improvement
		ZstdDecompressCtx ctx = ZSTD_CONTEXT_CACHE.poll();
		if (ctx == null)
		{
			ctx = new ZstdDecompressCtx();
		}
		
		int size = ctx.decompressByteArray(destination.elements(), 0, destination.size(), src, 0, srcSize);
		if (size != originalSize)
		{
			//return Arrays.copyOfRange(destination.elements(), 0, size);
			destination.size(size); // this assumes the size will only be smaller than the expected
		}
		
		ZSTD_CONTEXT_CACHE.add(ctx);
		
		return destination;
	}
	
}
