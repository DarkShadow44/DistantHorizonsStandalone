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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

/**
 * Indexes and sets everything up for the file handling and gui.
 * This should be init after singletons have been bound
 *
 * @author coolGi
 * @author Ran
 * @version 2023-8-26
 */
public class ConfigBase
{
	/** Our own config instance, don't modify unless you are the DH mod */
	public static ConfigBase INSTANCE;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
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
	public static final List<Class<?>> ACCEPTABLE_INPUTS = new ArrayList<Class<?>>()
	{{
		this.add(Boolean.class);
		this.add(Byte.class);
		this.add(Integer.class);
		this.add(Double.class);
		this.add(Short.class);
		this.add(Long.class);
		this.add(Float.class);
		this.add(String.class);
		
		// TODO[CONFIG]: Check the type of these is valid
		this.add(List.class);
		this.add(ArrayList.class);
		this.add(Map.class);
		this.add(HashMap.class);
	}};
	
	
	
	public ConfigFileHandler configFileHandler;
	
	public final int configVersion;
	
	public boolean isLoaded = false;
	
	/** Disables the minimum and maximum of any variable */
	public boolean disableMinMax = false; // Very fun to use, but should always be disabled by default
	public final List<AbstractConfigType<?, ?>> entries = new ArrayList<>();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public static void RunFirstTimeSetup()
	{
		if (INSTANCE != null)
		{
			LOGGER.debug("ConfigBase setup already run, ignoring.");
			return;
		}
		
		INSTANCE = new ConfigBase(Config.class, ModInfo.CONFIG_FILE_VERSION);
	}
	
	private ConfigBase(Class<?> configClass, int configVersion)
	{
		LOGGER.info("Initialising config for [" + ModInfo.NAME + "]");
		
		this.configVersion = configVersion;
		
		this.initNestedClass(configClass, ""); // Init root category
		
		Path configPath = getConfigPath(ModInfo.NAME);
		this.configFileHandler = new ConfigFileHandler(this, configPath);
		this.configFileHandler.loadFromFile();
		
		this.isLoaded = true;
		LOGGER.info("Config for [" + ModInfo.NAME + "] initialised");
	}
	/** Gets the default config path given a mod name */
	private static Path getConfigPath(String modName)
	{
		return SingletonInjector.INSTANCE.get(IMinecraftSharedWrapper.class)
				.getInstallationDirectory().toPath().resolve("config").resolve(modName + ".toml");
	}
	private void initNestedClass(Class<?> configClass, String category)
	{
		// Put all the entries in entries
		
		for (Field field : configClass.getFields())
		{
			if (AbstractConfigType.class.isAssignableFrom(field.getType()))
			{
				try
				{
					this.entries.add((AbstractConfigType<?, ?>) field.get(field.getType()));
				}
				catch (IllegalAccessException exception)
				{
					LOGGER.warn(exception);
				}
				
				AbstractConfigType<?, ?> entry = this.entries.get(this.entries.size() - 1);
				entry.category = category;
				entry.name = field.getName();
				entry.configBase = this;
				
				if (ConfigEntry.class.isAssignableFrom(field.getType()))
				{ 
					// If item is type ConfigEntry
					if (!isAcceptableType(entry.getType()))
					{
						LOGGER.error("Invalid variable type at [" + (category.isEmpty() ? "" : category + ".") + field.getName() + "].");
						LOGGER.error("Type [" + entry.getType() + "] is not one of these types [" + ACCEPTABLE_INPUTS.toString() + "]");
						this.entries.remove(this.entries.size() - 1); // Delete the entry if it is invalid so the game can still run
					}
				}
				
				if (ConfigCategory.class.isAssignableFrom(field.getType()))
				{ // If it's a category then init the stuff inside it and put it in the category list
					assert entry instanceof ConfigCategory;
					if (((ConfigCategory) entry).getDestination() == null)
						((ConfigCategory) entry).destination = entry.getNameWCategory();
					if (entry.get() != null)
					{
						this.initNestedClass(((ConfigCategory) entry).get(), ((ConfigCategory) entry).getDestination());
					}
				}
			}
		}
	}
	private static boolean isAcceptableType(Class<?> Clazz)
	{
		if (Clazz.isEnum())
		{
			return true;
		}
		
		return ACCEPTABLE_INPUTS.contains(Clazz);
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
		for (AbstractConfigType<?, ?> entry : this.entries)
		{
			String entryPrefix = "distanthorizons.config." + entry.getNameWCategory();
			
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
