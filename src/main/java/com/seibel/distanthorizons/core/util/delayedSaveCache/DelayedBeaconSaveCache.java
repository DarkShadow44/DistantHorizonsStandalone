package com.seibel.distanthorizons.core.util.delayedSaveCache;

import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos;
import com.seibel.distanthorizons.core.sql.dto.BeaconBeamDTO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DelayedBeaconSaveCache extends AbstractDelayedSaveCache<BeaconBeamDTO, DelayedBeaconSaveCache.BeaconSaveObjContainer>
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
		.name(DelayedBeaconSaveCache.class.getSimpleName())
		.build();
	
	private final @NotNull ISaveBeaconsFunc saveBeaconsFunc;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public DelayedBeaconSaveCache(
		@NotNull ISaveBeaconsFunc saveBeaconsFunc,
		int saveDelayInMs)
	{
		super(saveDelayInMs);
		this.saveBeaconsFunc = saveBeaconsFunc;
	}
	
	//endregion
	
	
	
	//================//
	// save to memory //
	//================//
	//region
	
	/**
	 * Replaces {@link DelayedBeaconSaveCache#writeToMemoryAndQueueSave(long, BeaconBeamDTO)}
	 * due to some additional information being needed.
	 */
	public void queueBeaconBeamUpdatesForChunkPos(@NotNull DhChunkPos chunkPos, @NotNull List<BeaconBeamDTO> activeBeamList)
	{
		BeaconSaveObjContainer container = super.writeToMemoryAndQueueSave(DhSectionPos.encodeContaining((byte)6, chunkPos), null);
		container.addBeaconsAtChunkPos(chunkPos, activeBeamList);
	}
	
	/** 
	 * deprecated since we want to use {@link DelayedBeaconSaveCache#queueBeaconBeamUpdatesForChunkPos} instead
	 * so we can track additional info.
	 */
	@Deprecated
	@Override
	public BeaconSaveObjContainer writeToMemoryAndQueueSave(long inputPos, BeaconBeamDTO inputObj) { throw new UnsupportedOperationException("Use queueBeaconBeamUpdatesForChunkPos instead"); }
	
	//endregion
	
	
	
	//====================//
	// abstract overrides //
	//====================//
	//region
	
	@Override 
	protected BeaconSaveObjContainer createEmptySaveObjContainer(long inputPos) { return new BeaconSaveObjContainer(inputPos); }
	
	@Override 
	protected void handleDataSourceRemoval(@NotNull BeaconSaveObjContainer saveContainer)
	{
		for (DhChunkPos chunkPos : saveContainer.beaconsByBlockPosByChunkPos.keySet())
		{
			HashMap<DhBlockPos, BeaconBeamDTO> beaconsByBlockPos = saveContainer.beaconsByBlockPosByChunkPos.get(chunkPos);
			ArrayList<BeaconBeamDTO> beaconList = new ArrayList<>(beaconsByBlockPos.values()); 
			
			int minBlockX = chunkPos.getMinBlockX();
			int minBlockZ = chunkPos.getMinBlockZ();
			int maxBlockX = chunkPos.getMaxBlockX();
			int maxBlockZ = chunkPos.getMaxBlockZ();
			
			this.saveBeaconsFunc.updateBeaconBeamsBetweenBlockPos(
				saveContainer.pos,
				minBlockX, maxBlockX,
				minBlockZ, maxBlockZ,
				beaconList
			);
		}
	}
	
	//endregion
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	@FunctionalInterface
	public interface ISaveBeaconsFunc
	{
		/** called after the timeout expires */
		void updateBeaconBeamsBetweenBlockPos(
			long sectionPosForLock,
			int minBlockX, int maxBlockX,
			int minBlockZ, int maxBlockZ,
			List<BeaconBeamDTO> activeBeamList
		);
	}
	
	public static class BeaconSaveObjContainer extends AbstractSaveObjContainer<BeaconBeamDTO>
	{
		public final long pos;
		public final HashMap<DhChunkPos, HashMap<DhBlockPos, BeaconBeamDTO>> beaconsByBlockPosByChunkPos = new HashMap<>();
		
		
		
		//=============//
		// constructor //
		//=============//
		//region
		
		public BeaconSaveObjContainer(long pos) { this.pos = pos; }
		
		//endregion
		
		
		
		//===========//
		// overrides //
		//===========//
		//region
		
		public void addBeaconsAtChunkPos(DhChunkPos chunkPos, List<BeaconBeamDTO> activeBeamList) 
		{
			HashMap<DhBlockPos, BeaconBeamDTO> beaconsByBlockPos = this.beaconsByBlockPosByChunkPos.get(chunkPos);
			if (!this.beaconsByBlockPosByChunkPos.containsKey(chunkPos))
			{
				beaconsByBlockPos = new HashMap<>();
				this.beaconsByBlockPosByChunkPos.put(chunkPos, beaconsByBlockPos);
			}
			
			for (BeaconBeamDTO beacon : activeBeamList)
			{
				beaconsByBlockPos.put(beacon.blockPos, beacon);
			}
		}
		
		/**
		 * This logic is handled via {@link BeaconSaveObjContainer#addBeaconsAtChunkPos} instead
		 * due to requiring some additional information.
		 */
		@Override 
		public void update(@Nullable BeaconBeamDTO newObj) { }
		
		//endregion
		
	}
	
	//endregion
	
	
	
}
