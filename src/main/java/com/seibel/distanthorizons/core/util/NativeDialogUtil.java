package com.seibel.distanthorizons.core.util;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Should be used instead of the direct call to {@link TinyFileDialogs}
 * so we can run additional validation and/or string cleanup.
 * Otherwise, we may get error messages back.
 * 
 * @see TinyFileDialogs
 */
public class NativeDialogUtil
{
	/**
	 * @param dialogType    the dialog type. One of:<br><table><tr><td>"ok"</td><td>"okcancel"</td><td>"yesno"</td><td>"yesnocancel"</td></tr></table>
	 * @param iconType      the icon type. One of:<br><table><tr><td>"info"</td><td>"warning"</td><td>"error"</td><td>"question"</td></tr></table>
	 */
	public static boolean showDialog(String title, String message, String dialogType, String iconType)
	{
		// Tinyfd doesn't support the following characters, attempting to display them will cause the message
		// to be replaced with an error message
		String unsafeCharsRegex = "['\"`]";
		
		title = title.replaceAll(unsafeCharsRegex, "`");
		message = message.replaceAll(unsafeCharsRegex, "`");
		
		return TinyFileDialogs.tinyfd_messageBox(title, message, dialogType, iconType, false);
	}
	
}
