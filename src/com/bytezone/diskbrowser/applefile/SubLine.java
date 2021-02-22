package com.bytezone.diskbrowser.applefile;

import static com.bytezone.diskbrowser.utilities.Utility.isDigit;
import static com.bytezone.diskbrowser.utilities.Utility.isHighBitSet;
import static com.bytezone.diskbrowser.utilities.Utility.isLetter;
import static com.bytezone.diskbrowser.utilities.Utility.isPossibleNumber;
import static com.bytezone.diskbrowser.utilities.Utility.isPossibleVariable;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.diskbrowser.utilities.HexFormatter;
import com.bytezone.diskbrowser.utilities.Utility;;

// -----------------------------------------------------------------------------------//
public class SubLine implements ApplesoftConstants
// -----------------------------------------------------------------------------------//
{
  SourceLine parent;

  byte[] buffer;
  int startPtr;
  int length;

  String[] nextVariables;
  String forVariable = "";

  int equalsPosition;               // used for aligning the equals sign

  String functionArgument;
  String functionName;

  String callTarget;

  private final List<Integer> gotoLines = new ArrayList<> ();
  private final List<Integer> gosubLines = new ArrayList<> ();

  private final List<String> variables = new ArrayList<> ();
  private final List<String> functions = new ArrayList<> ();
  private final List<String> arrays = new ArrayList<> ();

  private final List<Integer> constantsInt = new ArrayList<> ();
  private final List<Float> constantsFloat = new ArrayList<> ();

  private final List<String> stringsText = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  SubLine (SourceLine parent, int startPtr, int length)
  // ---------------------------------------------------------------------------------//
  {
    this.parent = parent;
    this.startPtr = startPtr;
    this.length = length;
    this.buffer = parent.buffer;

    int ptr = startPtr;
    byte firstByte = buffer[startPtr];

    if (isToken (firstByte))
    {
      doToken (firstByte);
      if (is (TOKEN_REM) || is (TOKEN_DATA))      // no further processing
        return;

      if (is (TOKEN_CALL))
        ptr = startPtr + callTarget.length ();
      else
        ptr = startPtr + 1;
    }
    else
    {
      ptr = startPtr;
      if (isDigit (firstByte))                   // split IF xx THEN nnn 
      {
        addXref (getLineNumber (buffer, startPtr), gotoLines);
        return;
      }
      else if (isLetter (firstByte))             // variable assignment
        setEqualsPosition ();
      else if (isEndOfLine (firstByte))          // empty subline
        return;
      else                                       // probably Beagle Bros 0D or 0A
        System.out.printf ("%s unexpected bytes at line %5d:%n%s%n", parent.parent.name,
            parent.lineNumber, HexFormatter.formatNoHeader (buffer, startPtr, length));
    }

    String var = "";

    boolean inQuote = false;
    boolean inFunction = false;
    boolean inDefine = false;
    int stringPtr = 0;

    int max = startPtr + length - 1;
    while (isEndOfLine (buffer[max]))
      --max;

    while (ptr <= max)
    {
      byte b = buffer[ptr++];

      if (inDefine)                                 // ignore the name and argument
      {
        if (b == TOKEN_EQUALS)
          inDefine = false;
        continue;
      }

      if (b == TOKEN_DEF)
      {
        inDefine = true;
        continue;
      }

      if (b == TOKEN_FN)
      {
        assert !inDefine;
        inFunction = true;
        continue;
      }

      if (inQuote)
      {
        if (b == Utility.ASCII_QUOTE)      // ignore strings
        {
          inQuote = false;
          addString (stringPtr, ptr);
        }
        continue;
      }

      if (b == Utility.ASCII_QUOTE)
      {
        inQuote = true;
        stringPtr = ptr;
        continue;
      }

      if (isPossibleVariable (b) || isPossibleNumber (b))
      {
        if (var.isEmpty () && isPossibleNumber (b) && buffer[ptr - 2] == TOKEN_MINUS)
          var = "-";

        var += (char) b;

        // allow for PRINT A$B$
        if ((b == Utility.ASCII_DOLLAR || b == Utility.ASCII_PERCENT)   // var name end
            && buffer[ptr] != Utility.ASCII_LEFT_BRACKET)               // not an array
        {
          checkVar (var, b);
          var = "";
        }
      }
      else
      {
        if (inFunction)
        {
          checkFunction (var, b);
          inFunction = false;
        }
        else
          checkVar (var, b);

        var = "";
      }
    }

    if (inQuote)
      addString (stringPtr, ptr);     // unterminated string
    else
      checkVar (var, (byte) 0);       // unprocessed variable or number
  }

  // ---------------------------------------------------------------------------------//
  private boolean isEndOfLine (byte b)
  // ---------------------------------------------------------------------------------//
  {
    return b == 0 || b == Utility.ASCII_COLON;
  }

  // ---------------------------------------------------------------------------------//
  private void addString (int stringPtr, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    stringsText.add (new String (buffer, stringPtr - 1, ptr - stringPtr + 1));
  }

  // ---------------------------------------------------------------------------------//
  private void checkFunction (String var, byte terminator)
  // ---------------------------------------------------------------------------------//
  {
    assert terminator == Utility.ASCII_LEFT_BRACKET;

    if (!functions.contains (var))
      functions.add (var);
  }

  // ---------------------------------------------------------------------------------//
  private void checkVar (String var, byte terminator)
  // ---------------------------------------------------------------------------------//
  {
    if (var.length () == 0)
      return;

    if (!isLetter ((byte) var.charAt (0)))
    {
      if (is (TOKEN_GOTO) || is (TOKEN_GOSUB) || is (TOKEN_ON) || is (TOKEN_ONERR))
        return;                     // ignore line numbers
      addNumber (var);
      return;
    }

    if (is (TOKEN_DEF) && (var.equals (functionName) || var.equals (functionArgument)))
      return;

    if (terminator == Utility.ASCII_LEFT_BRACKET)
    {
      if (!arrays.contains (var))
        arrays.add (var);
    }
    else if (!variables.contains (var))
      variables.add (var);
  }

  // ---------------------------------------------------------------------------------//
  private void doToken (byte b)
  // ---------------------------------------------------------------------------------//
  {
    switch (b)
    {
      case TOKEN_FOR:
        int p = startPtr + 1;
        while (buffer[p] != TOKEN_EQUALS)
          forVariable += (char) buffer[p++];
        break;

      case TOKEN_NEXT:
        if (length == 2)                // no variables
          nextVariables = new String[0];
        else
        {
          String varList = new String (buffer, startPtr + 1, length - 2);
          nextVariables = varList.split (",");
        }
        break;

      case TOKEN_LET:
        setEqualsPosition ();
        break;

      case TOKEN_GOTO:
        int targetLine = getLineNumber (buffer, startPtr + 1);
        addXref (targetLine, gotoLines);
        break;

      case TOKEN_GOSUB:
        targetLine = getLineNumber (buffer, startPtr + 1);
        addXref (targetLine, gosubLines);
        break;

      case TOKEN_ON:
        p = startPtr + 1;
        int max = startPtr + length - 1;
        while (p < max && buffer[p] != ApplesoftConstants.TOKEN_GOTO
            && buffer[p] != ApplesoftConstants.TOKEN_GOSUB)
          p++;

        switch (buffer[p++])
        {
          case TOKEN_GOSUB:
            for (int destLine : getLineNumbers (buffer, p))
              addXref (destLine, gosubLines);
            break;

          case TOKEN_GOTO:
            for (int destLine : getLineNumbers (buffer, p))
              addXref (destLine, gotoLines);
            break;

          default:
            System.out.println ("GOTO / GOSUB not found");
        }
        break;

      case TOKEN_ONERR:
        if (buffer[startPtr + 1] == ApplesoftConstants.TOKEN_GOTO)
        {
          targetLine = getLineNumber (buffer, startPtr + 2);
          addXref (targetLine, gotoLines);
        }
        break;

      case TOKEN_CALL:
        callTarget = getCallTarget ();
        break;

      case TOKEN_DEF:
        byte[] lineBuffer = getBuffer ();
        assert lineBuffer[0] == TOKEN_FN;

        int leftBracket = getPosition (lineBuffer, 1, Utility.ASCII_LEFT_BRACKET);
        int rightBracket =
            getPosition (lineBuffer, leftBracket + 1, Utility.ASCII_RIGHT_BRACKET);

        functionName = new String (lineBuffer, 1, leftBracket - 1);
        functionArgument =
            new String (lineBuffer, leftBracket + 1, rightBracket - leftBracket - 1);
        functions.add (functionName);

        break;

      case TOKEN_DATA:
        for (String chunk : new String (getBuffer ()).split (","))
        {
          chunk = chunk.trim ();
          if (chunk.isEmpty ())
            continue;
          b = (byte) chunk.charAt (0);
          if (isPossibleNumber (b) || b == Utility.ASCII_MINUS)
          {
            if (!addNumber (chunk))
              stringsText.add (chunk);
          }
          else
            stringsText.add (chunk);
        }

        break;
    }
  }

  // ---------------------------------------------------------------------------------//
  private boolean addNumber (String var)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      if (var.indexOf ('.') < 0)                    // no decimal point
      {
        int varInt = Integer.parseInt (var);
        if (!constantsInt.contains (varInt))
          constantsInt.add (varInt);
      }
      else
      {
        float varFloat = Float.parseFloat (var);
        if (!constantsFloat.contains (varFloat))
          constantsFloat.add (varFloat);
      }
    }
    catch (NumberFormatException nfe)
    {
      return false;
    }
    return true;
  }

  // ---------------------------------------------------------------------------------//
  private String getCallTarget ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    int ptr = startPtr + 1;
    int max = startPtr + length - 1;

    while (ptr < max)
    {
      byte b = buffer[ptr++];
      if (isToken (b))
        text.append (tokens[b & 0x7F]);
      else if (b == Utility.ASCII_COMMA)    // end of call target
        break;
      else
        text.append ((char) b);
    }

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  private int getPosition (byte[] buffer, int start, byte value)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = start; i < buffer.length; i++)
      if (buffer[i] == value)
        return i;

    return -1;
  }

  // ---------------------------------------------------------------------------------//
  private void addXref (int targetLine, List<Integer> list)
  // ---------------------------------------------------------------------------------//
  {
    if (!list.contains (targetLine))
      list.add (targetLine);
  }

  // ---------------------------------------------------------------------------------//
  private List<Integer> getLineNumbers (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    List<Integer> lineNumbers = new ArrayList<> ();
    int start = ptr;

    while (ptr < buffer.length && buffer[ptr] != 0 && buffer[ptr] != Utility.ASCII_COLON)
      ptr++;

    String s = new String (buffer, start, ptr - start);
    String[] chunks = s.split (",");

    try
    {
      for (String chunk : chunks)
        lineNumbers.add (Integer.parseInt (chunk));
    }
    catch (NumberFormatException e)
    {
      System.out.printf ("NFE2: %s%n", s);
    }

    return lineNumbers;
  }

  // ---------------------------------------------------------------------------------//
  private int getLineNumber (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    int lineNumber = 0;

    while (ptr < buffer.length && isDigit (buffer[ptr]))
      lineNumber = lineNumber * 10 + (buffer[ptr++] & 0xFF) - 0x30;

    return lineNumber;
  }

  // ---------------------------------------------------------------------------------//
  boolean isImpliedGoto ()
  // ---------------------------------------------------------------------------------//
  {
    return (isDigit (buffer[startPtr]));
  }

  // Record the position of the equals sign so it can be aligned with adjacent lines.
  // ---------------------------------------------------------------------------------//
  private void setEqualsPosition ()
  // ---------------------------------------------------------------------------------//
  {
    int p = startPtr + 1;
    int max = startPtr + length;

    while (buffer[p] != TOKEN_EQUALS && p < max)
      p++;

    if (buffer[p] == TOKEN_EQUALS)
      equalsPosition = toString ().indexOf ('=');           // use expanded line
  }

  // ---------------------------------------------------------------------------------//
  boolean isJoinableRem ()
  // ---------------------------------------------------------------------------------//
  {
    return is (TOKEN_REM) && !isFirst ();
  }

  // ---------------------------------------------------------------------------------//
  boolean isFirst ()
  // ---------------------------------------------------------------------------------//
  {
    return (parent.linePtr + 4) == startPtr;
  }

  // ---------------------------------------------------------------------------------//
  boolean is (byte token)
  // ---------------------------------------------------------------------------------//
  {
    return buffer[startPtr] == token;
  }

  // ---------------------------------------------------------------------------------//
  boolean has (byte token)
  // ---------------------------------------------------------------------------------//
  {
    int ptr = startPtr + 1;
    int max = startPtr + length;

    while (ptr < max)
    {
      if (buffer[ptr++] == token)
        return true;
    }
    return false;
  }

  // ---------------------------------------------------------------------------------//
  boolean isEmpty ()
  // ---------------------------------------------------------------------------------//
  {
    return length == 1 && buffer[startPtr] == 0;
  }

  // ---------------------------------------------------------------------------------//
  boolean containsToken ()
  // ---------------------------------------------------------------------------------//
  {
    // ignore first byte, check the rest for tokens
    for (int p = startPtr + 1, max = startPtr + length; p < max; p++)
      if (isToken (buffer[p]))
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  boolean containsControlChars ()
  // ---------------------------------------------------------------------------------//
  {
    for (int p = startPtr + 1, max = startPtr + length; p < max; p++)
    {
      int c = buffer[p] & 0xFF;
      if (c == 0)
        break;

      if (c < 32)
        return true;
    }

    return false;
  }

  // ---------------------------------------------------------------------------------//
  void addFormattedRem (StringBuilder text)
  // ---------------------------------------------------------------------------------//
  {
    int ptr = startPtr + 1;
    int max = startPtr + length - 1;

    if (isFirst ())
    {
      if (containsBackspaces (ptr, max))    // probably going to erase the line number
      {
        // apple format uses left-justified line numbers so the length varies
        text.setLength (0);
        text.append (String.format (" %d  REM ", parent.lineNumber));   // mimic apple
      }
      else
        text.append ("  REM ");
    }
    else
      text.append ("REM ");

    while (ptr < max)
    {
      switch (buffer[ptr])
      {
        case Utility.ASCII_BACKSPACE:
          if (text.length () > 0)
            text.deleteCharAt (text.length () - 1);
          break;

        case Utility.ASCII_CR:
          text.append ("\n");
          break;

        case Utility.ASCII_LF:
          int indent = Utility.getIndent (text);
          text.append ("\n");
          for (int i = 0; i < indent; i++)
            text.append (" ");
          break;

        default:
          text.append ((char) buffer[ptr]);     // do not mask with 0xFF
      }

      ptr++;
    }
  }

  // ---------------------------------------------------------------------------------//
  private boolean containsBackspaces (int ptr, int max)
  // ---------------------------------------------------------------------------------//
  {
    while (ptr < max)
      if (buffer[ptr++] == Utility.ASCII_BACKSPACE)
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  public String getAlignedText (int alignEqualsPos)
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder line = toStringBuilder ();      // get line

    // insert spaces before '=' until it lines up with the other assignment lines
    if (!is (TOKEN_REM))
      while (alignEqualsPos-- > equalsPosition)
        line.insert (equalsPosition, ' ');

    return line.toString ();
  }

  // ---------------------------------------------------------------------------------//
  public byte[] getBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    int len = length - 1;
    if (buffer[startPtr + len] == Utility.ASCII_COLON || buffer[startPtr + len] == 0)
      len--;
    byte[] buffer2 = new byte[len];
    System.arraycopy (buffer, startPtr + 1, buffer2, 0, buffer2.length);

    return buffer2;
  }

  // ---------------------------------------------------------------------------------//
  private boolean isToken (byte b)
  // ---------------------------------------------------------------------------------//
  {
    return isHighBitSet (b);
  }

  // ---------------------------------------------------------------------------------//
  List<String> getVariables ()
  // ---------------------------------------------------------------------------------//
  {
    return variables;
  }

  // ---------------------------------------------------------------------------------//
  List<String> getFunctions ()
  // ---------------------------------------------------------------------------------//
  {
    return functions;
  }

  // ---------------------------------------------------------------------------------//
  List<String> getArrays ()
  // ---------------------------------------------------------------------------------//
  {
    return arrays;
  }

  // ---------------------------------------------------------------------------------//
  List<Integer> getGotoLines ()
  // ---------------------------------------------------------------------------------//
  {
    return gotoLines;
  }

  // ---------------------------------------------------------------------------------//
  List<Integer> getGosubLines ()
  // ---------------------------------------------------------------------------------//
  {
    return gosubLines;
  }

  // ---------------------------------------------------------------------------------//
  List<Integer> getConstantsInt ()
  // ---------------------------------------------------------------------------------//
  {
    return constantsInt;
  }

  // ---------------------------------------------------------------------------------//
  List<Float> getConstantsFloat ()
  // ---------------------------------------------------------------------------------//
  {
    return constantsFloat;
  }

  // ---------------------------------------------------------------------------------//
  List<String> getStringsText ()
  // ---------------------------------------------------------------------------------//
  {
    return stringsText;
  }

  // ---------------------------------------------------------------------------------//
  public StringBuilder toStringBuilder ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder line = new StringBuilder ();

    // All sublines end with 0 or : except IF lines that are split into two
    int max = startPtr + length - 1;
    if (buffer[max] == 0)
      --max;

    if (isImpliedGoto () && !ApplesoftBasicProgram.basicPreferences.showThen)
      line.append ("GOTO ");

    for (int p = startPtr; p <= max; p++)
    {
      byte b = buffer[p];
      if (isToken (b))
      {
        if (line.length () > 0 && line.charAt (line.length () - 1) != ' ')
          line.append (' ');
        int val = b & 0x7F;
        if (b != TOKEN_THEN || ApplesoftBasicProgram.basicPreferences.showThen)
          line.append (ApplesoftConstants.tokens[val] + " ");
      }
      else if (Utility.isControlCharacter (b))
        line.append (ApplesoftBasicProgram.basicPreferences.showCaret
            ? "^" + (char) (b + 64) : "?");
      else
        line.append ((char) b);
    }

    return line;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return toStringBuilder ().toString ();
  }
}
