package com.bytezone.diskbrowser.visicalc;

public class Tan extends Function
{
  private final Value source;

  Tan (Cell cell, String text)
  {
    super (cell, text);

    assert text.startsWith ("@TAN(") : text;

    source = new Expression (parent, cell, functionText).reduce ();
    values.add (source);
  }

  @Override
  public void calculate ()
  {
    source.calculate ();

    if (!source.isValueType (ValueType.VALUE))
    {
      valueType = source.getValueType ();
      return;
    }

    value = Math.tan (source.getValue ());
    valueType = Double.isNaN (value) ? ValueType.ERROR : ValueType.VALUE;
  }
}