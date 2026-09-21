package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.AbstractPluginPacketSender;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

#if MC_VER >= MC_1_20_6
import com.seibel.distanthorizons.common.CommonPacketPayload;
#else // < 1.20.6
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
#endif

public class FabricPluginPacketSender extends AbstractPluginPacketSender
{
	@Override
	public void sendToServer(AbstractNetworkMessage message)
	{
		#if MC_VER >= MC_1_20_6
		ClientPlayNetworking.send(new CommonPacketPayload(message));
		#else // < 1.20.6
		FriendlyByteBuf buffer = PacketByteBufs.create();
		this.encodeMessage(buffer, message);
		ClientPlayNetworking.send(WRAPPER_PACKET_RESOURCE, buffer);
		#endif
	}
	
	@Override
	public void sendToClient(ServerPlayer serverPlayer, AbstractNetworkMessage message)
	{
		#if MC_VER >= MC_1_20_6
		ServerPlayNetworking.send(serverPlayer, new CommonPacketPayload(message));
		#else // < 1.20.6
		FriendlyByteBuf buffer = PacketByteBufs.create();
		this.encodeMessage(buffer, message);
		ServerPlayNetworking.send(serverPlayer, WRAPPER_PACKET_RESOURCE, buffer);
		#endif
	}
	
}