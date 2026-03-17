package com.seibel.distanthorizons.core.util.delayedSaveCache;

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.FullDataSourceV2;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DelayedDataSourceSaveCache extends AbstractDelayedSaveCache<FullDataSourceV2, DelayedDataSourceSaveCache.DataSourceSaveObjContainer>
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
		.name(DelayedDataSourceSaveCache.class.getSimpleName())
		.build();
	
	private final ISaveDataSourceFunc onSaveTimeoutAsyncFunc;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public DelayedDataSourceSaveCache(@NotNull ISaveDataSourceFunc onSaveTimeoutAsyncFunc, int saveDelayInMs)
	{
		super(saveDelayInMs);
		this.onSaveTimeoutAsyncFunc = onSaveTimeoutAsyncFunc;
	}
	
	//endregion
	
	
	
	//===========//
	// overrides //
	//===========//
	//region
	
	public void writeToMemoryAndQueueSave(@NotNull FullDataSourceV2 inputObj) { super.writeToMemoryAndQueueSave(inputObj.getPos(), inputObj); }
	
	@Override 
	protected DataSourceSaveObjContainer createEmptySaveObjContainer(long inputPos) { return new DataSourceSaveObjContainer(inputPos); }
	
	@Override 
	protected void handleDataSourceRemoval(@NotNull DataSourceSaveObjContainer saveContainer)
	{
		FullDataSourceV2 removedDataSource = saveContainer.dataSource;
		this.onSaveTimeoutAsyncFunc.saveAsync(removedDataSource)
			.handle((voidObj, throwable) ->
			{
				try
				{
					// if this close method is fired multiple times
					// monoliths can appear due to concurrent writing to the
					// backend arrays
					removedDataSource.close();
				}
				catch (Exception e)
				{
					LOGGER.error("Unable to close datasource ["+ DhSectionPos.toString(removedDataSource.getPos()) +"], error: ["+e.getMessage()+"].", e);
				}
				
				return null;
			});
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	@FunctionalInterface
	public interface ISaveDataSourceFunc
	{
		/** called after the timeout expires */
		CompletableFuture<Void> saveAsync(@NotNull FullDataSourceV2 inputDataSource);
	}
	
	public static class DataSourceSaveObjContainer extends AbstractSaveObjContainer<FullDataSourceV2>
	{
		private final @NotNull FullDataSourceV2 dataSource;
		
		public DataSourceSaveObjContainer(long inputPos)
		{
			this.dataSource = FullDataSourceV2.createEmpty(inputPos);
		}
		
		@Override 
		public void update(@Nullable FullDataSourceV2 newObj) 
		{
			// shouldn't happen, but just in case
			if (newObj == null)
			{
				throw new NullPointerException();
			}
			
			this.dataSource.updateFromDataSource(newObj);
		}
		
	}
	
	//endregion
	
	
	
	
}
