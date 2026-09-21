package com.seibel.distanthorizons.common;

#if MC_VER >= MC_1_20_6

import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CommonPacketPayload(@Nullable AbstractNetworkMessage message) implements CustomPacketPayload
{
	public static final Type<CommonPacketPayload> TYPE = new Type<>(AbstractPluginPacketSender.WRAPPER_PACKET_RESOURCE);
	private static final AbstractPluginPacketSender PACKET_SENDER = (AbstractPluginPacketSender) SingletonInjector.INSTANCE.get(IPluginPacketSender.class);
	
	@NotNull
	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
	
	
	public static class Codec implements StreamCodec<FriendlyByteBuf, CommonPacketPayload>
	{
		@NotNull
		@Override
		public CommonPacketPayload decode(@NotNull FriendlyByteBuf in)
		{ return new CommonPacketPayload(PACKET_SENDER.decodeMessage(in)); }
		
		@Override
		public void encode(@NotNull FriendlyByteBuf out, CommonPacketPayload payload)
		{ PACKET_SENDER.encodeMessage(out, payload.message()); }
		
	}
	
}

#endif