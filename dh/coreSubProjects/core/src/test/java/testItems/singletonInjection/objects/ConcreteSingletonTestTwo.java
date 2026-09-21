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

package testItems.singletonInjection.objects;

import com.seibel.distanthorizons.coreapi.DependencyInjection.DependencyInjector;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;
import testItems.singletonInjection.interfaces.ISingletonTestOne;
import testItems.singletonInjection.interfaces.ISingletonTestTwo;

/**
 * Dummy test implementation object for dependency injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public class ConcreteSingletonTestTwo implements ISingletonTestTwo, IBindable
{
	private final DependencyInjector<IBindable> dependencyInjector;
	private ISingletonTestOne testInterOne;
	
	public static int VALUE = 2;
	
	
	public ConcreteSingletonTestTwo(DependencyInjector<IBindable> newDependencyInjector)
	{
		dependencyInjector = newDependencyInjector;
	}
	
	
	@Override
	public void finishDelayedSetup() { testInterOne = dependencyInjector.get(ISingletonTestOne.class, true); }
	@Override
	public boolean getDelayedSetupComplete() { return testInterOne != null; }
	
	
	@Override
	public int getValue() { return VALUE; }
	
	@Override
	public int getDependentValue() { return testInterOne.getValue(); }
	
}
