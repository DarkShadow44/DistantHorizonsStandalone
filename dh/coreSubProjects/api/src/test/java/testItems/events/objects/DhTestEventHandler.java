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

package testItems.events.objects;

import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import testItems.events.abstractObjects.AbstractDhApiTestEvent;

/**
 * Dummy test event for unit tests.
 *
 * @author James Seibel
 * @version 2023-6-23
 */
public class DhTestEventHandler extends AbstractDhApiTestEvent
{
	public Boolean eventFiredValue = null;
	
	@Override
	public void onTestEvent(DhApiEventParam<Boolean> input) { this.eventFiredValue = input.value; }
	
	
	// test (non standard) methods //
	@Override
	public Boolean getTestValue() { return this.eventFiredValue; }
	
}
