package com.bytezone.diskbrowser.gui;

// -----------------------------------------------------------------------------------//
public class BasicPreferences
// -----------------------------------------------------------------------------------//
{
  public boolean splitRem = false;
  public boolean alignAssign = true;
  public boolean showTargets = true;
  public boolean showHeader = true;
  public boolean onlyShowTargetLineNumbers = true;
  public boolean showCaret = false;
  public boolean showThen = true;
  public int wrapPrintAt = 0;
  public int wrapRemAt = 60;
  public int wrapDataAt = 60;

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Split REM ............. %s%n", splitRem));
    text.append (String.format ("Align assign .......... %s%n", alignAssign));
    text.append (String.format ("Show targets .......... %s%n", showTargets));
    text.append (
        String.format ("Only target lines ..... %s%n", onlyShowTargetLineNumbers));
    text.append (String.format ("Show header ........... %s%n", showHeader));
    text.append (String.format ("Show caret ............ %s%n", showCaret));
    text.append (String.format ("Show THEN ............. %s%n", showThen));
    text.append (String.format ("Wrap PRINT at ......... %d%n", wrapPrintAt));
    text.append (String.format ("Wrap REM at .......... %d%n", wrapRemAt));
    text.append (String.format ("Wrap DATA at ......... %d", wrapDataAt));

    return text.toString ();
  }
}
