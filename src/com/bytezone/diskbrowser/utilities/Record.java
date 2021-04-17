package com.bytezone.diskbrowser.utilities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.diskbrowser.prodos.ProdosConstants;

// -----------------------------------------------------------------------------------//
class Record
// -----------------------------------------------------------------------------------//
{
  private static final byte[] NuFX = { 0x4E, (byte) 0xF5, 0x46, (byte) 0xD8 };
  private static String[] fileSystems =
      { "", "ProDOS/SOS", "DOS 3.3", "DOS 3.2", "Apple II Pascal", "Macintosh HFS",
        "Macintosh MFS", "Lisa File System", "Apple CP/M", "", "MS-DOS", "High Sierra",
        "ISO 9660", "AppleShare" };

  private static String[] storage = { "", "Seedling", "Sapling", "Tree", "", "Extended",
                                      "", "", "", "", "", "", "", "Subdirectory" };

  private static String[] accessChars = { "D", "R", "B", "", "", "I", "W", "R" };

  //  private final BlockHeader blockHeader;
  private final int totThreads;
  private final int crc;
  private final char separator;
  private final int fileSystemID;
  private final int attributes;
  private final int version;
  private final int access;
  private final int fileType;
  private final int auxType;
  private final int storType;
  private final DateTime created;
  private final DateTime modified;
  private final DateTime archived;
  private final int optionSize;
  private final int fileNameLength;
  private final String fileName;

  final List<Thread> threads = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public Record (byte[] buffer, int dataPtr) throws FileFormatException
  // ---------------------------------------------------------------------------------//
  {
    // check for NuFX
    if (!Utility.isMagic (buffer, dataPtr, NuFX))
      throw new FileFormatException ("NuFX not found");

    //    blockHeader = new BlockHeader (buffer, dataPtr);
    //    System.out.println (blockHeader);

    crc = Utility.getWord (buffer, dataPtr + 4);
    attributes = Utility.getWord (buffer, dataPtr + 6);
    version = Utility.getWord (buffer, dataPtr + 8);
    totThreads = Utility.getLong (buffer, dataPtr + 10);
    fileSystemID = Utility.getWord (buffer, dataPtr + 14);
    separator = (char) (buffer[dataPtr + 16] & 0x00FF);
    access = Utility.getLong (buffer, dataPtr + 18);
    fileType = Utility.getLong (buffer, dataPtr + 22);
    auxType = Utility.getLong (buffer, dataPtr + 26);
    storType = Utility.getWord (buffer, dataPtr + 30);
    created = new DateTime (buffer, dataPtr + 32);
    modified = new DateTime (buffer, dataPtr + 40);
    archived = new DateTime (buffer, dataPtr + 48);
    optionSize = Utility.getWord (buffer, dataPtr + 56);
    fileNameLength = Utility.getWord (buffer, dataPtr + attributes - 2);

    int len = attributes + fileNameLength - 6;
    byte[] crcBuffer = new byte[len + totThreads * 16];
    System.arraycopy (buffer, dataPtr + 6, crcBuffer, 0, crcBuffer.length);

    if (crc != Utility.getCRC (crcBuffer, 0))
    {
      System.out.println ("***** Header CRC mismatch *****");
      throw new FileFormatException ("Header CRC failed");
    }

    if (fileNameLength > 0)
    {
      int start = dataPtr + attributes;
      int end = start + fileNameLength;
      for (int i = start; i < end; i++)
        buffer[i] &= 0x7F;
      fileName = new String (buffer, start, fileNameLength);
    }
    else
      fileName = "";
  }

  // ---------------------------------------------------------------------------------//
  //  private boolean isNuFX (byte[] buffer, int ptr)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    if (buffer[ptr] == 0x4E && buffer[ptr + 1] == (byte) 0xF5 && buffer[ptr + 2] == 0x46
  //        && buffer[ptr + 3] == (byte) 0xD8)
  //      return true;
  //    return false;
  //  }

  // ---------------------------------------------------------------------------------//
  int getAttributes ()
  // ---------------------------------------------------------------------------------//
  {
    return attributes;
  }

  // ---------------------------------------------------------------------------------//
  int getFileNameLength ()
  // ---------------------------------------------------------------------------------//
  {
    return fileNameLength;
  }

  // ---------------------------------------------------------------------------------//
  int getTotalThreads ()
  // ---------------------------------------------------------------------------------//
  {
    return totThreads;
  }

  // ---------------------------------------------------------------------------------//
  boolean hasDisk ()
  // ---------------------------------------------------------------------------------//
  {
    for (Thread thread : threads)
      if (thread.hasDisk ())
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  boolean hasFile ()
  // ---------------------------------------------------------------------------------//
  {
    for (Thread thread : threads)
      if (thread.hasFile ())
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  boolean hasFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    for (Thread thread : threads)
      if (thread.hasFile (fileName))
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    for (Thread thread : threads)
      if (thread.hasFileName ())
      {
        String fileName = thread.getFileName ();
        if (separator != '/')
          return fileName.replace (separator, '/');
        return thread.getFileName ();
      }

    return "";
  }

  // ---------------------------------------------------------------------------------//
  int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileType;
  }

  // ---------------------------------------------------------------------------------//
  int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return auxType;
  }

  // ---------------------------------------------------------------------------------//
  LocalDateTime getCreated ()
  // ---------------------------------------------------------------------------------//
  {
    if (created == null)
      return null;
    return created.getLocalDateTime ();
  }

  // ---------------------------------------------------------------------------------//
  LocalDateTime getModified ()
  // ---------------------------------------------------------------------------------//
  {
    if (modified == null)
      return null;
    return modified.getLocalDateTime ();
  }

  // ---------------------------------------------------------------------------------//
  int getFileSize ()
  // ---------------------------------------------------------------------------------//
  {
    for (Thread thread : threads)
      if (thread.hasFile ())
        return thread.getFileSize ();

    return 0;
  }

  // ---------------------------------------------------------------------------------//
  int getUncompressedSize ()
  // ---------------------------------------------------------------------------------//
  {
    for (Thread thread : threads)
      if (thread.hasFile ())
        return thread.getUncompressedEOF ();

    return 0;
  }

  // ---------------------------------------------------------------------------------//
  byte[] getData ()
  // ---------------------------------------------------------------------------------//
  {
    for (Thread thread : threads)
      if (thread.hasFile ())
        return thread.getData ();
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String bits = "00000000" + Integer.toBinaryString (access & 0xFF);
    bits = bits.substring (bits.length () - 8);
    String decode = Utility.matchFlags (access, accessChars);

    text.append (String.format ("Header CRC ..... %,d  (%<04X)%n", crc));
    text.append (String.format ("Attributes ..... %d%n", attributes));
    text.append (String.format ("Version ........ %d%n", version));
    text.append (String.format ("Threads ........ %d%n", totThreads));
    text.append (String.format ("File sys id .... %d (%s)%n", fileSystemID,
        fileSystems[fileSystemID]));
    text.append (String.format ("Separator ...... %s%n", separator));
    text.append (String.format ("Access ......... %s  %s%n", bits, decode));
    if (storType < 16)
    {
      text.append (String.format ("File type ...... %,d  %s%n", fileType,
          ProdosConstants.fileTypes[fileType]));
      text.append (String.format ("Aux type ....... %,d%n", auxType));
      text.append (
          String.format ("Stor type ...... %,d  %s%n", storType, storage[storType]));
    }
    else
    {
      text.append (String.format ("Zero ........... %,d%n", fileType));
      text.append (String.format ("Total blocks ... %,d%n", auxType));
      text.append (String.format ("Block size ..... %,d%n", storType));
    }
    text.append (String.format ("Created ........ %s%n", created.format ()));
    text.append (String.format ("Modified ....... %s%n", modified.format ()));
    text.append (String.format ("Archived ....... %s%n", archived.format ()));
    text.append (String.format ("Option size .... %,d%n", optionSize));
    text.append (String.format ("Filename len ... %,d%n", fileNameLength));
    text.append (String.format ("Filename ....... %s", fileName));

    return text.toString ();
  }
}
