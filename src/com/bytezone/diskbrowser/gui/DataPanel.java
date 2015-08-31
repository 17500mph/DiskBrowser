package com.bytezone.diskbrowser.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bytezone.common.FontAction.FontChangeEvent;
import com.bytezone.common.FontAction.FontChangeListener;
import com.bytezone.diskbrowser.disk.DiskAddress;
import com.bytezone.diskbrowser.disk.SectorList;

class DataPanel extends JTabbedPane
    implements DiskSelectionListener, FileSelectionListener, SectorSelectionListener,
    //      PreferenceChangeListener, 
    FileNodeSelectionListener, FontChangeListener
{
  private static final int TEXT_WIDTH = 65;

  JTextArea hexText;
  JTextArea disassemblyText;

  // these two panes are interchangeable
  JScrollPane formattedPane;
  JScrollPane imagePane;

  JTextArea formattedText;
  ImagePanel imagePanel;// internal class

  boolean imageVisible = false;

  // used to determine whether the text has been set
  boolean formattedTextValid;
  boolean hexTextValid;
  boolean assemblerTextValid;
  DataSource currentDataSource;

  //  private Font font;
  final MenuHandler menuHandler;

  public DataPanel (MenuHandler mh, Preferences prefs)
  {
    //    String dataFontName =
    //          prefs.get (PreferencesDialog.prefsDataFont, PreferencesDialog.defaultFontName);
    //    System.out.println (dataFontName);
    //    int dataFontSize =
    //          prefs.getInt (PreferencesDialog.prefsDataFontSize, PreferencesDialog.defaultFontSize);
    //    font = new Font (dataFontName, Font.PLAIN, dataFontSize);

    this.menuHandler = mh;
    setTabPlacement (SwingConstants.BOTTOM);

    formattedText = new JTextArea (10, TEXT_WIDTH);
    formattedPane = setPanel (formattedText, "Formatted");
    formattedText.setLineWrap (mh.lineWrapItem.isSelected ());
    formattedText.setText ("Please use the 'File->Set HOME folder...' command to "
        + "\ntell DiskBrowser where your Apple disks are located."
        + "\n\nTo see the contents of a disk in more detail, double-click"
        + "\nthe disk. You will then be able to select individual files to view completely.");

    hexText = new JTextArea (10, TEXT_WIDTH);
    setPanel (hexText, "Hex dump");

    disassemblyText = new JTextArea (10, TEXT_WIDTH);
    setPanel (disassemblyText, "Disassembly");

    imagePanel = new ImagePanel ();
    imagePane =
        new JScrollPane (imagePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            // imagePane.getVerticalScrollBar ().setUnitIncrement (font.getSize ());

    //    setTabsFont (font);
    //    this.setMinimumSize (new Dimension (800, 200));

    addChangeListener (new ChangeListener ()
    {
      @Override
      public void stateChanged (ChangeEvent e)
      {
        switch (getSelectedIndex ())
        {
          case 0:
            if (!formattedTextValid)
            {
              if (currentDataSource == null)
                formattedText.setText ("");
              else
                setText (formattedText, currentDataSource.getText ());
              formattedTextValid = true;
            }
            break;
          case 1:
            if (!hexTextValid)
            {
              if (currentDataSource == null)
                hexText.setText ("");
              else
                setText (hexText, currentDataSource.getHexDump ());
              hexTextValid = true;
            }
            break;
          case 2:
            if (!assemblerTextValid)
            {
              if (currentDataSource == null)
                disassemblyText.setText ("");
              else
                setText (disassemblyText, currentDataSource.getAssembler ());
              assemblerTextValid = true;
            }
            break;
          default:
            System.out.println ("Invalid index selected in DataPanel");
        }
      }
    });

    mh.lineWrapItem.setAction (new LineWrapAction (formattedText));
  }

  private void setTabsFont (Font font)
  {
    formattedText.setFont (font);
    hexText.setFont (font);
    disassemblyText.setFont (font);
    imagePane.getVerticalScrollBar ().setUnitIncrement (font.getSize ());
  }

  public String getCurrentText ()
  {
    int index = getSelectedIndex ();
    return index == 0 ? formattedText.getText ()
        : index == 1 ? hexText.getText () : disassemblyText.getText ();
  }

  private JScrollPane setPanel (JTextArea outputPanel, String tabName)
  {
    outputPanel.setEditable (false);
    outputPanel.setMargin (new Insets (5, 5, 5, 5));

    JScrollPane outputScrollPane =
        new JScrollPane (outputPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    outputScrollPane.setBorder (null);// remove the ugly default border
    add (outputScrollPane, tabName);
    return outputScrollPane;
  }

  private void setDataSource (DataSource dataSource)
  {
    currentDataSource = dataSource;
    if (dataSource == null)
    {
      formattedText.setText ("");
      hexText.setText ("");
      disassemblyText.setText ("");
      checkImage ();
      return;
    }

    switch (getSelectedIndex ())
    {
      case 0:
        try
        {
          setText (formattedText, dataSource.getText ());
        }
        catch (Exception e)
        {
          setText (formattedText, e.toString ());
          e.printStackTrace ();
        }
        hexTextValid = false;
        assemblerTextValid = false;
        break;
      case 1:
        setText (hexText, dataSource.getHexDump ());
        formattedTextValid = false;
        assemblerTextValid = false;
        break;
      case 2:
        setText (disassemblyText, dataSource.getAssembler ());
        hexTextValid = false;
        formattedTextValid = false;
        break;
      default:
        System.out.println ("Invalid index selected in DataPanel");
    }

    BufferedImage image = dataSource.getImage ();
    if (image == null)
    {
      checkImage ();
    }
    else
    {
      imagePanel.setImage (image);
      imagePane.setViewportView (imagePanel);
      if (!imageVisible)
      {
        int selected = getSelectedIndex ();
        remove (formattedPane);
        add (imagePane, "Formatted", 0);
        setSelectedIndex (selected);
        imageVisible = true;
      }
    }
  }

  private void checkImage ()
  {
    if (imageVisible)
    {
      int selected = getSelectedIndex ();
      remove (imagePane);
      add (formattedPane, "Formatted", 0);
      setSelectedIndex (selected);
      imageVisible = false;
    }
  }

  private void setText (JTextArea textArea, String text)
  {
    textArea.setText (text);
    textArea.setCaretPosition (0);
  }

  private class ImagePanel extends JPanel
  {
    private BufferedImage image;
    private int scale = 1;

    public ImagePanel ()
    {
      this.setBackground (Color.gray);
    }

    private void setImage (BufferedImage image)
    {
      this.image = image;

      if (image != null)
      {
        Graphics2D g2 = image.createGraphics ();
        g2.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
      }

      int width = image.getWidth ();
      int height = image.getHeight ();

      if (width < 400)
        scale = (400 - 1) / width + 1;
      if (scale > 4)
        scale = 4;

      setPreferredSize (new Dimension (width * scale, height * scale));
      repaint ();
    }

    @Override
    public void paintComponent (Graphics g)
    {
      super.paintComponent (g);

      if (image != null)
      {
        Graphics2D g2 = ((Graphics2D) g);
        g2.transform (AffineTransform.getScaleInstance (scale, scale));
        g2.drawImage (image, (getWidth () - image.getWidth () * scale) / 2 / scale, 4,
                      this);
      }
    }
  }

  @Override
  public void diskSelected (DiskSelectedEvent event)
  {
    setSelectedIndex (0);
    setDataSource (null);
    if (event.getFormattedDisk () != null)
      setDataSource (event.getFormattedDisk ().getCatalog ().getDataSource ());
    else
      System.out.println ("bollocks in diskSelected()");
  }

  @Override
  public void fileSelected (FileSelectedEvent event)
  {
    setDataSource (event.file.getDataSource ());
  }

  @Override
  public void sectorSelected (SectorSelectedEvent event)
  {
    List<DiskAddress> sectors = event.getSectors ();
    if (sectors == null || sectors.size () == 0)
      return;

    if (sectors.size () == 1)
      setDataSource (event.getFormattedDisk ().getFormattedSector (sectors.get (0)));
    else
      setDataSource (new SectorList (event.getFormattedDisk (), sectors));
  }

  //  @Override
  //  public void preferenceChange (PreferenceChangeEvent evt)
  //  {
  //    if (evt.getKey ().equals (PreferencesDialog.prefsDataFont))
  //      font = new Font (evt.getNewValue (), Font.PLAIN, font.getSize ());
  //    if (evt.getKey ().equals (PreferencesDialog.prefsDataFontSize))
  //      font = new Font (font.getFontName (), Font.PLAIN, Integer.parseInt (evt.getNewValue ()));
  //    setTabsFont (font);
  //  }

  @Override
  public void fileNodeSelected (FileNodeSelectedEvent event)
  {
    setSelectedIndex (0);
    setDataSource (event.getFileNode ());
    //    FileNode node = event.getFileNode ();
  }

  @Override
  public void changeFont (FontChangeEvent fontChangeEvent)
  {
    setTabsFont (fontChangeEvent.font);
  }
}