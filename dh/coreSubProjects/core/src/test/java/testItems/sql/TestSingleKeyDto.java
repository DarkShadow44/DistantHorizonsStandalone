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

package testItems.sql;

import com.seibel.distanthorizons.core.sql.dto.IBaseDTO;

public class TestSingleKeyDto implements IBaseDTO<Integer>
{
	public int id;
	public String value;
	public long longValue;
	public byte byteValue;
	
	
	
	
	public TestSingleKeyDto(int id, String value, long longValue, byte byteValue) 
	{ 
		this.id = id;
		this.value = value;
		this.longValue = longValue;
		this.byteValue = byteValue;
	}
	
	@Override 
	public Integer getKey() { return this.id; }
	
	
	@Override
	public boolean equals(Object other)
	{
		if (other.getClass() != this.getClass())
		{
			return false;
		}
		else
		{
			TestSingleKeyDto otherDto = (TestSingleKeyDto) other;
			
			return otherDto.id == this.id
					&& otherDto.value.equals(this.value)
					&& otherDto.longValue == this.longValue
					&& otherDto.byteValue == this.byteValue;
		}
	}
	
	@Override
	public String toString()
	{
		return this.id + ", " + this.value + ", " + this.longValue + ", " + this.byteValue;
	}
	
	@Override
	public void close()
	{ /* no closing needed */ }
	
	
	
}
