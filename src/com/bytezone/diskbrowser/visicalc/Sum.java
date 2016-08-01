package com.bytezone.diskbrowser.visicalc;

class Sum extends RangeFunction
{
  public Sum (Sheet parent, String text)
  {
    super (parent, text);
  }

  @Override
  public Value calculate ()
  {
    value = 0;
    valueType = ValueType.VALUE;

    for (Address address : range)
    {
      Cell cell = parent.getCell (address);
      if (cell == null || cell.isValueType (ValueType.NA))
        continue;

      if (cell.isValueType (ValueType.ERROR))
      {
        valueType = ValueType.ERROR;
        break;
      }

      value += cell.getValue ();
    }

    return this;
  }
}