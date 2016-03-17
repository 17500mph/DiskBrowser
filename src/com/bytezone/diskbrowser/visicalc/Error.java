package com.bytezone.diskbrowser.visicalc;

class Error extends Function
{
  public Error (Sheet parent, String text)
  {
    super (parent, text);
  }

  @Override
  public boolean isError ()
  {
    return true;
  }

  @Override
  public boolean isNotAvailable ()
  {
    return false;
  }

  @Override
  public double getValue ()
  {
    return 0;
  }

  @Override
  public Value calculate ()
  {
    return this;
  }
}