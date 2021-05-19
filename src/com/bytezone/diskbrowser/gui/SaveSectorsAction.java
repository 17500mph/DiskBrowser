package com.bytezone.diskbrowser.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

// -----------------------------------------------------------------------------------//
class SaveSectorsAction extends AbstractSaveAction implements SectorSelectionListener
// -----------------------------------------------------------------------------------//
{
  SectorSelectedEvent event;

  // ---------------------------------------------------------------------------------//
  SaveSectorsAction ()
  // ---------------------------------------------------------------------------------//
  {
    super ("Save sectors...", "Save currently selected sectors");
    this.setEnabled (false);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void actionPerformed (ActionEvent evt)
  // ---------------------------------------------------------------------------------//
  {
    if (event == null)
    {
      JOptionPane.showMessageDialog (null, "No sectors selected");
      return;
    }

    if (fileChooser == null)
    {
      fileChooser = new JFileChooser ();
      fileChooser.setDialogTitle ("Save sectors");
    }

    fileChooser.setSelectedFile (new File ("savedSectors.bin"));

    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION)
    {
      File file = fileChooser.getSelectedFile ();
      try
      {
        byte[] buffer =
            event.getFormattedDisk ().getDisk ().readBlocks (event.getSectors ());
        Files.write (file.toPath (), buffer, StandardOpenOption.CREATE_NEW);
        JOptionPane.showMessageDialog (null, "File saved");
      }
      catch (IOException e)
      {
        e.printStackTrace ();
        JOptionPane.showMessageDialog (null, "File failed to save");
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void sectorSelected (SectorSelectedEvent event)
  // ---------------------------------------------------------------------------------//
  {
    this.event = event;
    this.setEnabled (true);
  }
}
