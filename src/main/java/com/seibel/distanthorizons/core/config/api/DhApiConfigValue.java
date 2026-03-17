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

package com.seibel.distanthorizons.core.config.api;

import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.coreapi.interfaces.config.IConverter;
import com.seibel.distanthorizons.core.config.api.converters.DefaultConverter;

import java.util.function.Consumer;

/**
 * A wrapper used to interface with Distant Horizon's Config. <br> <br>
 *
 * When using this object you need to explicitly define the generic types,
 * otherwise Intellij won't do any type checking and the wrong types can be used. <br>
 * For example a method returning {@literal IDhApiConfig<Integer> } when the config should be a Boolean.
 *
 * @param <apiType> The datatype you, an API dev will use.
 * @param <coreType> The datatype Distant Horizons uses in the background; implementing developers can ignore this.
 * @author James Seibel
 * @version 2022-6-30
 * @since API 1.0.0
 */
public class DhApiConfigValue<coreType, apiType> implements IDhApiConfigValue<apiType>
{
	private final ConfigEntry<coreType> configBase;
	
	private final IConverter<coreType, apiType> configConverter;
	
	
	/**
	 * This constructor should only be called internally. <br>
	 * There is no reason for API users to create this object. <br><br>
	 *
	 * Uses the default object converter, this requires coreType and apiType to be the same.
	 */
	@SuppressWarnings("unchecked") // DefaultConverter's cast is safe
	public DhApiConfigValue(ConfigEntry<coreType> configBase)
	{
		this.configBase = configBase;
		this.configConverter = (IConverter<coreType, apiType>) new DefaultConverter<coreType>();
	}
	
	/**
	 * This constructor should only be called internally. <br>
	 * There is no reason for API users to create this object. <br><br>
	 */
	public DhApiConfigValue(ConfigEntry<coreType> configBase, IConverter<coreType, apiType> newConverter)
	{
		this.configBase = configBase;
		this.configConverter = newConverter;
	}
	
	
	public apiType getValue() { return this.configConverter.convertToApiType(this.configBase.get()); }
	public apiType getTrueValue() { return this.configConverter.convertToApiType(this.configBase.getTrueValue()); }
	public apiType getApiValue() { return this.configConverter.convertToApiType(this.configBase.getApiValue()); }
	
	public boolean setValue(apiType newValue)
	{
		if (this.configBase.getAllowApiOverride())
		{
			this.configBase.setApiValue(this.configConverter.convertToCoreType(newValue));
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean clearValue()
	{
		if (this.configBase.getAllowApiOverride())
		{
			// no converter should be used here since null objects may need to be handled differently
			// TODO the API should just have a bool to keep track of whether the API value is in use instead of using NULL
			this.configBase.setApiValue(null);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean getCanBeOverrodeByApi() { return this.configBase.getAllowApiOverride(); }
	
	public apiType getDefaultValue() { return this.configConverter.convertToApiType(this.configBase.getDefaultValue()); }
	public apiType getMaxValue() { return this.configConverter.convertToApiType(this.configBase.getMax()); }
	public apiType getMinValue() { return this.configConverter.convertToApiType(this.configBase.getMin()); }
	
	
	public void addChangeListener(Consumer<apiType> onValueChangeFunc) 
	{
		this.configBase.addValueChangeListener((coreValue) -> 
		{
			apiType apiValue = this.configConverter.convertToApiType(coreValue);
			onValueChangeFunc.accept(apiValue);
		}); 
	}
	
}
