package com.bytezone.diskbrowser.visicalc;

interface Value
{
  //  public boolean hasValue ();

  public double getValue ();

  public String getText ();

  public boolean isError ();

  public boolean isNaN ();

  public void calculate ();
}