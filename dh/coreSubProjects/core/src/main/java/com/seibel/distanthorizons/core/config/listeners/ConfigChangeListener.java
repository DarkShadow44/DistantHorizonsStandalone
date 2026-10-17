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

package com.seibel.distanthorizons.core.config.listeners;

import com.seibel.distanthorizons.core.config.types.ConfigEntry;

import java.io.Closeable;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A basic {@link IConfigListener} that will fire a {@link Consumer}
 * when the value changes from the value the config started with
 * when this object was created.
 *
 * @param <T> the config value type
 */
public class ConfigChangeListener<T> implements IConfigListener, Closeable
{
	private final ConfigEntry<T> configEntry;
	private final OnChangeFunc<T> onChangeFunc;
	
	private T previousTrueValue;
	private T previousApiValue;
	
	
	
	public ConfigChangeListener(ConfigEntry<T> configEntry, OnChangeFunc<T> onChangeFunc)
	{
		this.configEntry = configEntry;
		this.onChangeFunc = onChangeFunc;
		
		this.configEntry.addListener(this);
		this.previousTrueValue = this.configEntry.get();
	}
	
	
	@Override
	public void onConfigValueSet()
	{
		// fire the event if either the API or True value was changed
		T newTrueValue = this.configEntry.getTrueValue();
		T newApiValue = this.configEntry.getApiValue();
		if (!Objects.equals(this.previousTrueValue, newTrueValue)
			|| !Objects.equals(this.previousApiValue, newApiValue))
		{
			this.previousTrueValue = newTrueValue;
			this.previousApiValue = newApiValue;
			
			this.onChangeFunc.OnConfigValueChanged(this.configEntry);
		}
	}
	
	@Override
	public void onUiModify() { /* do nothing, we only care about when the actual value is modified */ }
	
	
	
	/**
	 * Removes the config event listener. <br>
	 * Must be fired to prevent memory leaks.
	 */
	@Override
	public void close() { this.configEntry.removeListener(this); }
	
	
	
	//================//
	// helper classes //
	//================//
	//region
	
	@FunctionalInterface
	public interface OnChangeFunc<T>
	{
		void OnConfigValueChanged(ConfigEntry<T> entry);
	}
	
	//endregion
	
	
	
}
