package com.bytezone.diskbrowser.disk;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.bytezone.diskbrowser.disk.Nibblizer.AddressField;
import com.bytezone.diskbrowser.disk.Nibblizer.DataField;
import com.bytezone.diskbrowser.utilities.HexFormatter;

/*
 * from Gerard Putter's email:
 * Offsets in bytes
 * From To Meaning
 * 0000 0003 Length of disk image, from offset 8 to the end (big endian)
 * 0004 0007 Type indication; always 'D5NI'
 * 0008 0009 Number of tracks (typical value: $23, but can be different). Also big endian.
 * 000A 000B Track nummer * 4, starting with track # 0. Big endian.
 * 000C 000D Length of track, not counting this length-field. Big endian.
 * 000E 000E + length field - 1 Nibble-data of track, byte-for-byte.
 * After this, the pattern from offset 000A repeats for each track. Note that
 * "track number * 4" means that on a regular disk the tracks are numbered 0, 4, 8 etc.
 * Half-tracks are numbered 2, 6, etc. The stepper motor of the disk uses 4 phases to
 * travel from one track to the next, so quarter-tracks could also be stored with this
 * format (I have never heard of disk actually using quarter tracks, though).
 */

// Physical disk interleave:
// Info from http://www.applelogic.org/TheAppleIIEGettingStarted.html
// Block ..: 0 1 2 3 4 5 6 7 8 9 A B C D E F
// Position: 0 8 1 9 2 A 3 B 4 C 5 D 6 E 7 F - Prodos (.PO disks)
// Position: 0 7 E 6 D 5 C 4 B 3 A 2 9 1 8 F - Dos (.DO disks)

public class V2dDisk
{
  private static int[][] interleave =
      { { 0, 8, 1, 9, 2, 10, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15 },
        { 0, 8, 1, 9, 2, 10, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15 } };

  private static final int DOS = 0;
  private static final int PRODOS = 1;

  private static final int TRACK_LENGTH = 6304;

  private final Nibblizer nibbler = new Nibblizer ();

  final File file;
  final int tracks;

  final byte[] buffer = new byte[4096 * 35];

  public V2dDisk (File file)
  {
    this.file = file;
    int tracks = 0;

    try
    {
      byte[] diskBuffer = new byte[10];
      BufferedInputStream in = new BufferedInputStream (new FileInputStream (file));
      in.read (diskBuffer);

      int diskLength = HexFormatter.getLongBigEndian (diskBuffer, 0);   // 4 bytes
      String id = HexFormatter.getString (diskBuffer, 4, 4);            // 4 bytes
      tracks = HexFormatter.getShortBigEndian (diskBuffer, 8);          // 2 bytes

      assert diskLength + 8 == file.length ();
      assert "D5NI".equals (id);

      byte[] trackHeader = new byte[4];
      byte[] trackData = new byte[TRACK_LENGTH];

      for (int i = 0; i < tracks; i++)
      {
        in.read (trackHeader);
        int trackNumber = HexFormatter.getShortBigEndian (trackHeader, 0);
        int trackLength = HexFormatter.getShortBigEndian (trackHeader, 2);    // 6304

        assert trackLength == TRACK_LENGTH;

        int dataRead = in.read (trackData);
        assert dataRead == TRACK_LENGTH;

        int fullTrackNo = trackNumber / 4;
        int halfTrackNo = trackNumber % 4;

        if (halfTrackNo == 0)                               // only process full tracks
          processTrack (fullTrackNo, trackData, buffer);
        else
          System.out.printf ("%s skipping half track %02X / %02X%n", file.getName (),
              fullTrackNo, halfTrackNo);
      }

      in.close ();
    }
    catch (IOException e)
    {
      e.printStackTrace ();
    }

    this.tracks = tracks;
  }

  private boolean processTrack (int trackNo, byte[] buffer, byte[] diskBuffer)
  {
    int ptr = 0;

    while (buffer[ptr] == (byte) 0xEB)
    {
      System.out.printf ("%s overrun 0xEB offset %d in track %02X%n", file.getName (),
          ptr, trackNo);
      ++ptr;
    }

    ptr += nibbler.skipBytes (buffer, ptr, (byte) 0xFF);          // gap1

    while (ptr < buffer.length)
    {
      AddressField addressField = nibbler.getAddressField (buffer, ptr);
      if (!addressField.isValid ())
        return false;

      assert addressField.track == trackNo;

      ptr += addressField.size ();
      ptr += nibbler.skipBytes (buffer, ptr, (byte) 0xFF);        // gap2

      DataField dataField = nibbler.getDataField (buffer, ptr);
      if (!dataField.isValid ())
        return false;

      int offset = addressField.track * 4096 + interleave[DOS][addressField.sector] * 256;
      System.arraycopy (dataField.dataBuffer, 0, diskBuffer, offset, 256);

      ptr += dataField.size ();
      ptr += nibbler.skipBytes (buffer, ptr, (byte) 0xFF);        // gap3
    }

    return true;
  }
}