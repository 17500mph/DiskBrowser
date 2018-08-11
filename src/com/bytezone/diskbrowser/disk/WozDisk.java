package com.bytezone.diskbrowser.disk;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.bytezone.diskbrowser.utilities.HexFormatter;
import com.bytezone.diskbrowser.utilities.Utility;

class WozDisk
{
  private static final byte[] WOZ_DISK_HEADER =
      { 0x57, 0x4F, 0x5A, 0x31, (byte) 0xFF, 0x0a, 0x0D, 0x0A };
  private static final int TRK_SIZE = 0x1A00;
  private static final int INFO_SIZE = 0x3C;
  private static final int TMAP_SIZE = 0xA0;
  private static final int DATA_SIZE = TRK_SIZE - 10;

  private final boolean debug = false;
  private int diskType;                       // 5.25 or 3.5

  final File file;
  byte[] diskBuffer;

  private final MC3470 mc3470 = new MC3470 ();

  // ---------------------------------------------------------------------------------//
  // constructor
  // ---------------------------------------------------------------------------------//

  public WozDisk (File file) throws Exception
  {
    this.file = file;
    byte[] buffer = readFile ();

    if (!matches (WOZ_DISK_HEADER, buffer))
      throw new Exception ("Header error");

    int cs1 = readInt (buffer, 8, 4);
    int cs2 = Utility.crc32 (buffer, 12, buffer.length - 12);
    if (cs1 != cs2)
    {
      System.out.printf ("Checksum  : %08X%n", cs1);
      System.out.printf ("Calculated: %08X%n", cs2);
      throw new Exception ("Checksum error");
    }

    int ptr = 12;
    read: while (ptr < buffer.length)
    {
      String chunkId = new String (buffer, ptr, 4);
      ptr += 4;
      int chunkSize = readInt (buffer, ptr, 4);
      ptr += 4;

      if ("INFO".equals (chunkId))
      {
        if (debug)
        {
          System.out.printf ("Version ........... %02X%n", buffer[ptr]);
          System.out.printf ("Disk type ......... %02X%n", buffer[ptr + 1]);
          System.out.printf ("Write protected ... %02X%n", buffer[ptr + 2]);
          System.out.printf ("Synchronised ...... %02X%n", buffer[ptr + 3]);
          System.out.printf ("Cleaned ........... %02X%n", buffer[ptr + 4]);
          System.out.printf ("Creator ........... %s%n%n",
              new String (buffer, ptr + 5, 32));
        }
        diskType = buffer[ptr + 1] & 0xFF;
        ptr += INFO_SIZE;
      }
      else if ("TMAP".equals (chunkId))
      {
        if (debug)
        {
          for (int track = 0; track < 40; track++)
          {
            for (int qtr = 0; qtr < 4; qtr++)
              System.out.printf ("%02X ", buffer[ptr++]);
            System.out.println ();
          }
          System.out.println ();
        }
        else
          ptr += TMAP_SIZE;
      }
      else if ("TRKS".equals (chunkId))
      {
        if (debug)
          System.out.println ("Reading TRKS");
        int tracks = chunkSize / TRK_SIZE;

        for (int trackNo = 0; trackNo < tracks; trackNo++)
        {
          int bytesUsed = readInt (buffer, ptr + DATA_SIZE, 2);
          int bitCount = readInt (buffer, ptr + DATA_SIZE + 2, 2);

          if (debug)
          {
            System.out.printf ("Bytes used .... %,6d%n", bytesUsed);
            System.out.printf ("Bit count  .... %,6d%n", bitCount);
          }

          try
          {
            mc3470.readTrack (buffer, ptr, bytesUsed, bitCount);

            if (trackNo == 0)
            {
              if (mc3470.is13Sector ())
                diskBuffer = new byte[35 * 13 * 256];
              else if (mc3470.is16Sector ())
                diskBuffer = new byte[35 * 16 * 256];
              else
              {
                System.out.println ("unknown disk format");
                break read;
              }
            }

            mc3470.storeSectors (diskBuffer);
          }
          catch (Exception e)
          {
            e.printStackTrace ();
          }

          ptr += TRK_SIZE;
        }
      }
      else if ("META".equals (chunkId))
      {
        System.out.printf ("[%s]  %08X%n", chunkId, chunkSize);
        System.out.println (HexFormatter.format (buffer, ptr, chunkSize));
        ptr += chunkSize;
      }
      else
      {
        System.out.printf ("Unknown %08X%n", chunkSize);
        ptr += chunkSize;
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  // getSectorsPerTrack
  // ---------------------------------------------------------------------------------//

  int getSectorsPerTrack ()
  {
    return mc3470.is13Sector () ? 13 : 16;
  }

  // ---------------------------------------------------------------------------------//
  // readInt
  // ---------------------------------------------------------------------------------//

  private int readInt (byte[] buffer, int offset, int length)
  {
    int shift = 0;
    int value = 0;
    for (int i = 0; i < length; i++)
    {
      value |= (buffer[offset + i] & 0xFF) << shift;
      shift += 8;
    }
    return value;
  }

  // ---------------------------------------------------------------------------------//
  // readFile
  // ---------------------------------------------------------------------------------//

  private byte[] readFile ()
  {
    try
    {
      BufferedInputStream in = new BufferedInputStream (new FileInputStream (file));
      byte[] buffer = in.readAllBytes ();
      in.close ();
      return buffer;
    }
    catch (IOException e)
    {
      e.printStackTrace ();
      return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  // matches
  // ---------------------------------------------------------------------------------//

  private boolean matches (byte[] b1, byte[] b2)
  {
    for (int i = 0; i < b1.length; i++)
      if (b1[i] != b2[i])
        return false;
    return true;
  }
}
