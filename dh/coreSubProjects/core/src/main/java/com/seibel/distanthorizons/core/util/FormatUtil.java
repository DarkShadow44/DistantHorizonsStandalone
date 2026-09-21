package com.seibel.distanthorizons.core.util;

import java.time.Duration;

public class FormatUtil
{
	public static String formatEta(Duration duration)
	{
		return duration.toString()
				.substring(2)
				.replaceAll("(\\d[HMS])(?!$)", "$1 ")
				.replaceAll("\\.\\d+", "")
				.toLowerCase();
	}
	
}
