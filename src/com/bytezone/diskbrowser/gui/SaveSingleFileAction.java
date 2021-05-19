package com.bytezone.diskbrowser.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.bytezone.diskbrowser.applefile.AppleFileSource;
import com.bytezone.diskbrowser.utilities.DefaultAction;

// -----------------------------------------------------------------------------------//
class SaveSingleFileAction extends DefaultAction
//-----------------------------------------------------------------------------------//
{
  AppleFileSource appleFileSource;

  // ---------------------------------------------------------------------------------//
  SaveSingleFileAction ()
  // ---------------------------------------------------------------------------------//
  {
    super ("Save file...", "Save currently selected file");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void actionPerformed (ActionEvent evt)
  // ---------------------------------------------------------------------------------//
  {
    if (appleFileSource == null)
    {
      System.out.println ("No data source");
      return;
    }

    JFileChooser fileChooser = new JFileChooser ();
    fileChooser.setDialogTitle ("Save File");
    fileChooser.setSelectedFile (new File (appleFileSource.getUniqueName () + ".bin"));

    if (fileChooser.showSaveDialog (null) == JFileChooser.APPROVE_OPTION)
    {
      File file = fileChooser.getSelectedFile ();
      try
      {
        Files.write (file.toPath (), appleFileSource.getDataSource ().getBuffer (),
            StandardOpenOption.CREATE_NEW);
        JOptionPane.showMessageDialog (null, "File saved");
      }
      catch (IOException e)
      {
        e.printStackTrace ();
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  void setFile (AppleFileSource dataSource)
  // ---------------------------------------------------------------------------------//
  {
    this.appleFileSource = dataSource;
  }
}
