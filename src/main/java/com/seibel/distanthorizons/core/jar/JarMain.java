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
package com.seibel.distanthorizons.core.jar;


import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.sql.dto.FullDataSourceV2DTO;
import com.seibel.distanthorizons.core.sql.repo.FullDataSourceV2Repo;
import com.seibel.distanthorizons.coreapi.ModInfo;
import org.apache.logging.log4j.LogManager;
import com.seibel.distanthorizons.core.logging.DhLogger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Only partially implemented.
 * No one has asked for the ability to parse/submit DH data outside the game
 * (and those who have gone down that path have all wanted to handle the
 * parsing themselves via their own implementations). <Br>
 * But in the off chance someone in the future wants to add this functionality
 * James will leave the code he's done so far here as a starting place.
 * <Br><Br>
 * 
 * Once built this would be in core/build/libs/DistantHorizons-<Version>-dev-all.jar
 */
public class JarMain
{
	public static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	
	
	public static void main(String[] args)
	{
		List<String> argList = Arrays.asList(args);
		
		if (!argList.contains("--no-custom-logger"))
		{
			LoggerContext context = (LoggerContext) LogManager.getContext(false);
			try
			{
				context.setConfigLocation(JarUtils.accessFileURI("/log4jConfig.xml"));
			}
			catch (Exception e)
			{
				LOGGER.error("Failed to set log4j config. Try running with the [--no-custom-logger] argument", e);
			}
		}
		
		LOGGER.debug("Running " + ModInfo.READABLE_NAME + " standalone jar");
		LOGGER.warn("The standalone jar is still a massive WIP, expect bugs");
		LOGGER.debug("Java version " + System.getProperty("java.version"));
		
		
		
		Byte exportDetailLevel = null;
		Long exportPos = null;
		
		boolean showHelp = argList.contains("help");
		if (!showHelp)
		{
			// assume something is wrong unless we find a valid arg set
			showHelp = true;
			
			
			
			if (argList.size() == 1)
			{
				// only --export
				showHelp = false;
			}
			else if (argList.size() == 2)
			{
				// --export 0
				
				String detailLevelString = argList.get(1);
				try
				{
					exportDetailLevel = Byte.parseByte(detailLevelString);
					showHelp = false;
				}
				catch (NumberFormatException e)
				{
					LOGGER.error("Unable to parse detail level ["+detailLevelString+"], error: ["+e.getMessage()+"].");
				}
			}
			else if (argList.size() == 4)
			{
				// --export 0 1 -2
				
				String detailLevelString = argList.get(1);
				String posXString = argList.get(2);
				String posZString = argList.get(3);
				try
				{
					byte detailLevel = Byte.parseByte(detailLevelString);
					int posX = Integer.parseInt(posXString);
					int posZ = Integer.parseInt(posZString);
					
					exportPos = DhSectionPos.encode(detailLevel, posX, posZ);
					showHelp = false;
				}
				catch (NumberFormatException e)
				{
					LOGGER.error("Unable to parse position ["+detailLevelString+"], ["+posXString+"], ["+posZString+"], error: ["+e.getMessage()+"].");
				}
			}
		}
		
		if (showHelp)
		{
			LOGGER.info("--export parses the 'DistantHorizons.sqlite' file next to this jar and exports the given data into a CSV file. \n" +
					"Usage: \n" +
					"--export [LOD position Detail Level] [LOD position X] [LOD position Z] \n" +
						"\tExport the given position's data if present. \n" +
						"\tThe detail level should be absolute, IE 0 = block sized, 1 = 2x2 blocks, etc. \n" +
					"--export [LOD position Detail Level]\n" +
						"\tExport all data for a given detail level.\n" +
						"\tThe detail level should be absolute, IE 0 = block sized, 1 = 2x2 blocks, etc. \n" +
					"--export\n" +
						"\tExport the entire database.\n");
			return;
		}
		
		
		// find the database file
		File dbFile = new File("./DistantHorizons.sqlite");
		if (!dbFile.exists())
		{
			LOGGER.error("Unable to find a database to parse at: ["+dbFile.getAbsolutePath()+"].");
			return;
		}
		
		
		
		// set the export file
		File exportFile = new File("DistantHorizons-export.csv");
		if (exportFile.isDirectory())
		{
			LOGGER.error("Export file can't be a folder. Given path: ["+exportFile+"].");
			return;
		}
		
		
		// create the export file
		try
		{
			boolean ignored = exportFile.mkdirs(); // we don't care if we're making new directories of if they already exist
			
			if (exportFile.exists())
			{
				LOGGER.error("Export file already exists: ["+exportFile.getAbsolutePath()+"].");
				return;
			}
			else if (exportFile.createNewFile())
			{
				LOGGER.error("Failed to create file: ["+exportFile.getAbsolutePath()+"].");
				return;
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Unable to create export file: ["+exportFile.getAbsolutePath()+"].");
			return;
		}
		
		LOGGER.info("LOD data will be exported to ["+exportFile.getAbsolutePath()+"].");
		
		
		FullDataSourceV2Repo repo;
		try
		{
			repo = new FullDataSourceV2Repo(FullDataSourceV2Repo.DEFAULT_DATABASE_TYPE, dbFile);
		}
		catch (SQLException | IOException e)
		{
			LOGGER.error("Failed to initialize connection with database: ["+exportFile.getAbsolutePath()+"], error: ["+e.getMessage()+"].", e);
			return;
		}
		
		if (exportPos != null)
		{
			exportLodDataAtPosition(repo, exportFile, exportPos);
		}
		else if (exportDetailLevel != null)
		{
			exportAllAtDetailLevel(repo, exportFile, exportDetailLevel);
		}
		else
		{
			exportEntireDatabase(repo, exportFile);
		}
	}
	
	private static void exportLodDataAtPosition(FullDataSourceV2Repo repo, File exportFile, long pos)
	{
		FullDataSourceV2DTO dto = repo.getByKey(pos);
		if (dto == null)
		{
			LOGGER.error("Unable to find any data at the position ["+DhSectionPos.toString(pos)+"].");
			return;
		}
		
		// In order to finish this, we'd need a way to create datasources (specifically data mappings) without a MC level object to deserialize with.
		// Granted it would be possible to just export the mapping data as the raw string data it's stored as.
		//dto.createPooledDataSource();
		
	}
	private static void exportAllAtDetailLevel(FullDataSourceV2Repo repo, File exportFile, byte detailLevel)
	{
		throw new UnsupportedOperationException("Method Not Implemented");
	}
	private static void exportEntireDatabase(FullDataSourceV2Repo repo, File exportFile)
	{
		throw new UnsupportedOperationException("Method Not Implemented");
	}
	
	
	
}
