/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.util.objects.dataStreams;

import com.github.luben.zstd.Zstd;
import com.seibel.distanthorizons.api.enums.config.EDhApiDataCompressionMode;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameOutputStream;
import net.jpountz.xxhash.XXHashFactory;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.tukaani.xz.*;

import java.io.*;

/**
 * See {@link DhDataInputStream} for more information about these custom streams.
 *
 * @see DhDataInputStream
 */
public class DhDataOutputStream extends DataOutputStream
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final ThreadLocal<ResettableArrayCache> LZMA_RESETTABLE_ARRAY_CACHE_GETTER = ThreadLocal.withInitial(() -> new ResettableArrayCache(new LzmaArrayCache()));
	
	private final ByteArrayList outputByteArray;
	private final ByteArrayOutputStream wrappedByteStream;
	private final EDhApiDataCompressionMode compressionMode;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	/**
	 * @param outputByteArray where the contents of this stream will be written to when done
	 */
	public static DhDataOutputStream create(EDhApiDataCompressionMode compressionMode, ByteArrayList outputByteArray) throws IOException
	{ return new DhDataOutputStream(new ByteArrayOutputStream(), compressionMode, outputByteArray); }
	private DhDataOutputStream(ByteArrayOutputStream wrappedByteStream, EDhApiDataCompressionMode compressionMode, ByteArrayList outputByteArray) throws IOException
	{ 
		super(warpStream(wrappedByteStream, compressionMode));

		this.wrappedByteStream = wrappedByteStream;
		this.outputByteArray = outputByteArray;
		this.compressionMode = compressionMode;
	}
	@SuppressWarnings("deprecation")
	private static OutputStream warpStream(ByteArrayOutputStream stream, EDhApiDataCompressionMode compressionMode) throws IOException
	{
		try
		{
			switch (compressionMode)
			{
				case UNCOMPRESSED:
					return stream;	
				case LZ4:
					return new LZ4FrameOutputStream(stream, 
							LZ4FrameOutputStream.BLOCKSIZE.SIZE_64KB, -1L,
							// using native libraries has the least GC impact, however they also prevent
							// shadowJar remapping, so we're going to use the normal fast instance for now
							LZ4Factory.fastestInstance().fastCompressor(),
							XXHashFactory.fastestInstance().hash32(),
							//LZ4Factory.nativeInstance().fastCompressor(),
							//XXHashFactory.nativeInstance().hash32(),
							LZ4FrameOutputStream.FLG.Bits.BLOCK_INDEPENDENCE);
				case Z_STD_BLOCK:
					// ZStd compression should be handled after the stream is closed
					// just return the stream
					return stream;
				case LZMA2:
					// using an array cache significantly reduces GC pressure
					ResettableArrayCache arrayCache = LZMA_RESETTABLE_ARRAY_CACHE_GETTER.get();
					arrayCache.reset();
					// Note: if the LZMA2Options are changed the array cache may need to be re-tested.
					// the array cache was specifically tested and tuned for LZMA preset 3/4
					return new XZOutputStream(stream, new LZMA2Options(3),
							XZ.CHECK_CRC64, arrayCache);
				
				case Z_STD_STREAM: // deprecated, only used for legacy support
					//return new ZstdOutputStream(stream, 3, true, true);
					throw new UnsupportedOperationException("Z_STD_STREAM is deprecated and shouldn't be used for encoding. The faster block encoding format should be used instead.");
					
				default:
					throw new IllegalArgumentException("No compressor defined for ["+compressionMode+"]");
				}
			}
			catch (Error e)
			{
				// Should only happen if there's a library issue
				LOGGER.error("Unexpected error when wrapping DhDataInputStream, error: ["+e.getMessage()+"].", e);
				throw e;
			}
	}
	
	
	
	//================//
	// base overrides //
	//================//
	
	@Override
	public void close() throws IOException 
	{
		super.close();
		
		
		this.outputByteArray.clear();
		if (this.compressionMode == EDhApiDataCompressionMode.Z_STD_BLOCK)
		{
			this.outputByteArray.addElements(0, Zstd.compress(this.wrappedByteStream.toByteArray(), 3));
		}
		else
		{
			this.outputByteArray.addElements(0, this.wrappedByteStream.toByteArray());
		}
		
	}
	
	
	
}
