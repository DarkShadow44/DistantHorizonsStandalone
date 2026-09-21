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

import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.sql.dto.IBaseDTO;

public class TestCompoundKeyDto implements IBaseDTO<DhChunkPos>
{
	public DhChunkPos id;
	public String value;
	
	
	
	public TestCompoundKeyDto(DhChunkPos id, String value) 
	{ 
		this.id = id;
		this.value = value;
	}
	
	@Override 
	public DhChunkPos getKey() { return this.id; }
	
	
	@Override
	public boolean equals(Object other)
	{
		if (other.getClass() != this.getClass())
		{
			return false;
		}
		else
		{
			TestCompoundKeyDto otherDto = (TestCompoundKeyDto) other;
			
			return otherDto.id.equals(this.id)
					&& otherDto.value.equals(this.value);
		}
	}
	
	@Override
	public String toString()
	{
		return this.id + ", " + this.value;
	}
	
	@Override
	public void close()
	{ /* no closing needed */ }
	
	
	
}
