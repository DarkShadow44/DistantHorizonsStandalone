package com.seibel.distanthorizons.core.enums;

/**
 * might be deprecated in the future? in that case we'll probably want a wrapper 
 * function to handle colors for new MC versions
 * <br><br>
 * source: https://minecraft.wiki/w/Formatting_codes
 */
public enum EMinecraftColor // TODO EMinecraftTextFormat
{
	BLACK("\u00A70"),
	DARK_BLUE("\u00A71"),
	DARK_GREEN("\u00A72"),
	DARK_AQUA("\u00A73"),
	DARK_RED("\u00A74"),
	DARK_PURPLE("\u00A75"),
	ORANGE("\u00A76"),
	GRAY("\u00A77"),
	DARK_GRAY("\u00A78"),
	BLUE("\u00A79"),
	GREEN("\u00A7a"),
	AQUA("\u00A7b"),
	RED("\u00A7c"),
	LIGHT_PURPLE("\u00A7d"),
	YELLOW("\u00A7e"),
	WHITE("\u00A7f"),
	
	OBFUSCATED("\u00A7k"),
	BOLD("\u00A7l"),
	STRIKETHROUGH("\u00A7m"),
	UNDERLINE("\u00A7n"),
	ITALIC("\u00A7o"),
	CLEAR_FORMATTING("\u00A7r");
	
	public final String colorCode;
	
	EMinecraftColor(String colorCode)
	{
		this.colorCode = colorCode;
	}
	
	
	
	@Override 
	public String toString() { return this.colorCode; }
	
}
