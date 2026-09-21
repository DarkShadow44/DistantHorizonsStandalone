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

import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.coreapi.DependencyInjection.DependencyInjector;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;
import com.seibel.distanthorizons.coreapi.DependencyInjection.OverrideInjector;
import com.seibel.distanthorizons.coreapi.DependencyInjection.WorldGeneratorInjector;

import org.junit.Assert;
import org.junit.Test;
import testItems.overrideInjection.interfaces.IOverrideTest;
import testItems.overrideInjection.objects.OverrideTestAssembly;
import testItems.overrideInjection.objects.OverrideTestCore;
import testItems.overrideInjection.objects.OverrideTestPrimary;
import testItems.singletonInjection.interfaces.ISingletonTestOne;
import testItems.singletonInjection.interfaces.ISingletonTestTwo;
import testItems.singletonInjection.objects.ConcreteSingletonTestBoth;
import testItems.singletonInjection.objects.ConcreteSingletonTestOne;
import testItems.singletonInjection.objects.ConcreteSingletonTestTwo;
import testItems.worldGeneratorInjection.objects.*;


/**
 * @author James Seibel
 * @version 2022-12-10
 */
public class DependencyInjectorTest
{
	
	@Test
	public void testSingleImplementationSingleton()
	{
		// Injector setup
		DependencyInjector<IBindable> TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		
		
		// pre-dependency setup
		Assert.assertNull(ISingletonTestOne.class.getSimpleName() + " should not have been bound.", TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class));
		
		
		// dependency setup
		TEST_SINGLETON_HANDLER.bind(ISingletonTestOne.class, new ConcreteSingletonTestOne(TEST_SINGLETON_HANDLER));
		TEST_SINGLETON_HANDLER.bind(ISingletonTestTwo.class, new ConcreteSingletonTestTwo(TEST_SINGLETON_HANDLER));
		
		TEST_SINGLETON_HANDLER.runDelayedSetup();
		
		
		// basic dependencies
		ISingletonTestOne testInterOne = TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class);
		Assert.assertNotNull(ISingletonTestOne.class.getSimpleName() + " not bound.", testInterOne);
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getValue(), ConcreteSingletonTestOne.VALUE);
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class).getValue(), ConcreteSingletonTestOne.VALUE);
		
		ISingletonTestTwo testInterTwo = TEST_SINGLETON_HANDLER.get(ISingletonTestTwo.class);
		Assert.assertNotNull(ISingletonTestTwo.class.getSimpleName() + " not bound.", testInterTwo);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getValue(), ConcreteSingletonTestTwo.VALUE);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", TEST_SINGLETON_HANDLER.get(ISingletonTestTwo.class).getValue(), ConcreteSingletonTestTwo.VALUE);
		
		
		// circular dependencies (if this throws an exception the dependency isn't set up)
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getDependentValue(), ConcreteSingletonTestTwo.VALUE);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getDependentValue(), ConcreteSingletonTestOne.VALUE);
		
	}
	
	@Test
	public void testMultipleImplementationSingleton()
	{
		// Injector setup
		DependencyInjector<IBindable> TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		
		
		// pre-dependency setup
		Assert.assertNull(ISingletonTestOne.class.getSimpleName() + " should not have been bound.", TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class));
		
		
		// dependency setup
		ConcreteSingletonTestBoth concreteInstance = new ConcreteSingletonTestBoth();
		
		TEST_SINGLETON_HANDLER.bind(ISingletonTestOne.class, concreteInstance);
		TEST_SINGLETON_HANDLER.bind(ISingletonTestTwo.class, concreteInstance);
		
		
		// basic dependencies
		ISingletonTestOne testInterOne = TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class);
		Assert.assertNotNull(ISingletonTestOne.class.getSimpleName() + " not bound.", testInterOne);
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getValue(), ConcreteSingletonTestBoth.VALUE);
		
		ISingletonTestTwo testInterTwo = TEST_SINGLETON_HANDLER.get(ISingletonTestTwo.class);
		Assert.assertNotNull(ISingletonTestTwo.class.getSimpleName() + " not bound.", testInterTwo);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getValue(), ConcreteSingletonTestBoth.VALUE);
		
	}
	
	@Test
	public void testOverrideInjection()
	{
		OverrideInjector TEST_INJECTOR = new OverrideInjector(OverrideTestAssembly.getPackagePath(2));
		OverrideInjector CORE_INJECTOR = new OverrideInjector();
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNull("Nothing should have been bound.", CORE_INJECTOR.get(IOverrideTest.class));
		
		
		// variables to use later
		IOverrideTest override;
		OverrideTestCore coreOverride = new OverrideTestCore();
		OverrideTestPrimary primaryOverride = new OverrideTestPrimary();
		
		
		// core override binding
		try
		{
			TEST_INJECTOR.bind(IOverrideTest.class, coreOverride);
		}
		catch (IllegalArgumentException e)
		{
			Assert.fail("Core override should be bindable for test package injector.");
		}
		
		try
		{
			CORE_INJECTOR.bind(IOverrideTest.class, coreOverride);
			Assert.fail("Core override should not be bindable for core package injector.");
		}
		catch (IllegalArgumentException e)
		{ /* this exception should be thrown */ }
		
		
		// core override
		Assert.assertNotNull("Test injector should've bound core override.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNull("Core injector should not have bound core override.", CORE_INJECTOR.get(IOverrideTest.class));
		// priority gets
		Assert.assertNotNull("Core override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, OverrideInjector.CORE_PRIORITY));
		Assert.assertNull("Non-core override should not be bound yet.", TEST_INJECTOR.get(IOverrideTest.class, OverrideTestPrimary.PRIORITY));
		// standard get
		override = TEST_INJECTOR.get(IOverrideTest.class);
		Assert.assertEquals("Override returned incorrect override type.", override.getPriority(), OverrideInjector.CORE_PRIORITY);
		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestCore.VALUE);
		
		
		// default override
		TEST_INJECTOR.bind(IOverrideTest.class, primaryOverride);
		// priority gets
		Assert.assertNotNull("Test injector should've bound secondary override.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNotNull("Core override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, OverrideInjector.CORE_PRIORITY));
		Assert.assertNotNull("Secondary override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, OverrideTestPrimary.PRIORITY));
		// standard get
		override = TEST_INJECTOR.get(IOverrideTest.class);
		Assert.assertEquals("Override returned incorrect override type.", override.getPriority(), OverrideTestPrimary.PRIORITY);
		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestPrimary.VALUE);
		
		
		// in-line get
		// (make sure the returned type is correct and compiles, the actual value doesn't matter)
		TEST_INJECTOR.get(IOverrideTest.class).getValue();
		
	}
	
	@Test
	public void testWorldGeneratorInjection()
	{
		WorldGeneratorInjector TEST_INJECTOR = new WorldGeneratorInjector(WorldGeneratorTestAssembly.getPackagePath(2));
		
		
		// variables to use later
		IDhApiWorldGenerator generator;
		WorldGeneratorTestCore coreLevelGenerator = new WorldGeneratorTestCore();
		WorldGeneratorTestPrimary primaryLevelGenerator = new WorldGeneratorTestPrimary();
		WorldGeneratorTestSecondary secondaryLevelGenerator = new WorldGeneratorTestSecondary();
		
		IDhApiLevelWrapper boundLevel = new LevelWrapperTest();
		IDhApiLevelWrapper unboundLevel = new LevelWrapperTest();
		
		
		// validate nothing has been bound yet
		Assert.assertNull("Nothing should have been bound yet.", TEST_INJECTOR.get(boundLevel));
		
		
		
		// bind the core generator //
		try
		{
			TEST_INJECTOR.bind(boundLevel, coreLevelGenerator);
		}
		catch (IllegalArgumentException e)
		{
			Assert.fail("[" + coreLevelGenerator.getClass().getSimpleName() + "] should be bindable for test package injector.");
		}
		
		// validate the core generator was bound
		generator = TEST_INJECTOR.get(boundLevel);
		Assert.assertNotNull("Level generator not bound.", generator);
		Assert.assertEquals("Incorrect level generator bound.", generator.getPriority(), WorldGeneratorTestCore.PRIORITY);
		Assert.assertEquals("Incorrect level generator bound.", generator.getSmallestDataDetailLevel(), WorldGeneratorTestCore.SMALLEST_DETAIL_LEVEL);
		
		// unbound level should still return null
		Assert.assertNull("Nothing should have been bound to this level.", TEST_INJECTOR.get(unboundLevel));
		
		
		
		// bind the secondary generator //
		try
		{
			TEST_INJECTOR.bind(boundLevel, secondaryLevelGenerator);
		}
		catch (IllegalArgumentException e)
		{
			Assert.fail("[" + secondaryLevelGenerator.getClass().getSimpleName() + "] should be bindable for test package injector.");
		}
		
		// validate the secondary generator overrides the core generator
		generator = TEST_INJECTOR.get(boundLevel);
		Assert.assertNotNull("Level generator not bound.", generator);
		Assert.assertEquals("Incorrect level generator bound.", generator.getPriority(), WorldGeneratorTestSecondary.PRIORITY);
		Assert.assertEquals("Incorrect level generator bound.", generator.getSmallestDataDetailLevel(), WorldGeneratorTestSecondary.SMALLEST_DETAIL_LEVEL);
		
		// the unbound level should still return null
		Assert.assertNull("Nothing should have been bound to this level.", TEST_INJECTOR.get(unboundLevel));
		
		
		
		// bind the primary generator //
		try
		{
			TEST_INJECTOR.bind(boundLevel, primaryLevelGenerator);
		}
		catch (IllegalArgumentException e)
		{
			Assert.fail("[" + primaryLevelGenerator.getClass().getSimpleName() + "] should be bindable for test package injector.");
		}
		
		// validate the primary generator overrides both the core and secondary generator
		generator = TEST_INJECTOR.get(boundLevel);
		Assert.assertNotNull("Level generator not bound.", generator);
		Assert.assertEquals("Incorrect level generator bound.", generator.getPriority(), WorldGeneratorTestPrimary.PRIORITY);
		Assert.assertEquals("Incorrect level generator bound.", generator.getSmallestDataDetailLevel(), WorldGeneratorTestPrimary.SMALLEST_DETAIL_LEVEL);
		
		// the unbound level should still return null
		Assert.assertNull("Nothing should have been bound to this level.", TEST_INJECTOR.get(unboundLevel));
		
		
	}
	
}



