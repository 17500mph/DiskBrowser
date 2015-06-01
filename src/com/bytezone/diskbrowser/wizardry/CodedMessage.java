package com.bytezone.diskbrowser.wizardry;

import com.bytezone.diskbrowser.HexFormatter;

class CodedMessage extends Message
{
	public static int codeOffset = 185;

	public CodedMessage (byte[] buffer)
	{
		super (buffer);
	}

	@Override
	protected String getLine (int offset)
	{
		int length = HexFormatter.intValue (buffer[offset]);
		byte[] translation = new byte[length];
		codeOffset--;
		for (int j = 0; j < length; j++)
		{
			translation[j] = buffer[offset + 1 + j];
			translation[j] -= codeOffset - j * 3;
		}
		return HexFormatter.getString (translation, 0, length);
	}
}