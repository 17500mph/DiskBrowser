package com.bytezone.diskbrowser.visicalc;

class Sum extends Function
{
  private final Range range;

  public Sum (Sheet parent, String text)
  {
    super (parent, text);
    range = getRange (text);
  }

  @Override
  public Value calculate ()
  {
    value = 0;
    valueType = ValueType.VALUE;

    for (Address address : range)
    {
      Cell cell = parent.getCell (address);
      if (cell == null || cell.isNotAvailable ())
        continue;

      if (cell.isError ())
      {
        valueType = ValueType.ERROR;
        break;
      }

      value += cell.getValue ();
    }

    return this;
  }
}