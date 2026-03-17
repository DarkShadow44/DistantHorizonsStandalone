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

package com.seibel.distanthorizons.core.config.gui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 */
public class JavaScreenHandlerScreen extends AbstractScreen
{
	public static Frame frame;
	public static boolean firstRun = true;
	public final Component jComponent;
	
	
	
	static
	{
		// Note: this code can cause Mac
		// to lock up and refuse the load (there's a bug with Java.awt texture loading)
		
		// Needs to be called before any Swing code is called, otherwise
		// Swing will get stuck thinking it's headless
		System.setProperty("java.awt.headless", "false");
	}
	
	public JavaScreenHandlerScreen(@NotNull Component component)
	{
		this.jComponent = component;
	}
	
	@Override
	public void init()
	{
		if (firstRun)
		{
			frame = EmbeddedFrameUtil.embeddedFrameCreate(this.minecraftWindow); // Don't call this multiple times
		}
		
		frame.add(this.jComponent);
		frame.setBackground(new Color(0, 125, 155));
		
		JavaScreenHandlerScreen thiss = this;
		
		frame.addKeyListener(new KeyListener() 
		{
			@Override
			public void keyPressed(KeyEvent keyEvent)
			{
				System.out.println("Key pressed code=" + keyEvent.getKeyCode() + ", char=" + keyEvent.getKeyChar());
				if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					thiss.close = true;
				}
			}
			
			@Override
			public void keyTyped(KeyEvent keyEvent) { }
			@Override
			public void keyReleased(KeyEvent keyEvent) { }
		});
		
		if (firstRun)
		{
			EmbeddedFrameUtil.embeddedFrameSetBounds(frame, 0, 0, this.width, this.height);
			firstRun = false;
		}
		
		EmbeddedFrameUtil.showFrame(frame);
	}
	
	/** A testing/debug screen */
	public static class ExampleScreen extends JComponent
	{
		public ExampleScreen()
		{
			this.setLayout(new GridBagLayout());
			this.setBackground(new Color(255, 0, 0)); // doesn't appear to be used
			
			GridBagConstraints helloWorldConstraints = new GridBagConstraints();
			helloWorldConstraints.weightx = 0.5;
			helloWorldConstraints.gridx = 0;
			helloWorldConstraints.gridy = 0;
			//helloWorldConstraints.fill = GridBagConstraints.BOTH;
			this.add(new JLabel("Hello World!"), helloWorldConstraints);
			
			
			GridBagConstraints buttonConstraints = new GridBagConstraints();
			buttonConstraints.weightx = 0.5;
			buttonConstraints.gridx = 0;
			buttonConstraints.gridy = 1;
			//buttonConstraints.fill = GridBagConstraints.BOTH;
			JButton button = new JButton();
			button.setBackground(Color.GREEN);
			button.setFocusable(false); // otherwise we can't use escape to leave
			button.setAction(new ExampleButtonEventHandler("Button text"));
			this.add(button, buttonConstraints);
		}
		
		private class ExampleButtonEventHandler extends AbstractAction
		{
			public ExampleButtonEventHandler(String text)
			{
				super(text);
				//this.putValue(SHORT_DESCRIPTION, text);
				//this.putValue(MNEMONIC_KEY, text);
			}
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.out.println("button pressed");
			}
		}
		
	}
	
	
	@Override
	public void render(float delta)
	{
		// TODO: Make screen only update on this being called
	}
	
	@Override
	public void onResize()
	{
		EmbeddedFrameUtil.embeddedFrameSetBounds(frame, 0, 0, this.width, this.height);
	}
	
	@Override
	public void onClose()
	{
		frame.remove(this.jComponent);
		EmbeddedFrameUtil.hideFrame(frame);
	}
	
	
	
}
