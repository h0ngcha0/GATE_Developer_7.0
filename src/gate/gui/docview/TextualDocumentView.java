/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 22 March 2004
 *
 *  $Id: TextualDocumentView.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */
package gate.gui.docview;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.*;


import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.corpora.DocumentContentImpl;
import gate.event.DocumentEvent;
import gate.event.DocumentListener;
import gate.gui.annedit.AnnotationData;
import gate.util.*;


/**
 * This class provides a central view for a textual document.
 */

public class TextualDocumentView extends AbstractDocumentView {

  public TextualDocumentView(){
    blinkingTagsForAnnotations = new HashMap<AnnotationData, HighlightData>();
    //use linked lists as they grow and shrink in constant time and direct access
    //is not required.
    highlightsToAdd = new LinkedList<HighlightData>();
    highlightsToRemove = new LinkedList<HighlightData>();
    blinkingHighlightsToRemove = new HashSet<AnnotationData>();
    blinkingHighlightsToAdd = new LinkedList<AnnotationData>();
    gateDocListener = new GateDocumentListener();
  }

  @Override
  public void cleanup() {
    super.cleanup();
    highlightsMinder.stop();
  }

  public Object addHighlight(AnnotationData aData, Color colour){
    HighlightData hData = new HighlightData(aData, colour);
    synchronized(TextualDocumentView.this) {
      highlightsToAdd.add(hData);
    }
    highlightsMinder.restart();
    return hData;
  }

  /**
   * Adds several highlights in one go.
   * This method should be called from within the UI thread.
   * @param annotations the collection of annotations for which highlights
   * are to be added.
   * @param colour the colour for the highlights.
   * @return the list of tags for the added highlights. The order of the
   * elements corresponds to the order defined by the iterator of the
   * collection of annotations provided.
   */
  public List addHighlights(Collection<AnnotationData> annotations, Color colour){
    List<Object> tags = new ArrayList<Object>();
    for(AnnotationData aData : annotations) tags.add(addHighlight(aData, colour));
    return tags;
  }

  public void removeHighlight(Object tag){
    synchronized(TextualDocumentView.this) {
      highlightsToRemove.add((HighlightData)tag);
    }
    highlightsMinder.restart();
  }

  /**
   * Same as {@link #addHighlights(java.util.Collection, java.awt.Color)} but
   * without the intermediate creation of HighlightData objects.
   * @param list list of HighlightData
   */
  public void addHighlights(List<HighlightData> list) {
    for (HighlightData highlightData : list) {
      synchronized(TextualDocumentView.this) {
        highlightsToAdd.add(highlightData);
      }
    }
    highlightsMinder.restart();
  }

  /**
   * Removes several highlights in one go.
   * @param tags the tags for the highlights to be removed
   */
  public void removeHighlights(Collection tags){
    //this might get an optimised implementation at some point,
    //for the time being this seems to work fine.
    for(Object tag : tags) removeHighlight(tag);
  }


  /**
   * Set the text orientation in the document.
   * 
   * @param orientation
   *          either left to right or right to left
   */
  public void changeOrientation(ComponentOrientation orientation) {
    // set the orientation
    textView.setComponentOrientation(orientation);
    // disable the listener
    gateDocListener.setActive(false);
    // this is required as eventhough orientation gets applied,
    // the screen is not updated unless a character input is
    // detected by the textView
    textView.insert("a", 0);
    textView.replaceRange("", 0, 1);
    // enabling the listener again
    gateDocListener.setActive(true);
  }//changeOrientation

  public void scrollAnnotationToVisible(Annotation ann){
    //if at least part of the blinking section is visible then we
    //need to do no scrolling
    //this is required for long annotations that span more than a
    //screen
    Rectangle visibleView = scroller.getViewport().getViewRect();
    int viewStart = textView.viewToModel(visibleView.getLocation());
    Point endPoint = new Point(visibleView.getLocation());
    endPoint.translate(visibleView.width, visibleView.height);
    int viewEnd = textView.viewToModel(endPoint);
    int annStart = ann.getStartNode().getOffset().intValue();
    int annEnd = ann.getEndNode().getOffset().intValue();
    if(annEnd < viewStart || viewEnd < annStart){
      try{
        textView.scrollRectToVisible(textView.modelToView(annStart));
      }catch(BadLocationException ble){
        //this should never happen
        throw new GateRuntimeException(ble);
      }
    }
  }



  /**
   * Gives access to the highliter's change highlight operation. Can be used to
   * change the offset of an existing highlight.
   * @param tag the tag for the highlight
   * @param newStart new start offset.
   * @param newEnd new end offset.
   * @throws BadLocationException
   */
  public void moveHighlight(Object tag, int newStart, int newEnd)
    throws BadLocationException{
    if(tag instanceof HighlightData){
      textView.getHighlighter().changeHighlight(((HighlightData)tag).tag, newStart, newEnd);
    }
  }



  /**
   * Removes all blinking highlights and shows the new ones, corresponding to
   * the new set of selected annotations
   */
  @Override
  public void setSelectedAnnotations(List<AnnotationData> selectedAnnots) {
    synchronized(blinkingTagsForAnnotations){
      //clear the pending queue, if any
      blinkingHighlightsToAdd.clear();
      //request the removal of existing highlights
      blinkingHighlightsToRemove.addAll(blinkingTagsForAnnotations.keySet());
      //add all the new annotations to the "to add" queue
      for(AnnotationData aData : selectedAnnots){
        blinkingHighlightsToAdd.add(aData);
      }
      //restart the timer
      highlightsMinder.restart();
    }
  }

//  public void addBlinkingHighlight(Annotation ann){
//    synchronized(TextualDocumentView.this){
//      blinkingHighlightsToAdd.add(ann);
//
////      blinkingTagsForAnnotations.put(ann.getId(),
////              new HighlightData(ann, null, null));
//      highlightsMinder.restart();
//    }
//  }

//  public void removeBlinkingHighlight(Annotation ann){
//    synchronized(TextualDocumentView.this) {
//      blinkingHighlightsToRemove.add(ann.getId());
//      highlightsMinder.restart();
//    }
//  }


//  public void removeAllBlinkingHighlights(){
//    synchronized(TextualDocumentView.this){
//      //clear the pending queue, if any
//      blinkingHighlightsToAdd.clear();
//      //request the removal of existing highlights
//      blinkingHighlightsToRemove.addAll(blinkingTagsForAnnotations.keySet());
////      Iterator annIdIter = new ArrayList(blinkingTagsForAnnotations.keySet()).
////        iterator();
////      while(annIdIter.hasNext()){
////        HighlightData annTag = blinkingTagsForAnnotations.remove(annIdIter.next());
////        Object tag = annTag.tag;
////        if(tag != null){
////          Highlighter highlighter = textView.getHighlighter();
////          highlighter.removeHighlight(tag);
////        }
////      }
//      highlightsMinder.restart();
//    }
//  }


  public int getType() {
    return CENTRAL;
  }

  /**
   * Stores the target (which should always be a {@link Document}) into the
   * {@link #document} field.
   */
  public void setTarget(Object target) {
    if(document != null){
      //remove the old listener
      document.removeDocumentListener(gateDocListener);
    }
    super.setTarget(target);
    //register the new listener
    this.document.addDocumentListener(gateDocListener);
  }

  public void setEditable(boolean editable) {
  	textView.setEditable(editable);
  }

  /* (non-Javadoc)
   * @see gate.gui.docview.AbstractDocumentView#initGUI()
   */
  protected void initGUI() {
//    textView = new JEditorPane();
//    textView.setContentType("text/plain");
//    textView.setEditorKit(new RawEditorKit());

    textView = new JTextArea();
    textView.setAutoscrolls(false);
    textView.setLineWrap(true);
    textView.setWrapStyleWord(true);
    // the selection is hidden when the focus is lost for some system
    // like Linux, so we make sure it stays
    // it is needed when doing a selection in the search textfield
    textView.setCaret(new PermanentSelectionCaret());
    scroller = new JScrollPane(textView);

    textView.setText(document.getContent().toString());
    textView.getDocument().addDocumentListener(new SwingDocumentListener());
    // display and put the caret at the beginning of the file
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          if (textView.modelToView(0) != null) {
            textView.scrollRectToVisible(textView.modelToView(0));
          }
          textView.select(0, 0);
          textView.requestFocus();
        } catch(BadLocationException e) {
          e.printStackTrace();
        }
      }
    });
//    contentPane = new JPanel(new BorderLayout());
//    contentPane.add(scroller, BorderLayout.CENTER);

//    //get a pointer to the annotation list view used to display
//    //the highlighted annotations
//    Iterator horizViewsIter = owner.getHorizontalViews().iterator();
//    while(annotationListView == null && horizViewsIter.hasNext()){
//      DocumentView aView = (DocumentView)horizViewsIter.next();
//      if(aView instanceof AnnotationListView)
//        annotationListView = (AnnotationListView)aView;
//    }
    highlightsMinder = new Timer(BLINK_DELAY, new UpdateHighlightsAction());
    highlightsMinder.setInitialDelay(HIGHLIGHT_DELAY);
    highlightsMinder.setDelay(BLINK_DELAY);
    highlightsMinder.setRepeats(true);
    highlightsMinder.setCoalesce(true);
    highlightsMinder.start();

//    blinker = new Timer(this.getClass().getCanonicalName() + "_blink_timer",
//            true);
//    final BlinkAction blinkAction = new BlinkAction();
//    blinker.scheduleAtFixedRate(new TimerTask(){
//      public void run() {
//        blinkAction.actionPerformed(null);
//      }
//    }, 0, BLINK_DELAY);
    initListeners();
  }

  public Component getGUI(){
//    return contentPane;
    return scroller;
  }

  protected void initListeners(){
//    textView.addComponentListener(new ComponentAdapter(){
//      public void componentResized(ComponentEvent e){
//        try{
//    	    scroller.getViewport().setViewPosition(
//    	            textView.modelToView(0).getLocation());
//    	    scroller.paintImmediately(textView.getBounds());
//        }catch(BadLocationException ble){
//          //ignore
//        }
//      }
//    });

    // stop control+H from deleting text and transfers the key to the parent
    textView.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_H
         && e.isControlDown()) {
          getGUI().dispatchEvent(e);
          e.consume();
        }
      }
    });
  }

  protected void unregisterHooks(){}
  protected void registerHooks(){}

  /**
   * Blinks the blinking highlights if any.
   */
  protected class UpdateHighlightsAction extends AbstractAction{
    public void actionPerformed(ActionEvent evt){
      synchronized(blinkingTagsForAnnotations){
        updateBlinkingHighlights();
        updateNormalHighlights();
      }
    }


    protected void updateBlinkingHighlights(){
      //this needs to either add or remove the highlights

      //first remove the queued highlights
      Highlighter highlighter = textView.getHighlighter();
      for(AnnotationData aData : blinkingHighlightsToRemove){
        HighlightData annTag = blinkingTagsForAnnotations.remove(aData);
        if(annTag != null){
          Object tag = annTag.tag;
          if(tag != null){
            //highlight was visible and will be removed
            highlighter.removeHighlight(tag);
            annTag.tag = null;
          }
        }
      }
      blinkingHighlightsToRemove.clear();
      //then add the queued highlights
      for(AnnotationData aData : blinkingHighlightsToAdd){
        blinkingTagsForAnnotations.put(aData,
                new HighlightData(aData, null));
      }
      blinkingHighlightsToAdd.clear();

      //finally switch the state of the current blinking highlights
      //get out as quickly as possible if nothing to do
      if(blinkingTagsForAnnotations.isEmpty()) return;
      Iterator annIdIter = new ArrayList(blinkingTagsForAnnotations.keySet()).
        iterator();

      if(highlightsShown){
        //hide current highlights
        while(annIdIter.hasNext()){
          HighlightData annTag =
              blinkingTagsForAnnotations.get(annIdIter.next());
//          Annotation ann = annTag.annotation;
          if (annTag != null) {
            Object tag = annTag.tag;
            if(tag != null) highlighter.removeHighlight(tag);
            annTag.tag = null;
          }
        }
        highlightsShown = false;
      }else{
        //show highlights
        while(annIdIter.hasNext()){
          HighlightData annTag =
              blinkingTagsForAnnotations.get(annIdIter.next());
          if (annTag != null) {
            Annotation ann = annTag.annotation;
            try{
              Object tag = highlighter.addHighlight(
                      ann.getStartNode().getOffset().intValue(),
                      ann.getEndNode().getOffset().intValue(),
                      new DefaultHighlighter.DefaultHighlightPainter(
                              textView.getSelectionColor()));
              annTag.tag = tag;
//              scrollAnnotationToVisible(ann);
            }catch(BadLocationException ble){
              //this should never happen
              throw new GateRuntimeException(ble);
            }
          }
        }
        highlightsShown = true;
      }
    }

    protected void updateNormalHighlights(){
      synchronized(TextualDocumentView.this) {
        if((highlightsToRemove.size() + highlightsToAdd.size()) > 0){
//          Point viewPosition = scroller.getViewport().getViewPosition();
          Highlighter highlighter = textView.getHighlighter();
//          textView.setVisible(false);
//          scroller.getViewport().setView(new JLabel("Updating"));
          //add all new highlights
          while(highlightsToAdd.size() > 0){
            HighlightData hData = highlightsToAdd.remove(0);
            try{
              hData.tag = highlighter.addHighlight(
                      hData.annotation.getStartNode().getOffset().intValue(),
                      hData.annotation.getEndNode().getOffset().intValue(),
                      new DefaultHighlighter.DefaultHighlightPainter(hData.colour));
            }catch(BadLocationException ble){
              //the offsets should always be OK as they come from an annotation
              ble.printStackTrace();
            }
//            annotationListView.addAnnotation(hData, hData.annotation,
//                    hData.set);
          }

          //remove all the highlights that need removing
          while(highlightsToRemove.size() > 0){
            HighlightData hData = highlightsToRemove.remove(0);
            if(hData.tag != null){
              highlighter.removeHighlight(hData.tag);
            }
//            annotationListView.removeAnnotation(hData);
          }


          //restore the updated view
//          scroller.getViewport().setView(textView);
//          textView.setVisible(true);
//          scroller.getViewport().setViewPosition(viewPosition);
        }
      }
    }
    protected boolean highlightsShown = false;
  }

  private class HighlightData{
    Annotation annotation;
    AnnotationSet set;
    Color colour;
    Object tag;

    public HighlightData(AnnotationData aData, Color colour) {
      this.annotation = aData.getAnnotation();
      this.set = aData.getAnnotationSet();
      this.colour = colour;
    }
  }

  protected class GateDocumentListener implements DocumentListener{

    public void annotationSetAdded(DocumentEvent e) {
    }

    public void annotationSetRemoved(DocumentEvent e) {
    }

    public void contentEdited(DocumentEvent e) {
      if(active){
        //reload the content.
        textView.setText(document.getContent().toString());
      }
    }

    public void setActive(boolean active){
      this.active = active;
    }
    private boolean active = true;
  }

  protected class SwingDocumentListener implements javax.swing.event.DocumentListener{
    public void insertUpdate(final javax.swing.event.DocumentEvent e) {
      //propagate the edit to the document
      try{
        //deactivate our own listener so we don't get cycles
        gateDocListener.setActive(false);
        document.edit(new Long(e.getOffset()), new Long(e.getOffset()),
                      new DocumentContentImpl(
                        e.getDocument().getText(e.getOffset(), e.getLength())));
      }catch(BadLocationException ble){
        ble.printStackTrace(Err.getPrintWriter());
      }catch(InvalidOffsetException ioe){
        ioe.printStackTrace(Err.getPrintWriter());
      }finally{
        //reactivate our listener
        gateDocListener.setActive(true);
      }
//      //update the offsets in the list
//      Component listView = annotationListView.getGUI();
//      if(listView != null) listView.repaint();
    }

    public void removeUpdate(final javax.swing.event.DocumentEvent e) {
      //propagate the edit to the document
      try{
        //deactivate our own listener so we don't get cycles
        gateDocListener.setActive(false);
        document.edit(new Long(e.getOffset()),
                      new Long(e.getOffset() + e.getLength()),
                      new DocumentContentImpl(""));
      }catch(InvalidOffsetException ioe){
        ioe.printStackTrace(Err.getPrintWriter());
      }finally{
        //reactivate our listener
        gateDocListener.setActive(true);
      }
//      //update the offsets in the list
//      Component listView = annotationListView.getGUI();
//      if(listView != null) listView.repaint();
    }

    public void changedUpdate(javax.swing.event.DocumentEvent e) {
      //some attributes changed: we don't care about that
    }
  }//class SwingDocumentListener implements javax.swing.event.DocumentListener

  // When the textPane loses the focus it doesn't really lose
  // the selection, it just stops painting it so we need to force
  // the painting
  public class PermanentSelectionCaret extends DefaultCaret {
    private boolean isFocused;
    public void setSelectionVisible(boolean hasFocus) {
      if (hasFocus != isFocused) {
        isFocused = hasFocus;
        super.setSelectionVisible(false);
        super.setSelectionVisible(true);
      }
    }
    public void focusGained(FocusEvent e) {
      super.focusGained(e);
      // force displaying the caret even if the document is not editable
      super.setVisible(true);
    }
  }

  /**
   * The scroll pane holding the text
   */
  protected JScrollPane scroller;
//  protected AnnotationListView annotationListView;

//  /**
//   * The main panel containing the text scroll in the central location.
//   */
//  protected JPanel contentPane;

  protected GateDocumentListener gateDocListener;

  /**
   * The annotations used for blinking highlights and their tags. A map from
   * {@link AnnotationData} to tag(i.e. {@link Object}).
   */
  protected Map<AnnotationData, HighlightData> blinkingTagsForAnnotations;

  /**
   * This list stores the {@link TextualDocumentView.HighlightData} values for annotations pending
   * highlighting
   */
  protected List<HighlightData> highlightsToAdd;
  
  /**
   * This list stores the {@link TextualDocumentView.HighlightData} values for highlights that need
   * to be removed
   */
  protected List<HighlightData> highlightsToRemove;
  
  /**
   * Used internally to store the annotations for which blinking highlights 
   * need to be removed.
   */
  protected Set<AnnotationData> blinkingHighlightsToRemove;  
  
  /**
   * Used internally to store the annotations for which blinking highlights 
   * need to be added.
   */
  protected List<AnnotationData> blinkingHighlightsToAdd;  
  
  protected Timer highlightsMinder;
  
  protected JTextArea textView;
  
  /**
   * The delay used by the blinker.
   */
  protected final static int BLINK_DELAY = 400;
  
  /**
   * The delay used by the highlights minder.
   */
  protected final static int HIGHLIGHT_DELAY = 100;

  /**
   * @return the textView
   */
  public JTextArea getTextView() {
    return textView;
  }
}
