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

package com.seibel.distanthorizons.coreapi.DependencyInjection;

import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;
import com.seibel.distanthorizons.coreapi.util.StringUtil;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class takes care of dependency injection for world generators. <Br>
 * This is done so other mods can override our world generator(s) to improve or replace them.
 *
 * @author James Seibel
 * @version 2022-12-10
 */
public class WorldGeneratorInjector
{
	public static final WorldGeneratorInjector INSTANCE = new WorldGeneratorInjector();
	
	/** Concurrent since each level's request thread can bind generators at the same time. */
	private final ConcurrentHashMap<IDhApiLevelWrapper, OverrideInjector> worldGeneratorByLevelWrapper = new ConcurrentHashMap<>();
	
	/**
	 * This is used to determine if an override is part of Distant Horizons'
	 * Core or not.
	 * This probably isn't the best way of going about this, but it works for now.
	 */
	private final String corePackagePath;
	
	
	
	//==============//
	// constructors //
	//==============//
	//region
	
	public WorldGeneratorInjector()
	{
		String thisPackageName = this.getClass().getPackage().getName();
		int secondPackageEndingIndex = StringUtil.nthIndexOf(thisPackageName, ".", 3);
		this.corePackagePath = thisPackageName.substring(0, secondPackageEndingIndex); // this should be "com.seibel.distanthorizons"
	}
	
	/** This constructor should only be used for testing different corePackagePaths. */
	public WorldGeneratorInjector(String newCorePackagePath)
	{ this.corePackagePath = newCorePackagePath; }
	
	//endregion
	
	
	
	//=========//
	// binding //
	//=========//
	//region
	
	/**
	 * Binds a world generator to the given level. <Br>
	 * See {@link DependencyInjector#bind(Class, IBindable) bind(Class, IBindable)} for full documentation.
	 *
	 * @throws NullPointerException if any parameter is null
	 * @throws IllegalArgumentException if a non-Distant Horizons world generator with the priority CORE is passed in
	 * @see DependencyInjector#bind(Class, IBindable)
	 */
	public void bind(IDhApiLevelWrapper levelForWorldGenerator, IDhApiWorldGenerator worldGeneratorImplementation) throws NullPointerException, IllegalArgumentException
	{
		// validate inputs
		if (levelForWorldGenerator == null)
		{
			throw new NullPointerException("A [" + IDhApiLevelWrapper.class.getSimpleName() + "] is required when binding a world generator.");
		}
		
		if (worldGeneratorImplementation == null)
		{
			throw new NullPointerException("No [" + IDhApiWorldGenerator.class.getSimpleName() + "] given.");
		}
		
		
		// bind this generator to the given level
		this.worldGeneratorByLevelWrapper
			.computeIfAbsent(levelForWorldGenerator, (IDhApiLevelWrapper level) -> new OverrideInjector(this.corePackagePath))
			.bind(IDhApiWorldGenerator.class, worldGeneratorImplementation);
		
	}
	
	/** Removes every world generator bound to a given level. */
	public void unbind(IDhApiLevelWrapper levelForWorldGenerator)
	{
		if (levelForWorldGenerator != null)
		{
			this.worldGeneratorByLevelWrapper.remove(levelForWorldGenerator);
		}
	}
	
	/** Removes all bound world generators. */
	public void clear() { this.worldGeneratorByLevelWrapper.clear(); }
	
	//endregion
	
	
	
	//=========//
	// getting //
	//=========//
	//region
	
	/**
	 * Returns the bound world generator with the highest priority. <br>
	 * Returns null if no world generators have been bound for this specific level. <br><br>
	 *
	 * See {@link OverrideInjector#get(Class) get(Class)} for full documentation.
	 *
	 * @see OverrideInjector#get(Class)
	 */
	public IDhApiWorldGenerator get(IDhApiLevelWrapper levelForWorldGenerator) throws ClassCastException
	{
		OverrideInjector injector = this.worldGeneratorByLevelWrapper.get(levelForWorldGenerator);
		if (injector == null)
		
		{
			// no generator exists for this specific level.
			return null;
		}
		
		// use the existing world generator
		return injector.get(IDhApiWorldGenerator.class);
	}
	
	/** Used for unit tests */
	public int boundLevelCount() { return this.worldGeneratorByLevelWrapper.size(); }
	
	//endregion
	
	
	
}
