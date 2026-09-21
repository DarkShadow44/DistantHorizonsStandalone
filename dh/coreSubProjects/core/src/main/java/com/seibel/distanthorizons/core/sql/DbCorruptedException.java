package com.seibel.distanthorizons.core.sql;

import java.sql.SQLException;

/** 
 * Used to simplify handling when a database is corrupted
 * since Java doesn't have a specific exception to handle corrupted databases 
 */
public class DbCorruptedException extends SQLException
{
	public DbCorruptedException(String message) { super(message); }
	public DbCorruptedException(String message, Throwable cause) { super(message, cause); }
	public DbCorruptedException(Throwable cause) { super(cause); }
	
	
	// helper methods //
	
	public static boolean isCorruptedException(SQLException e) 
	{
		String message = e.getMessage().toLowerCase();
		return message.contains("sqlite_corrupt")
				|| message.contains("malformed");
	}
	
}
