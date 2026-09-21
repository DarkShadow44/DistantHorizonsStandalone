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

import com.seibel.distanthorizons.core.util.KeyedLockContainer;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @see KeyedLockContainer
 *
 * @author James Seibel
 * @version 2025-10-02
 */
public class KeyedLockTest
{
	
	@Test
	public void BasicKeyedLockTest()
	{
		KeyedLockContainer<Long> lockContainer = new KeyedLockContainer<>();
		
		for (long a = -10; a < 10; a++)
		{
			ReentrantLock aLock = lockContainer.getLockForPos(a);
			
			for (long b = -10; b < 10; b++)
			{
				ReentrantLock bLock = lockContainer.getLockForPos(a);
				
				// we only care that the same position always map to the same object
				// if different positions map to the same object,
				// that's expected hash-collision behavior and is fine
				if (a == b)
				{
					Assert.assertEquals("long values ["+a+"] and ["+b+"] should have returned the same lock", aLock, bLock);
				}
			}
		}
		
	}
	
}
