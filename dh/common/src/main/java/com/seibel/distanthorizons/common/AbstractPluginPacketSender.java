package com.seibel.distanthorizons.common;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.event.internal.IncompatibleMessageInternalEvent;
import com.seibel.distanthorizons.core.network.event.internal.ProtocolErrorInternalEvent;
import com.seibel.distanthorizons.core.network.messages.MessageRegistry;
import com.seibel.distanthorizons.core.network.messages.AbstractNetworkMessage;
import com.seibel.distanthorizons.core.network.messages.base.CloseReasonMessage;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBufUtil;
#if MC_VER <= MC_1_12_2
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
#else
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
#endif

#if MC_VER <= MC_1_12_2
import net.minecraft.util.ResourceLocation;
#elif MC_VER <= MC_1_21_10
import net.minecraft.resources.ResourceLocation;
#else
import net.minecraft.resources.Identifier;
#endif

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("deprecation") // ResourceLocation is deprecated on some MC versions
public abstract class AbstractPluginPacketSender implements IPluginPacketSender
{
	private static final DhLogger LOGGER = new DhLoggerBuilder()
			.fileLevelConfig(Config.Common.Logging.logNetworkEventToFile)
			.build();
	
	#if MC_VER <= MC_1_12_2
	public static final String WRAPPER_PACKET_RESOURCE = ModInfo.RESOURCE_NAMESPACE + ModInfo.WRAPPER_PACKET_PATH;
	#elif MC_VER <= MC_1_20_6
	public static final ResourceLocation WRAPPER_PACKET_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.WRAPPER_PACKET_PATH);
	#elif  MC_VER <= MC_1_21_10
	public static final ResourceLocation WRAPPER_PACKET_RESOURCE = ResourceLocation.fromNamespaceAndPath(ModInfo.RESOURCE_NAMESPACE, ModInfo.WRAPPER_PACKET_PATH);
	#else
	public static final Identifier WRAPPER_PACKET_RESOURCE = Identifier.fromNamespaceAndPath(ModInfo.RESOURCE_NAMESPACE, ModInfo.WRAPPER_PACKET_PATH);
	#endif
	
	// "Forge byte" is an unused packet ID. We have our own system which works with all mod loaders,
	// so we're just accounting for it by reading the protocol version as a byte instead of a short in Forge, to keep cross-loader compatibility
	private final boolean forgeByteInProtocolVersion;
	
	
	public AbstractPluginPacketSender() { this(false); }
	public AbstractPluginPacketSender(boolean forgeByteInProtocolVersion)
	{
		this.forgeByteInProtocolVersion = forgeByteInProtocolVersion;
	}
	
	@Override
	public final void sendToClient(IServerPlayerWrapper serverPlayer, AbstractNetworkMessage message)
	{
		#if MC_VER <= MC_1_12_2
		this.sendToClient((EntityPlayerMP) serverPlayer.getWrappedMcObject(), message);
		#else
		this.sendToClient((ServerPlayer) serverPlayer.getWrappedMcObject(), message);
		#endif
	}
	
	#if MC_VER <= MC_1_12_2
	public abstract void sendToClient(EntityPlayerMP serverPlayer, AbstractNetworkMessage message);
	#else
	public abstract void sendToClient(ServerPlayer serverPlayer, AbstractNetworkMessage message);
	#endif
	
	
	@Override
	public abstract void sendToServer(AbstractNetworkMessage message);
	
	#if MC_VER <= MC_1_12_2
	public AbstractNetworkMessage decodeMessage(PacketBuffer in)
	#else
	public AbstractNetworkMessage decodeMessage(FriendlyByteBuf in)
	#endif
	{
		AbstractNetworkMessage message = null;
		
		try
		{
			in.markReaderIndex();
			
			int protocolVersion = this.forgeByteInProtocolVersion ? in.readByte() : in.readShort();
			if (protocolVersion != ModInfo.PROTOCOL_VERSION)
			{
				return new IncompatibleMessageInternalEvent(protocolVersion);
			}
			
			message = MessageRegistry.INSTANCE.createMessage(in.readUnsignedShort());
			message.decode(in);
			
			if (in.isReadable())
			{
				throw new IOException("Buffer has not been fully read");
			}
			
			return message;
		}
		catch (Exception e)
		{
			in.resetReaderIndex();
			
			LOGGER.error("Failed to decode message", e);
			LOGGER.error("Buffer: ["+in+"]");
			LOGGER.error("Buffer contents: ["+ByteBufUtil.hexDump(in)+"]");
			
			return new ProtocolErrorInternalEvent(e, message, true);
		}
		finally
		{
			// Prevent connection crashing if not entire buffer has been read
			in.readerIndex(in.writerIndex());
		}
	}
	
	#if MC_VER <= MC_1_12_2
	public void encodeMessage(PacketBuffer out, AbstractNetworkMessage message) 
	#else 
	public void encodeMessage(FriendlyByteBuf out, AbstractNetworkMessage message)
	#endif
	{
		// This is intentionally unhandled, because errors related to this are unlikely to appear in wild
		Objects.requireNonNull(message);
		
		if (this.forgeByteInProtocolVersion)
		{
			out.writeByte(ModInfo.PROTOCOL_VERSION);
		}
		else
		{
			out.writeShort(ModInfo.PROTOCOL_VERSION);
		}
		
		try
		{
			out.markWriterIndex();
			out.writeShort(MessageRegistry.INSTANCE.getMessageId(message));
			message.encode(out);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to encode message", e);
			LOGGER.error("Message: ["+message+"]");
			
			message.getSession().tryHandleMessage(new ProtocolErrorInternalEvent(e, message, false));
			
			// Encode close reason message instead
			out.resetWriterIndex();
			message = new CloseReasonMessage("Internal error on other side");
			out.writeShort(MessageRegistry.INSTANCE.getMessageId(message));
			message.encode(out);
		}
	}
	
}