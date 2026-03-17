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

package com.seibel.distanthorizons.core.config.file;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.seibel.distanthorizons.core.config.ConfigHandler;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.config.types.AbstractConfigBase;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.jar.EPlatform;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import org.apache.logging.log4j.LogManager;
import com.seibel.distanthorizons.core.logging.DhLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles reading and writing config files.
 *
 * @author coolGi
 * @version 2023-8-26
 */
public class ConfigFileHandler
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	public final Path configPath;
	
	/** This is the object for night-config */
	private final CommentedFileConfig nightConfig;
	
	/** prevents readers/writers from overlapping and causing the config file from being duplicated or corrupted */
	private final ReentrantLock readWriteLock = new ReentrantLock();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public ConfigFileHandler(Path configPath)
	{
		this.configPath = configPath;
		
		this.nightConfig = CommentedFileConfig
				.builder(this.configPath.toFile())
				// sync is needed so file reading/writing only happens during locked sections,
				// otherwise some GUI changes may be lost when changing screens
				.sync()
				.build();
	}
	
	
	
	//====================//
	// entire config file //
	//====================//
	
	/** Saves the entire config to the file */
	public void saveToFile() { this.saveToFile(this.nightConfig); }
	/** Saves the entire config to the file */
	public void saveToFile(CommentedFileConfig nightConfig)
	{
		try
		{
			this.readWriteLock.lock();
			
			
			
			if (!Files.exists(this.configPath)) // Try to check if the config exists
			{
				reCreateFile(this.configPath);
			}
			
			
			this.loadNightConfig(nightConfig);
			
			
			for (AbstractConfigBase<?> entry : ConfigHandler.INSTANCE.configBaseList)
			{
				if (ConfigEntry.class.isAssignableFrom(entry.getClass()))
				{
					this.createComment((ConfigEntry<?>) entry, nightConfig);
					this.saveEntry((ConfigEntry<?>) entry, nightConfig);
				}
			}
			
			
			try
			{
				nightConfig.save();
			}
			catch (Exception e)
			{
				// If it fails to save, crash game
				SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class).crashMinecraft("Failed to save config at [" + this.configPath + "]", e);
			}
			
		}
		finally
		{
			this.readWriteLock.unlock();
		}
	}
	
	/**
	 * Loads the entire config from the file
	 *
	 * @apiNote This overwrites any value currently stored in the config
	 */
	public void loadFromFile()
	{
		try
		{
			this.readWriteLock.lock();
			
			int currentCfgVersion = ModInfo.CONFIG_FILE_VERSION;
			try
			{
				// Dont load the real `this.nightConfig`, instead create a tempoary one
				CommentedFileConfig tmpNightConfig = CommentedFileConfig.builder(this.configPath.toFile()).build();
				tmpNightConfig.load();
				// Attempt to get the version number
				currentCfgVersion = (Integer) tmpNightConfig.get("_version");
				tmpNightConfig.close();
			}
			catch (Exception ignored) { }
			
			if (currentCfgVersion == ModInfo.CONFIG_FILE_VERSION)
			{
				// handle normally
			}
			else if (currentCfgVersion > ModInfo.CONFIG_FILE_VERSION)
			{
				LOGGER.warn("Found config version [" + currentCfgVersion + "] which is newer than current mods config version of [" + ModInfo.CONFIG_FILE_VERSION + "]. You may have downgraded the mod and items may have been moved, you have been warned");
			}
			else // if (currentCfgVersion < configBase.configVersion)
			{
				LOGGER.warn(ModInfo.NAME + " config is of an older version, currently there is no config updater... so resetting config");
				try
				{
					Files.delete(this.configPath);
				}
				catch (Exception e)
				{
					LOGGER.error("Unable to delete outdated config file at: ["+this.configPath+"], error: ["+e.getMessage()+"].", e);
				}
			}
			
			this.loadFromFile(this.nightConfig);
			this.nightConfig.set("_version", ModInfo.CONFIG_FILE_VERSION);
		}
		finally
		{
			this.readWriteLock.unlock();
		}
	}
	/**
	 * Loads the entire config from the file
	 *
	 * @apiNote This overwrites any value currently stored in the config
	 */
	private void loadFromFile(CommentedFileConfig nightConfig)
	{
		// Attempt to load the file and if it fails then save config to file
		if (Files.exists(this.configPath))
		{
			this.loadNightConfig(nightConfig);
		}
		else
		{
			reCreateFile(this.configPath);
		}
		
		
		// Load all the entries
		for (AbstractConfigBase<?> entry : ConfigHandler.INSTANCE.configBaseList)
		{
			if (ConfigEntry.class.isAssignableFrom(entry.getClass())
				&& entry.getAppearance().showInFile)
			{
				this.createComment((ConfigEntry<?>) entry, nightConfig);
				this.loadEntry((ConfigEntry<?>) entry, nightConfig);
			}
		}
		
		
		try
		{
			nightConfig.save();
		}
		catch (Exception e)
		{
			// If it fails to save, crash game
			SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class).crashMinecraft("Failed to save config at [" + this.configPath + "]", e);
		}
	}
	
	
	
	//=======================//
	// single config entries //
	//=======================//
	
	// Save an entry when only given the entry
	public void saveEntry(ConfigEntry<?> entry)
	{
		this.saveEntry(entry, this.nightConfig);
		this.nightConfig.save();
	}
	/** Save an entry */
	public void saveEntry(ConfigEntry<?> entry, CommentedFileConfig workConfig)
	{
		if (!entry.getAppearance().showInFile)
		{
			return;
		}
		else if (entry.getTrueValue() == null)
		{
			// TODO when can this happen?
			throw new IllegalArgumentException("BlockBiomeWrapperPair [" + entry.getNameAndCategory() + "] is null, this may be a problem with [" + ModInfo.NAME + "]. Please contact the authors.");
		}
		
		workConfig.set(entry.getNameAndCategory(), ConfigTypeConverters.attemptToConvertToString(entry.getType(), entry.getTrueValue()));
	}
	
	/** Loads an entry when only given the entry */
	public void loadEntry(ConfigEntry<?> entry) { this.loadEntry(entry, this.nightConfig); }
	/** Loads an entry */
	@SuppressWarnings("unchecked")
	public <T> void loadEntry(ConfigEntry<T> entry, CommentedFileConfig nightConfig)
	{
		if (!entry.getAppearance().showInFile)
		{
			return;
		}
		
		if (!nightConfig.contains(entry.getNameAndCategory()))
		{
			this.saveEntry(entry, nightConfig);
			return;
		}
		
		
		try
		{
			if (entry.getType().isEnum())
			{
				entry.setWithoutFiringEvents((T) (nightConfig.getEnum(entry.getNameAndCategory(), (Class<? extends Enum>) entry.getType())));
				return;
			}
			
			// try converting the value if necessary
			Class<?> expectedValueClass = entry.getType();
			Object value = nightConfig.get(entry.getNameAndCategory());
			Object convertedValue = ConfigTypeConverters.attemptToConvertFromString(expectedValueClass, value);
			if (!convertedValue.getClass().equals(expectedValueClass))
			{
				LOGGER.error("Unable to convert config value ["+value+"] from ["+(value != null ? value.getClass() : "NULL")+"] to ["+expectedValueClass+"] for config ["+entry.name+"], " +
						"the default config value will be used instead ["+entry.getDefaultValue()+"]. " +
						"Make sure a converter is defined in ["+ConfigTypeConverters.class.getSimpleName()+"].");
				convertedValue = entry.getDefaultValue();
			}
			entry.setWithoutFiringEvents((T) convertedValue);
			
			if (entry.getTrueValue() == null) 
			{
				LOGGER.warn("BlockBiomeWrapperPair [" + entry.getNameAndCategory() + "] returned as null from the config. Using default value.");
				entry.setWithoutFiringEvents(entry.getDefaultValue());
			}
		}
		catch (Exception e)
		{
			LOGGER.warn("BlockBiomeWrapperPair [" + entry.getNameAndCategory() + "] had an invalid value when loading the config. Using default value.");
			entry.setWithoutFiringEvents(entry.getDefaultValue());
		}
	}
	
	// Creates the comment for an entry when only given the entry
	public void createComment(ConfigEntry<?> entry) { this.createComment(entry, this.nightConfig); }
	// Creates a comment for an entry
	public void createComment(ConfigEntry<?> entry, CommentedFileConfig nightConfig)
	{
		if (!entry.getAppearance().showInFile 
			|| entry.getComment() == null)
		{
			return;
		}
		
		
		
		String comment = entry.getComment().replaceAll("\n", "\n ").trim();
		// the new line makes it easier to read and separate configs
		// the space makes sure the first word of a comment isn't directly in line with the "#" 
		comment = "\n " + comment;
		nightConfig.setComment(entry.getNameAndCategory(), comment);
	}
	
	
	
	//=============//
	// nightconfig //
	//=============//
	
	/**
	 * Uses {@link ConfigFileHandler#nightConfig} to do {@link CommentedFileConfig#load()} but with error checking
	 *
	 * @apiNote This overwrites any value currently stored in the config
	 */
	public void loadNightConfig() { this.loadNightConfig(this.nightConfig); }
	/**
	 * Does {@link CommentedFileConfig#load()} but with error checking
	 *
	 * @apiNote This overwrites any value currently stored in the config
	 */
	public void loadNightConfig(CommentedFileConfig nightConfig)
	{
		try
		{
			try
			{
				if (!Files.exists(this.configPath))
					Files.createFile(this.configPath);
				nightConfig.load();
			}
			catch (Exception e)
			{
				LOGGER.warn("Loading file failed because of this expectation:\n" + e);
				
				reCreateFile(this.configPath);
				
				nightConfig.load();
			}
		}
		catch (Exception e)
		{
			LOGGER.error("File creation failed at ["+this.configPath+"], error: ["+e.getMessage()+"].", e);
			
			// TODO is there a reason this is lazily gotten?
			IMinecraftClientWrapper mc = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
			mc.crashMinecraft("Loading file and resetting config file failed at path [" + this.configPath + "]. Please check the file is ok and you have the permissions", e);
		}
	}
	
	
	
	//===============//
	// file handling //
	//===============//
	
	public static void reCreateFile(Path path)
	{
		try
		{
			Files.deleteIfExists(path);
			
			if (!path.getParent().toFile().exists())
			{
				Files.createDirectory(path.getParent());
			}
			Files.createFile(path);
		}
		catch (IOException e)
		{
			LOGGER.error("Unable to recreate config file, error: ["+e.getMessage()+"].", e);
		}
	}
	
	
	
}
