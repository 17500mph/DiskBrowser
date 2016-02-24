package com.bytezone.diskbrowser.cpm;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.diskbrowser.utilities.HexFormatter;

public class DirectoryEntry
{
  private final int userNumber;
  private final String name;
  private final String type;
  private final int ex;
  private final int s1;
  private final int s2;
  private final int rc;
  private final byte[] blockList = new byte[16];
  private final List<DirectoryEntry> entries = new ArrayList<DirectoryEntry> ();

  public DirectoryEntry (byte[] buffer, int offset)
  {
    userNumber = buffer[offset] & 0xFF;
    name = new String (buffer, offset + 1, 8).trim ();
    type = new String (buffer, offset + 9, 3).trim ();
    ex = buffer[offset + 12] & 0xFF;
    s2 = buffer[offset + 13] & 0xFF;
    s1 = buffer[offset + 14] & 0xFF;
    rc = buffer[offset + 15] & 0xFF;
    System.arraycopy (buffer, offset + 16, blockList, 0, 16);
  }

  public boolean matches (DirectoryEntry directoryEntry)
  {
    return userNumber == directoryEntry.userNumber && name.equals (directoryEntry.name)
        && type.equals (directoryEntry.type);
  }

  public void add (DirectoryEntry entry)
  {
    entries.add (entry);
  }

  @Override
  public String toString ()
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("User number .... %d%n", userNumber));
    text.append (String.format ("File name ...... %s%n", name + "." + type));
    text.append (String.format ("Extents lo ..... %d%n", ex));
    text.append (String.format ("Extents hi ..... %d%n", s2));
    text.append (String.format ("Reserved ....... %d%n", s1));

    int blocks = ((rc & 0xF0) >> 3) + (((rc & 0x0F) + 7) / 8);
    text.append (String.format ("Records ........ %02X  (%d)%n", rc, blocks));

    String bytes = HexFormatter.getHexString (blockList, 0, 16);
    text.append (String.format ("Allocation ..... %s%n", bytes));

    for (DirectoryEntry entry : entries)
    {
      bytes = HexFormatter.getHexString (entry.blockList, 0, 16);
      text.append (String.format ("                 %s%n", bytes));
    }

    return text.toString ();
  }
}