package com.bytezone.diskbrowser.gui;

/*****************************************************************************************
 * JPanel used to display a scrolling JTree containing details of a single disk. The JTree
 * consists entirely of AppleFileSource objects. Any number of these objects are contained
 * in Catalog Panel, along with a single FileSystemTab.
 ****************************************************************************************/

import java.awt.Font;
import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import com.bytezone.diskbrowser.applefile.AppleFileSource;
import com.bytezone.diskbrowser.disk.DiskFactory;
import com.bytezone.diskbrowser.disk.FormattedDisk;
import com.bytezone.diskbrowser.gui.RedoHandler.RedoEvent;

class AppleDiskTab extends AbstractTab
{
  FormattedDisk disk;

  public AppleDiskTab (FormattedDisk disk, DiskAndFileSelector selector,
      RedoHandler redoHandler, Font font, FileSelectedEvent event)
  {
    super (redoHandler, selector, font);
    create (disk);
    redoHandler.fileSelected (event);
  }

  public AppleDiskTab (FormattedDisk disk, DiskAndFileSelector selector,
      RedoHandler redoHandler, Font font, SectorSelectedEvent event)
  {
    super (redoHandler, selector, font);
    create (disk);
    redoHandler.sectorSelected (event);
  }

  // This constructor is only called when lastFileUsed is not null, but the disk
  // couldn't find the file entry. Either the file has been deleted, or it is a disk
  // with redefined files (Wizardry, Infocom etc).
  public AppleDiskTab (FormattedDisk disk, DiskAndFileSelector selector,
      RedoHandler navMan, Font font, String lastFileUsed)
  {
    super (navMan, selector, font);
    create (disk);
    //    System.out.println ("ooh - couldn't find the previous file");
    DefaultMutableTreeNode node = findNode (lastFileUsed);
    if (node != null)
    {
      AppleFileSource afs = (AppleFileSource) node.getUserObject ();
      FileSelectedEvent event = new FileSelectedEvent (this, afs);
      navMan.fileSelected (event);
    }
  }

  // User is selecting a new disk from the catalog
  public AppleDiskTab (FormattedDisk disk, DiskAndFileSelector selector,
      RedoHandler navMan, Font font)
  {
    super (navMan, selector, font);
    create (disk);

    AppleFileSource afs = (AppleFileSource) findNode (2).getUserObject (); // select Catalog
    if (afs == null)
      afs = (AppleFileSource) findNode (1).getUserObject (); // select Disk
    navMan.fileSelected (new FileSelectedEvent (this, afs));
  }

  private void create (FormattedDisk disk)
  {
    this.disk = disk;
    setTree (disk.getCatalogTree ());
    setSelectionListener (tree);
  }

  @Override
  public void activate ()
  {
    //    System.out.println ("=========== Activating AppleDiskTab =============");
    eventHandler.redo = true;
    eventHandler.fireDiskSelectionEvent (disk);
    eventHandler.redo = false;
    tree.setSelectionPath (null); // turn off any current selection to force an event
    navMan.setCurrentData (redoData);
  }

  @Override
  public void refresh () // called when the user gives ALT-R command
  {
    Object o = getSelectedObject ();
    String currentFile = (o == null) ? null : ((AppleFileSource) o).getUniqueName ();
    disk = DiskFactory.createDisk (disk.getAbsolutePath ());
    setTree (disk.getCatalogTree ());
    setSelectionListener (tree);
    selectNode (currentFile);
  }

  private void selectNode (String nodeName)
  {
    DefaultMutableTreeNode selectNode = null;
    if (nodeName != null)
      selectNode = findNode (nodeName);
    if (selectNode == null)
      selectNode = findNode (2);
    if (selectNode != null)
      showNode (selectNode);
    else
      System.out.println ("First node not found");
  }

  void redoEvent (RedoEvent event)
  {
    selectNode (((FileSelectedEvent) event.value).file.getUniqueName ());
  }

  private DefaultMutableTreeNode findNode (String nodeName)
  {
    DefaultMutableTreeNode rootNode = getRootNode ();
    Enumeration<DefaultMutableTreeNode> children = rootNode.breadthFirstEnumeration ();
    while (children.hasMoreElements ())
    {
      DefaultMutableTreeNode node = children.nextElement ();
      Object o = node.getUserObject ();
      if (o instanceof AppleFileSource)
      {
        AppleFileSource afs = (AppleFileSource) node.getUserObject ();
        if (nodeName.equals (afs.getUniqueName ()))
          return node;
      }
    }
    return null;
  }

  public boolean contains (FormattedDisk disk)
  {
    return this.disk.getAbsolutePath ().equals (disk.getAbsolutePath ());
  }

  // This action is triggered by AppleDiskTab.selectNode (String), which calls
  // AbstractTab.showNode (DefaultMutableTreeNode). That will trigger this listener
  // ONLY if the value is different, so it is set to null first to force the event.
  private void setSelectionListener (JTree tree)
  {
    tree.addTreeSelectionListener (new TreeSelectionListener ()
    {
      @Override
      public void valueChanged (TreeSelectionEvent e)
      {
        // A null happens when there is a click in the DiskLayoutPanel, in order
        // to turn off the currently selected file
        AppleFileSource afs = (AppleFileSource) getSelectedObject ();
        if (afs != null)
          eventHandler.fireFileSelectionEvent (afs);
      }
    });
  }
}