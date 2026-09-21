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

import com.seibel.distanthorizons.coreapi.DependencyInjection.ApiEventInjector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import testItems.events.abstractObjects.AbstractDhApiCancelableOneTimeTestEvent;
import testItems.events.abstractObjects.AbstractDhApiRemoveAfterFireTestEvent;
import testItems.events.objects.*;
import testItems.events.abstractObjects.AbstractDhApiTestEvent;

import java.util.ArrayList;


/**
 * @author James Seibel
 * @version 2023-6-23
 */
public class EventInjectorTest
{
	@Before
	public void testSetup()
	{
		// reset the injectors
		ApiEventInjector.INSTANCE.clear();
	}
	
	@Test
	public void testGeneralAndRecurringEvents() // this also tests list dependencies since there can be more than one event handler bound per event
	{
		// Injector setup
		ApiEventInjector TEST_EVENT_HANDLER = ApiEventInjector.INSTANCE;
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_EVENT_HANDLER.get(AbstractDhApiTestEvent.class));
		
		
		// dependency setup
		TEST_EVENT_HANDLER.bind(AbstractDhApiTestEvent.class, new DhTestEventHandler());
		TEST_EVENT_HANDLER.bind(AbstractDhApiTestEvent.class, new DhTestEventHandlerAlt());
		TEST_EVENT_HANDLER.runDelayedSetup();
		
		
		// get first
		AbstractDhApiTestEvent afterRenderEvent = TEST_EVENT_HANDLER.get(AbstractDhApiTestEvent.class);
		Assert.assertNotNull("Event not bound.", afterRenderEvent);
		
		
		// get list
		ArrayList<AbstractDhApiTestEvent> afterRenderEventList = TEST_EVENT_HANDLER.getAll(AbstractDhApiTestEvent.class);
		Assert.assertEquals("Bound list doesn't contain the correct number of items.", 2, afterRenderEventList.size());
		// object one
		Assert.assertNotNull("Event not bound.", afterRenderEventList.get(0));
		Assert.assertEquals("First event object setup incorrectly.", null, afterRenderEventList.get(0).getTestValue());
		// object two
		Assert.assertNotNull("Event not bound.", afterRenderEventList.get(1));
		Assert.assertEquals("First event object setup incorrectly.", null, afterRenderEventList.get(1).getTestValue());
		
		
		// event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", false, TEST_EVENT_HANDLER.fireAllEvents(AbstractDhApiTestEvent.class, true));
		// object one
		Assert.assertEquals("Event not fired for first object.", true, afterRenderEventList.get(0).getTestValue());
		// object two
		Assert.assertEquals("Event not fired for second object.", true, afterRenderEventList.get(1).getTestValue());
		
		
		// unbind
		AbstractDhApiTestEvent unboundEvent = afterRenderEventList.get(0);
		Assert.assertTrue("Unbind should've removed item.", TEST_EVENT_HANDLER.unbind(AbstractDhApiTestEvent.class, DhTestEventHandler.class));
		Assert.assertFalse("Unbind should've already removed item.", TEST_EVENT_HANDLER.unbind(AbstractDhApiTestEvent.class, DhTestEventHandler.class));
		
		// check unbinding
		afterRenderEventList = TEST_EVENT_HANDLER.getAll(AbstractDhApiTestEvent.class);
		Assert.assertEquals("Unbound list doesn't contain the correct number of items.", 1, afterRenderEventList.size());
		Assert.assertNotNull("Unbinding removed all items.", afterRenderEventList.get(0));
		
		
		// check unbound event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", false, TEST_EVENT_HANDLER.fireAllEvents(AbstractDhApiTestEvent.class, false));
		// remaining event
		Assert.assertEquals("Event not fired for remaining object.", false, ((DhTestEventHandlerAlt) afterRenderEventList.get(0)).eventFiredValue);
		// unbound event
		Assert.assertEquals("Event fired for unbound object.", true, unboundEvent.getTestValue());
		
		
		// prevent event handlers from being bound to the wrong event interface
		Assert.assertThrows("Event bound to a non-implementing interface.", IllegalArgumentException.class, () -> { TEST_EVENT_HANDLER.bind(AbstractDhApiCancelableOneTimeTestEvent.class, new DhTestEventHandler()); });
		Assert.assertThrows("Event bound to a non-implementing interface.", IllegalArgumentException.class, () -> { TEST_EVENT_HANDLER.bind(AbstractDhApiTestEvent.class, new DhCancelableOneTimeTestEventHandler()); });
		
	}
	
	
	@Test
	public void oneTimeEventTestPreFireBinding() { this.oneTimeEventTest(false); }
	@Test
	public void oneTimeEventTestPostFireBinding() { this.oneTimeEventTest(true); }
	
	public void oneTimeEventTest(boolean bindEventBeforeOneTimeFiring)
	{
		// Injector setup
		ApiEventInjector TEST_EVENT_HANDLER = ApiEventInjector.INSTANCE;
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_EVENT_HANDLER.get(AbstractDhApiCancelableOneTimeTestEvent.class));
		
		
		if (bindEventBeforeOneTimeFiring)
		{
			TEST_EVENT_HANDLER.bind(AbstractDhApiCancelableOneTimeTestEvent.class, new DhCancelableOneTimeTestEventHandler());
		}
		TEST_EVENT_HANDLER.runDelayedSetup();
		
		
		// pre-bound event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", bindEventBeforeOneTimeFiring, TEST_EVENT_HANDLER.fireAllEvents(AbstractDhApiCancelableOneTimeTestEvent.class, true));
		
		// validate pre-bound events fired correctly
		ArrayList<AbstractDhApiCancelableOneTimeTestEvent> oneTimeEventList;
		if (bindEventBeforeOneTimeFiring)
		{
			oneTimeEventList = TEST_EVENT_HANDLER.getAll(AbstractDhApiCancelableOneTimeTestEvent.class);
			Assert.assertEquals("Event not fired for pre-fire object.", true, oneTimeEventList.get(0).getTestValue());
		}
		
		
		// post-event fire binding
		// the event should fire instantly
		TEST_EVENT_HANDLER.bind(AbstractDhApiCancelableOneTimeTestEvent.class, new DhCancelableOneTimeTestEventHandlerAlt());
		// validate both events have fired
		oneTimeEventList = TEST_EVENT_HANDLER.getAll(AbstractDhApiCancelableOneTimeTestEvent.class);
		for (int i = 0; i < oneTimeEventList.size(); i++)
		{
			Assert.assertEquals("Event not fired for object [" + i + "].", true, oneTimeEventList.get(i).getTestValue());
		}
		
		
		
		// recurring event test
		TEST_EVENT_HANDLER.bind(AbstractDhApiTestEvent.class, new DhTestEventHandler());
		ArrayList<AbstractDhApiTestEvent> recurringEventList = TEST_EVENT_HANDLER.getAll(AbstractDhApiTestEvent.class);
		Assert.assertNull("This unrelated recurring event shouldn't have been fired.", recurringEventList.get(0).getTestValue());
		
	}
	
	
	@Test
	public void testRemoveAfterFireEvents()
	{
		// Injector setup
		ApiEventInjector TEST_EVENT_HANDLER = ApiEventInjector.INSTANCE;
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_EVENT_HANDLER.get(AbstractDhApiRemoveAfterFireTestEvent.class));
		
		
		// dependency setup
		TEST_EVENT_HANDLER.bind(AbstractDhApiRemoveAfterFireTestEvent.class, new DhRemoveAfterFireTestEventHandler());
		TEST_EVENT_HANDLER.runDelayedSetup();
		
		
		// get first
		AbstractDhApiRemoveAfterFireTestEvent event = TEST_EVENT_HANDLER.get(AbstractDhApiRemoveAfterFireTestEvent.class);
		Assert.assertNotNull("Event not bound.", event);
		
		
		// get list
		ArrayList<AbstractDhApiRemoveAfterFireTestEvent> eventList = TEST_EVENT_HANDLER.getAll(AbstractDhApiRemoveAfterFireTestEvent.class);
		Assert.assertEquals("Bound list doesn't contain the correct number of items.", 1, eventList.size());
		// object one
		Assert.assertNotNull("Event not bound.", eventList.get(0));
		Assert.assertEquals("First event object setup incorrectly.", null, eventList.get(0).getTestValue());
		
		
		// event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", false, TEST_EVENT_HANDLER.fireAllEvents(AbstractDhApiRemoveAfterFireTestEvent.class, true));
		// object one
		Assert.assertEquals("Event not fired for first object.", true, event.getTestValue());
		
		
		// check that the event was automatically unbound
		eventList = TEST_EVENT_HANDLER.getAll(AbstractDhApiRemoveAfterFireTestEvent.class);
		Assert.assertEquals("Unbound list doesn't contain the correct number of items.", 1, eventList.size());
		Assert.assertNull("Event wasn't automatically unbound after firing.", eventList.get(0));
		
	}
	
}



