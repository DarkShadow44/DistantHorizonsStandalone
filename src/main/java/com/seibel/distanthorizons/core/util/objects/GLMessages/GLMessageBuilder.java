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

package com.seibel.distanthorizons.core.util.objects.GLMessages;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;
import java.util.function.Function;

/** Expected message formats can be found in GLMessageTest. */
public class GLMessageBuilder
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	/** how many stages are present in the message parser */
	private static final int FINAL_LEGACY_PARSER_STAGE_INDEX = 15;
	private static final int FINAL_NEW_PARSER_STAGE_INDEX = 5;
	
	
	
	private EGLMessageType type;
	private EGLMessageSeverity severity;
	private EGLMessageSource source;
	
	/** if the function returns false the message will be allowed */
	private final Function<EGLMessageType, Boolean> typeFilter;
	/** if the function returns false the message will be allowed */
	private final Function<EGLMessageSeverity, Boolean> severityFilter;
	/** if the function returns false the message will be allowed */
	private final Function<EGLMessageSource, Boolean> sourceFilter;
	
	private String id;
	private String message;
	/** how far into the message parser this builder is */
	private int parserStage = 0;
	
	private boolean legacyMessage = true;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public GLMessageBuilder() { this(null, null, null); }
	
	public GLMessageBuilder(
			Function<EGLMessageType, Boolean> typeFilter,
			Function<EGLMessageSeverity, Boolean> severityFilter,
			Function<EGLMessageSource, Boolean> sourceFilter)
	{
		this.typeFilter = typeFilter;
		this.severityFilter = severityFilter;
		this.sourceFilter = sourceFilter;
	}
	
	
	
	//=================//
	// message parsing //
	//=================//
	
	/**
	 * Adds the given string to the message builder. <br> <br>
	 *
	 * Will log a warning if the string given wasn't expected
	 * for the next stage of the OpenGL message format.<br> <br>
	 *
	 * @return null if the message isn't complete
	 */
	public GLMessage add(String str)
	{
		str = str.trim();
		if (str.isEmpty())
		{
			return null;
		}
		
		boolean messageFinished = false;
		boolean parseSuccess = this.runNextParserStage(str);
		
		
		if (this.legacyMessage
				&& this.parserStage > FINAL_LEGACY_PARSER_STAGE_INDEX)
		{
			messageFinished = true;
		}
		else if (!this.legacyMessage
				&& this.parserStage > FINAL_NEW_PARSER_STAGE_INDEX)
		{
			messageFinished = true;
		}
		
		
		if (parseSuccess && messageFinished)
		{
			this.parserStage = 0;
			GLMessage msg = new GLMessage(this.type, this.severity, this.source, this.id, this.message);
			if (this.doesMessagePassFilters(msg))
			{
				return msg;
			}
		}
		else if (!parseSuccess && messageFinished)
		{
			LOGGER.warn("Failed to parse GLMessage line [" + str + "] at stage [" + this.parserStage + "]");
		}
		
		// the message isn't finished yet
		return null;
	}
	
	private boolean doesMessagePassFilters(GLMessage msg)
	{
		if (this.sourceFilter != null && !this.sourceFilter.apply(msg.source))
			return false;
		else if (this.typeFilter != null && !this.typeFilter.apply(msg.type))
			return false;
		else if (this.severityFilter != null && !this.severityFilter.apply(msg.severity))
			return false;
		else
			return true;
	}
	
	/** @return true if the given string was expected next for the OpenGL message format */
	private boolean runNextParserStage(String str)
	{
		if (this.parserStage == 0)
		{
			return this.checkExactAndIncStage(str, GLMessage.HEADER);
		}
		else if (this.parserStage == 1)
		{
			// legacy message only contains "ID" (not the colon)
			this.legacyMessage = !str.contains("ID: ");
		}
		
		
		if (this.legacyMessage)
		{
			return this.runNextLegacyParserStage(str);
		}
		else
		{
			return this.runNextNewParserStage(str);
		}
	}
	/** MC 1.20.2 and older */
	private boolean runNextLegacyParserStage(String str)
	{
		switch (this.parserStage)
		{
			case 0:
				throw new IllegalStateException("Parser should be past stage ["+this.parserStage+"], next stage is [1].");
			case 1:
				return this.checkExactAndIncStage(str, "ID");
			case 2:
				return this.checkExactAndIncStage(str, ":");
			case 3:
				this.id = str;
				this.parserStage++;
				return true;
			case 4:
				return this.checkExactAndIncStage(str, "Source");
			case 5:
				return this.checkExactAndIncStage(str, ":");
			case 6:
				this.source = EGLMessageSource.get(str);
				this.parserStage++;
				return true;
			case 7:
				return this.checkExactAndIncStage(str, "Type");
			case 8:
				return this.checkExactAndIncStage(str, ":");
			case 9:
				this.type = EGLMessageType.get(str);
				this.parserStage++;
				return true;
			case 10:
				return this.checkExactAndIncStage(str, "Severity");
			case 11:
				return this.checkExactAndIncStage(str, ":");
			case 12:
				this.severity = EGLMessageSeverity.get(str);
				this.parserStage++;
				return true;
			case 13:
				return this.checkExactAndIncStage(str, "Message");
			case 14:
				return this.checkExactAndIncStage(str, ":");
			case 15:
				this.message = str;
				this.parserStage++;
				return true;
			default:
				return false;
		}
	}
	/** after MC 1.20.2 */
	private boolean runNextNewParserStage(String str)
	{
		switch (this.parserStage)
		{
			case 0:
				throw new IllegalStateException("Parser should be past stage [" + this.parserStage + "], next stage is [1].");
			case 1:
				String idPrefix = "ID: ";
				return this.checkPrefixAndRun(str, idPrefix,
						(line) ->
						{
							this.id = trySubstring(str, idPrefix.length());
							this.parserStage++;
						});
			case 2:
				String sourcePrefix = "Source: ";
				return this.checkPrefixAndRun(str, sourcePrefix,
						(line) ->
						{
							String sourceString = trySubstring(str, sourcePrefix.length());
							this.source = EGLMessageSource.get(sourceString);
							this.parserStage++;
						});
			case 3:
				String typePrefix = "Type: ";
				return this.checkPrefixAndRun(str, typePrefix,
						(line) ->
						{
							String sourceString = trySubstring(str, typePrefix.length());
							this.type = EGLMessageType.get(sourceString);
							this.parserStage++;
						});
			case 4:
				String severityPrefix = "Severity: ";
				return this.checkPrefixAndRun(str, severityPrefix,
						(line) ->
						{
							String sourceString = trySubstring(str, severityPrefix.length());
							this.severity = EGLMessageSeverity.get(sourceString);
							this.parserStage++;
						});
			case 5:
				String messagePrefix = "Message: ";
				return this.checkPrefixAndRun(str, messagePrefix,
						(line) ->
						{
							this.message = trySubstring(str, messagePrefix.length());
							this.parserStage++;
						});
			default:
				return false;
		}
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/**
	 * Returns true and increments the parserStage
	 * if the message and expected strings are the same.
	 */
	private boolean checkExactAndIncStage(String message, String expectedString)
	{
		boolean equal = message.equals(expectedString);
		if (equal)
		{
			this.parserStage++;
		}
		return equal;
	}
	
	/**
	 * Returns true and increments the parserStage
	 * if the message starts with the given prefix.
	 */
	private boolean checkPrefixAndRun(String message, String expectedPrefix, Consumer<String> successConsumer)
	{
		boolean equal = message.startsWith(expectedPrefix);
		if (equal)
		{
			successConsumer.accept(message);
		}
		return equal;
	}
	/** returns "" if the string isn't long enough */
	private static String trySubstring(String string, int beginIndex)
	{
		if (beginIndex > string.length())
		{
			// prevent index-out-of-bounds errors
			// if the message isn't what we expected
			return "";
		}
		
		return string.substring(beginIndex);
	}
	
	
}
