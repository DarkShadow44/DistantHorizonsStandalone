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

package com.seibel.distanthorizons.core.config.types;


import com.seibel.distanthorizons.core.config.ConfigHandler;
import com.seibel.distanthorizons.core.util.NumberUtil;
import com.seibel.distanthorizons.core.config.file.ConfigFileHandler;
import com.seibel.distanthorizons.core.config.listeners.ConfigChangeListener;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.config.types.enums.EConfigEntryAppearance;
import com.seibel.distanthorizons.core.config.types.enums.EConfigValidity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This config type allows for entering text, number, or enum values.
 *
 * @author coolGi
 */
public class ConfigEntry<T> extends AbstractConfigBase<T>
{
	private final String comment;
	private T min;
	private T max;
	private final ArrayList<IConfigListener> listenerList;
	private final String chatCommandName;
	
	/**
	 * If true this config can be controlled by the API <br>
	 * and any get() method calls will return the apiValue if it is set.
	 */
	private final boolean allowApiOverride;
	/** Will be null if un-set */
	@Nullable
	private T apiValue;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	private ConfigEntry(
			EConfigEntryAppearance appearance, 
			String comment, String chatCommandName, 
			T value, T min, T max,
			boolean allowApiOverride, 
			ArrayList<IConfigListener> listenerList)
	{
		super(appearance, value);
		
		this.comment = comment;
		this.min = min;
		this.max = max;
		this.chatCommandName = chatCommandName;
		this.allowApiOverride = allowApiOverride;
		this.listenerList = listenerList;
	}
	
	
	
	//==========================//
	// property getters/setters //
	//==========================//
	
	/** the string used when entering the config into the command line or chat */
	public String getChatCommandName() { return this.chatCommandName; }
	
	public String getComment() { return this.comment; }
	
	/**
	 * If true this config can be controlled by the API <br>
	 * and any get() method calls will return the apiValue if it is set.
	 */
	public boolean getAllowApiOverride() { return this.allowApiOverride; }
	
	public T getMin() { return this.min; }
	public void setMin(T newMin) { this.min = newMin; }
	public T getMax() { return this.max; }
	public void setMax(T newMax) { this.max = newMax; }
	
	
	
	//===============//
	// value setters //
	//===============//
	
	public void setApiValue(T newApiValue)
	{
		this.apiValue = newApiValue;
		this.listenerList.forEach(IConfigListener::onConfigValueSet);
	}
	
	public boolean apiIsOverriding() 
	{ 
		return this.allowApiOverride 
				&& this.apiValue != null; 
	}
		
	/** 
	 * Should only be used when loading the config from file. <Br>
	 * Sets the value without informing the rest of the code (ie, it doesn't call listeners, or saving the value to file).
	 * @see ConfigFileHandler
	 */
	public void setWithoutFiringEvents(T newValue) { super.set(newValue); }
	
	/** Sets the value without saving */
	public void setWithoutSaving(T newValue)
	{
		super.set(newValue);
		this.listenerList.forEach(IConfigListener::onConfigValueSet);
	}
	@Override
	public void set(T newValue)
	{
		this.setWithoutSaving(newValue);
		this.save();
	}
	
	public void uiSetWithoutSaving(T newValue)
	{
		this.setWithoutSaving(newValue);
		this.listenerList.forEach(IConfigListener::onUiModify);
	}
	public void uiSet(T newValue)
	{
		this.set(newValue);
		this.listenerList.forEach(IConfigListener::onUiModify);
	}
	
	
	
	//===============//
	// value getters //
	//===============//
	
	@Override
	public T get()
	{
		if (this.allowApiOverride 
			&& this.apiValue != null)
		{
			return this.apiValue;
		}
		
		return super.get();
	}
	/** Ignores the API value if set. */
	public T getTrueValue() { return super.get(); }
	
	public T getDefaultValue() { return super.defaultValue; }
	
	@Nullable
	public T getApiValue() { return this.apiValue; }
	
	
	
	//===========//
	// listeners //
	//===========//
	
	/** Fired whenever the config value changes to a new value. */
	public void addValueChangeListener(Consumer<T> onValueChangeFunc)
	{
		ConfigChangeListener<T> changeListener = new ConfigChangeListener<>(this, onValueChangeFunc);
		this.addListener(changeListener);
	}
	/** Fired whenever the config value is updated, including when the value doesn't change (IE when the UI changes state or the config is reloaded). */
	public void addListener(IConfigListener newListener) { this.listenerList.add(newListener); }
	
	//public void removeValueChangeListener(Consumer<T> onValueChangeFunc) { } // not currently implemented
	public void removeListener(IConfigListener oldListener) { this.listenerList.remove(oldListener); }
	
	public void clearListeners() { this.listenerList.clear(); }
	public ArrayList<IConfigListener> getListeners() { return this.listenerList; }
	/** Replaces the listener list */
	public void setListeners(ArrayList<IConfigListener> newListeners)
	{
		this.listenerList.clear();
		this.listenerList.addAll(newListeners);
	}
	public void setListeners(IConfigListener... newListeners) { this.listenerList.addAll(Arrays.asList(newListeners)); }
	
	
	
	//====================//
	// min/max validation //
	//====================//
	
	/** Checks if this config's current value is valid */
	public EConfigValidity getValidity() { return this.getValidity(this.value, this.min, this.max); }
	/** Checks if the given value is valid */
	public EConfigValidity getValidity(@Nullable T value) { return this.getValidity(value, this.min, this.max); }
	/** Checks if the given value is valid */
	public EConfigValidity getValidity(@Nullable T value, @Nullable T min, @Nullable T max)
	{
		if (!ConfigHandler.INSTANCE.runMinMaxValidation)
		{
			return EConfigValidity.VALID;
		}
		else if (min == null 
				&& max == null)
		{
			// no validation is needed for this field
			return EConfigValidity.VALID;
		}
		else if (value == null 
				|| this.value == null
				|| value.getClass() != this.value.getClass())
		{
			// If the 2 variables aren't the same type
			// or the input is missing
			// then it will be invalid
			return EConfigValidity.INVALID;
		}
		else if (value instanceof Number)
		{ 
			// Only check min/max if this config's type is a number
			if (max != null 
				&& NumberUtil.greaterThan((Number) value, (Number) max))
			{
				return EConfigValidity.NUMBER_TOO_HIGH;
			}
			
			if (min != null 
				&& NumberUtil.lessThan((Number) value, (Number) min))
			{
				return EConfigValidity.NUMBER_TOO_LOW;
			}
			
			return EConfigValidity.VALID;
		}
		else
		{
			return EConfigValidity.VALID;
		}
	}
	
	
	
	//===============//
	// file handling //
	//===============//
	
	/** This should normally not be called since set() automatically calls this */
	public void save() { ConfigHandler.INSTANCE.configFileHandler.saveEntry(this); }
	/** This should normally not be called except for special circumstances */
	public void load() { ConfigHandler.INSTANCE.configFileHandler.loadEntry(this); }
	
	
	
	//================//
	// base overrides //
	//================//
	
	public boolean equals(AbstractConfigBase<?> obj) 
	{
		return obj.getClass() == ConfigEntry.class 
				&& this.equals((ConfigEntry<?>) obj); 
	}
	/** Is the value of this equal to another */
	public boolean equals(ConfigEntry<?> obj)
	{
		// Can all of this just be "return this.value.equals(obj.value)"?
		
		if (Number.class.isAssignableFrom(this.value.getClass()))
		{
			return this.value == obj.value;
		}
		else
		{
			return this.value.equals(obj.value);
		}
	}
	
	
	
	//=========//
	// builder //
	//=========//
	
	public static class Builder<T> extends AbstractConfigBase.Builder<T, Builder<T>>
	{
		private String tmpComment = null;
		private T tmpMin = null;
		private T tmpMax = null;
		protected String tmpChatCommandName = null;
		private boolean tmpUseApiOverwrite = true;
		protected ArrayList<IConfigListener> tmpIConfigListener = new ArrayList<>();
		
		
		
		public Builder<T> comment(String newComment)
		{
			this.tmpComment = newComment;
			return this;
		}
		
		/** Allows most values to be set by 1 setter */
		public Builder<T> setMinDefaultMax(T newMin, T newDefault, T newMax)
		{
			this.set(newDefault);
			this.setMinMax(newMin, newMax);
			return this;
		}
		
		public Builder<T> setMinMax(T newMin, T newMax)
		{
			this.tmpMin = newMin;
			this.tmpMax = newMax;
			return this;
		}
		
		public Builder<T> setMin(T newMin)
		{
			this.tmpMin = newMin;
			return this;
		}
		
		public Builder<T> setMax(T newMax)
		{
			this.tmpMax = newMax;
			return this;
		}
		
		public Builder<T> setChatCommandName(String name)
		{
			this.tmpChatCommandName = name;
			return this;
		}
		
		public Builder<T> setUseApiOverwrite(boolean newUseApiOverwrite)
		{
			this.tmpUseApiOverwrite = newUseApiOverwrite;
			return this;
		}
		
		
		
		public Builder<T> replaceListeners(ArrayList<IConfigListener> newConfigListener)
		{
			this.tmpIConfigListener = newConfigListener;
			return this;
		}
		
		public Builder<T> addListeners(IConfigListener... newConfigListener)
		{
			this.tmpIConfigListener.addAll(Arrays.asList(newConfigListener));
			return this;
		}
		
		public Builder<T> addListener(IConfigListener newConfigListener)
		{
			this.tmpIConfigListener.add(newConfigListener);
			return this;
		}
		
		public Builder<T> clearListeners()
		{
			this.tmpIConfigListener.clear();
			return this;
		}
		
		
		
		// build //
		
		public ConfigEntry<T> build()
		{
			return new ConfigEntry<>(
					this.tmpAppearance,
					this.tmpComment, this.tmpChatCommandName, this.tmpValue, this.tmpMin, this.tmpMax,
					this.tmpUseApiOverwrite, 
					this.tmpIConfigListener);
		}
		
	}
	
}
