/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  CorefEditor.java
 *
 *  Niraj Aswani, 24-Jun-2004
 *
 *  $Id: CorefEditor.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.gui.docview;

import gate.Annotation;
import gate.AnnotationSet;
import gate.creole.ANNIEConstants;
import gate.event.AnnotationSetEvent;
import gate.event.AnnotationSetListener;
import gate.gui.MainFrame;
import gate.swing.ColorGenerator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * Display a tree that contains the co-references type of the document,
 * highlight co-references in the document, allow creating
 * co-references from existing annotations, editing and deleting co-references.
 */
public class CorefEditor
    extends AbstractDocumentView
    implements ActionListener, gate.event.FeatureMapListener,
    gate.event.DocumentListener, AnnotationSetListener {

  // default AnnotationSet Name
  private final static String DEFAULT_ANNOTSET_NAME = "Default";

  private JPanel mainPanel, topPanel, subPanel;
  private JToggleButton showAnnotations;
  private JComboBox annotSets, annotTypes;
  private DefaultComboBoxModel annotSetsModel, annotTypesModel;

  // Co-reference Tree
  private JTree corefTree;

  // Root node
  private CorefTreeNode rootNode;

  // top level hashMap (corefChains)
  // AnnotationSet(CorefTreeNode) --> (CorefTreeNode type ChainNode --> ArrayList AnnotationIds)
  private HashMap corefChains;

  // This is used to store the annotationSet name and its respective corefTreeNode
  // annotationSetName --> CorefTreeNode of type (AnnotationSet)
  private HashMap corefAnnotationSetNodesMap;

  // annotationSetName --> (chainNodeString --> Boolean)
  private HashMap selectionChainsMap;

  // chainString --> Boolean
  private HashMap currentSelections;

  // annotationSetName --> (chainNodeString --> Color)
  private HashMap colorChainsMap;

  // chainNodeString --> Color
  private HashMap currentColors;

  private ColorGenerator colorGenerator;
  private TextualDocumentView textView;
  private JTextArea textPane;

  /* ChainNode --> (HighlightedTags) */
  private HashMap highlightedTags;

  /* This arraylist stores the highlighted tags for the specific selected annotation type */
  private ArrayList typeSpecificHighlightedTags;
  private TextPaneMouseListener textPaneMouseListener;

  /* This stores Ids of the highlighted Chain Annotations*/
  private ArrayList highlightedChainAnnots = new ArrayList();
  /* This stores start and end offsets of the highlightedChainAnnotations */
  private int[] highlightedChainAnnotsOffsets;

  /* This stores Ids of the highlighted Annotations of particular type */
  private ArrayList highlightedTypeAnnots = new ArrayList();
  /* This stores start and end offsets of highlightedTypeAnnots */
  private int[] highlightedTypeAnnotsOffsets;

  /* Timer for the Chain Tool tip action */
  private ChainToolTipAction chainToolTipAction;
  private javax.swing.Timer chainToolTipTimer;

  private NewCorefAction newCorefAction;
  private javax.swing.Timer newCorefActionTimer;

  private Annotation annotToConsiderForChain = null;
  private JWindow popupWindow;
  private boolean explicitCall = false;
  private Highlighter highlighter;

  /**
   * This method intiates the GUI for co-reference editor
   */
  protected void initGUI() {

    //get a pointer to the textual view used for highlights
    Iterator centralViewsIter = owner.getCentralViews().iterator();
    while (textView == null && centralViewsIter.hasNext()) {
      DocumentView aView = (DocumentView) centralViewsIter.next();
      if (aView instanceof TextualDocumentView)
        textView = (TextualDocumentView) aView;
    }
    textPane = (JTextArea) ( (JScrollPane) textView.getGUI()).getViewport().
               getView();
    highlighter = textPane.getHighlighter();
    chainToolTipAction = new ChainToolTipAction();
    chainToolTipTimer = new javax.swing.Timer(500, chainToolTipAction);
    chainToolTipTimer.setRepeats(false);
    newCorefAction = new NewCorefAction();
    newCorefActionTimer = new javax.swing.Timer(500, newCorefAction);
    newCorefActionTimer.setRepeats(false);

    colorGenerator = new ColorGenerator();

    // main Panel
    mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());

    // topPanel
    topPanel = new JPanel();
    topPanel.setLayout(new BorderLayout());

    // subPanel
    subPanel = new JPanel();
    subPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

    // showAnnotations Button
    showAnnotations = new JToggleButton("Show");
    showAnnotations.addActionListener(this);

    // annotSets
    annotSets = new JComboBox();
    annotSets.addActionListener(this);

    // get all the annotationSets
    Map annotSetsMap = document.getNamedAnnotationSets();
    annotSetsModel = new DefaultComboBoxModel();
    if (annotSetsMap != null) {
      Object [] array = annotSetsMap.keySet().toArray();
      for(int i=0;i<array.length;i++) {
        ((AnnotationSet) annotSetsMap.get((String) array[i])).addAnnotationSetListener(this);
      }
      annotSetsModel = new DefaultComboBoxModel(array);
    }
    document.getAnnotations().addAnnotationSetListener(this);
    annotSetsModel.insertElementAt(DEFAULT_ANNOTSET_NAME, 0);
    annotSets.setModel(annotSetsModel);

    // annotTypes
    annotTypesModel = new DefaultComboBoxModel();
    annotTypes = new JComboBox(annotTypesModel);
    annotTypes.addActionListener(this);
    subPanel.add(new JLabel("Sets : "));
    subPanel.add(annotSets);
    JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    tempPanel.add(new JLabel("Types : "));
    tempPanel.add(annotTypes);
    tempPanel.add(showAnnotations);
    // intialises the Data
    initData();

    // and creating the tree
    corefTree = new JTree(rootNode);
    corefTree.putClientProperty("JTree.lineStyle", "None");
    corefTree.setRowHeight(corefTree.getRowHeight() * 2);
    corefTree.setLargeModel(true);
    corefTree.setAutoscrolls(true);

    //corefTree.setRootVisible(false);
    //corefTree.setShowsRootHandles(false);
    corefTree.addMouseListener(new CorefTreeMouseListener());
    corefTree.setCellRenderer(new CorefTreeCellRenderer());

    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(new JScrollPane(corefTree), BorderLayout.CENTER);
    topPanel.add(subPanel, BorderLayout.CENTER);
    topPanel.add(tempPanel, BorderLayout.SOUTH);

    // get the highlighter
    textPaneMouseListener = new TextPaneMouseListener();
    annotSets.setSelectedIndex(0);

    // finally show the tree
    //annotSetSelectionChanged();

    document.addDocumentListener(this);
    document.getFeatures().addFeatureMapListener(this);
  }

  public void reinitAllVariables() {

    if (highlightedChainAnnots != null)
      highlightedChainAnnots.clear();
    if (highlightedTypeAnnots != null)
      highlightedTypeAnnots.clear();
    if (typeSpecificHighlightedTags != null)
      typeSpecificHighlightedTags.clear();
    highlightedChainAnnotsOffsets = null;
    highlightedTypeAnnotsOffsets = null;
    if (highlightedTags != null && highlightedTags.values() != null) {
      Iterator highlightsIter = highlightedTags.values().iterator();
      while (highlightsIter.hasNext()) {
        ArrayList tags = (ArrayList) highlightsIter.next();
        for (int i = 0; i < tags.size(); i++) {
          highlighter.removeHighlight(tags.get(i));
        }
      }
      highlightedTags.clear();
    }

    // we need to find out all the annotSetNodes and remove all the chainNodes
    // under it
    Iterator annotSetsIter = corefAnnotationSetNodesMap.keySet().iterator();
    while (annotSetsIter.hasNext()) {
      CorefTreeNode annotSetNode = (CorefTreeNode) corefAnnotationSetNodesMap.
                                   get(annotSetsIter.next());
      annotSetNode.removeAllChildren();
      colorChainsMap.put(annotSetNode.toString(), new HashMap());
      selectionChainsMap.put(annotSetNode.toString(), new HashMap());
      corefChains.put(annotSetNode, new HashMap());
    }
  }

      /** This methods cleans up the memory by removing all listener registrations */
  public void cleanup() {
    document.removeDocumentListener(this);
    document.getFeatures().removeFeatureMapListener(this);
  }

  /** Given arrayList containing Ids of the annotations, and an annotationSet, this method
   * returns the annotations that has longest string among the matches
   */
  public Annotation findOutTheLongestAnnotation(ArrayList matches,
                                                AnnotationSet set) {
    if (matches == null || matches.size() == 0) {
      return null;
    }
    int length = 0;
    int index = 0;
    for (int i = 0; i < matches.size(); i++) {
      Annotation currAnn = set.get( (Integer) matches.get(i));
      int start = currAnn.getStartNode().getOffset().intValue();
      int end = currAnn.getEndNode().getOffset().intValue();
      if ( (end - start) > length) {
        length = end - start;
        index = i;
      }
    }
    // so now we now have the longest String annotations at index
    return set.get( (Integer) matches.get(index));
  }

  /**
   * This method is called when any annotationSet is removed outside the
   * co-reference editor..
   * @param de
   */
  public void annotationSetRemoved(gate.event.DocumentEvent de) {
    // this method removes the annotationSet from the annotSets
    // and all chainNodes under it

    String annotSet = de.getAnnotationSetName();
    annotSet = (annotSet == null) ? DEFAULT_ANNOTSET_NAME : annotSet;
    // find out the currently Selected annotationSetName
    String annotSetName = (String) annotSets.getSelectedItem();
    // remove it from the main data store
    corefChains.remove(corefAnnotationSetNodesMap.get(annotSet));
    // remove it from the main data store
    corefAnnotationSetNodesMap.remove(annotSet);
    // remove it from the annotationSetModel (combobox)
    annotSetsModel.removeElement(annotSet);
    annotSets.setModel(annotSetsModel);
    // remove it from the colorChainMap
    colorChainsMap.remove(annotSet);
    // remove it from the selectionChainMap
    selectionChainsMap.remove(annotSet);
    if (annotSetsModel.getSize() == 0) {
      // no annotationSet to display
      // so set visible false
      if (popupWindow != null && popupWindow.isVisible()) {
        popupWindow.setVisible(false);
      }
      corefTree.setVisible(false);
    }
    else {
      if (annotSetName.equals(annotSet)) {
        if (popupWindow != null && popupWindow.isVisible()) {
          popupWindow.setVisible(false);
        }
        if (!corefTree.isVisible())
          corefTree.setVisible(true);

        annotSets.setSelectedIndex(0);
        //annotSetSelectionChanged();
      }
    }
  }

  /**
   * This method is called when any new annotationSet is added
   * @param de
   */
  public void annotationSetAdded(gate.event.DocumentEvent de) {

    String annotSet = de.getAnnotationSetName();
    if(annotSet == null)
      document.getAnnotations().addAnnotationSetListener(this);
    else
      document.getAnnotations(annotSet).addAnnotationSetListener(this);

    annotSet = (annotSet == null) ? DEFAULT_ANNOTSET_NAME : annotSet;
    // find out the currently Selected annotationSetName
    String annotSetName = (String) annotSets.getSelectedItem();

    // check if newly added annotationSet is the default AnnotationSet
    CorefTreeNode annotSetNode = null;

    if (annotSet.equals(DEFAULT_ANNOTSET_NAME))
      annotSetNode = createAnnotSetNode(document.getAnnotations(), true);
    else
      annotSetNode = createAnnotSetNode(document.getAnnotations(annotSet), false);

    corefAnnotationSetNodesMap.put(annotSet, annotSetNode);
    /*annotSetsModel.addElement(annotSet);
    annotSets.setModel(annotSetsModel);*/

    if (annotSetName != null)
      annotSets.setSelectedItem(annotSetName);
    else
      annotSets.setSelectedIndex(0);

      //annotSetSelectionChanged();
  }

  /**Called when the content of the document has changed through an edit
   * operation.
   */
  public void contentEdited(gate.event.DocumentEvent e) {
    //ignore
  }

  public void annotationAdded(AnnotationSetEvent ase) {
    // ignore
  }

  public void annotationRemoved(AnnotationSetEvent ase) {
    Annotation delAnnot = ase.getAnnotation();
    Integer id = delAnnot.getId();
    Map matchesMap = null;
    Object matchesMapObject = document.getFeatures().get(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME);
    if(!(matchesMapObject instanceof Map)) {
      // no need to do anything
      // and return
      return;
    }


    matchesMap = (Map) matchesMapObject;

    if(matchesMap == null)
      return;

    Set keySet = matchesMap.keySet();
    if(keySet == null)
      return;

    Iterator iter = keySet.iterator();
    boolean found = false;
    while(iter.hasNext()) {
      String currSet = (String) iter.next();
      java.util.List matches = (java.util.List) matchesMap.get(currSet);
      if(matches == null || matches.size() == 0)
        continue;
      else {
        for(int i=0;i<matches.size();i++) {
          ArrayList ids = (ArrayList) matches.get(i);
          if(ids.contains(id)) {
            // found
            // so remove this
            found = true;
            ids.remove(id);
            matches.set(i, ids);
            break;
          }
        }
        if(found) {
          matchesMap.put(currSet, matches);
          explicitCall = true;
          document.getFeatures().put(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME,
                                     matchesMap);
          explicitCall = false;
          break;
        }
      }
    }
    if(found)
      featureMapUpdated();
  }

  /**
   * Called when features are changed outside the co-refEditor
   */
  public void featureMapUpdated() {

    if (explicitCall)
      return;

    // we would first save the current settings
    // 1. Current AnnotSet
    // 2. Current AnnotType
    // 3. ShowAnnotation Status
    String currentAnnotSet = (String) annotSets.getSelectedItem();
    String currentAnnotType = (String) annotTypes.getSelectedItem();
    boolean currentShowAnnotationStatus = showAnnotations.isSelected();

    // there is some change in the featureMap
    Map matchesMap = null;
    Object matchesMapObject = document.getFeatures().get(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME);
    if(!(matchesMapObject instanceof Map)) {
      // no need to do anything
      // and return
      reinitAllVariables();
      explicitCall = false;
      annotSets.setSelectedIndex(0);
      return;
    }

    matchesMap = (Map) matchesMapObject;

    if (matchesMap == null) {
      reinitAllVariables();
      explicitCall = false;
      annotSets.setSelectedIndex(0);
      return;
    }

    //AnnotationSetName --> List of ArrayLists
    //each ArrayList contains Ids of related annotations
    Iterator setIter = matchesMap.keySet().iterator();
    HashMap annotSetsNamesMap = new HashMap();
    for (int i = 0; i < annotSets.getItemCount(); i++) {
      annotSetsNamesMap.put( (String) annotSets.getItemAt(i), new Boolean(false));
    }
    outer:while (setIter.hasNext()) {
      String currentSet = (String) setIter.next();
      java.util.List matches = (java.util.List) matchesMap.get(currentSet);
      currentSet = (currentSet == null) ? DEFAULT_ANNOTSET_NAME : currentSet;

      if (matches == null)
        continue;

      AnnotationSet currAnnotSet = getAnnotationSet(currentSet);
      annotSetsNamesMap.put(currentSet, new Boolean(true));

      Iterator entitiesIter = matches.iterator();
      //each entity is a list of annotation IDs

      if (corefAnnotationSetNodesMap.get(currentSet) == null) {
        // we need to create the node for this
        if (currentSet.equals(DEFAULT_ANNOTSET_NAME)) {
          corefAnnotationSetNodesMap.put(DEFAULT_ANNOTSET_NAME,
                                         createChain(document.getAnnotations(), true));
        }
        else {
          corefAnnotationSetNodesMap.put(currentSet,
                                         createChain(document.getAnnotations(
              currentSet), false));
        }
        continue outer;
      }

      HashMap chains = (HashMap) corefChains.get(corefAnnotationSetNodesMap.get(currentSet));
      HashMap visitedList = new HashMap();

      if (chains != null) {
        Iterator chainsList = chains.keySet().iterator();

        // intially no chainHead is visited
        while (chainsList.hasNext()) {
          visitedList.put( (CorefTreeNode) chainsList.next(), new Boolean(false));
        }

        // now we need to search for the chainHead of each group
        ArrayList idsToRemove = new ArrayList();
        while (entitiesIter.hasNext()) {
          ArrayList ids = (ArrayList) entitiesIter.next();
          if (ids == null || ids.size() == 0) {
            idsToRemove.add(ids);
            continue;
          }

          CorefTreeNode chainHead = null;
          for (int i = 0; i < ids.size(); i++) {
            Integer id = (Integer) ids.get(i);
            // now lets find out the headnode for this, if it is available
            chainHead = findOutTheChainHead(currAnnotSet.get(id), currentSet);
            if (chainHead != null) {
              visitedList.put(chainHead, new Boolean(true));
              break;
            }
          }

          if (chainHead != null) {
            // we found the chainHead for this
            // so we would replace the ids
            // but before that we would check if chainHead should be replaced
            Annotation longestAnn = findOutTheLongestAnnotation(ids, getAnnotationSet(currentSet));
            if (getString(longestAnn).equals(chainHead.toString())) {
              chains.put(chainHead, ids);
              corefChains.put(corefAnnotationSetNodesMap.get(currentSet),
                              chains);
            }
            else {
              // we first check if new longestAnnotation String is already available as some other chain Node head
              if (currentColors.containsKey(getString(longestAnn))) {
                // yes one chainHead with this string already exists
                // so we need to merge them together
                String longestString = getString(longestAnn);
                CorefTreeNode tempChainHead = findOutChainNode(longestString, currentSet);
                // now all the ids under current chainHead should be placed under the tempChainHead
                ArrayList tempIds = (ArrayList) chains.get(tempChainHead);
                ArrayList currentChainHeadIds = (ArrayList) chains.get(
                    chainHead);
                // so lets merge them
                tempIds.addAll(currentChainHeadIds);

                // and update the chains
                chains.remove(chainHead);
                chains.put(tempChainHead, tempIds);
                corefChains.put(corefAnnotationSetNodesMap.get(currentSet),
                                chains);
                visitedList.put(chainHead, new Boolean(false));
                visitedList.put(tempChainHead, new Boolean(true));

              }
              else {
                String previousString = chainHead.toString();
                String newString = getString(longestAnn);
                chainHead.setUserObject(newString);

                // we need to change the colors
                Color color = (Color) currentColors.get(previousString);
                currentColors.remove(previousString);
                currentColors.put(newString, color);
                colorChainsMap.put(newString, currentColors);

                // we need to change the selections
                Boolean val = (Boolean) currentSelections.get(previousString);
                currentSelections.remove(previousString);
                currentSelections.put(newString, val);
                selectionChainsMap.put(newString, currentSelections);

                chains.put(chainHead, ids);
                corefChains.put(corefAnnotationSetNodesMap.get(currentSet),
                                chains);
              }
            }
          }
          else {
            // this is something new addition
            // so we need to create a new chainNode
            // this is the new chain
            // get the current annotSetNode
            CorefTreeNode annotSetNode = (CorefTreeNode)
                                         corefAnnotationSetNodesMap.get(
                currentSet);

            // we need to find out the longest string annotation
            AnnotationSet actSet = getAnnotationSet(currentSet);

            Annotation ann = findOutTheLongestAnnotation(ids, getAnnotationSet(currentSet));
            // so before creating a new chainNode we need to find out if
            // any of the chainNodes has the same string that of this chainNode
            HashMap tempSelection = (HashMap) selectionChainsMap.get(
                currentSet);
            CorefTreeNode chainNode = null;
            if (tempSelection.containsKey(getString(ann))) {
              chainNode = findOutChainNode(getString(ann), currentSet);

              // ArrayList matches
              HashMap newHashMap = (HashMap) corefChains.get(annotSetNode);
              newHashMap.put(chainNode, ids);
              corefChains.put(annotSetNode, newHashMap);

              visitedList.put(chainNode, new Boolean(true));
            }
            else {
              // create the new chainNode
              chainNode = new CorefTreeNode(getString(ann), false,
                                            CorefTreeNode.CHAIN_NODE);

              // add this to tree
              annotSetNode.add(chainNode);
              corefAnnotationSetNodesMap.put(currentSet, annotSetNode);

              // ArrayList matches
              HashMap newHashMap = (HashMap) corefChains.get(annotSetNode);
              newHashMap.put(chainNode, ids);
              corefChains.put(annotSetNode, newHashMap);

              boolean selectionValue = false;
              if(currentAnnotSet.equals(currentSet))
                selectionValue = true;

              // entry into the selection
              tempSelection.put(chainNode.toString(), new Boolean(selectionValue));
              selectionChainsMap.put(currentSet, tempSelection);

              // entry into the colors
              float components[] = colorGenerator.getNextColor().getComponents(null);
              Color color = new Color(components[0],
                                      components[1],
                                      components[2],
                                      0.5f);
              HashMap tempColors = (HashMap) colorChainsMap.get(currentSet);
              tempColors.put(chainNode.toString(), color);
              colorChainsMap.put(annotSets.getSelectedItem(), tempColors);
            }
          }
        }

        // ok we need to remove Idsnow
        Iterator removeIter = idsToRemove.iterator();
        while (removeIter.hasNext()) {
          explicitCall = true;
          ArrayList ids = (ArrayList) removeIter.next();
          matches.remove(ids);
          String set = currentSet.equals(DEFAULT_ANNOTSET_NAME) ? null :
                       currentSet;
          matchesMap.put(set, matches);
          explicitCall = false;
        }
        explicitCall = true;
        document.getFeatures().put(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME,
                                   matchesMap);
        explicitCall = false;

        // here we need to find out the chainNodes those are no longer needed
        Iterator visitedListIter = visitedList.keySet().iterator();
        while (visitedListIter.hasNext()) {
          CorefTreeNode chainNode = (CorefTreeNode) visitedListIter.next();
          if (! ( (Boolean) visitedList.get(chainNode)).booleanValue()) {
            // yes this should be deleted
            CorefTreeNode annotSetNode = (CorefTreeNode)
                                         corefAnnotationSetNodesMap.get(
                currentSet);

            // remove from the tree
            annotSetNode.remove(chainNode);
            corefAnnotationSetNodesMap.put(currentSet, annotSetNode);

            // ArrayList matches
            HashMap newHashMap = (HashMap) corefChains.get(annotSetNode);
            newHashMap.remove(chainNode);
            corefChains.put(annotSetNode, newHashMap);

            // remove from the selections
            HashMap tempSelection = (HashMap) selectionChainsMap.get(
                currentSet);
            tempSelection.remove(chainNode.toString());
            selectionChainsMap.put(currentSet, tempSelection);

            // remove from the colors
            HashMap tempColors = (HashMap) colorChainsMap.get(currentSet);
            tempColors.remove(chainNode.toString());
            colorChainsMap.put(currentSet, currentColors);
          }
        }
      }
    }

    Iterator tempIter = annotSetsNamesMap.keySet().iterator();
    while (tempIter.hasNext()) {
      String currentSet = (String) tempIter.next();
      if (! ( (Boolean) annotSetsNamesMap.get(currentSet)).booleanValue()) {
        String annotSet = currentSet;
        // find out the currently Selected annotationSetName
        String annotSetName = (String) annotSets.getSelectedItem();
        // remove it from the main data store
        corefChains.remove(corefAnnotationSetNodesMap.get(annotSet));
        // remove it from the main data store
        corefAnnotationSetNodesMap.remove(annotSet);
        // remove it from the annotationSetModel (combobox)
        annotSetsModel.removeElement(annotSet);
        annotSets.setModel(annotSetsModel);
        annotSets.updateUI();
        // remove it from the colorChainMap
        colorChainsMap.remove(annotSet);
        // remove it from the selectionChainMap
        selectionChainsMap.remove(annotSet);
      }
    }

    if (annotSetsModel.getSize() == 0) {
      // no annotationSet to display
      // so set visible false
      if (popupWindow != null && popupWindow.isVisible()) {
        popupWindow.setVisible(false);
      }
      corefTree.setVisible(false);

      // remove all highlights
      ArrayList allHighlights = new ArrayList();
      if(typeSpecificHighlightedTags != null)
        allHighlights.addAll(typeSpecificHighlightedTags);
      if(highlightedTags != null) {
        Iterator iter = highlightedTags.values().iterator();
        while(iter.hasNext()) {
          ArrayList highlights = (ArrayList) iter.next();
          allHighlights.addAll(highlights);
        }
      }
      for(int i=0;i<allHighlights.size();i++) {
        highlighter.removeHighlight(allHighlights.get(i));
      }

      //highlighter.removeAllHighlights();
      highlightedTags = null;
      typeSpecificHighlightedTags = null;
      return;
    }
    else {

      if (popupWindow != null && popupWindow.isVisible()) {
        popupWindow.setVisible(false);
      }

      // remove all highlights
      ArrayList allHighlights = new ArrayList();
      if(typeSpecificHighlightedTags != null)
        allHighlights.addAll(typeSpecificHighlightedTags);
      if(highlightedTags != null) {
        Iterator iter = highlightedTags.values().iterator();
        while(iter.hasNext()) {
          ArrayList highlights = (ArrayList) iter.next();
          allHighlights.addAll(highlights);
        }
      }
      for (int i = 0; i < allHighlights.size(); i++) {
        highlighter.removeHighlight(allHighlights.get(i));
      }

      //highlighter.removeAllHighlights();
      highlightedTags = null;
      typeSpecificHighlightedTags = null;
      if (currentAnnotSet != null) {
        annotSets.setSelectedItem(currentAnnotSet);
        currentSelections = (HashMap) selectionChainsMap.get(currentAnnotSet);
        currentColors = (HashMap) colorChainsMap.get(currentAnnotSet);
        highlightAnnotations();

        showAnnotations.setSelected(currentShowAnnotationStatus);
        if (currentAnnotType != null)
          annotTypes.setSelectedItem(currentAnnotType);
        else
        if (annotTypes.getModel().getSize() > 0) {
          annotTypes.setSelectedIndex(0);
        }
      }
      else {
        explicitCall = false;
        annotSets.setSelectedIndex(0);
      }
    }
  }

  /**
   * ActionPerformed Activity
   * @param ae
   */
  public void actionPerformed(ActionEvent ae) {
    // when annotationSet value changes
    if (ae.getSource() == annotSets) {
      if (!explicitCall) {
        annotSetSelectionChanged();
      }
    }
    else if (ae.getSource() == showAnnotations) {
      if (!explicitCall) {
        showTypeWiseAnnotations();
      }
    }
    else if (ae.getSource() == annotTypes) {
      if (!explicitCall) {
        if (typeSpecificHighlightedTags != null) {
          for (int i = 0; i < typeSpecificHighlightedTags.size(); i++) {
            highlighter.removeHighlight(typeSpecificHighlightedTags.get(i));
          }
        }
        typeSpecificHighlightedTags = null;
        showTypeWiseAnnotations();
      }
    }
  }

  /**
   * When user preses the show Toggle button, this will show up annotations
   * of selected Type from selected AnnotationSet
   */
  private void showTypeWiseAnnotations() {
    if (typeSpecificHighlightedTags == null) {
      highlightedTypeAnnots = new ArrayList();
      typeSpecificHighlightedTags = new ArrayList();
    }

    if (showAnnotations.isSelected()) {
      // get the annotationsSet and its type
      AnnotationSet set = getAnnotationSet( (String) annotSets.getSelectedItem());
      String type = (String) annotTypes.getSelectedItem();
      if (type == null) {
        try {
          JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                        "No annotation type found to display");
        }
        catch (Exception e) {
          e.printStackTrace();
        }
        showAnnotations.setSelected(false);
        return;
      }

      Color color = AnnotationSetsView.getColor(getAnnotationSet((String)annotSets.getSelectedItem()).getName(),type);
      if (type != null) {
        AnnotationSet typeSet = set.get(type);
        Iterator<Annotation> iter = typeSet.iterator();
        while (iter.hasNext()) {
          Annotation ann = iter.next();
          highlightedTypeAnnots.add(ann);
          try {
            typeSpecificHighlightedTags.add(highlighter.addHighlight(ann.
                getStartNode().
                getOffset().intValue(),
                ann.getEndNode().getOffset().intValue(),
                new DefaultHighlighter.
                DefaultHighlightPainter(color)));
          }
          catch (Exception e) {
            e.printStackTrace();
          }
          //typeSpecificHighlightedTags.add(textView.addHighlight(ann, getAnnotationSet((String)annotSets.getSelectedItem()),color));
        }
      }
    }
    else {
      for (int i = 0; i < typeSpecificHighlightedTags.size(); i++) {
        //textView.removeHighlight(typeSpecificHighlightedTags.get(i));
        highlighter.removeHighlight(typeSpecificHighlightedTags.get(i));
      }
      typeSpecificHighlightedTags = new ArrayList();
      highlightedTypeAnnots = new ArrayList();
      highlightedTypeAnnotsOffsets = null;
    }

    // This is to make process faster.. instead of accessing each annotation and
    // its offset, we create an array with its annotation offsets to search faster
    Collections.sort(highlightedTypeAnnots, new gate.util.OffsetComparator());
    highlightedTypeAnnotsOffsets = new int[highlightedTypeAnnots.size() * 2];
    for (int i = 0, j = 0; j < highlightedTypeAnnots.size(); i += 2, j++) {
      Annotation ann1 = (Annotation) highlightedTypeAnnots.get(j);
      highlightedTypeAnnotsOffsets[i] = ann1.getStartNode().getOffset().
                                        intValue();
      highlightedTypeAnnotsOffsets[i +
          1] = ann1.getEndNode().getOffset().intValue();
    }

  }

  /**
   * Returns annotation Set
   * @param annotSet
   * @return
   */
  private AnnotationSet getAnnotationSet(String annotSet) {
    return (annotSet.equals(DEFAULT_ANNOTSET_NAME)) ? document.getAnnotations() :
        document.getAnnotations(annotSet);
  }

  /**
   * When annotationSet selection changes
   */
  private void annotSetSelectionChanged() {
    if (annotSets.getModel().getSize() == 0) {
      if (popupWindow != null && popupWindow.isVisible()) {
        popupWindow.setVisible(false);
      }
      corefTree.setVisible(false);
      return;
    }

    String currentAnnotSet = (String) annotSets.getSelectedItem();
    // get all the types of the currently Selected AnnotationSet
    if (currentAnnotSet == null)
      currentAnnotSet = (String) annotSets.getItemAt(0);
    AnnotationSet temp = getAnnotationSet(currentAnnotSet);
    Set types = temp.getAllTypes();
    annotTypesModel = new DefaultComboBoxModel();
    if (types != null) {
      annotTypesModel = new DefaultComboBoxModel(types.toArray());
    }
    annotTypes.setModel(annotTypesModel);
    annotTypes.updateUI();

    // and redraw the CorefTree
    if (rootNode.getChildCount() > 0)
      rootNode.removeAllChildren();

    CorefTreeNode annotSetNode = (CorefTreeNode) corefAnnotationSetNodesMap.get(
        currentAnnotSet);

    if (annotSetNode != null) {
      rootNode.add(annotSetNode);
      currentSelections = (HashMap) selectionChainsMap.get(currentAnnotSet);
      currentColors = (HashMap) colorChainsMap.get(currentAnnotSet);
      if (!corefTree.isVisible()) {
        if (popupWindow != null && popupWindow.isVisible()) {
          popupWindow.setVisible(false);
        }
        corefTree.setVisible(true);
      }
      corefTree.repaint();
      corefTree.updateUI();

    }
    else {
      corefTree.setVisible(false);
    }
  }

  /**
   * This will initialise the data
   */
  private void initData() {

    rootNode = new CorefTreeNode("Co-reference Data", true,
                                 CorefTreeNode.ROOT_NODE);
    corefChains = new HashMap();
    selectionChainsMap = new HashMap();
    currentSelections = new HashMap();
    colorChainsMap = new HashMap();
    currentColors = new HashMap();
    corefAnnotationSetNodesMap = new HashMap();

    // now we need to findout the chains
    // for the defaultAnnotationSet
    CorefTreeNode annotSetNode = createChain(document.getAnnotations(), true);
    if (annotSetNode != null) {
      corefAnnotationSetNodesMap.put(DEFAULT_ANNOTSET_NAME, annotSetNode);
    }

    // and for the rest AnnotationSets
    Map annotSets = document.getNamedAnnotationSets();
    if (annotSets != null) {
      Iterator annotSetsIter = annotSets.keySet().iterator();
      while (annotSetsIter.hasNext()) {
        String annotSetName = (String) annotSetsIter.next();
        annotSetNode = createChain(document.getAnnotations(annotSetName), false);
        if (annotSetNode != null) {
          corefAnnotationSetNodesMap.put(annotSetName, annotSetNode);
        }
      }
    }
  }

  private CorefTreeNode createAnnotSetNode(AnnotationSet set, boolean isDefaultSet) {
    // create the node for setName
    String setName = isDefaultSet ? DEFAULT_ANNOTSET_NAME : set.getName();
    CorefTreeNode annotSetNode = new CorefTreeNode(setName, true,
        CorefTreeNode.ANNOTSET_NODE);

    // see if this setName available in the annotSets
    boolean found = false;
    for (int i = 0; i < annotSets.getModel().getSize(); i++) {
      if ( ( (String) annotSets.getModel().getElementAt(i)).equals(setName)) {
        found = true;
        break;
      }
    }
    if (!found) {
      explicitCall = true;
      annotSets.addItem(setName);
      explicitCall = false;
    }

    // the internal datastructure
    HashMap chainLinks = new HashMap();
    HashMap selectionMap = new HashMap();
    HashMap colorMap = new HashMap();

    corefChains.put(annotSetNode, chainLinks);
    selectionChainsMap.put(setName, selectionMap);
    colorChainsMap.put(setName, colorMap);
    return annotSetNode;

  }

  /**
   * Creates the internal data structure
   * @param set
   */
  private CorefTreeNode createChain(AnnotationSet set, boolean isDefaultSet) {

    // create the node for setName
    String setName = isDefaultSet ? DEFAULT_ANNOTSET_NAME : set.getName();
    CorefTreeNode annotSetNode = new CorefTreeNode(setName, true,
        CorefTreeNode.ANNOTSET_NODE);

    // see if this setName available in the annotSets
    boolean found = false;
    for (int i = 0; i < annotSets.getModel().getSize(); i++) {
      if ( ( (String) annotSets.getModel().getElementAt(i)).equals(setName)) {
        found = true;
        break;
      }
    }
    if (!found) {
      explicitCall = true;
      annotSets.addItem(setName);
      explicitCall = false;
    }

    // the internal datastructure
    HashMap chainLinks = new HashMap();
    HashMap selectionMap = new HashMap();
    HashMap colorMap = new HashMap();

    // map for all the annotations with matches feature in it
    Map matchesMap = null;
    Object matchesMapObject = document.getFeatures().get(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME);
    if(matchesMapObject instanceof Map) {
      matchesMap = (Map) matchesMapObject;
    }


    // what if this map is null
    if (matchesMap == null) {
      corefChains.put(annotSetNode, chainLinks);
      selectionChainsMap.put(setName, selectionMap);
      colorChainsMap.put(setName, colorMap);
      return annotSetNode;
    }

    //AnnotationSetName --> List of ArrayLists
    //each ArrayList contains Ids of related annotations
    Iterator setIter = matchesMap.keySet().iterator();
    java.util.List matches1 = (java.util.List) matchesMap.get(isDefaultSet ? null :
        setName);
    if (matches1 == null) {
      corefChains.put(annotSetNode, chainLinks);
      selectionChainsMap.put(setName, selectionMap);
      colorChainsMap.put(setName, colorMap);
      return annotSetNode;
    }

    Iterator tempIter = matches1.iterator();

    while (tempIter.hasNext()) {
      ArrayList matches = (ArrayList) tempIter.next();
      int length = 0;
      int index = 0;
      if (matches == null)
        matches = new ArrayList();

      if (matches.size() > 0 && set.size() > 0) {

        String longestString = getString((Annotation) findOutTheLongestAnnotation(matches,
            set));
        // so this should become one of the tree node
        CorefTreeNode chainNode = new CorefTreeNode(longestString, false,
            CorefTreeNode.CHAIN_NODE);
        // and add it under the topNode
        annotSetNode.add(chainNode);

        // chainNode --> All related annotIds
        chainLinks.put(chainNode, matches);
        selectionMap.put(chainNode.toString(), new Boolean(false));
        // and generate the color for this chainNode
        float components[] = colorGenerator.getNextColor().getComponents(null);
        Color color = new Color(components[0],
                                components[1],
                                components[2],
                                0.5f);
        colorMap.put(chainNode.toString(), color);
      }
    }

    corefChains.put(annotSetNode, chainLinks);
    selectionChainsMap.put(setName, selectionMap);
    colorChainsMap.put(setName, colorMap);
    return annotSetNode;
  }

  /**
   * Given an annotation, this method returns the string of that annotation
   */
  public String getString(Annotation ann) {
  	return document.getContent().toString().substring(ann.
          getStartNode().getOffset().intValue(),
          ann.getEndNode().getOffset().intValue()
          ).replaceAll("\\r\\n|\\r|\\n", " ");
  }

  /**
   * Removes the reference of this annotation from the current chain.
   * @param annot annotation to remove
   * @param chainHead co-reference chain to modify
   */
  public void removeChainReference(Annotation annot, CorefTreeNode chainHead) {

    // so we would find out the matches
    CorefTreeNode currentNode = chainHead;
    ArrayList ids = (ArrayList) ( (HashMap)
                                 corefChains.get(corefAnnotationSetNodesMap.get(
        annotSets.getSelectedItem()))).get(chainHead);

   String currentSet = (String) annotSets.getSelectedItem();
   currentSet = (currentSet.equals(DEFAULT_ANNOTSET_NAME)) ? null : currentSet;

    // we need to update the Co-reference document feature
    Map matchesMap = null;
    java.util.List matches = null;
    Object matchesMapObject = document.getFeatures().get(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME);
    if(matchesMapObject instanceof Map) {
      matchesMap = (Map) matchesMapObject;
      matches = (java.util.List) matchesMap.get(currentSet);
    } else {
      matchesMap = new HashMap();
    }

    if (matches == null)
      matches = new ArrayList();

    int index = matches.indexOf(ids);
    if (index != -1) {
      // yes found
      ids.remove(annot.getId());
      Annotation ann = findOutTheLongestAnnotation(ids,
          getAnnotationSet( (String) annotSets.getSelectedItem()));

      matches.set(index, ids);
      matchesMap.put(currentSet, matches);
      document.getFeatures().put(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME,
                                 matchesMap);
    }
  }

  /**
   * Given an annotation, this will find out the chainHead
   * @param ann
   * @return
   */
  private CorefTreeNode findOutTheChainHead(Annotation ann, String set) {
    HashMap chains = (HashMap) corefChains.get(corefAnnotationSetNodesMap.get(
        set));
    if (chains == null)
      return null;
    Iterator iter = chains.keySet().iterator();
    while (iter.hasNext()) {
      CorefTreeNode head = (CorefTreeNode) iter.next();
      if ( ( (ArrayList) chains.get(head)).contains(ann.getId())) {
        return head;
      }
    }
    return null;
  }

  /**
   * This methods highlights the annotations
   */
  public void highlightAnnotations() {

    if (highlightedTags == null) {
      highlightedTags = new HashMap();
      highlightedChainAnnots = new ArrayList();
    }

    AnnotationSet annotSet = getAnnotationSet( (String) annotSets.
                                              getSelectedItem());
    CorefTreeNode annotSetNode = (CorefTreeNode) corefAnnotationSetNodesMap.get(
        annotSets.getSelectedItem());
    if (annotSetNode == null) {
      return;
    }
    HashMap chainMap = (HashMap) corefChains.get(annotSetNode);
    Iterator iter = chainMap.keySet().iterator();

    while (iter.hasNext()) {
      CorefTreeNode currentNode = (CorefTreeNode) iter.next();
      if ( ( (Boolean) currentSelections.get(currentNode.toString())).
          booleanValue()) {
        if (!highlightedTags.containsKey(currentNode)) {
          // find out the arrayList
          ArrayList ids = (ArrayList) chainMap.get(currentNode);
          ArrayList highlighTag = new ArrayList();
          if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
              Annotation ann = annotSet.get( (Integer) ids.get(i));
              highlightedChainAnnots.add(ann);
              Color color = (Color) currentColors.get(currentNode.toString());
              try {
                highlighTag.add(highlighter.addHighlight(ann.getStartNode().
                    getOffset().intValue(),
                    ann.getEndNode().getOffset().intValue(),
                    new DefaultHighlighter.
                    DefaultHighlightPainter(color)));
              }
              catch (Exception e) {
                e.printStackTrace();
              }
              //highlighTag.add(textView.addHighlight(ann, getAnnotationSet((String) annotSets.getSelectedItem()), color));
            }
            highlightedTags.put(currentNode, highlighTag);
          }
        }
      }
      else {
        if (highlightedTags.containsKey(currentNode)) {
          ArrayList highlights = (ArrayList) highlightedTags.get(currentNode);
          for (int i = 0; i < highlights.size(); i++) {
            //textView.removeHighlight(highlights.get(i));
            highlighter.removeHighlight(highlights.get(i));
          }
          highlightedTags.remove(currentNode);
          ArrayList ids = (ArrayList) chainMap.get(currentNode);
          if (ids != null) {
            for (int i = 0; i < ids.size(); i++) {
              Annotation ann = annotSet.get( (Integer) ids.get(i));
              highlightedChainAnnots.remove(ann);
            }
          }
        }
      }
    }

    // This is to make process faster.. instead of accessing each annotation and
    // its offset, we create an array with its annotation offsets to search faster
    Collections.sort(highlightedChainAnnots, new gate.util.OffsetComparator());
    highlightedChainAnnotsOffsets = new int[highlightedChainAnnots.size() * 2];
    for (int i = 0, j = 0; j < highlightedChainAnnots.size(); i += 2, j++) {
      Annotation ann1 = (Annotation) highlightedChainAnnots.get(j);
      highlightedChainAnnotsOffsets[i] = ann1.getStartNode().getOffset().
                                         intValue();
      highlightedChainAnnotsOffsets[i +
          1] = ann1.getEndNode().getOffset().intValue();
    }
  }

  protected void registerHooks() {
    textPane.addMouseListener(textPaneMouseListener);
    textPane.addMouseMotionListener(textPaneMouseListener);

  }

  protected void unregisterHooks() {
    textPane.removeMouseListener(textPaneMouseListener);
    textPane.removeMouseMotionListener(textPaneMouseListener);
  }

  public Component getGUI() {
    return mainPanel;
  }

  public int getType() {
    return VERTICAL;
  }

  //**********************************************
   // MouseListener and MouseMotionListener Methods
   //***********************************************

    protected class TextPaneMouseListener
        extends MouseInputAdapter {

      public TextPaneMouseListener() {
        chainToolTipTimer.setRepeats(false);
        newCorefActionTimer.setRepeats(false);
      }

      public void mouseMoved(MouseEvent me) {
        int textLocation = textPane.viewToModel(me.getPoint());
        chainToolTipAction.setTextLocation(textLocation);
        chainToolTipAction.setMousePointer(me.getPoint());
        chainToolTipTimer.restart();

        newCorefAction.setTextLocation(textLocation);
        newCorefAction.setMousePointer(me.getPoint());
        newCorefActionTimer.restart();
      }
    }

  public void mouseClicked(MouseEvent me) {
    if (popupWindow != null && popupWindow.isVisible()) {
      popupWindow.setVisible(false);
    }
  }

  public CorefTreeNode findOutChainNode(String chainNodeString, String set) {
    if (corefChains == null || corefAnnotationSetNodesMap == null) {
      return null;
    }
    HashMap chains = (HashMap) corefChains.get(corefAnnotationSetNodesMap.get(set));
    if (chains == null) {
      return null;
    }
    Iterator iter = chains.keySet().iterator();
    while (iter.hasNext()) {
      CorefTreeNode currentNode = (CorefTreeNode) iter.next();
      if (currentNode.toString().equals(chainNodeString))
        return currentNode;
    }
    return null;
  }

  /**
   * When user hovers over the annotations which have been highlighted by
   * show button
   */
  protected class NewCorefAction
      implements ActionListener {

    int textLocation;
    Point mousePoint;
    JLabel label = new JLabel();
    JPanel panel = new JPanel();
    JPanel subPanel = new JPanel();
    String field = "";
    JButton add = new JButton("OK");
    JButton cancel = new JButton("Cancel");
    JComboBox list = new JComboBox();
    JPanel mainPanel = new JPanel();
    JPopupMenu popup1 = new JPopupMenu();
    ListEditor listEditor = null;
    ComboBoxModel model = new DefaultComboBoxModel();
    boolean firstTime = true;

    public NewCorefAction() {
      popupWindow = new JWindow(SwingUtilities.getWindowAncestor(textView.
          getGUI()));
      popupWindow.setBackground(UIManager.getLookAndFeelDefaults().
                                getColor("ToolTip.background"));
      mainPanel.setLayout(new BorderLayout());
      mainPanel.setOpaque(true);
      mainPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
      mainPanel.setBackground(UIManager.getLookAndFeelDefaults().
                              getColor("ToolTip.background"));
      popupWindow.setContentPane(mainPanel);

      panel.setLayout(new BorderLayout());
      panel.setOpaque(false);
      panel.add(new JScrollPane(list), BorderLayout.CENTER);

      subPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
      subPanel.add(add);
      subPanel.add(cancel);
      subPanel.setOpaque(false);
      panel.add(subPanel, BorderLayout.SOUTH);
      mainPanel.add(label, BorderLayout.NORTH);
      mainPanel.add(panel, BorderLayout.CENTER);

      // and finally load the data for the list
      AddAction action = new AddAction();
      add.addActionListener(action);
      cancel.addActionListener(action);
      listEditor = new ListEditor(action);
      list.setMaximumRowCount(5);
      list.setEditable(true);
      list.setEditor(listEditor);
      list.setModel(model);
    }

    public void actionPerformed(ActionEvent ae) {
      int index = -1;
      if (highlightedChainAnnotsOffsets != null) {
        for (int i = 0; i < highlightedChainAnnotsOffsets.length; i += 2) {
          if (textLocation >= highlightedChainAnnotsOffsets[i] &&
              textLocation <= highlightedChainAnnotsOffsets[i + 1]) {
            index = (i == 0) ? i : i / 2;
            break;
          }
        }
      }

      // yes it is put on highlighted so show the annotationType
      if (highlightedChainAnnotsOffsets != null &&
          index < highlightedChainAnnotsOffsets.length && index >= 0) {
        return;
      }

      if (highlightedTypeAnnotsOffsets != null) {
        for (int i = 0; i < highlightedTypeAnnotsOffsets.length; i += 2) {
          if (textLocation >= highlightedTypeAnnotsOffsets[i] &&
              textLocation <= highlightedTypeAnnotsOffsets[i + 1]) {
            index = (i == 0) ? i : i / 2;
            break;
          }
        }
      }

      // yes it is put on highlighted so show the annotationType
      if (highlightedTypeAnnotsOffsets != null &&
          index < highlightedTypeAnnotsOffsets.length && index >= 0) {
        textPane.removeAll();
        annotToConsiderForChain = (Annotation) highlightedTypeAnnots.get(index);
        // now check if this annotation is already linked with something
        CorefTreeNode headNode = findOutTheChainHead(annotToConsiderForChain, (String) annotSets.getSelectedItem());
        if (headNode != null) {
          popup1 = new JPopupMenu();
          popup1.setBackground(UIManager.getLookAndFeelDefaults().
                               getColor("ToolTip.background"));
          JLabel label1 = new JLabel("Annotation co-referenced to : \"" +
                                     headNode.toString() + "\"");
          popup1.setLayout(new FlowLayout());
          popup1.add(label1);
          if (popupWindow != null && popupWindow.isVisible()) {
            popupWindow.setVisible(false);
          }
          popup1.setVisible(true);
          popup1.show(textPane, (int) mousePoint.getX(), (int) mousePoint.getY());
        }
        else {
          popupWindow.setVisible(false);
          ArrayList set = new ArrayList(currentSelections.keySet());
          Collections.sort(set);
          set.add(0, "[New Chain]");
          model = new DefaultComboBoxModel(set.toArray());
          list.setModel(model);
          listEditor.setItem("");
          label.setText("Add \"" + getString(annotToConsiderForChain) +
                        "\" to ");
          Point topLeft = textPane.getLocationOnScreen();
          int x = topLeft.x + (int) mousePoint.getX();
          int y = topLeft.y + (int) mousePoint.getY();
          popupWindow.setLocation(x, y);
          if (popup1.isVisible()) {
            popup1.setVisible(false);
          }
          popupWindow.pack();
          popupWindow.setVisible(true);
          listEditor.requestFocus();

          if (firstTime) {
            firstTime = false;
            popupWindow.pack();
            popupWindow.repaint();
            listEditor.requestFocus();
          }
        }
      }
    }

    public void setTextLocation(int textLocation) {
      this.textLocation = textLocation;
    }

    public void setMousePointer(Point point) {
      this.mousePoint = point;
    }

    /** Custom Editor for the ComboBox to enable key events */
    private class ListEditor
        extends KeyAdapter
        implements ComboBoxEditor {
      JTextField myField = new JTextField(20);
      AddAction action = null;
      Vector myList = new Vector();

      public ListEditor(AddAction action) {
        this.action = action;
        myField.addKeyListener(this);
      }

      public void addActionListener(ActionListener al) {
        myField.addActionListener(al);
      }

      public void removeActionListener(ActionListener al) {
        myField.removeActionListener(al);
      }

      public Component getEditorComponent() {
        return myField;
      }

      public Object getItem() {
        return myField.getText();
      }

      public void selectAll() {
        if (myField.getText() != null && myField.getText().length() > 0) {
          myField.setSelectionStart(0);
          myField.setSelectionEnd(myField.getText().length());
        }
      }

      public void setItem(Object item) {
        myField.setText( (String) item);
        field = myField.getText();
      }

      public void requestFocus() {
        myField.requestFocus();
      }

      public void keyReleased(KeyEvent ke) {
        if (myField.getText() == null) {
          myField.setText("");
          field = myField.getText();
        }

        if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
          if (myList.size() == 1) {
            myField.setText( (String) myList.get(0));
          }
          else if (list.getSelectedIndex() < list.getModel().getSize() - 1) {
            list.setSelectedIndex(list.getSelectedIndex());
            myField.setText( (String) list.getSelectedItem());
          }
          field = myField.getText();
          myField.requestFocus();
          return;
        }
        else if (ke.getKeyCode() == KeyEvent.VK_UP) {
          if (list.getSelectedIndex() > 0) {
            list.setSelectedIndex(list.getSelectedIndex());
          }
          myField.setText( (String) list.getSelectedItem());
          field = myField.getText();
          return;
        }
        else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
          field = myField.getText();
          action.actionPerformed(new ActionEvent(add,
                                                 ActionEvent.ACTION_PERFORMED,
                                                 "add"));
          return;
        }
        else if (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
        }
        else if (Character.isJavaIdentifierPart(ke.getKeyChar()) ||
                 Character.isSpaceChar(ke.getKeyChar()) ||
                 Character.isDefined(ke.getKeyChar())) {
        }
        else {
          return;
        }

        String startWith = myField.getText();
        myList = new Vector();
        ArrayList set = new ArrayList(currentSelections.keySet());
        Collections.sort(set);
        set.add(0, "[New Chain]");
        boolean first = true;
        for (int i = 0; i < set.size(); i++) {
          String currString = (String) set.get(i);
          if (currString.toLowerCase().startsWith(startWith.toLowerCase())) {
            if (first) {
              myField.setText(currString.substring(0, startWith.length()));
              first = false;
            }
            myList.add(currString);
          }
        }
        ComboBoxModel model = new DefaultComboBoxModel(myList);
        list.setModel(model);
        myField.setText(startWith);
        field = myField.getText();
        list.showPopup();
      }
    }

    private class AddAction
        extends AbstractAction {
      public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == cancel) {
          popupWindow.setVisible(false);
          return;
        }
        else if (ae.getSource() == add) {
          if (field.length() == 0) {
            try {
              JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                            "No Chain Selected",
                                            "New Chain - Error",
                                            JOptionPane.ERROR_MESSAGE);
            }
            catch (Exception e) {
              e.printStackTrace();
            }
            return;
          }
          else {
            // we want to add this
            // now first find out the annotation
            Annotation ann = annotToConsiderForChain;
            if (ann == null)
              return;
            // yes it is available
            // find out the CorefTreeNode for the chain under which it is to be inserted
            if (field.equals("[New Chain]")) {
              // we want to add this
              // now first find out the annotation
              if (ann == null)
                return;
              CorefTreeNode chainNode = findOutChainNode(getString(ann), (String) annotSets.getSelectedItem());
              if (chainNode != null) {
                try {
                  JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                                "Chain with " + getString(ann) +
                                                " title already exists",
                                                "New Chain - Error",
                                                JOptionPane.ERROR_MESSAGE);
                }
                catch (Exception e) {
                  e.printStackTrace();
                }
                return;
              }

              popupWindow.setVisible(false);

              String currentSet = (String) annotSets.getSelectedItem();
              currentSet = (currentSet.equals(DEFAULT_ANNOTSET_NAME)) ? null :
                           currentSet;

              Map matchesMap = null;
              Object matchesMapObject = document.getFeatures().get(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME);
              if(matchesMapObject instanceof Map) {
                matchesMap = (Map) matchesMapObject;
              }

              if (matchesMap == null) {
                matchesMap = new HashMap();
              }

              java.util.List matches = (java.util.List) matchesMap.get(
                  currentSet);
              ArrayList tempList = new ArrayList();
              tempList.add(ann.getId());
              if (matches == null)
                matches = new ArrayList();
              matches.add(tempList);
              matchesMap.put(currentSet, matches);
              document.getFeatures().put(ANNIEConstants.
                                         DOCUMENT_COREF_FEATURE_NAME,
                                         matchesMap);
              return;
            }

            CorefTreeNode chainNode = findOutChainNode(field, (String) annotSets.getSelectedItem());
            HashMap chains = (HashMap)
                             corefChains.get(corefAnnotationSetNodesMap.get(
                annotSets.getSelectedItem()));
            if (chainNode == null) {
              try {
                JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                              "Incorrect Chain Title",
                                              "New Chain - Error",
                                              JOptionPane.ERROR_MESSAGE);
              }
              catch (Exception e) {
                e.printStackTrace();
              }
              return;
            }
            popupWindow.setVisible(false);
            ArrayList ids = (ArrayList) chains.get(chainNode);

            Map matchesMap = null;
            Object matchesMapObject = document.getFeatures().get(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME);
            if(matchesMapObject instanceof Map) {
              matchesMap = (Map) matchesMapObject;
            }

            if (matchesMap == null) {
              matchesMap = new HashMap();
            }
            String currentSet = (String) annotSets.getSelectedItem();
            currentSet = (currentSet.equals(DEFAULT_ANNOTSET_NAME)) ? null :
                         currentSet;
            java.util.List matches = (java.util.List) matchesMap.get(currentSet);
            if (matches == null)
              matches = new ArrayList();
            int index = matches.indexOf(ids);
            if (index != -1) {
              ArrayList tempIds = (ArrayList) matches.get(index);
              tempIds.add(ann.getId());
              matches.set(index, tempIds);
              matchesMap.put(currentSet, matches);
              document.getFeatures().put(ANNIEConstants.
                                         DOCUMENT_COREF_FEATURE_NAME,
                                         matchesMap);
            }
            return;
          }
        }
      }
    }
  }

  /** When user hovers over the chainnodes */
  protected class ChainToolTipAction
      extends AbstractAction {

    int textLocation;
    Point mousePoint;
    JPopupMenu popup = new JPopupMenu();

    public ChainToolTipAction() {
      popup.setBackground(UIManager.getLookAndFeelDefaults().
                          getColor("ToolTip.background"));
    }

    public void actionPerformed(ActionEvent ae) {

      int index = -1;
      if (highlightedChainAnnotsOffsets != null) {
        for (int i = 0; i < highlightedChainAnnotsOffsets.length; i += 2) {
          if (textLocation >= highlightedChainAnnotsOffsets[i] &&
              textLocation <= highlightedChainAnnotsOffsets[i + 1]) {
            index = (i == 0) ? i : i / 2;
            break;
          }
        }
      }

      // yes it is put on highlighted so show the annotationType
      if (highlightedChainAnnotsOffsets != null &&
          index < highlightedChainAnnotsOffsets.length && index >= 0) {

        if (popupWindow != null && popupWindow.isVisible()) {
          popupWindow.setVisible(false);
        }

        popup.setVisible(false);
        popup.removeAll();
        final int tempIndex = index;
        CorefTreeNode chainHead = findOutTheChainHead( (Annotation)
            highlightedChainAnnots.get(index), (String) annotSets.getSelectedItem());
        final HashMap tempMap = new HashMap();
        popup.setLayout(new FlowLayout(FlowLayout.LEFT));
        if (chainHead != null) {
          JPanel tempPanel = new JPanel();
          tempPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
          tempPanel.add(new JLabel(chainHead.toString()));
          tempPanel.setBackground(UIManager.getLookAndFeelDefaults().
                                  getColor("ToolTip.background"));
          final JButton deleteButton = new JButton("Delete");
          tempPanel.add(deleteButton);
          popup.add(tempPanel);
          deleteButton.setActionCommand(chainHead.toString());
          tempMap.put(chainHead.toString(), chainHead);
          deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
              try {
                int confirm = JOptionPane.showConfirmDialog(MainFrame.getInstance(),
                    "Are you sure?", "Removing reference...",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                  popup.setVisible(false);
                  // remove it
                  removeChainReference( (Annotation) highlightedChainAnnots.get(
                      tempIndex),
                      (CorefTreeNode) tempMap.get(deleteButton.getActionCommand()));
                }
              }
              catch (Exception e1) {
                e1.printStackTrace();
              }
            }
          });
        }
        //label.setText("Remove \""+getString((Annotation) highlightedChainAnnots.get(index)) + "\" from \""+ findOutTheChainHead((Annotation) highlightedChainAnnots.get(index)).toString()+"\"");
        popup.revalidate();
        if (popupWindow != null && popupWindow.isVisible()) {
          popupWindow.setVisible(false);
        }
        popup.setVisible(true);
        popup.show(textPane, (int) mousePoint.getX(), (int) mousePoint.getY());
      }
    }

    public void setTextLocation(int textLocation) {
      this.textLocation = textLocation;
    }

    public void setMousePointer(Point point) {
      this.mousePoint = point;
    }

  }

  // Class that represents each individual tree node in the corefTree
  protected class CorefTreeNode
      extends DefaultMutableTreeNode {
    public final static int ROOT_NODE = 0;
    public final static int ANNOTSET_NODE = 1;
    public final static int CHAIN_NODE = 2;

    private int type;

    public CorefTreeNode(Object value, boolean allowsChildren, int type) {
      super(value, allowsChildren);
      this.type = type;
    }

    public int getType() {
      return this.type;
    }

  }

  /**
   * Action for mouseClick on the Tree
   */
  protected class CorefTreeMouseListener
      extends MouseAdapter {

    public void mouseClicked(MouseEvent me) { }
    public void mouseReleased(MouseEvent me) { }

    public void mousePressed(MouseEvent me) {
      if (popupWindow != null && popupWindow.isVisible()) {
        popupWindow.setVisible(false);
      }
      textPane.removeAll();
      // ok now find out the currently selected node
      int x = me.getX();
      int y = me.getY();
      int row = corefTree.getRowForLocation(x, y);
      TreePath path = corefTree.getPathForRow(row);

      if (path != null) {
        final CorefTreeNode node = (CorefTreeNode) path.
                                   getLastPathComponent();

        // if it only chainNode
        if (node.getType() != CorefTreeNode.CHAIN_NODE) {
          return;
        }

        // see if user clicked the right click
        if (SwingUtilities.isRightMouseButton(me)) {
          // it is right click
          // we need to show the popup window
          final JPopupMenu popup = new JPopupMenu();
          JButton delete = new JButton("Delete");
          delete.setToolTipText("Delete Chain");
          ToolTipManager.sharedInstance().registerComponent(delete);
          JButton cancel = new JButton("Close");
          cancel.setToolTipText("Closes this popup");
          JButton changeColor = new JButton("Change Color");
          changeColor.setToolTipText("Changes Color");
          ToolTipManager.sharedInstance().registerComponent(cancel);
          ToolTipManager.sharedInstance().registerComponent(changeColor);
          JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
          panel.setOpaque(false);
          panel.add(changeColor);
          panel.add(delete);
          panel.add(cancel);
          popup.setLayout(new BorderLayout());
          popup.setOpaque(true);
          popup.setBackground(UIManager.getLookAndFeelDefaults().
                              getColor("ToolTip.background"));
          popup.add(new JLabel("Chain \"" + node.toString() + "\""),
                    BorderLayout.NORTH);
          popup.add(panel, BorderLayout.SOUTH);

          changeColor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
              String currentAnnotSet = (String) annotSets.getSelectedItem();
              currentColors = (HashMap) colorChainsMap.get(currentAnnotSet);
              Color colour = (Color) currentColors.get(node.toString());
              Color col = JColorChooser.showDialog(getGUI(),
                  "Select colour for \"" + node.toString() + "\"",
                  colour);
              if (col != null) {
                Color colAlpha = new Color(col.getRed(), col.getGreen(),
                                           col.getBlue(), 128);

                // make change in the datastructures
                currentColors.put(node.toString(),colAlpha);
                colorChainsMap.put(currentAnnotSet, currentColors);
                // and redraw the tree
                corefTree.repaint();

                // remove all highlights
                ArrayList allHighlights = new ArrayList();
                if(typeSpecificHighlightedTags != null)
                  allHighlights.addAll(typeSpecificHighlightedTags);
                if(highlightedTags != null) {
                  Iterator iter = highlightedTags.values().iterator();
                  while(iter.hasNext()) {
                    ArrayList highlights = (ArrayList) iter.next();
                    allHighlights.addAll(highlights);
                  }
                }
                for (int i = 0; i < allHighlights.size(); i++) {
                  highlighter.removeHighlight(allHighlights.get(i));
                }

                //highlighter.removeAllHighlights();
                highlightedTags = null;
                highlightAnnotations();
                typeSpecificHighlightedTags = null;
                showTypeWiseAnnotations();
              }
              popup.setVisible(false);
            }
          });

          delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
              // get the ids of current chainNode
              HashMap chains = (HashMap)
                               corefChains.get(corefAnnotationSetNodesMap.get(
                  annotSets.getSelectedItem()));
              ArrayList ids = (ArrayList) chains.get(node);

              String currentSet = (String) annotSets.getSelectedItem();
              currentSet = (currentSet.equals(DEFAULT_ANNOTSET_NAME)) ? null : currentSet;

              // now search this in the document feature map
              Map matchesMap = null;
              java.util.List matches = null;

              Object matchesMapObject = document.getFeatures().get(ANNIEConstants.DOCUMENT_COREF_FEATURE_NAME);
              if(matchesMapObject instanceof Map) {
                matchesMap = (Map) matchesMapObject;
                matches = (java.util.List) matchesMap.get(currentSet);
              }

              if(matchesMap == null) {
                matchesMap = new HashMap();
              }

              if (matches == null)
                matches = new ArrayList();

              int index = matches.indexOf(ids);
              if (index != -1) {
                // yes found
                matches.remove(index);
                matchesMap.put(currentSet, matches);
                document.getFeatures().put(ANNIEConstants.
                                           DOCUMENT_COREF_FEATURE_NAME,
                                           matchesMap);
              }
              popup.setVisible(false);
            }
          });

          cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
              popup.setVisible(false);
            }
          });
          popup.setVisible(true);
          popup.show(corefTree, x, y);
          return;
        }

        boolean isSelected = ! ( (Boolean) currentSelections.get(node.toString())).
                             booleanValue();
        currentSelections.put(node.toString(), new Boolean(isSelected));

        // so now we need to highlight all the stuff
        highlightAnnotations();
        corefTree.repaint();
        corefTree.updateUI();
        corefTree.repaint();
        corefTree.updateUI();

      }
    }
  }

  /**
   * Cell renderer to add the checkbox in the tree
   */
  protected class CorefTreeCellRenderer
      extends JPanel
      implements TreeCellRenderer {

    private JCheckBox check;
    private JLabel label;

    /**
     * Constructor.
     */
    public CorefTreeCellRenderer() {
      setOpaque(true);
      check = new JCheckBox();
      check.setBackground(Color.white);
      label = new JLabel();
      setLayout(new BorderLayout(5, 10));
      add(check, BorderLayout.WEST);
      add(label, BorderLayout.CENTER);
    }

    /**
     * Renderer class
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean isSelected,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {

      CorefTreeNode userObject = (CorefTreeNode) value;
      label.setText(userObject.toString());
      this.setSize(label.getWidth(),
                   label.getFontMetrics(label.getFont()).getHeight() * 2);
      tree.expandRow(row);
      if (userObject.getType() == CorefTreeNode.ROOT_NODE || userObject.getType() ==
          CorefTreeNode.ANNOTSET_NODE) {
        this.setBackground(Color.white);
        this.check.setVisible(false);
        return this;
      }
      else {
        this.setBackground( (Color) currentColors.get(userObject.toString()));
        check.setVisible(true);
        check.setBackground(Color.white);
      }

      // if node should be selected
      boolean selected = ( (Boolean) currentSelections.get(userObject.toString())).
                         booleanValue();
      check.setSelected(selected);
      return this;
    }
  }
}
