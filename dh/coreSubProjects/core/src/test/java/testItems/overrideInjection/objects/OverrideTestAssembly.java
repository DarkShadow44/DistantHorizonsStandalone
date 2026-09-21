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

package testItems.overrideInjection.objects;

import com.seibel.distanthorizons.coreapi.util.StringUtil;

/**
 * assembly classes are used to reference the package they are in.
 *
 * @author james seibel
 * @version 2022-7-19
 */
public class OverrideTestAssembly
{
	
	/** Returns the first N packages in this class' path. */
	public static String getPackagePath(int numberOfPackagesToReturn)
	{
		String thisPackageName = OverrideTestAssembly.class.getPackage().getName();
		int secondPackageEndingIndex = StringUtil.nthIndexOf(thisPackageName, ".", numberOfPackagesToReturn);
		return thisPackageName.substring(0, secondPackageEndingIndex);
	}
	
}
