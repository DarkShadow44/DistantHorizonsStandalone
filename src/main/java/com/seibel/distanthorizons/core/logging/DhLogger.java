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

package com.seibel.distanthorizons.core.logging;

import com.seibel.distanthorizons.api.enums.config.EDhApiLoggerLevel;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.enums.EMinecraftColor;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/** 
 * should only be created in a static context,
 * otherwise leaks will occur.
 * 
 * @see DhLoggerBuilder
 */
public class DhLogger implements IConfigListener
{
	private static final List<WeakReference<DhLogger>> LOGGER_REF_LIST = Collections.synchronizedList(new LinkedList<>());
	private static final ThreadPoolExecutor TICKER_THREAD = ThreadUtil.makeSingleDaemonThreadPool("Log Ticker");
	
	/**
	 * This logger uses the base 4J logger so we don't have to worry about
	 * setup order and can track errors when setting up DH loggers.
	 * <br>
	 * Yo dog, I heard you liked loggers, so I put a logger in your logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();
	
	
	private static IMinecraftClientWrapper mc_client = null;
	
	// both global configs start at "all" so we don't accidentally loose any logging messages
	private static EDhApiLoggerLevel globalMaxFileLevel = EDhApiLoggerLevel.ALL;
	private static EDhApiLoggerLevel globalMaxChatLevel = EDhApiLoggerLevel.ALL;
	
	
	private EDhApiLoggerLevel fileLevel;
	private EDhApiLoggerLevel chatLevel;
	
	private boolean delayedSetupComplete = false;
	
	
	@Nullable
	private final ConfigEntry<EDhApiLoggerLevel> fileLevelConfig;
	@Nullable
	private final ConfigEntry<EDhApiLoggerLevel> chatLevelConfig;
	
	/** if less than 0 then this logger won't be rate limited */
	private final int maxLogCountPerSecond;
	private final AtomicInteger logCountsThisSecondRef = new AtomicInteger(0);
	private final Logger logger;
	
	
	
	//==============//
	// static setup //
	//==============//
	
	static { TICKER_THREAD.execute(() -> runTickerLoop()); }
	
	/** 
	 * needs to be run at a later date since loggers
	 * are created before the config is set up.
	 */
	public static void runDelayedConfigSetup()
	{
		LOGGER.info("Applying config to loggers");
		
		LOGGER_REF_LIST.forEach((loggerRef) -> 
		{
		 	DhLogger logger = loggerRef.get();
			
			if (logger != null 
				&& !logger.delayedSetupComplete)
			{
				logger.delayedSetupComplete = true;
				
				Config.Common.Logging.globalChatMaxLevel.addListener(logger);
				Config.Common.Logging.globalFileMaxLevel.addListener(logger);
				
				logger.onConfigValueSet();
			}
		});
	}
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public DhLogger(String loggerName, 
			@Nullable ConfigEntry<EDhApiLoggerLevel> chatLevelConfig, @Nullable ConfigEntry<EDhApiLoggerLevel> fileLevelConfig,
			int maxLogPerSec)
	{
		this.logger = LogManager.getLogger(ModInfo.NAME + "-" + loggerName);
		
		this.maxLogCountPerSecond = maxLogPerSec;
		
		
		// chat config //
		
		this.chatLevelConfig = chatLevelConfig;
		if (this.chatLevelConfig != null)
		{
			this.chatLevel = this.chatLevelConfig.get();
			this.chatLevelConfig.addListener(this);
		}
		else
		{
			// chat messages should only be sent when explicitly desired
			this.chatLevel = EDhApiLoggerLevel.DISABLED;
		}
		
		
		// file config //
		
		this.fileLevelConfig = fileLevelConfig;
		if (this.fileLevelConfig != null)
		{
			this.fileLevel = this.fileLevelConfig.get();
			this.fileLevelConfig.addListener(this);
		}
		else
		{
			this.fileLevel = EDhApiLoggerLevel.ALL;
		}
		
		
		LOGGER_REF_LIST.add(new WeakReference<>(this));
	}
	
	
	
	//========//
	// events //
	//========//
	
	@Override
	public void onConfigValueSet()
	{ 
		if (this.fileLevelConfig != null)
		{
			this.fileLevel = this.fileLevelConfig.get();
		}
		
		if (this.chatLevelConfig != null)
		{
			this.chatLevel = this.chatLevelConfig.get();
		}
		
		globalMaxFileLevel = Config.Common.Logging.globalFileMaxLevel.get();
		globalMaxChatLevel = Config.Common.Logging.globalChatMaxLevel.get();
	}
	
	
	
	//===============//
	// log filtering //
	//===============//
	
	public boolean canLog()
	{
		// is this logger enabled?
		if (this.fileLevel == EDhApiLoggerLevel.DISABLED
			&& this.chatLevel == EDhApiLoggerLevel.DISABLED)
		{
			return false;
		}
		
		// is logging globally enabled?
		if (globalMaxFileLevel == EDhApiLoggerLevel.DISABLED
			&& globalMaxChatLevel == EDhApiLoggerLevel.DISABLED)
		{
			return false;
		}
		
		// has this logger run to many times this second already?
		if (this.maxLogCountPerSecond > 0
			&& this.logCountsThisSecondRef.get() >= this.maxLogCountPerSecond)
		{
			return false;
		}
		
		return true;
	}
	
	
	
	//=========//
	// logging //
	//=========//
	
	public void fatal(String str, Object... param) { this.log(Level.FATAL, str, param); }
	public void error(String str, Object... param) { this.log(Level.ERROR, str, param); }
	public void warn(String str, Object... param){ this.log(Level.WARN, str, param); }
	public void info(String str, Object... param) { this.log(Level.INFO, str, param); }
	public void debug(String str, Object... param) { this.log(Level.DEBUG, str, param); }
	public void trace(String str, Object... param) { this.log(Level.TRACE, str, param); }
	
	public void log(Level level, String str, Object... param)
	{
		if (!this.canLog())
		{
			return;
		}
		
		
		Message msg = this.logger.getMessageFactory().newMessage(str, param);
		String msgStr = msg.getFormattedMessage();
		
		boolean messageLogged = false;
		
		
		// file
		if (canLogThisLevel(this.fileLevel.level, globalMaxFileLevel.level, level))
		{
			// by default MC's console/file logging config doesn't include debug or trace
			// logs, so to make sure our messages are included we need to set any lower levels as "info"
			Level logLevel = loggingLevelIsLessSpecificThan(level, Level.INFO) ? Level.INFO : level;
			
			if (param.length > 0
				&& param[param.length - 1] instanceof Throwable)
			{
				this.logger.log(logLevel, msgStr, (Throwable) param[param.length - 1]);
			}
			else
			{
				this.logger.log(logLevel, msgStr);
			}
			
			messageLogged = true;
		}
		
		// chat
		if (canLogThisLevel(this.chatLevel.level, globalMaxChatLevel.level, level))
		{
			// lazy initialization since loggers may be created before the wrapper has been bound
			if (mc_client == null)
			{
				mc_client = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
			}
			
			if (mc_client != null)
			{
				this.logToChat(level, msgStr);
				messageLogged = true;
			}
		}
		
		
		if (messageLogged)
		{
			this.logCountsThisSecondRef.incrementAndGet();
		}
	}
	private static boolean canLogThisLevel(Level thisLoggingLevel, Level thisGlobalLoggingLevel, Level requestedLogLevel)
	{
		// we can only continue if this logger's level and the global level
		// are at the same or a higher level than the requested level
		return thisLoggingLevel.intLevel() >= requestedLogLevel.intLevel()
				&& thisGlobalLoggingLevel.intLevel() >= requestedLogLevel.intLevel();
	}
	private static boolean loggingLevelIsLessSpecificThan(Level thisLoggingLevel, Level requestedLogLevel)
	{ return thisLoggingLevel.intLevel() >= requestedLogLevel.intLevel(); }
	/** Sends the given message to chat with a formatted prefix and color based on the log level. */
	private void logToChat(Level logLevel, String message)
	{
		String prefix = "[" + ModInfo.READABLE_NAME + "] ";
		if (logLevel == Level.ERROR)
		{
			prefix += EMinecraftColor.DARK_RED;
		}
		else if (logLevel == Level.WARN)
		{
			prefix += EMinecraftColor.ORANGE;
		}
		else if (logLevel == Level.INFO)
		{
			prefix += EMinecraftColor.AQUA;
		}
		else if (logLevel == Level.DEBUG)
		{
			prefix += EMinecraftColor.GREEN;
		}
		else if (logLevel == Level.TRACE)
		{
			prefix += EMinecraftColor.DARK_GRAY;
		}
		else
		{
			prefix += EMinecraftColor.WHITE;
		}
		
		prefix += EMinecraftColor.BOLD + "" + EMinecraftColor.WHITE;
		prefix += logLevel.name();
		prefix += EMinecraftColor.CLEAR_FORMATTING + " ";
		
		mc_client.sendChatMessage(prefix + message);
	}
	
	
	
	//===============//
	// static ticker //
	//===============//
	
	private static void runTickerLoop()
	{
		while (true)
		{
			try
			{
				// tick once every second
				Thread.sleep(1_000);
				
				LOGGER_REF_LIST.removeIf((logger) ->
				{
					boolean loggerGarbageCollected = logger.get() == null;
					if (loggerGarbageCollected)
					{
						LOGGER.warn("Logger garbage collected. Loggers should only be created in static contexts otherwise memory leaks may occur.");
					}
					return loggerGarbageCollected;
				});
				LOGGER_REF_LIST.forEach((loggerRef) ->
				{
					DhLogger logger = loggerRef.get();
					if (logger != null)
					{
						// reset the number of logs for this second
						logger.logCountsThisSecondRef.set(0);
					}
				});
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected error in ticker thread: [" + e.getMessage() + "].", e);
			}
		}
	}
	
	
	
}
