package com.bytezone.diskbrowser.visicalc;

public class Sum extends Function
{
  private final Range range;
  private boolean hasChecked;
  private double sum = 0;

  public Sum (Sheet parent, String text)
  {
    super (parent, text);
    range = getRange (text);
  }

  @Override
  public boolean hasValue ()
  {
    if (!hasChecked)
      calculate ();
    return hasValue;
  }

  @Override
  public double getValue ()
  {
    if (!hasChecked)
      calculate ();
    return hasValue ? sum : 0;
  }

  public void calculate ()
  {
    hasChecked = true;
    hasValue = false;

    for (Address address : range)
    {
      Cell cell = parent.getCell (address);
      if (cell != null && cell.hasValue ())
      {
        hasValue = true;
        sum += cell.getValue ();
      }
    }
  }
}