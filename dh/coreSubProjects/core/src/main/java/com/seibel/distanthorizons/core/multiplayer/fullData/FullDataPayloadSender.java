package com.seibel.distanthorizons.core.multiplayer.fullData;

import com.seibel.distanthorizons.core.network.messages.fullData.FullDataSplitMessage;
import com.seibel.distanthorizons.core.network.session.NetworkSession;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.TimerUtil;
import io.netty.buffer.ByteBuf;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.*;

public class FullDataPayloadSender implements AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final int TICK_RATE = 20;
	
	/** 1 Mebibyte minus 576 bytes for other info */
	public static final int FULL_DATA_SPLIT_SIZE_IN_BYTES = 1_048_000;
	
	/** max number of queued real-time updates before the oldest unsent one is dropped */
	public static final int MAX_PENDING_REALTIME_UPDATES = 512;
	/** how often the dropped real-time update warning can be logged */
	private static final long MIN_MS_BETWEEN_REALTIME_DROP_LOGS = 30_000;
	
	
	private static final Timer UPLOAD_TIMER = TimerUtil.CreateTimer("FullDataPayloadSender");
	private final TimerTask tickTimerTask = TimerUtil.createTimerTask(this::tick);
	
	private volatile boolean closed;
	
	private final NetworkSession session;
	private final IntSupplier maxKBpsSupplier;
	private final ConcurrentLinkedQueue<PendingTransfer> transferQueue = new ConcurrentLinkedQueue<>();
	
	private final SharedBandwidthLimit sharedBandwidthLimit;
	
	private long lastRealTimeDropLogMs = 0;
	
	
	public FullDataPayloadSender(NetworkSession session, IntSupplier maxKBpsSupplier, SharedBandwidthLimit sharedBandwidthLimit)
	{
		this.session = session;
		this.maxKBpsSupplier = maxKBpsSupplier;
		this.sharedBandwidthLimit = sharedBandwidthLimit;
		UPLOAD_TIMER.scheduleAtFixedRate(this.tickTimerTask, 0, 1000 / TICK_RATE);
	}
	
	@Override
	public void close()
	{
		this.closed = true;
		this.tickTimerTask.cancel();
		this.sharedBandwidthLimit.setSenderActive(this, false);
		
		// payload buffers are shared between every player the payload is sent to
		// and are unpooled heap buffers, so they're dropped instead of released
		synchronized (this.transferQueue)
		{
			this.transferQueue.clear();
		}
	}
	
	
	public void sendInChunks(FullDataPayload payload, Runnable sendFinalMessage)
	{ this.enqueueTransfer(payload, sendFinalMessage, false); }
	
	public void sendRealTimeUpdate(FullDataPayload payload, Runnable sendFinalMessage)
	{ this.enqueueTransfer(payload, sendFinalMessage, true); }
	
	private void enqueueTransfer(FullDataPayload payload, Runnable sendFinalMessage, boolean realTime)
	{
		synchronized (this.transferQueue)
		{
			if (this.closed)
			{
				return;
			}
			
			if (realTime)
			{
				int realTimeCount = 0;
				PendingTransfer oldestUnsentRealTime = null;
				for (PendingTransfer pendingTransfer : this.transferQueue)
				{
					if (!pendingTransfer.realTime)
					{
						continue;
					}
					
					realTimeCount++;
					if (oldestUnsentRealTime == null 
						&& pendingTransfer.buffer.readerIndex() == 0)
					{
						oldestUnsentRealTime = pendingTransfer;
					}
				}
				
				if (realTimeCount >= MAX_PENDING_REALTIME_UPDATES
					&& oldestUnsentRealTime != null
					&& this.transferQueue.remove(oldestUnsentRealTime))
				{
					this.logDroppedRealTimeUpdate();
				}
			}
			
			this.transferQueue.add(new PendingTransfer(payload, sendFinalMessage, realTime));
		}
	}
	
	private void logDroppedRealTimeUpdate()
	{
		long now = System.currentTimeMillis();
		if (now - this.lastRealTimeDropLogMs < MIN_MS_BETWEEN_REALTIME_DROP_LOGS)
		{
			return;
		}
		
		this.lastRealTimeDropLogMs = now;
		LOGGER.warn("Dropped a real-time LOD update, too many updates are queued. Max queued real-time updates: ["+MAX_PENDING_REALTIME_UPDATES+"].");
	}
	
	private void tick()
	{
		int bandwidthShare = this.sharedBandwidthLimit.getBandwidthShare();
		int maxPlayerRate = Math.min(this.maxKBpsSupplier.getAsInt(), bandwidthShare);
		
		// + 1 to account for rounding errors on values of < 4
		int bytesToSend = maxPlayerRate > 0
				? (maxPlayerRate * 1000) / TICK_RATE + 1
				: Integer.MAX_VALUE;
		
		this.sharedBandwidthLimit.setSenderActive(this, !this.transferQueue.isEmpty());
		
		while (bytesToSend > 0)
		{
			PendingTransfer pendingTransfer;
			FullDataSplitMessage chunkMessage;
			int chunkSize;
			boolean transferFinished;

			// synchronized so a transfer can't be dropped (see enqueueTransfer) while it's partially sent
			synchronized (this.transferQueue)
			{
				pendingTransfer = this.transferQueue.peek();
				if (pendingTransfer == null)
				{
					return;
				}

				chunkSize = Math.min(Math.min(bytesToSend, FULL_DATA_SPLIT_SIZE_IN_BYTES), pendingTransfer.buffer.readableBytes());
				boolean isFirstChunk = pendingTransfer.buffer.readerIndex() == 0;
				chunkMessage = new FullDataSplitMessage(pendingTransfer.bufferId, pendingTransfer.buffer.readSlice(chunkSize).retain(), isFirstChunk);

				transferFinished = (pendingTransfer.buffer.readableBytes() == 0);
				if (transferFinished)
				{
					this.transferQueue.poll();
				}
			}

			this.session.sendMessage(chunkMessage);
			bytesToSend -= chunkSize;

			if (transferFinished)
			{
				try
				{
					pendingTransfer.sendFinalMessage.run();
				}
				catch (Throwable e)
				{
					LOGGER.error("Failed to send the completed full data transfer", e);
				}
			}
		}
	}
	
	
	private static class PendingTransfer
	{
		public final int bufferId;
		public final ByteBuf buffer;
		public final Runnable sendFinalMessage;
		public final boolean realTime;
		
		private PendingTransfer(FullDataPayload payload, Runnable sendFinalMessage, boolean realTime)
		{
			this.bufferId = payload.dtoBufferId;
			this.buffer = payload.dtoBuffer.duplicate().readerIndex(0);
			this.sendFinalMessage = sendFinalMessage;
			this.realTime = realTime;
		}
		
	}
	
}
