package com.seibel.distanthorizons.fabric.testing;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkProcessingEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.io.IOException;

public class TestChunkInputReplacerEvent extends DhApiChunkProcessingEvent
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final String REPLACEMENT_BLOCK_STATE_NAMESPACE = "minecraft:stone";
	
	private IDhApiBlockStateWrapper stoneBlockWrapper = null;
	private boolean initialBlockSetupComplete = false;
	
	
	
	@Override
	public void blockOrBiomeChangedDuringChunkProcessing(DhApiEventParam<EventParam> event)
	{
		if (!this.initialBlockSetupComplete)
		{
			// this method can be called on multiple threads
			synchronized (this)
			{
				this.initialBlockSetupComplete = true;
				try
				{
					this.stoneBlockWrapper = DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(REPLACEMENT_BLOCK_STATE_NAMESPACE, event.value.levelWrapper);
				}
				catch (IOException e)
				{
					LOGGER.error("Unable to get ["+REPLACEMENT_BLOCK_STATE_NAMESPACE+"] block replacement cannot continue and is now disabled, error: ["+e.getMessage()+"].", e);
					DhApi.events.unbind(DhApiChunkProcessingEvent.class, this.getClass());
				}
			}
		}
		
		// will happen if the initial setup fails until the unbind call is processed
		// which likely won't happen until the current chunk has finished processing
		if (this.stoneBlockWrapper == null)
		{
			return;
		}
		
		
		
		// replace any dirt or grass block with stone
		IDhApiBlockStateWrapper block = event.value.currentBlock;
		if (block.getSerialString().contains("grass_block")
			|| block.getSerialString().contains("dirt"))
		{
			event.value.setBlockOverride(this.stoneBlockWrapper);
		}
	}
	
	
	
}
