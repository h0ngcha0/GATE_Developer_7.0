/*
 *  LogArea.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Cristian URSU, 26/03/2001
 *
 *  $Id: LogArea.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */

package gate.gui;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.text.*;

import gate.swing.XJTextPane;
import gate.util.Err;
import gate.util.Out;

/**
  * This class is used to log all messages from GATE. When an object of this
  * class is created, it redirects the output of {@link gate.util.Out} &
  * {@link gate.util.Err}.
  * The output from Err is written with <font color="red">red</font> and the
  * one from Out is written in <b>black</b>.
  */
public class LogArea extends XJTextPane {

  /** Field needed in inner classes*/
  protected LogArea thisLogArea = null;

  /** The popup menu with various actions*/
  protected JPopupMenu popup = null;

  /** Start position from the document. */
  protected Position startPos;
  /** End position from the document. */
  protected Position endPos;

  /** The original printstream on System.out */
  protected PrintStream originalOut;

  /** The original printstream on System.err */
  protected PrintStream originalErr;
  /** This fields defines the Select all behaviour*/
  protected SelectAllAction selectAllAction = null;

  /** This fields defines the copy  behaviour*/
  protected CopyAction copyAction = null;

  /** This fields defines the clear all  behaviour*/
  protected ClearAllAction clearAllAction = null;

  /** Constructs a LogArea object and captures the output from Err and Out. The
    * output from System.out & System.err is not captured.
    */
  public LogArea(){
    thisLogArea = this;
    this.setEditable(false);

    LogAreaOutputStream err = new LogAreaOutputStream(true);
    LogAreaOutputStream out = new LogAreaOutputStream(false);

    // Redirecting Err
    try{
      Err.setPrintWriter(new UTF8PrintWriter(err,true));
    }catch(UnsupportedEncodingException uee){
      uee.printStackTrace();
    }
    // Redirecting Out
    try{
      Out.setPrintWriter(new UTF8PrintWriter(out,true));
    }catch(UnsupportedEncodingException uee){
      uee.printStackTrace();
    }

    // Redirecting System.out
    originalOut = System.out;
    try{
      System.setOut(new UTF8PrintStream(out, true));
    }catch(UnsupportedEncodingException uee){
      uee.printStackTrace();
    }

    // Redirecting System.err
    originalErr = System.err;
    try{
      System.setErr(new UTF8PrintStream(err, true));
    }catch(UnsupportedEncodingException uee){
      uee.printStackTrace(originalErr);
    }
    popup = new JPopupMenu();
    selectAllAction = new SelectAllAction();
    copyAction = new CopyAction();
    clearAllAction = new ClearAllAction();
    startPos = getDocument().getStartPosition();
    endPos = getDocument().getEndPosition();

    popup.add(selectAllAction);
    popup.add(copyAction);
    popup.addSeparator();
    popup.add(clearAllAction);
    initListeners();
  }// LogArea

  /**
   * Overriddent to fetch new start and end Positions when the document is
   * changed.
   */
  public void setDocument(Document d) {
    super.setDocument(d);
    startPos = d.getStartPosition();
    endPos = d.getEndPosition();
  }

  public void setStyledDocument(StyledDocument d) {
    this.setDocument(d);
  }

  /** Init all listeners for this object*/
  public void initListeners(){
    super.initListeners();
    this.addMouseListener(new MouseAdapter(){
      public void mouseClicked(MouseEvent e){
        if(SwingUtilities.isRightMouseButton(e)){
          popup.show(thisLogArea, e.getPoint().x, e.getPoint().y);
        }//End if
      }// end mouseClicked()
    });// End addMouseListener();
  }

  /** Returns the original printstream on System.err */
  public PrintStream getOriginalErr() {
    return originalErr;
  }

  /** Returns the original printstream on System.out */
  public PrintStream getOriginalOut() {
    return originalOut;
  }// initListeners();

  /** Inner class that defines the behaviour of SelectAll action.*/
  protected class SelectAllAction extends AbstractAction{
    public SelectAllAction(){
      super("Select all");
    }// SelectAll
    public void actionPerformed(ActionEvent e){
      thisLogArea.selectAll();
    }// actionPerformed();
  }// End class SelectAllAction

  /** Inner class that defines the behaviour of copy action.*/
  protected class CopyAction extends AbstractAction{
    public CopyAction(){
      super("Copy");
    }// CopyAction
    public void actionPerformed(ActionEvent e){
      thisLogArea.copy();
    }// actionPerformed();
  }// End class CopyAction

  /**
   * A runnable that adds a bit of text to the area; needed so we can write
   * from the Swing thread.
   */
  protected class SwingWriter implements Runnable{
    SwingWriter(String text, Style style){
      this.text = text;
      this.style = style;
    }

    public void run(){
      try{
        if(endPos.getOffset() > 1){
          Rectangle place = modelToView(endPos.getOffset() - 1);
          if(place != null) scrollRectToVisible(place);
        }
      } catch(BadLocationException e) {
        // ignore a BLE at this point, just don't bother scrolling
        originalErr.println("Exception encountered when trying to scroll to "
            + "end of messages pane: " + e);
      }

      try {
        // endPos is always one past the real end position because of the
        // implicit newline character at the end of any Document
        getDocument().insertString(endPos.getOffset() - 1, text, style);
      } catch(BadLocationException e){
        // a BLE here is a real problem
        handleBadLocationException(e, text, style);
      }// End try
    }
    String text;
    Style style;
  }

  /**
   * Try and recover from a BadLocationException thrown when inserting a string
   * into the log area.  This method must only be called on the AWT event
   * handling thread.
   */
  private void handleBadLocationException(BadLocationException e,
      String textToInsert, Style style) {
    originalErr.println("BadLocationException encountered when writing to "
        + "the log area: " + e);
    originalErr.println("trying to recover...");

    Document newDocument = new DefaultStyledDocument();
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("An error occurred when trying to write a message to the log area.  The log\n");
      sb.append("has been cleared to try and recover from this problem.\n\n");
      sb.append(textToInsert);

      newDocument.insertString(0, sb.toString(), style);
    }
    catch(BadLocationException e2) {
      // oh dear, all bets are off now...
      e2.printStackTrace(originalErr);
      return;
    }
    // replace the log area's document with the new one
    setDocument(newDocument);
  }

  /**
   * A print writer that uses UTF-8 to convert from char[] to byte[]
   */
  public static class UTF8PrintWriter extends PrintWriter{
    public UTF8PrintWriter(OutputStream out)
           throws UnsupportedEncodingException{
      this(out, true);
    }

    public UTF8PrintWriter(OutputStream out, boolean autoFlush)
           throws UnsupportedEncodingException{
      super(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")),
            autoFlush);
    }
  }

  /**
   * A print writer that uses UTF-8 to convert from char[] to byte[]
   */
  public static class UTF8PrintStream extends PrintStream{
    public UTF8PrintStream(OutputStream out)
           throws UnsupportedEncodingException{
      this(out, true);
    }

    public UTF8PrintStream(OutputStream out, boolean autoFlush)
           throws UnsupportedEncodingException{
      super(out, autoFlush);
    }

    /**
     * Overriden so it uses UTF-8 when converting a string to byte[]
     * @param s the string to be printed
     */
    public void print(String s) {
      try{
        write(s.getBytes("UTF-8"));
      }catch(UnsupportedEncodingException uee){
        //support for UTF-8 is guaranteed by the JVM specification
      }catch(IOException ioe){
        //print streams don't throw exceptions
        setError();
      }
    }

    /**
     * Overriden so it uses UTF-8 when converting a char[] to byte[]
     * @param s the string to be printed
     */
    public void print(char s[]) {
      print(String.valueOf(s));
    }
  }

  /** Inner class that defines the behaviour of clear all action.*/
  protected class ClearAllAction extends AbstractAction{
    public ClearAllAction(){
      super("Clear all");
    }// ClearAllAction
    public void actionPerformed(ActionEvent e){
      try{
        thisLogArea.getDocument().remove(startPos.getOffset(),endPos.getOffset() - startPos.getOffset() - 1);
      } catch (BadLocationException e1){
        // it's OK to print this exception to the current log area
        e1.printStackTrace(Err.getPrintWriter());
      }// End try
    }// actionPerformed();
  }// End class ClearAllAction

  /** Inner class that defines the behaviour of an OutputStream that writes to
   *  the LogArea.
   */
  class LogAreaOutputStream extends OutputStream{
    /** This field dictates the style on how to write */
    private boolean isErr = false;
    /** Char style*/
    private Style style = null;

    /** Constructs an Out or Err LogAreaOutputStream*/
    public LogAreaOutputStream(boolean anIsErr){
      isErr = anIsErr;
      if (isErr){
        style = addStyle("error", getStyle("default"));
        StyleConstants.setForeground(style, Color.red);
      }else {
        style = addStyle("out",getStyle("default"));
        StyleConstants.setForeground(style, Color.black);
      }// End if
    }// LogAreaOutputStream

    /** Writes an int which must be a the code of a char, into the LogArea,
     *  using the style specified in constructor. The int is downcast to a byte.
     */
    public void write(int charCode){
      // charCode int must be a char. Let us be sure of that
      charCode &= 0x000000FF;
      // Convert the byte to a char before put it into the log area
      char c = (char)charCode;
      // Insert it in the log Area
      SwingUtilities.invokeLater(new SwingWriter(String.valueOf(c), style));
    }// write(int charCode)

    /** Writes an array of bytes into the LogArea,
     *  using the style specified in constructor.
     */
    public void write(byte[] data, int offset, int length){
      // Insert the string to the log area
      try{
        SwingUtilities.invokeLater(new SwingWriter(new String(data,offset,
                                                              length, "UTF-8"),
                                                   style));
      }catch(UnsupportedEncodingException uee){
        // should never happen - all JREs are required to support UTF-8
        uee.printStackTrace(originalErr);
      }
    }// write(byte[] data, int offset, int length)
  }////End class LogAreaOutputStream
}//End class LogArea
