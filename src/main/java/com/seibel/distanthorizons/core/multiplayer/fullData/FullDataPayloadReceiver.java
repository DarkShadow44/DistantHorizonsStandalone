package com.seibel.distanthorizons.core.multiplayer.fullData;

import com.google.common.cache.CacheBuilder;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.INetworkObject;
import com.seibel.distanthorizons.core.network.messages.fullData.FullDataSplitMessage;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.util.LodUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class FullDataPayloadReceiver implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logNetworkEventToFile)
			.build();
	
	private final ConcurrentMap<Integer, CompositeByteBuf> buffersById = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.SECONDS)
			.<Integer, CompositeByteBuf>build().asMap();
	
	@Override
	public void close()
	{
		this.buffersById.clear();
	}
	
	public void receiveChunk(FullDataSplitMessage message)
	{
		this.buffersById.compute(message.bufferId, (bufferId, composite) ->
		{
			if (message.isFirst)
			{
				composite = Unpooled.compositeBuffer();
				LOGGER.debug("Created new full data buffer [" + message.bufferId + "]: [" + composite + "]");
			}
			else if (composite == null)
			{
				LOGGER.debug("Received non-first full data chunk for empty buffer [" + message.bufferId + "]: [" + message.buffer + "].");
				return null;
			}
			
			composite.addComponent(message.buffer);
			composite.writerIndex(composite.writerIndex() + message.buffer.writerIndex());
			LOGGER.debug("Updated full data buffer [" + message.bufferId + "]: [" + composite + "].");
			return composite;
		});
	}
	
	public FullDataSourceV2DTO decodeDataSource(FullDataPayload payload)
	{
		CompositeByteBuf compositeByteBuffer = this.buffersById.get(payload.dtoBufferId);
		LodUtil.assertTrue(compositeByteBuffer != null, "decoded data source missing byte buffer");
		
		try
		{
			FullDataSourceV2DTO dataSourceDto = INetworkObject.decodeToInstance(FullDataSourceV2DTO.CreateEmptyDataSourceForDecoding(), compositeByteBuffer);
			return dataSourceDto;
		}
		finally
		{
			// Releasing the buffer is handled by cache
			this.buffersById.remove(payload.dtoBufferId);
		}
	}
	
}
