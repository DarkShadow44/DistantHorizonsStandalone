package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.AbstractPluginPacketSender;
import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import net.minecraft.server.level.ServerPlayer;

#if MC_VER >= MC_1_20_2
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
#elif MC_VER >= MC_1_18_2
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
#elif MC_VER >= MC_1_17_1
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
#else // < 1.17.1
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.network.PacketDistributor;
#endif

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ForgePluginPacketSender extends AbstractPluginPacketSender
{
	public static final SimpleChannel PLUGIN_CHANNEL =
			#if MC_VER >= MC_1_20_2
			ChannelBuilder.named(AbstractPluginPacketSender.WRAPPER_PACKET_RESOURCE)
					.networkProtocolVersion(1)
					.serverAcceptedVersions((status, version) -> true)
					.clientAcceptedVersions((status, version) -> true)
					.simpleChannel();
			#else // < 1.20.2
			NetworkRegistry.newSimpleChannel(
					AbstractPluginPacketSender.WRAPPER_PACKET_RESOURCE,
					() -> "1",
					ignored -> true,
					ignored -> true
			);
			#endif
	
	public ForgePluginPacketSender() { super(true); }
	
	public void setPacketHandler(Consumer<AbstractNetworkMessage> consumer)
	{
		this.setPacketHandler((player, message) -> consumer.accept(message));
	}
	public void setPacketHandler(BiConsumer<IServerPlayerWrapper, AbstractNetworkMessage> consumer)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.messageBuilder(MessageWrapper.class, 0)
				.encoder((wrapper, out) -> this.encodeMessage(out, wrapper.message))
				.decoder(in -> new MessageWrapper(this.decodeMessage(in)))
				.consumerNetworkThread((wrapper, context) ->
				{
					if (wrapper.message != null)
					{
						if (context.getSender() != null)
						{
							consumer.accept(ServerPlayerWrapper.getWrapper(context.getSender()), wrapper.message);
						}
						else
						{
							consumer.accept(null, wrapper.message);
						}
					}
					context.setPacketHandled(true);
				})
				.add();
		#else // < 1.20.2
		PLUGIN_CHANNEL.registerMessage(0, MessageWrapper.class,
				(wrapper, out) -> this.encodeMessage(out, wrapper.message),
				in -> new MessageWrapper(this.decodeMessage(in)),
				(wrapper, context) ->
				{
					if (wrapper.message != null)
					{
						if (context.get().getSender() != null)
						{
							consumer.accept(ServerPlayerWrapper.getWrapper(context.get().getSender()), wrapper.message);
						}
						else
						{
							consumer.accept(null, wrapper.message);
						}
					}
					context.get().setPacketHandled(true);
				}
		);
		#endif
	}
	
	@Override
	public void sendToServer(AbstractNetworkMessage message)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.send(new MessageWrapper(message), PacketDistributor.SERVER.noArg());
		#else // < 1.20.2
		PLUGIN_CHANNEL.send(PacketDistributor.SERVER.noArg(), new MessageWrapper(message));
		#endif
	}
	
	@Override
	public void sendToClient(ServerPlayer serverPlayer, AbstractNetworkMessage message)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.send(new MessageWrapper(message), PacketDistributor.PLAYER.with(serverPlayer));
		#else // < 1.20.2
		PLUGIN_CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new MessageWrapper(message));
		#endif
	}
	
	// Forge doesn't support using abstract classes
	@SuppressWarnings({"ClassCanBeRecord", "RedundantSuppression"})
	public static class MessageWrapper
	{
		public final AbstractNetworkMessage message;
		
		public MessageWrapper(AbstractNetworkMessage message) { this.message = message; }
		
	}
	
}