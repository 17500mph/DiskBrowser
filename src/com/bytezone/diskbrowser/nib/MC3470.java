package com.bytezone.diskbrowser.nib;

import java.util.ArrayList;
import java.util.List;

class MC3470
{
  private static final int MAX_DATA = 999;
  private final boolean debug = false;
  private final boolean dump = false;

  private List<RawDiskSector> diskSectors;

  private State currentState;
  private RawDiskSector currentDiskSector;

  private int expectedDataSize;
  private boolean finished;
  private boolean restarted;

  private DiskReader diskReader;
  private final DiskReader diskReader16Sector = new DiskReader16Sector ();
  private final DiskReader diskReader13Sector = new DiskReader13Sector ();

  private final byte[] dataBuffer = new byte[MAX_DATA];
  private int dataPtr;

  // D5 AA 96   16 sector address prologue
  // D5 AA B5   13 sector address prologue
  // D5 AA AD   data prologue
  // DE AA EB   epilogue

  // non-standard:
  // D4 AA 96   address prologue - Bouncing Kamungas
  // D5 BB CF   data prologue    - Hard Hat Mac
  // DA AA EB   address epilogue - Bouncing Kamungas

  private static final byte[] address16prologue =
      { (byte) 0xD5, (byte) 0xAA, (byte) 0x96 };
  private static final byte[] address13prologue =
      { (byte) 0xD5, (byte) 0xAA, (byte) 0xB5 };
  private static final byte[] dataPrologue = { (byte) 0xD5, (byte) 0xAA, (byte) 0xAD };
  private static final byte[] epilogue = { (byte) 0xDE, (byte) 0xAA, (byte) 0xEB };

  private static final byte[] address16prologueX =
      { (byte) 0xD4, (byte) 0xAA, (byte) 0x96 };
  private static final byte[] dataPrologueX = { (byte) 0xD5, (byte) 0xBB, (byte) 0xCF };
  private static final byte[] epilogueX = { (byte) 0xDA, (byte) 0xAA, (byte) 0xEB };

  private enum State
  {
    ADDRESS, DATA, OTHER
  }

  // ---------------------------------------------------------------------------------//
  // readTrack
  // ---------------------------------------------------------------------------------//

  List<RawDiskSector> readTrack (byte[] buffer, int offset, int bytesUsed, int bitCount)
      throws DiskNibbleException
  {
    int totalBits = 0;
    int totalBytes = 0;

    diskSectors = new ArrayList<> ();
    diskReader = null;
    currentDiskSector = null;
    currentState = State.OTHER;
    expectedDataSize = MAX_DATA;
    restarted = false;

    byte value = 0;                     // value to be stored
    dataPtr = 0;

    int inPtr = offset;                 // keep offset in case we have to loop around
    final int max = offset + bytesUsed;
    finished = false;

    while (inPtr < max && !finished)
    {
      byte b = buffer[inPtr++];

      if (!restarted)
        totalBytes++;

      for (int mask = 0x80; mask != 0; mask >>>= 1)
      {
        value <<= 1;
        if ((b & mask) != 0)
          value |= 0x01;

        if ((value & 0x80) != 0)     // value is not valid until the hi-bit is set
        {
          if (dump)
          {
            if (dataPtr % 16 == 0)
              System.out.printf ("%n%04X: ", dataPtr);
            System.out.printf ("%02X ", value);
          }

          if (dataPtr >= MAX_DATA)
            throw new DiskNibbleException ("No prologues found");

          dataBuffer[dataPtr++] = value;
          value = 0;

          if (currentState == State.OTHER)
            checkState ();
          else if (dataPtr == expectedDataSize)     // DATA or ADDRESS is now complete
            setState (State.OTHER);
        }

        if (++totalBits == bitCount)      // only use this many bits
          break;
      }

      // check for unfinished data block, we may need to restart from the beginning
      if (inPtr == max && currentState == State.DATA && !restarted)
      {
        inPtr = offset;
        restarted = true;
      }
    }

    if (debug)
    {
      System.out.println ("**************************************");
      System.out.printf ("*  total bits  : %,5d  *%n", bitCount);
      System.out.printf ("*  bits used   : %,5d  *%n", totalBits);
      System.out.printf ("*  total bytes : %,5d  *%n", bytesUsed);
      System.out.printf ("*  bytes used  : %,5d  *%n", totalBytes);
      System.out.println ("**************************************");
    }

    return diskSectors;
  }

  // ---------------------------------------------------------------------------------//
  // checkState
  // ---------------------------------------------------------------------------------//

  private void checkState () throws DiskNibbleException
  {
    assert currentState == State.OTHER;

    if (dataPtr < 3)            // not enough bytes to test
      return;

    //    System.out.printf ("checking %02X %02X %02X%n", dataBuffer[dataPtr - 3],
    //        dataBuffer[dataPtr - 2], dataBuffer[dataPtr - 1]);

    if (match (address16prologue) || match (address16prologueX))
    {
      diskReader = diskReader16Sector;
      setState (State.ADDRESS);
    }
    else if (match (address13prologue))
    {
      diskReader = diskReader13Sector;
      setState (State.ADDRESS);
    }
    else if (match (dataPrologue) || match (dataPrologueX))
      setState (State.DATA);
    else if (match (epilogue) || match (epilogueX))
      setState (State.OTHER);
  }

  // ---------------------------------------------------------------------------------//
  // match
  // ---------------------------------------------------------------------------------//

  private boolean match (byte[] pattern)
  {
    for (int i = 0, j = dataPtr - 3; i < 3; i++, j++)
      if (pattern[i] != dataBuffer[j])
        return false;

    return true;
  }

  // ---------------------------------------------------------------------------------//
  // setState
  // ---------------------------------------------------------------------------------//

  private void setState (State newState) throws DiskNibbleException
  {
    if (currentState == State.OTHER && newState == State.OTHER)
      return;

    assert currentState != newState : currentState + " -> " + newState;

    switch (currentState)                     // this state is now finished
    {
      case ADDRESS:
        if (currentDiskSector != null)
          throw new DiskNibbleException ("unused ADDRESS: " + currentDiskSector);

        currentDiskSector = new RawDiskSector (new DiskAddressField (dataBuffer));
        if (dump)
          System.out.println ("\n" + currentDiskSector);
        break;

      case DATA:
        if (currentDiskSector == null)
          throw new DiskNibbleException ("cannot store DATA without ADDRESS");

        currentDiskSector.setBuffer (diskReader.decodeSector (dataBuffer));
        diskSectors.add (currentDiskSector);
        currentDiskSector = null;
        if (diskSectors.size () == diskReader.sectorsPerTrack)
          finished = true;

        break;

      case OTHER:       // triggered by an epilogue or full address/data buffer
        break;
    }

    switch (newState)                       // this state is now starting
    {
      case ADDRESS:
        if (currentDiskSector != null)
          throw new DiskNibbleException ("cannot start ADDRESS: " + currentDiskSector);
        expectedDataSize = 8;
        if (dump)
          System.out.print ("ADDRESS  ");
        break;

      case DATA:
        if (currentDiskSector == null)
          throw new DiskNibbleException ("cannot start DATA without ADDRESS");
        expectedDataSize = diskReader.expectedDataSize ();
        if (dump)
          System.out.println ("DATA");
        break;

      case OTHER:
        expectedDataSize = MAX_DATA;        // what is the maximum filler?
        if (dump)
          System.out.println ("OTHER");
        break;
    }

    currentState = newState;
    dataPtr = 0;                            // start collecting new buffer
  }

  // ---------------------------------------------------------------------------------//
  // storeSectors
  // ---------------------------------------------------------------------------------//

  void storeSectors (List<RawDiskSector> diskSectors, byte[] diskBuffer)
      throws DiskNibbleException
  {
    if (diskReader == null)
      throw new DiskNibbleException ("No DiskReader");

    for (RawDiskSector diskSector : diskSectors)
      diskReader.storeBuffer (diskSector, diskBuffer);
  }

  // ---------------------------------------------------------------------------------//
  // is13Sector
  // ---------------------------------------------------------------------------------//

  boolean is13Sector ()
  {
    return diskSectors.size () == 13 && diskReader.sectorsPerTrack == 13;
  }

  // ---------------------------------------------------------------------------------//
  // is16Sector
  // ---------------------------------------------------------------------------------//

  boolean is16Sector ()
  {
    return diskSectors.size () == 16 && diskReader.sectorsPerTrack == 16;
  }
}
