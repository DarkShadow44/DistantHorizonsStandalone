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

package com.seibel.distanthorizons.core.config;

import com.seibel.distanthorizons.core.config.file.ConfigFileHandler;
import com.seibel.distanthorizons.core.config.types.*;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.ILangWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

/**
 * Sets up everything in {@link Config} for the file/GUI and keeps track of all
 * entries therein. <br>
 * This should be run after the singletons have been bound.
 *
 * @author coolGi
 * @author Ran
 * 
 * @see Config
 */
public class ConfigHandler
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftSharedWrapper MC_SHARED = SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class);
	
	/**
	 * What the config works with
	 * <br> 
	 * <br> {@link Enum}
	 * <br> {@link Boolean}
	 * <br> {@link Byte}
	 * <br> {@link Integer}
	 * <br> {@link Double}
	 * <br> {@link Short}
	 * <br> {@link Long}
	 * <br> {@link Float}
	 * <br> {@link String}
	 * <br> 
	 * <br> // Below, "T" should be a value from above
	 * <br> // Note: This is not checked, so we trust that you are doing the right thing (TODO: Check it)
	 * <br> List<T>
	 * <br> ArrayList<T>
	 * <br> Map<String, T>
	 * <br> HashMap<String, T>
	 */
	private static final List<Class<?>> ACCEPTABLE_INPUTS = new ArrayList<Class<?>>()
	{{
		this.add(Boolean.class);
		this.add(Byte.class);
		this.add(Integer.class);
		this.add(Double.class);
		this.add(Short.class);
		this.add(Long.class);
		this.add(Float.class);
		this.add(String.class);
		
		// partially implemented but not entirely
		this.add(List.class);
		this.add(ArrayList.class);
		this.add(Map.class);
		this.add(HashMap.class);
	}};
	
	
	
	public static final ConfigHandler INSTANCE = new ConfigHandler();
	
	public final ConfigFileHandler configFileHandler = new ConfigFileHandler(getConfigPath());
	public final List<AbstractConfigBase<?>> configBaseList = new ArrayList<>();
	
	public boolean isLoaded = false;
	/** 
	 * Disables the minimum and maximum validation. <Br>
	 * Fun to use, but should be disabled by default.
	 */
	public boolean runMinMaxValidation = true;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public static void tryRunFirstTimeSetup()
	{
		if (INSTANCE.isLoaded)
		{
			LOGGER.debug("ConfigHandler setup already run, ignoring.");
			return;
		}
		
		INSTANCE.runFirstTimeSetup();
	}
	private void runFirstTimeSetup()
	{
		LOGGER.info("Initialising config for [" + ModInfo.NAME + "]");
		
		this.initNestedClass(Config.class, ""); // Init root category
		
		this.configFileHandler.loadFromFile();
		this.runMinMaxValidation = !Config.Client.Advanced.Debugging.allowUnsafeValues.get();
		
		this.isLoaded = true;
		LOGGER.info("[" + ModInfo.NAME + "] Config initialised");
	}
	/** Gets the default config path given a mod name */
	private static Path getConfigPath()
	{
		return MC_SHARED
				.getInstallationDirectory().toPath()
				.resolve("config")
				.resolve(ModInfo.NAME + ".toml");
	}
	/** Put all the config entries into configEntryList */
	private void initNestedClass(Class<?> configClass, String category)
	{
		Field[] fields = configClass.getFields();
		for (Field field : fields)
		{
			// ignore any non-config variables
			if (!AbstractConfigBase.class.isAssignableFrom(field.getType()))
			{
				continue;
			}
			
			
			// add this config to the master list
			try
			{
				this.configBaseList.add((AbstractConfigBase<?>) field.get(field.getType()));
			}
			catch (IllegalAccessException e)
			{
				LOGGER.warn("Unable to add config ["+field.getType().getName()+"], error: ["+e.getMessage()+"].", e);
				continue;
			}
			
			
			// set any necessary variables in this config
			AbstractConfigBase<?> configBase = this.configBaseList.get(this.configBaseList.size() - 1);
			configBase.category = category;
			configBase.name = field.getName();
			
			
			// validate the config's input type
			if (ConfigEntry.class.isAssignableFrom(field.getType()))
			{
				if (!isAcceptableType(configBase.getType()))
				{
					LOGGER.error("Invalid variable type at [" + (category.isEmpty() ? "" : category + ".") + field.getName() + "].");
					LOGGER.error("Type [" + configBase.getType() + "] is not one of these types [" + ACCEPTABLE_INPUTS.toString() + "]");
					this.configBaseList.remove(this.configBaseList.size() - 1); // Delete the entry if it is invalid so the game can still run
				}
			}
			
			// recursively add deeper categories if present
			if (ConfigCategory.class.isAssignableFrom(field.getType()))
			{
				ConfigCategory configCategory = (ConfigCategory) configBase;
				
				if (configCategory.getDestination() == null)
				{
					configCategory.destination = configBase.getNameAndCategory();
				}
				
				// shouldn't happen, but just in case
				if (configBase.get() != null)
				{
					this.initNestedClass(configCategory.get(), configCategory.getDestination());
				}
			}
		}
	}
	private static boolean isAcceptableType(Class<?> inputClass)
	{
		if (inputClass.isEnum())
		{
			return true;
		}
		
		return ACCEPTABLE_INPUTS.contains(inputClass);
	}
	
	
	
	//===============//
	// lang handling //
	//===============//
	
	/**
	 * Used for checking that all the lang files for the config exist.
	 * This is just to re-format the lang or check if there is something in the lang that is missing
	 *
	 * @param onlyShowMissing If false then this will remake the entire config lang
	 * @param checkEnums Checks if all the lang for the enum's exist
	 */
	@SuppressWarnings("unchecked")
	public String generateLang(boolean onlyShowMissing, boolean checkEnums)
	{
		ILangWrapper langWrapper = SingletonInjector.INSTANCE.get(ILangWrapper.class);
		List<Class<? extends Enum<?>>> enumList = new ArrayList<>();
		
		String generatedLang = "";
		
		String starter = "  \"";
		String separator = "\":\n    \"";
		String ending = "\",\n";
		
		// config entries
		for (AbstractConfigBase<?> entry : this.configBaseList)
		{
			String entryPrefix = "distanthorizons.config." + entry.getNameAndCategory();
			
			if (checkEnums 
				&& entry.getType().isEnum() 
				&& !enumList.contains(entry.getType()))
			{ 
				// Put it in an enum list to work with at the end
				enumList.add((Class<? extends Enum<?>>) entry.getType());
			}
			
			
			// config file items don't need lang entries
			if (!entry.getAppearance().showInGui)
			{
				continue;
			}
			
			// some entries don't need localization
			if (ConfigUiLinkedEntry.class.isAssignableFrom(entry.getClass())
				|| ConfigUISpacer.class.isAssignableFrom(entry.getClass()))
			{
				continue;
			}
			
			if (ConfigUIComment.class.isAssignableFrom(entry.getClass())
				&& ((ConfigUIComment)entry).parentConfigPath != null)
			{
				// TODO this could potentially add the same item multiple times
				entryPrefix = "distanthorizons.config." + ((ConfigUIComment)entry).parentConfigPath;
			}
			
			
			if (langWrapper.langExists(entryPrefix)
				&& onlyShowMissing)
			{
				continue;
			}
			
			
			generatedLang += starter
					+ entryPrefix
					+ separator
					+ langWrapper.getLang(entryPrefix)
					+ ending
			;
			
			// only add tooltips for entries that are also missing
			// their primary lang
			// this is done since not all menu items need a tooltip
			if (!langWrapper.langExists(entryPrefix + ".@tooltip") 
				|| !onlyShowMissing)
			{
				generatedLang += starter
						+ entryPrefix + ".@tooltip"
						+ separator
						+ langWrapper.getLang(entryPrefix + ".@tooltip")
						.replaceAll("\n", "\\\\n")
						.replaceAll("\"", "\\\\\"")
						+ ending
				;
			}
		}
		
		
		// enums
		if (!enumList.isEmpty())
		{
			generatedLang += "\n"; // Separate the main lang with the enum's
			
			for (Class<? extends Enum> anEnum : enumList)
			{
				for (Object enumStr : new ArrayList<Object>(EnumSet.allOf(anEnum)))
				{
					String enumPrefix = "distanthorizons.config.enum." + anEnum.getSimpleName() + "." + enumStr.toString();
					
					if (!langWrapper.langExists(enumPrefix) 
						|| !onlyShowMissing)
					{
						generatedLang += starter
								+ enumPrefix
								+ separator
								+ langWrapper.getLang(enumPrefix)
								+ ending
						;
					}
				}
			}
		}
		
		return generatedLang;
	}
	
	
	
}
