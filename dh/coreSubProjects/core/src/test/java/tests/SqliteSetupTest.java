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

package tests;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.sql.*;

/**
 * Validates Sqlite is setup correctly.
 */
public class SqliteSetupTest
{
	public static String DATABASE_TYPE = "jdbc:sqlite";
	
	
	@Test
	public void testSqliteFile()
	{
		String databaseLocation = "sample.sqlite";
		testSqliteDatabase(DATABASE_TYPE, databaseLocation);
		
		File dbFile = new File(databaseLocation);
		Assert.assertTrue("Unable to delete test database.", dbFile.delete());
	}
	
	//@Test
	//public void testInMemorySqlite()
	//{
	//	String databaseLocation = ":memory:";
	//	testSqliteDatabase(DATABASE_TYPE, databaseLocation);
	//}
	
	
	
	//================//
	// helper methods //
	//================//
	
	private static void testSqliteDatabase(String databaseType, String databaseLocation)
	{
		Connection connection = null;
		try
		{
			// create a database connection
			connection = DriverManager.getConnection(databaseType+":"+databaseLocation);
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);  // set timeout to 30 sec.
			
			
			// create the database
			statement.executeUpdate("drop table if exists person");
			statement.executeUpdate("create table person (id integer, name string)");
			
			// insert test values
			statement.executeUpdate("insert into person values(1, 'leo')");
			statement.executeUpdate("insert into person values(2, 'yui')");
			
			// get the values
			ResultSet rs = statement.executeQuery("select * from person");
			
			
			// read the result set
			Assert.assertTrue(rs.next());
			int id = rs.getInt("id");
			Assert.assertEquals(1, id);
			String name = rs.getString("name");
			Assert.assertEquals("leo", name);
			
			Assert.assertTrue(rs.next());
			id = rs.getInt("id");
			Assert.assertEquals(2, id);
			name = rs.getString("name");
			Assert.assertEquals("yui", name);
			
			// all results have been read
			Assert.assertFalse(rs.next());
		}
		catch(SQLException e)
		{
			// if the error message is "out of memory",
			// it probably means no database file is found
			Assert.fail("Unexpected error: " + e.getMessage());
		}
		finally
		{
			try
			{
				if(connection != null)
				{
					connection.close();
				}
			}
			catch(SQLException e)
			{
				// connection close failed.
				Assert.fail("Unable to close the connection: " + e.getMessage());
			}
		}
	}
	
}
