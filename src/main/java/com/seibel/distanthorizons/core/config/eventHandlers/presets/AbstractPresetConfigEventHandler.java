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

package com.seibel.distanthorizons.core.config.eventHandlers.presets;

import com.seibel.distanthorizons.core.config.ConfigHandler;
import com.seibel.distanthorizons.core.config.ConfigPresetOptions;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.config.types.AbstractConfigBase;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.util.TimerUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.IConfigGui;
import com.seibel.distanthorizons.coreapi.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractPresetConfigEventHandler<TPresetEnum extends Enum<?>> implements IConfigListener
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	private static final long MS_DELAY_BEFORE_APPLYING_PRESET = 3_000;
	
	@Nullable
	private static final IConfigGui CONFIG_GUI = SingletonInjector.INSTANCE.get(IConfigGui.class);
	
	protected final ArrayList<ConfigPresetOptions<TPresetEnum, ?>> configList = new ArrayList<>();
	/** this timer is used so each preset isn't applied while a user is clicking through the config options */
	protected Timer applyPresetTimer = null;
	/** the enum to apply after the timer expires or the UI screen changes. */
	protected TPresetEnum waitingPresetEnum = null;
	
	protected boolean changingPreset = false;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public AbstractPresetConfigEventHandler()
	{
		// don't update the UI when running on a server
		if (CONFIG_GUI != null) 
		{
			CONFIG_GUI.addOnScreenChangeListener(this::onConfigUiClosed);
		}
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	/**
	 * Set the UI only config based on what is set in the file. <br>
	 * This should only be called once.
	 */
	public void setUiOnlyConfigValues()
	{
		TPresetEnum currentQualitySetting = this.getCurrentPreset();
		this.getPresetConfigEntry().set(currentQualitySetting);
	}
	
	
	//===========//
	// listeners //
	//===========//
	
	@Override
	public void onConfigValueSet()
	{
		// don't try modifying the config before it's been loaded from file
		if (!ConfigHandler.INSTANCE.isLoaded)
		{
			return;
		}
		
		// don't have this method run on top of itself
		if (this.changingPreset)
		{
			return;
		}
		
		
		
		// if the quick value is custom, nothing needs to be changed
		TPresetEnum presetEnum = this.getPresetConfigEntry().get();
		if (presetEnum == this.getCustomPresetEnum())
		{
			return;
		}
		this.waitingPresetEnum = presetEnum;
		
		
		// stop the previous timer if one exists
		if (this.applyPresetTimer != null)
		{
			this.applyPresetTimer.cancel();
		}
		
		// reset the timer
		TimerTask task = new TimerTask()
		{
			public void run() { AbstractPresetConfigEventHandler.this.applyPreset(); }
		};
		this.applyPresetTimer = TimerUtil.CreateTimer("ApplyConfigPresetTimer");
		this.applyPresetTimer.schedule(task, MS_DELAY_BEFORE_APPLYING_PRESET);
		
	}
	private void applyPreset()
	{
		TPresetEnum newPresetEnum = this.waitingPresetEnum;
		this.waitingPresetEnum = null;
		
		// only continue if a preset was waiting to be applied
		if (newPresetEnum == null)
		{
			return;
		}
		
		
		
		LOGGER.debug("changing preset to: [" + newPresetEnum + "].");
		this.changingPreset = true;
		
		// update the controlled config values
		for (ConfigPresetOptions<TPresetEnum, ?> configEntry : this.configList)
		{
			configEntry.updateConfigEntry(newPresetEnum);
		}
		// update the preset value (required to make sure the UI value changes correctly after the UI page changes).
		this.setUiOnlyConfigValues();
		
		this.changingPreset = false;
		LOGGER.debug("preset active: [" + newPresetEnum + "].");
	}
	
	/**
	 * listen for changed graphics settings and set the
	 * quick quality to "custom" if anything was changed
	 */
	public void onConfigValueChanged()
	{
		if (this.changingPreset)
		{
			// if a preset is currently being applied, ignore all changes
			return;
		}
		
		
		TPresetEnum newPreset = this.getCurrentPreset();
		TPresetEnum currentPreset = this.getPresetConfigEntry().get();
		
		if (newPreset != currentPreset)
		{
			this.getPresetConfigEntry().set(newPreset);
		}
	}
	
	public void onConfigUiClosed()
	{
		// apply the preset if one is waiting to be applied
		if (this.applyPresetTimer != null)
		{
			this.applyPresetTimer.cancel();
			this.applyPreset();
		}
	}
	
	
	//================//
	// helper methods //
	//================//
	
	/** @return what {@link TPresetEnum} is currently viable based on the {@link AbstractPresetConfigEventHandler#configList}. */
	public TPresetEnum getCurrentPreset()
	{
		// get all quick options
		HashSet<TPresetEnum> possiblePresetSet = new HashSet<>(this.getPresetEnumList());
		
		
		// remove any quick options that aren't possible with the currently selected options
		for (ConfigPresetOptions<TPresetEnum, ?> configEntry : this.configList)
		{
			HashSet<TPresetEnum> optionPresetSet = configEntry.getPossibleQualitiesFromCurrentOptionValue();
			possiblePresetSet.retainAll(optionPresetSet);
		}
		
		
		
		ArrayList<TPresetEnum> possiblePrestList = new ArrayList<>(possiblePresetSet);
		if (possiblePrestList.size() > 1)
		{
			// we shouldn't have multiple options, but just in case
			LOGGER.warn("Multiple potential preset options [" + StringUtil.join(", ", possiblePrestList) + "], defaulting to the first one.");
		}
		
		if (possiblePrestList.size() == 0)
		{
			// if no options are valid, return "CUSTOM"
			possiblePrestList.add(this.getCustomPresetEnum());
		}
		
		return possiblePrestList.get(0);
	}
	
	
	
	//==================//
	// abstract methods //
	//==================//
	
	protected abstract AbstractConfigBase<TPresetEnum> getPresetConfigEntry();
	
	protected abstract List<TPresetEnum> getPresetEnumList();
	protected abstract TPresetEnum getCustomPresetEnum();
	
	
}
