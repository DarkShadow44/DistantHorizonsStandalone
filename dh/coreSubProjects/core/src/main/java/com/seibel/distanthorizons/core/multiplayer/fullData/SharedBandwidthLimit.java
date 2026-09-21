package com.seibel.distanthorizons.core.multiplayer.fullData;

import com.seibel.distanthorizons.core.config.Config;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SharedBandwidthLimit
{
	private final Set<FullDataPayloadSender> senders = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public void setSenderActive(FullDataPayloadSender sender, boolean active)
	{
		if (active)
		{
			this.senders.add(sender);
		}
		else
		{
			this.senders.remove(sender);
		}
	}
	
	public int getBandwidthShare()
	{
		int globalBandwidthLimit = Config.Server.globalBandwidthLimit.get();
		if (globalBandwidthLimit == 0)
		{
			globalBandwidthLimit = Integer.MAX_VALUE;
		}
		
		return globalBandwidthLimit / Math.max(this.senders.size(), 1);
	}
	
}
