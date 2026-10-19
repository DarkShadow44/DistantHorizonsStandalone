/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package tests;

import com.seibel.distanthorizons.core.multiplayer.fullData.FullDataPayloadSender;
import com.seibel.distanthorizons.core.multiplayer.fullData.SharedBandwidthLimit;
import com.seibel.distanthorizons.core.network.messages.base.CodecCrashMessage;
import com.seibel.distanthorizons.core.network.session.NetworkSession;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Regression tests for server-side per-player cleanup.
 *
 * @see NetworkSession#unregisterHandler
 * @see FullDataPayloadSender
 */
public class ServerSessionCleanupTest
{

	@Test
	public void unregisteredHandlerIsNoLongerCalled()
	{
		NetworkSession session = new NetworkSession(null);

		AtomicInteger removedHandlerCount = new AtomicInteger();
		AtomicInteger keptHandlerCount = new AtomicInteger();
		Consumer<CodecCrashMessage> removedHandler = (msg) -> removedHandlerCount.incrementAndGet();
		Consumer<CodecCrashMessage> keptHandler = (msg) -> keptHandlerCount.incrementAndGet();

		session.registerHandler(CodecCrashMessage.class, removedHandler);
		session.registerHandler(CodecCrashMessage.class, keptHandler);

		session.tryHandleMessage(new CodecCrashMessage());
		Assert.assertEquals(1, removedHandlerCount.get());
		Assert.assertEquals(1, keptHandlerCount.get());

		session.unregisterHandler(CodecCrashMessage.class, removedHandler);
		session.tryHandleMessage(new CodecCrashMessage());
		Assert.assertEquals("unregistered handler must not receive messages", 1, removedHandlerCount.get());
		Assert.assertEquals("other handlers must keep working", 2, keptHandlerCount.get());

		// re-registering after an unregister must work (the handler set is kept)
		session.registerHandler(CodecCrashMessage.class, removedHandler);
		session.tryHandleMessage(new CodecCrashMessage());
		Assert.assertEquals(2, removedHandlerCount.get());

	}

	@Test
	public void idleAndClosedSendersDontHoldBandwidthShare() throws Exception
	{
		NetworkSession session = new NetworkSession(null);
		SharedBandwidthLimit sharedBandwidthLimit = new SharedBandwidthLimit();
		Set<?> senders = getSenders(sharedBandwidthLimit);

		FullDataPayloadSender sender = new FullDataPayloadSender(session, () -> 500, sharedBandwidthLimit);
		try
		{
			// let the sender tick a few times with nothing queued
			Thread.sleep(250);
			Assert.assertFalse("a sender with nothing queued must not take a bandwidth share", senders.contains(sender));

			sharedBandwidthLimit.setSenderActive(sender, true);
			sender.close();
			Assert.assertFalse("a closed sender must release its bandwidth share", senders.contains(sender));
		}
		finally
		{
			sender.close();
		}
	}

	private static Set<?> getSenders(SharedBandwidthLimit sharedBandwidthLimit) throws Exception
	{
		Field sendersField = SharedBandwidthLimit.class.getDeclaredField("senders");
		sendersField.setAccessible(true);
		return (Set<?>) sendersField.get(sharedBandwidthLimit);
	}

}
