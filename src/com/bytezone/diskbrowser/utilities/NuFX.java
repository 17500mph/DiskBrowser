package com.bytezone.diskbrowser.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.diskbrowser.prodos.write.DiskFullException;
import com.bytezone.diskbrowser.prodos.write.ProdosDisk;

// -----------------------------------------------------------------------------------//
public class NuFX
// -----------------------------------------------------------------------------------//
{
  private MasterHeader masterHeader;
  private final byte[] buffer;
  private final boolean debug = false;

  private final List<Record> records = new ArrayList<> ();
  private int totalFiles;
  private int totalDisks;
  private int totalBlocks;

  private List<String> paths = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public NuFX (Path path) throws FileFormatException, IOException
  // ---------------------------------------------------------------------------------//
  {
    buffer = Files.readAllBytes (path);

    masterHeader = new MasterHeader (buffer);

    int dataPtr = 48;
    if (masterHeader.bin2)
      dataPtr += 128;

    if (debug)
      System.out.printf ("%s%n%n", masterHeader);

    for (int rec = 0; rec < masterHeader.getTotalRecords (); rec++)
    {
      Record record = new Record (buffer, dataPtr);
      //      if (record.getFileSystemID () != 1)
      //      {
      //        System.out.println ("Not Prodos: " + record.getFileSystemID ());
      //        break;
      //      }
      records.add (record);

      if (debug)
        System.out.printf ("Record: %d%n%n%s%n%n", rec, record);

      dataPtr += record.getAttributes () + record.getFileNameLength ();
      int threadsPtr = dataPtr;
      dataPtr += record.getTotalThreads () * 16;

      for (int i = 0; i < record.getTotalThreads (); i++)
      {
        Thread thread = new Thread (buffer, threadsPtr + i * 16, dataPtr);
        record.threads.add (thread);
        dataPtr += thread.getCompressedEOF ();

        if (debug)
          System.out.printf ("Thread: %d%n%n%s%n%n", i, thread);
      }

      if (record.hasFile ())
      {
        ++totalFiles;
        int blocks = (record.getFileSize () - 1) / 512 + 1;
        if (blocks == 1)                      // seedling
          totalBlocks += blocks;
        else if (blocks <= 256)               // sapling
          totalBlocks += blocks + 1;
        else                                  // tree
          totalBlocks += blocks + (blocks / 256) + 2;
        storePath (record.getFileName ());
      }

      if (record.hasDisk ())
        ++totalDisks;
    }

    if (false)
    {
      System.out.println ("Unique paths:");
      if (paths.size () == 0)
        System.out.println ("<none>");
      for (String pathName : paths)
        System.out.println (pathName);
    }
  }

  // ---------------------------------------------------------------------------------//
  private void storePath (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    int pos = fileName.lastIndexOf ('/');
    if (pos > 0)
    {
      String path = fileName.substring (0, pos);
      for (int i = 0; i < paths.size (); i++)
      {
        String cmp = paths.get (i);
        if (cmp.startsWith (path))        // longer path already there
          return;
        if (path.startsWith (cmp))
        {
          paths.set (i, path);            // replace shorter path with longer path
          return;
        }
      }
      paths.add (path);
    }
  }

  // ---------------------------------------------------------------------------------//
  public byte[] getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (totalDisks > 0)
    {
      for (Record record : records)
        for (Thread thread : record.threads)
          if (thread.hasDisk ())
            return thread.getData ();
    }
    else if (totalFiles > 0)
    {
      int[] diskSizes = { 280, 800, 1600, 3200, 6400, 65536 };
      //      System.out.printf ("Files require: %d blocks%n", totalBlocks);

      // choose Volume Name
      String volumeName = "DiskBrowser";
      int nameOffset = 0;

      if (paths.size () == 1)                         // exactly one directory path
      {
        String onlyPath = paths.get (0);
        int pos = onlyPath.indexOf ('/');
        if (pos == -1)                                // no separators
          volumeName = onlyPath;
        else                                          // use first component
          volumeName = onlyPath.substring (0, pos);
        nameOffset = volumeName.length () + 1;        // skip volume name in all paths
      }

      for (int diskSize : diskSizes)      // in case we choose a size that is too small
      {
        //        System.out.printf ("Checking %d %d%n", diskSize, totalBlocks);
        if (diskSize < (totalBlocks + 10))
          continue;

        try
        {
          ProdosDisk disk = new ProdosDisk (diskSize, volumeName);
          int count = 0;
          for (Record record : records)
          {
            if (record.hasFile ())
            {
              String fileName = record.getFileName ();
              //              int fileSize = record.getFileSize ();
              byte fileType = (byte) record.getFileType ();
              int eof = record.getUncompressedSize ();
              int auxType = record.getAuxType ();
              LocalDateTime created = record.getCreated ();
              LocalDateTime modified = record.getModified ();
              byte[] buffer = record.getData ();

              if (nameOffset > 0)         // remove volume name from path
                fileName = fileName.substring (nameOffset);

              if (false)
                System.out.printf ("%3d %-35s %02X %,7d %,7d %,7d  %s  %s%n", ++count,
                    fileName, fileType, auxType, eof, buffer.length, created, modified);

              disk.addFile (fileName, fileType, auxType, created, modified, buffer);
            }
          }

          disk.close ();

          return disk.getBuffer ();
        }
        catch (IOException e)
        {
          e.printStackTrace ();
          return null;
        }
        catch (DiskFullException e)
        {
          System.out.println ("disk full: " + diskSize);    // go round again
        }
      }
    }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  public byte[] getFileBuffer (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    for (Record record : records)
      if (record.hasFile (fileName))
        for (Thread thread : record.threads)
          if (thread.hasFile ())
            return thread.getData ();

    return null;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return totalFiles;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalDisks ()
  // ---------------------------------------------------------------------------------//
  {
    return totalDisks;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  private void listFiles ()
  // ---------------------------------------------------------------------------------//
  {
    int count = 0;
    for (Record record : records)
    {
      if (record.hasFile ())
        System.out.printf ("%3d %-35s %,7d  %d  %,7d%n", count, record.getFileName (),
            record.getFileSize (), record.getFileType (), record.getUncompressedSize ());
      count++;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    for (Record record : records)
      for (Thread thread : record.threads)
        if (thread.hasDisk ())
          return thread.toString ();

    return "no disk";
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args) throws FileFormatException, IOException
  // ---------------------------------------------------------------------------------//
  {
    File file = new File (
        "/Users/denismolony/Dropbox/Examples/SHK/Disk Disintegrator Deluxe 5.0_D1.SHK");

    NuFX nufx = new NuFX (file.toPath ());
    System.out.println (nufx);
  }
}