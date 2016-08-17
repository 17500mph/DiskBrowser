package com.bytezone.diskbrowser.wizardry;

import com.bytezone.diskbrowser.applefile.AbstractFile;

// Based on a pascal routine by Tom Ewers

// link for possible display algorithm:
// http://stackoverflow.com/questions/14184655/set-position-for-drawing-binary-tree

public class Huffman extends AbstractFile
{
  private static final int LEFT = 256;
  private static final int RIGHT = 512;

  private static final byte[] mask = { 2, 1 };
  private static final int[] offset = { RIGHT, LEFT };

  private int bitNo;
  private int msgPtr;
  private int currentByte;
  private byte[] message;

  private String bufferContents;

  public Huffman (String name, byte[] buffer)
  {
    super (name, buffer);
  }

  public String decodeMessage (byte[] message)
  {
    this.message = message;
    bitNo = 0;
    msgPtr = 0;
    currentByte = 0;

    int len = getChar ();
    StringBuilder text = new StringBuilder ();
    for (int i = 0; i < len; i++)
      text.append ((char) getChar ());

    return text.toString ();
  }

  private byte getChar ()
  {
    int treePtr = 0;                                // start at the root

    while (true)
    {
      if (bitNo-- == 0)
      {
        bitNo += 8;
        currentByte = message[msgPtr++] & 0xFF;
      }

      int currentBit = currentByte % 2;             // get the next bit to process
      currentByte /= 2;

      // use currentBit to determine whether to use the left or right node
      byte nodeValue = buffer[treePtr + offset[currentBit]];

      // if the node is a leaf, return its contents
      if ((buffer[treePtr] & mask[currentBit]) != 0)
        return nodeValue;

      // else continue traversal
      treePtr = nodeValue & 0xFF;
    }
  }

  @Override
  public String getText ()
  {
    if (bufferContents == null)
    {
      StringBuilder text = new StringBuilder ();
      walk (0, "", text);
      bufferContents = text.toString ();
    }
    return bufferContents;
  }

  private void walk (int treePtr, String path, StringBuilder text)
  {
    // check left node
    if ((buffer[treePtr] & 0x01) == 0)
      walk (buffer[treePtr + LEFT] & 0xFF, path + "1", text);
    else
      print (path + "1", buffer[treePtr + LEFT], text);

    // check right node
    if ((buffer[treePtr] & 0x02) == 0)
      walk (buffer[treePtr + RIGHT] & 0xFF, path + "0", text);
    else
      print (path + "0", buffer[treePtr + RIGHT], text);
  }

  private void print (String path, byte value, StringBuilder text)
  {
    int val = value & 0xFF;
    char c = val < 32 || val >= 127 ? ' ' : (char) val;
    text.append (String.format ("%3d  %1.1s  %s%n", val, c, path));
  }
}