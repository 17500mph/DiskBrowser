package com.bytezone.diskbrowser.applefile;

// pack::: ~/exomizer-3.0.2/src/exomizer mem -q -P23 -lnone LODE148@0x4000 -o LODE148c
// unpack: ~/exomizer-3.0.2/src/exomizer raw -d -b -P23 LODE148c,0,-2 -o LODE148x

// -----------------------------------------------------------------------------------//
public class ExoBuffer
{
  private static int PBIT_BITS_ORDER_BE = 0;
  private static int PBIT_BITS_COPY_GT_7 = 1;
  private static int PBIT_IMPL_1LITERAL = 2;
  private static int PBIT_BITS_ALIGN_START = 3;
  private static int PBIT_4_OFFSET_TABLES = 4;

  private static int PFLAG_BITS_ORDER_BE = (1 << PBIT_BITS_ORDER_BE);
  private static int PFLAG_BITS_COPY_GT_7 = (1 << PBIT_BITS_COPY_GT_7);
  private static int PFLAG_IMPL_1LITERAL = (1 << PBIT_IMPL_1LITERAL);
  private static int PFLAG_BITS_ALIGN_START = (1 << PBIT_BITS_ALIGN_START);
  private static int PFLAG_4_OFFSET_TABLES = (1 << PBIT_4_OFFSET_TABLES);

  private byte[] inBuffer;
  private byte[] outBuffer = new byte[0x8000];

  private int inPos;
  private int outPos;

  private int bitBuffer;
  private int flags;

  private int tableBit[] = new int[8];
  private int tableOff[] = new int[8];
  private int tableBi[] = new int[100];
  private int tableLo[] = new int[100];
  private int tableHi[] = new int[100];

  // ---------------------------------------------------------------------------------//
  public ExoBuffer (byte[] inBuffer)
  // ---------------------------------------------------------------------------------//
  {
    reverse (inBuffer);

    this.inBuffer = inBuffer;

    inPos = 2;
    outPos = 0;
    flags = 23;

    if ((flags & PFLAG_BITS_ALIGN_START) != 0)
      bitBuffer = 0;
    else
      bitBuffer = getByte ();

    tableInit ();
    decrunch ();

    if (outPos < outBuffer.length)
    {
      byte[] outBuffer2 = new byte[outPos];
      System.arraycopy (outBuffer, 0, outBuffer2, 0, outPos);
      outBuffer = outBuffer2;
    }

    reverse (outBuffer);
  }

  // ---------------------------------------------------------------------------------//
  private void reverse (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int lo = 0;
    int hi = buffer.length - 1;

    while (lo < hi)
    {
      byte temp = buffer[lo];
      buffer[lo++] = buffer[hi];
      buffer[hi--] = temp;
    }
  }

  // ---------------------------------------------------------------------------------//
  public byte[] getExpandedBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return outBuffer;
  }

  // ---------------------------------------------------------------------------------//
  private int bitBufRotate (int carry)
  // ---------------------------------------------------------------------------------//
  {
    int carryOut;

    if ((flags & PFLAG_BITS_ORDER_BE) != 0)
    {
      carryOut = (bitBuffer & 0x80) == 0 ? 0 : 1;
      bitBuffer = (bitBuffer << 1) & 0xFF;

      if (carry != 0)
        bitBuffer |= 0x01;
    }
    else
    {
      carryOut = bitBuffer & 0x01;
      bitBuffer = (bitBuffer >>> 1) & 0xFF;

      if (carry != 0)
        bitBuffer |= 0x80;
    }

    return carryOut;
  }

  // ---------------------------------------------------------------------------------//
  private int getByte ()
  // ---------------------------------------------------------------------------------//
  {
    return inBuffer[inPos++] & 0xFF;
  }

  // ---------------------------------------------------------------------------------//
  private int getBits (int count)
  // ---------------------------------------------------------------------------------//
  {
    int byteCopy = 0;
    int value = 0;

    if ((flags & PFLAG_BITS_COPY_GT_7) != 0)
    {
      while (count > 7)
      {
        byteCopy = count >>> 3;
        count &= 7;
      }
    }

    while (count-- > 0)
    {
      int carry = bitBufRotate (0);

      if (bitBuffer == 0)
      {
        bitBuffer = getByte ();
        carry = bitBufRotate (1);
      }
      value <<= 1;
      value |= carry;
    }

    while (byteCopy-- > 0)
    {
      value <<= 8;
      value |= getByte ();
    }

    return value;
  }

  // ---------------------------------------------------------------------------------//
  private void tableInit ()
  // ---------------------------------------------------------------------------------//
  {
    int end;
    int a = 0;
    int b = 0;

    tableBit[0] = 2;
    tableBit[1] = 4;
    tableBit[2] = 4;

    if ((flags & PFLAG_4_OFFSET_TABLES) != 0)
    {
      end = 68;

      tableBit[3] = 4;

      tableOff[0] = 64;
      tableOff[1] = 48;
      tableOff[2] = 32;
      tableOff[3] = 16;
    }
    else
    {
      end = 52;

      tableOff[0] = 48;
      tableOff[1] = 32;
      tableOff[2] = 16;
    }

    for (int i = 0; i < end; i++)
    {
      if ((i & 0x0F) != 0)
        a += (1 << b);
      else
        a = 1;

      tableLo[i] = a & 0xFF;
      tableHi[i] = a >>> 8;

      if ((flags & PFLAG_BITS_COPY_GT_7) != 0)
      {
        b = getBits (3);
        b |= getBits (1) << 3;
      }
      else
        b = getBits (4);

      tableBi[i] = b;
    }
    //    tableDump ();
  }

  // ---------------------------------------------------------------------------------//
  private void tableDump ()
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < 16; i++)
      System.out.printf ("%X", tableBi[i]);

    for (int j = 0; j < 3; j++)
    {
      System.out.printf (",");
      int start = tableOff[j];
      int end = start + (1 << tableBit[j]);
      for (int i = start; i < end; i++)
        System.out.printf ("%X", tableBi[i]);
    }
    System.out.println ();
  }

  // ---------------------------------------------------------------------------------//
  private void decrunch ()
  // ---------------------------------------------------------------------------------//
  {
    int len;
    int srcPtr = 0;
    boolean literal;
    int threshold = (flags & PFLAG_4_OFFSET_TABLES) != 0 ? 4 : 3;

    if ((flags & PFLAG_IMPL_1LITERAL) != 0)
    {
      len = 1;
      literal = true;
      srcPtr = copy (len, literal, srcPtr);
    }

    while (true)
    {
      if (getBits (1) != 0)
      {
        len = 1;
        literal = true;
      }
      else
      {
        int val = getGammaCode ();

        if (val == 16)
          break;

        if (val == 17)
        {
          len = getBits (16);
          literal = true;
        }
        else
        {
          len = getCooked (val);
          literal = false;

          int i = (len > threshold ? threshold : len) - 1;
          srcPtr = outPos - getCooked (tableOff[i] + getBits (tableBit[i]));
        }
      }
      srcPtr = copy (len, literal, srcPtr);
    }
  }

  // ---------------------------------------------------------------------------------//
  private int getGammaCode ()
  // ---------------------------------------------------------------------------------//
  {
    int gammaCode = 0;

    while (getBits (1) == 0)
      ++gammaCode;

    return gammaCode;
  }

  // ---------------------------------------------------------------------------------//
  private int getCooked (int index)
  // ---------------------------------------------------------------------------------//
  {
    int base = tableLo[index] | (tableHi[index] << 8);
    return base + getBits (tableBi[index]);
  }

  // ---------------------------------------------------------------------------------//
  private int copy (int len, boolean literal, int src)
  // ---------------------------------------------------------------------------------//
  {
    do
    {
      int val = literal ? getByte () : outBuffer[src++];
      outBuffer[outPos++] = (byte) (val & 0xFF);

    } while (--len > 0);

    return src;
  }
}
