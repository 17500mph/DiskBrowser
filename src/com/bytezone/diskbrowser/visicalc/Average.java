package com.bytezone.diskbrowser.visicalc;

public class Average extends Function
{
  private final Range range;

  public Average (Sheet parent, String text)
  {
    super (parent, text);

    range = new Range (parent, text);
  }

  @Override
  public void calculate ()
  {
    double total = 0.0;
    int totalChecked = 0;

    for (Address address : range)
    {
      Cell cell = parent.getCell (address);

      if (cell.isValueType (ValueType.NA))
        continue;

      if (!cell.isValueType (ValueType.VALUE))
      {
        valueType = cell.getValueType ();
        return;
      }

      total += cell.getValue ();
      totalChecked++;
    }

    if (totalChecked == 0)
    {
      valueType = ValueType.ERROR;
      return;
    }

    value = total / totalChecked;
    valueType = ValueType.VALUE;
  }
}