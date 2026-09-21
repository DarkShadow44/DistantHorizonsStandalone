package com.seibel.distanthorizons.common.wrappers.level;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.level.IServerKeyedClientLevel;
#if MC_VER <= MC_1_12_2
import net.minecraft.client.multiplayer.WorldClient;
#else
import net.minecraft.client.multiplayer.ClientLevel;
#endif

public class ServerKeyedClientLevelWrapper extends ClientLevelWrapper implements IServerKeyedClientLevel
{
	/** Returns the folder name the server wants the client to use. */
	private final String serverKey;
	
	/** A unique identifier (generally the level's name) for differentiating multiverse levels */
	private final String serverLevelKey;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	#if MC_VER <= MC_1_12_2
	public ServerKeyedClientLevelWrapper(WorldClient level, String serverKey, String serverLevelKey)
	#else
	public ServerKeyedClientLevelWrapper(ClientLevel level, String serverKey, String serverLevelKey)
    #endif
	{
		super(level);
		this.serverKey = serverKey;
		this.serverLevelKey = serverLevelKey;
	}
	
	//endregion
	
	
	
	//======================//
	// level identification //
	//======================//
	//region
	
	@Override
	public String getServerKey() { return this.serverKey; }
	
	@Override
	public String getServerLevelKey() { return this.serverLevelKey; }
	
	@Override
	public String getDhIdentifier() { return this.getServerLevelKey(); }
	
	//endregion
	
	
	
}
