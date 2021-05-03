package com.bytezone.diskbrowser.prodos.write;

import static com.bytezone.diskbrowser.prodos.ProdosConstants.BLOCK_SIZE;
import static com.bytezone.diskbrowser.prodos.ProdosConstants.SAPLING;
import static com.bytezone.diskbrowser.prodos.ProdosConstants.SEEDLING;
import static com.bytezone.diskbrowser.prodos.ProdosConstants.TREE;

// -----------------------------------------------------------------------------------//
public class FileWriter
// -----------------------------------------------------------------------------------//
{
  private final ProdosDisk disk;
  private final byte[] buffer;

  private IndexBlock indexBlock = null;
  private MasterIndexBlock masterIndexBlock = null;

  byte storageType;
  int keyPointer;
  int blocksUsed;
  int eof;

  // ---------------------------------------------------------------------------------//
  FileWriter (ProdosDisk disk, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    this.disk = disk;
    this.buffer = buffer;
  }

  // ---------------------------------------------------------------------------------//
  void writeFile (byte[] dataBuffer, int eof) throws DiskFullException
  // ---------------------------------------------------------------------------------//
  {
    this.eof = eof;

    int dataPtr = 0;
    int remaining = eof;

    while (dataPtr < eof)
    {
      int actualBlockNo = allocateNextBlock ();
      map (dataPtr / BLOCK_SIZE, actualBlockNo);

      int bufferPtr = actualBlockNo * BLOCK_SIZE;
      int tfr = Math.min (remaining, BLOCK_SIZE);

      System.arraycopy (dataBuffer, dataPtr, buffer, bufferPtr, tfr);

      dataPtr += BLOCK_SIZE;
      remaining -= BLOCK_SIZE;
    }

    writeIndices ();
  }

  // ---------------------------------------------------------------------------------//
  void writeRecord (int recordNo, byte[] dataBuffer, int recordLength)
      throws DiskFullException
  // ---------------------------------------------------------------------------------//
  {
    assert recordLength > 0;

    int destPtr = recordLength * recordNo;
    int remaining = Math.min (recordLength, dataBuffer.length);
    int max = destPtr + remaining;
    int dataPtr = 0;

    if (eof < max)
      eof = max;

    while (destPtr < max)
    {
      int logicalBlockNo = destPtr / BLOCK_SIZE;
      int blockOffset = destPtr % BLOCK_SIZE;
      int tfr = Math.min (BLOCK_SIZE - blockOffset, remaining);

      int actualBlockNo = getActualBlockNo (logicalBlockNo);
      int bufferPtr = actualBlockNo * BLOCK_SIZE + blockOffset;

      System.arraycopy (dataBuffer, dataPtr, buffer, bufferPtr, tfr);

      destPtr += tfr;
      dataPtr += tfr;
      remaining -= tfr;
    }

    writeIndices ();
  }

  // ---------------------------------------------------------------------------------//
  private int allocateNextBlock () throws DiskFullException
  // ---------------------------------------------------------------------------------//
  {
    blocksUsed++;
    return disk.allocateNextBlock ();
  }

  // ---------------------------------------------------------------------------------//
  private int getActualBlockNo (int logicalBlockNo) throws DiskFullException
  // ---------------------------------------------------------------------------------//
  {
    int actualBlockNo = 0;

    switch (storageType)
    {
      case TREE:
        actualBlockNo =
            masterIndexBlock.get (logicalBlockNo / 256).get (logicalBlockNo % 256);
        break;

      case SAPLING:
        if (logicalBlockNo < 256)
          actualBlockNo = indexBlock.get (logicalBlockNo);
        break;

      case SEEDLING:
        if (logicalBlockNo == 0)
          actualBlockNo = keyPointer;
        break;
    }

    if (actualBlockNo == 0)
    {
      actualBlockNo = allocateNextBlock ();
      map (logicalBlockNo, actualBlockNo);
    }

    return actualBlockNo;
  }

  // ---------------------------------------------------------------------------------//
  private void writeIndices ()
  // ---------------------------------------------------------------------------------//
  {
    if (storageType == TREE)
      masterIndexBlock.write (buffer);
    else if (storageType == SAPLING)
      indexBlock.write (buffer);
  }

  // ---------------------------------------------------------------------------------//
  private void map (int logicalBlockNo, int actualBlockNo) throws DiskFullException
  // ---------------------------------------------------------------------------------//
  {
    if (logicalBlockNo > 255)                         // potential TREE
    {
      if (storageType != TREE)
      {
        masterIndexBlock = new MasterIndexBlock (allocateNextBlock ());

        if (storageType == SAPLING)                   // sapling -> tree
        {
          masterIndexBlock.set (0, indexBlock);
        }
        else if (storageType == SEEDLING)             // seedling -> sapling -> tree
        {
          indexBlock = new IndexBlock (allocateNextBlock ());
          indexBlock.set (0, keyPointer);
          masterIndexBlock.set (0, indexBlock);
        }

        keyPointer = masterIndexBlock.blockNo;
        storageType = TREE;
        indexBlock = null;
      }

      getIndexBlock (logicalBlockNo / 256).set (logicalBlockNo % 256, actualBlockNo);
    }
    else if (logicalBlockNo > 0)                      // potential SAPLING
    {
      if (storageType == TREE)                        // already a tree
      {
        getIndexBlock (0).set (logicalBlockNo, actualBlockNo);
      }
      else if (storageType == SAPLING)                // already a sapling
      {
        indexBlock.set (logicalBlockNo, actualBlockNo);
      }
      else                                            // new file or already a seedling
      {
        indexBlock = new IndexBlock (allocateNextBlock ());
        if (storageType == SEEDLING)                  // seedling -> sapling
          indexBlock.set (0, keyPointer);

        keyPointer = indexBlock.blockNo;
        storageType = SAPLING;
        indexBlock.set (logicalBlockNo, actualBlockNo);
      }
    }
    else if (logicalBlockNo == 0)                     // potential SEEDLING
    {
      if (storageType == TREE)                        // already a tree
      {
        getIndexBlock (0).set (0, actualBlockNo);
      }
      else if (storageType == SAPLING)                // already a sapling
      {
        indexBlock.set (0, actualBlockNo);
      }
      else
      {
        keyPointer = actualBlockNo;
        storageType = SEEDLING;
      }
    }
    else
      System.out.println ("Error");
  }

  // ---------------------------------------------------------------------------------//
  private IndexBlock getIndexBlock (int position) throws DiskFullException
  // ---------------------------------------------------------------------------------//
  {
    IndexBlock indexBlock = masterIndexBlock.get (position);

    if (indexBlock == null)
    {
      indexBlock = new IndexBlock (allocateNextBlock ());
      masterIndexBlock.set (position, indexBlock);
    }

    return indexBlock;
  }
}
