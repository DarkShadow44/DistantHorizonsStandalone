package com.seibel.distanthorizons.core.multiplayer.client;

import com.seibel.distanthorizons.core.network.messages.fullData.FullDataSplitMessage;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ClientCongestionControl
{
	private static final double ADDITIVE_INCREASE = 50000;
	private static final long INTERVAL_MS = 1000;
	
	private final Runnable rateUpdateHandler;
	
	private final AtomicLong bytesReceived = new AtomicLong(0);
	
	private double desiredRate;
	private long lastAdjustTime;
	
	
	public ClientCongestionControl(
			Runnable rateUpdateHandler
	)
	{
		this.rateUpdateHandler = rateUpdateHandler;
		this.reset();
	}
	
	public void reset()
	{
		this.desiredRate = ADDITIVE_INCREASE;
		this.lastAdjustTime = System.currentTimeMillis();
		this.bytesReceived.set(0);
	}
	
	
	public void onPayloadReceived(FullDataSplitMessage message)
	{
		long now = System.currentTimeMillis();
		if (now - this.lastAdjustTime >= INTERVAL_MS)
		{
			this.adjustRate(now);
		}
		
		this.bytesReceived.addAndGet(message.buffer.readableBytes());
	}
	
	private void adjustRate(long now)
	{
		double throughput = this.bytesReceived.getAndSet(0);
		if (throughput >= this.desiredRate)
		{
			this.desiredRate += ADDITIVE_INCREASE;
		}
		else
		{
			this.desiredRate = Math.max(throughput - ADDITIVE_INCREASE / 2, 1000);
		}
		
		this.lastAdjustTime = now;
		this.rateUpdateHandler.run();
	}
	
	public int getDesiredRate()
	{
		return (int) (this.desiredRate / 1000);
	}
	
}
