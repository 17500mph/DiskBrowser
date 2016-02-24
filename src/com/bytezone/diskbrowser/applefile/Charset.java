package com.bytezone.diskbrowser.applefile;

import com.bytezone.diskbrowser.utilities.HexFormatter;

public class Charset extends AbstractFile
{
	public Charset (String name, byte[] buffer)
	{
		super (name, buffer);
	}

	public String getText ()
	{
		StringBuilder text = new StringBuilder ();
		for (int i = 0; i < buffer.length; i += 8)
		{
			for (int line = 7; line >= 0; line--)
			{
				int value = HexFormatter.intValue (buffer[i + line]);
				for (int bit = 0; bit < 8; bit++)
				{
					text.append ((value & 0x01) == 1 ? "X" : ".");
					value >>= 1;
				}
				text.append ("\n");
			}
			text.append ("\n");
		}
		return text.toString ();
	}
}