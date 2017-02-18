package com.bytezone.diskbrowser.visicalc;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Range implements Iterable<Address>
{
  private static final Pattern rangePattern =
      Pattern.compile ("([A-B]?[A-Z])([0-9]{1,3})\\.\\.\\.([A-B]?[A-Z])([0-9]{1,3})");
  private static final Pattern addressList = Pattern.compile ("\\(([^,]+(,[^,]+)*)\\)");

  Address from, to;
  List<Address> range = new ArrayList<Address> ();

  public Range (String rangeText)
  {
    setRange (rangeText);
  }

  public Range (Address from, Address to)
  {
    this.from = from;
    this.to = to;

    addRange ();
  }

  public Range (String[] cells)
  {
    for (String s : cells)
      range.add (new Address (s));
  }

  private void addRange ()
  {
    range.add (from);
    Address tempFrom = from;

    if (from.row == to.row)
      while (from.compareTo (to) < 0)
      {
        from = from.nextColumn ();
        range.add (from);
      }
    else if (from.column == to.column)
      while (from.compareTo (to) < 0)
      {
        from = from.nextRow ();
        range.add (from);
      }
    else
      throw new InvalidParameterException ();
    from = tempFrom;
  }

  boolean isHorizontal ()
  {
    Address first = range.get (0);
    Address last = range.get (range.size () - 1);
    return first.row == last.row;
  }

  boolean isVertical ()
  {
    Address first = range.get (0);
    Address last = range.get (range.size () - 1);
    return first.column == last.column;
  }

  @Override
  public Iterator<Address> iterator ()
  {
    return range.iterator ();
  }

  private void setRange (String text)
  {
    Matcher m = rangePattern.matcher (text);
    if (m.find ())
    {
      from = new Address (m.group (1), m.group (2));
      to = new Address (m.group (3), m.group (4));
      addRange ();
      return;
    }

    m = addressList.matcher (text);
    if (m.find ())
    {
      System.out.printf ("Address list:%s%n", text);
      String[] cells = m.group (1).split (",");
      for (String s : cells)
        range.add (new Address (s));
      return;
    }

    int pos = text.indexOf ("...");
    if (pos > 0)
    {
      String fromAddress = text.substring (0, pos);
      String toAddress = text.substring (pos + 3);
      from = new Address (fromAddress);
      to = new Address (toAddress);
      addRange ();
      return;
    }

    System.out.printf ("null range [%s]%n", text);
  }

  @Override
  public String toString ()
  {
    if (from == null || to == null)
    {
      StringBuilder text = new StringBuilder ();
      for (Address address : range)
        text.append (address.text + ",");
      if (text.length () > 0)
        text.deleteCharAt (text.length () - 1);
      return text.toString ();
    }
    return String.format ("      %s -> %s", from.text, to.text);
  }
}