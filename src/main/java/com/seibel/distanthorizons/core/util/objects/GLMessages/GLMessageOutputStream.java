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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public final class GLMessageOutputStream extends OutputStream
{
	final Consumer<GLMessage> func;
	final GLMessageBuilder builder;
	
	
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
	public GLMessageOutputStream(Consumer<GLMessage> func, GLMessageBuilder builder)
	{
		this.func = func;
		this.builder = builder;
	}
	
	@Override
	public void write(int b)
	{
		this.buffer.write(b);
		if (b == '\n')
		{
			this.flush();
		}
	}
	
	@Override
	public void flush()
	{
		String str = this.buffer.toString();
		GLMessage msg = this.builder.add(str);
		if (msg != null)
		{
			this.func.accept(msg);
		}
		this.buffer.reset();
	}
	
	@Override
	public void close() throws IOException
	{
		this.flush();
		this.buffer.close();
	}
	
}
