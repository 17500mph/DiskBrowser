package com.bytezone.diskbrowser.visicalc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bytezone.diskbrowser.utilities.HexFormatter;
import com.bytezone.diskbrowser.visicalc.Cell.CellType;

public class Sheet
{
  private static final Pattern addressPattern =
      Pattern.compile ("([AB]?[A-Z])([0-9]{1,3}):");
  private static final byte END_OF_LINE_TOKEN = (byte) 0x8D;
  private static final int FORMAT_LENGTH = 3;

  private final Map<Integer, Cell> rowOrderCells = new TreeMap<Integer, Cell> ();
  private final Map<Integer, Cell> columnOrderCells = new TreeMap<Integer, Cell> ();
  private final List<String> lines = new ArrayList<String> ();

  private final Map<Integer, Integer> columnWidths = new TreeMap<Integer, Integer> ();
  private int columnWidth = 9;

  private char globalFormat = ' ';
  private char recalculation = 'A';               // auto/manual
  private char recalculationOrder = 'C';          // row/column 

  private int minColumn = 9999;
  private int maxColumn;
  private int minRow = 9999;
  private int maxRow;

  // Maximum cell = BK254

  //Commands:
  //  /G<address>     goto
  //  /B              blank the cell
  //  /C              clear and home
  //  A-Z             label (" to force a label)
  //  0-9.+-()*/@#    value (+ to force a value)

  //  /S              Storage (ISLDWR)
  //  /SI             Storage init
  //  /SS             Storage Save
  //  /SL             Storage Load
  //  /SD             Storage Delete
  //  /SW             Storage Write (to cassette)
  //  /SR             Storage Read (from cassette)

  //  /R              Replicate

  //  /G              Global (CORF)
  //  /GF             Global Format (DGILR$*)
  //  /GFI            Global Format Integer
  //  /GF$            Global Format Currency
  //  /GC             Global Column <width>
  //  /GR             Global 
  //  /GRA            Recalculation Auto
  //  /GO             Global 
  //  /GOC            Calculation Order - Columns first
  //  /GOR            Calculation Order - Rows first

  //  /T              Titles (HVBN)
  //  /TH             fix Horizontal Titles
  //  /TV             fix Vertical Titles
  //  /TB             fix Both Titles
  //  /TN             fix Neither

  //  /W              Window (HV1SU)
  //  /WV             Window Vertical (split on cursor column)
  //  /WH             Window Horizontal (split on cursor row)

  /*
  from: Apple II TextFiles
  http://www.textfiles.com/apple/
    
                   
                      *----------------------*
                       VISICALC COMMAND CHART
                           BY: NIGHT HAWK
                      *----------------------*
  
  /B   SET AN ENTRY TO BLANK
  
  /C   CLEARS THE SHEET, SETTING ALL ENTRIES TO BLANK
  
  /D   DELETE
      /DR THE ROW
      /DC COLUMN ON WHICH THE CURSOR LIES
  
  /E   ALLOWS EDITING OF THE ENTRY CONTENTS OF ANY ENTRY POSITION
       BY REDISPLAYING IT ON THE EDIT LINE. USE <- -> KEYS & ESC.
  
  /F   FORMATS:
       /FG  GENERAL
       /FI  INTEGER
       /F$  DOLLAR AND CENTS
       /FL  LEFT JUSTIFIED
       /FR  RIGHT JUSTIFIED
       /F*  GRAPH
       /FD  DEFAULT
  
  /G   GLOBAL COMMANDS. THESE APPLY TO THE ENTIRE SHEET OR WINDOW.
           /GC  SETS COLUMN WIDTH
           /GF  SETS THE GLOBAL DEFAULT FORMAT
           /GO  SETS THE ORDER OF RECALCULATION TO BE DOWN THE
                COLUMNS OR ACROSS THE ROWS
           /GR  SETS RECALCULATION TO BE AUTOMATIC(/GRA) OR MANUAL(/GRM).
  
  /I   INSERTS A ROW(/IR) OR A COLUMN(/IC)
  /M   MOVES AN ENTIRE ROW OR COLUMN TO A NEW POSITION.
  /P   PRINT COMMAND
  /R   REPLICATE COMMAND
   
  /S   STORAGE COMMANDS
      /SS  SAVE
      /SL  LOAD (left/right arrow to scroll through catalog)
      /SD  DELETES SPECIFIED FILE ON DISK
      /SI  INITIALIZE A DISK ON SPECIFIED DRIVE
      /SQ  QUITS VISICALC
  
  /T   
      /TH SETS A HORIZONTAL TITLE AREA
      /TV SETS A VERTICAL TITLE AREA
      /TB SET BOTH A HORIZONTAL & VERTICAL TITLE AREA
      /TN RESETS THE WINDOWS TO HAVE NO TITLE AREAS
      
  /V   DISPLAYS VISICALC'S VERSION NUMBER ON THE PROMPT LINE
  /W   WINDOW CONTROL
      /WH  HORIZONTAL WINDOW
      /WV  VERTICAL WINDOW
      /W1  RETURNS SCREEN TO ONE WINDOW
      /WS  SYNCHRONIZED WINDOWS
      /WU  UNSYNCHRONIZED
  
  /-  REPEATING LABEL
  */

  // /X!/X>A3:>A7:      A3:top-left cell in window, A7:cell to place cursor

  public Sheet (byte[] buffer)
  {
    int last = buffer.length;
    while (buffer[--last] == 0)     // ignore trailing zeroes
      ;

    int ptr = 0;
    while (ptr < last)
    {
      int length = getLineLength (buffer, ptr);
      String line = HexFormatter.getString (buffer, ptr, length);
      assert !line.isEmpty ();
      lines.add (line);

      if (line.startsWith ("/"))
        doFormat (line);
      else if (line.startsWith (">"))       // GOTO cell
        processLine (line);
      else
        System.out.printf ("Error [%s]%n", line);

      ptr += length + 1;            // +1 for end-of-line token
    }

    // might have to keep recalculating until nothing changes??
    System.out.println ("\n*********** Calculating\n");
    calculate (recalculationOrder);
    //    System.out.println ("\n*********** Calculating\n");
    //    calculate (recalculationOrder);
  }

  private void calculate (char order)
  {
    int count = 0;
    Map<Integer, Cell> cells = order == 'R' ? rowOrderCells : columnOrderCells;
    for (Cell cell : cells.values ())
      if (cell.isCellType (CellType.VALUE))
      {
        System.out.printf ("%5d  start %s%n", count, cell.getAddressText ());
        cell.calculate ();
        System.out.printf ("%5d  stop  %s%n", count++, cell.getAddressText ());
      }
  }

  private int getLineLength (byte[] buffer, int offset)
  {
    int ptr = offset;
    while (buffer[ptr] != END_OF_LINE_TOKEN)
      ptr++;
    return ptr - offset;
  }

  private void processLine (String line)
  {
    Cell currentCell = null;

    Matcher m = addressPattern.matcher (line);          // <cell address>:<contents>
    if (m.find ())
    {
      Address address = new Address (m.group (1), m.group (2));
      currentCell = rowOrderCells.get (address.getRowKey ());

      int pos = line.indexOf (':');                     // end of cell address
      line = line.substring (pos + 1);                  // remove address from line

      if (currentCell == null)
      {
        currentCell = new Cell (this, address);
        if (!line.startsWith ("/G"))
          addCell (currentCell);
      }
    }
    else
    {
      System.out.printf ("Invalid cell address: %s%n", line);
      return;
    }

    assert currentCell != null;

    if (line.startsWith ("/G"))                         // global column widths
    {
      if (line.charAt (2) == 'C' && line.charAt (3) == 'C')
      {
        int width = Integer.parseInt (line.substring (4));
        columnWidths.put (currentCell.getAddress ().getColumn (), width);
      }
      else
        System.out.printf ("Unknown Global:[%s]%n", line);

      return;
    }

    // check for formatting commands
    while (line.startsWith ("/"))
    {
      if (line.charAt (1) == '-')                 // repeating label
      {
        currentCell.setFormat (line);
        line = "";
      }
      else
      {
        currentCell.setFormat (line.substring (0, FORMAT_LENGTH));
        line = line.substring (FORMAT_LENGTH);
      }
    }

    // if there is anything left it must be an expression
    if (!line.isEmpty ())
      currentCell.setValue (line);               // expression
  }

  private void addCell (Cell cell)
  {
    rowOrderCells.put (cell.getAddress ().getRowKey (), cell);
    columnOrderCells.put (cell.getAddress ().getColumnKey (), cell);

    minRow = Math.min (minRow, cell.getAddress ().getRow ());
    minColumn = Math.min (minColumn, cell.getAddress ().getColumn ());

    maxRow = Math.max (maxRow, cell.getAddress ().getRow ());
    maxColumn = Math.max (maxColumn, cell.getAddress ().getColumn ());
  }

  Cell getCell (String addressText)
  {
    return getCell (new Address (addressText));
  }

  Cell getCell (Address address)
  {
    Cell cell = rowOrderCells.get (address.getRowKey ());
    if (cell == null)
    {
      cell = new Cell (this, address);
      addCell (cell);
    }
    return cell;
  }

  boolean cellExists (Address address)
  {
    return rowOrderCells.get (address.getRowKey ()) != null;
  }

  public int size ()
  {
    return rowOrderCells.size ();
  }

  private void doFormat (String line)
  {
    switch (line.charAt (1))
    {
      case 'G':
        setGlobal (line);
        break;
      case 'W':
        break;
      case 'X':
        break;
      default:
        System.out.printf ("Skipping [%s]%n", line);
    }
  }

  private void setGlobal (String line)
  {
    switch (line.charAt (2))
    {
      case 'C':
        columnWidth = Integer.parseInt (line.substring (3));
        break;
      case 'O':
        recalculationOrder = line.charAt (3);
        break;
      case 'R':
        recalculation = line.charAt (3);
        break;
      case 'F':
        globalFormat = line.charAt (3);
        break;
      case 'P':
        //              System.out.printf ("Skipping [%s]%n", line);
        break;
      default:
        System.out.printf ("Unknown global format [%s]%n", line);
        break;
    }
  }

  public String getTextDisplay (boolean debug)
  {
    StringBuilder text = new StringBuilder ();
    String longLine;
    if (false)
      longLine = "%+++++++++++++++++++++++++++++++++++++++++++++++++++++++"
          + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++";
    else
      longLine = "                                                          "
          + "                                                                      ";
    String underline = "---------------------------------------------------------"
        + "-----------------------------------------------------------------";
    String left = "";
    String right = "";
    if (columnWidth > 2)
    {
      left = underline.substring (0, (columnWidth - 2) / 2);
      right = underline.substring (0, columnWidth - 3 - left.length ()) + "+";
    }

    int lastRow = 0;
    int lastColumn = 0;

    StringBuilder heading = new StringBuilder ("    ");
    for (int column = 0; column <= maxColumn; column++)
    {
      int width = columnWidth;
      if (columnWidths.containsKey (column))
        width = columnWidths.get (column);

      char letter1 = column < 26 ? '-' : column < 52 ? 'A' : 'B';
      char letter2 = (char) ((column % 26) + 'A');

      if (width == 1)
        heading.append (letter2);
      else if (width == 2)
        heading.append (String.format ("%s%s", letter1, letter2));
      else
      {
        String fmt =
            String.format ("%s%s%%%d.%ds", letter1, letter2, (width - 2), (width - 2));
        heading.append (String.format (fmt, underline));
        //        heading.append (left);
        //        heading.append (String.format ("%s%s", letter1, letter2));
        //        heading.append (right);
      }
    }

    text.append (String.format ("Global format : %s%n", globalFormat));
    text.append (String.format ("Column width  : %d%n", columnWidth));
    text.append (String.format ("Recalc  order : %s%n",
        recalculationOrder == 'R' ? "Row" : "Column"));
    text.append (String.format ("Recalculation : %s%n",
        recalculation == 'A' ? "Automatic" : "Manual"));
    text.append (String.format ("Cells         : %d%n", size ()));

    if (size () > 0)
      text.append (String.format ("Range         : %s:%s%n%n",
          Address.getCellName (minRow + 1, minColumn),
          Address.getCellName (maxRow + 1, maxColumn)));
    else
      text.append ("\n\n");

    if (debug)
    {
      text.append (heading);
      text.append ("\n001:");
    }

    for (Cell cell : rowOrderCells.values ())
    {
      Address cellAddress = cell.getAddress ();

      // insert newlines for empty rows
      while (lastRow < cellAddress.getRow ())
      {
        ++lastRow;
        lastColumn = 0;
        if (debug)
          text.append (String.format ("%n%03d:", lastRow + 1));
        else
          text.append ("\n");
      }

      // pad out empty columns
      while (lastColumn < cellAddress.getColumn ())
      {
        int width = columnWidth;
        if (columnWidths.containsKey (lastColumn))
          width = columnWidths.get (lastColumn);
        text.append (longLine.substring (0, width));
        ++lastColumn;
      }

      ++lastColumn;

      int colWidth = columnWidth;
      if (columnWidths.containsKey (cellAddress.getColumn ()))
        colWidth = columnWidths.get (cellAddress.getColumn ());

      text.append (cell.getText (colWidth, globalFormat));
    }

    if (debug)
    {
      text.append ("\n\n");
      int last = -1;
      for (Cell cell : columnOrderCells.values ())
      {
        if (last < cell.getAddress ().getColumn ())
        {
          String columnName = Address.getCellName (1, cell.getAddress ().getColumn ());
          columnName = columnName.substring (0, columnName.length () - 1);
          text.append ("\n                                    *** Column " + columnName
              + " ***\n\n");
          last = cell.getAddress ().getColumn ();
        }
        text.append (cell.getDebugText ());
        text.append ("\n");
      }

      text.append ("\n");
      for (String line : lines)
      {
        text.append (line);
        text.append ("\n");
      }

      if (text.length () > 0)
        text.deleteCharAt (text.length () - 1);
    }

    return text.toString ();
  }
}