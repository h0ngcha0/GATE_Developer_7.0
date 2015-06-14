/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 23/01/2001
 *
 *  $Id: NameBearerHandle.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */

package gate.gui;

import gate.Controller;
import gate.Corpus;
import gate.CorpusController;
import gate.DataStore;
import gate.DataStoreRegister;
import gate.Document;
import gate.Executable;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateConstants;
import gate.LanguageResource;
import gate.ProcessingResource;
import gate.Resource;
import gate.VisualResource;
import gate.corpora.DocumentStaxUtils;
import gate.creole.AbstractResource;
import gate.creole.AnnotationSchema;
import gate.creole.ConditionalController;
import gate.creole.ConditionalSerialAnalyserController;
import gate.creole.ResourceData;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.ir.DefaultIndexDefinition;
import gate.creole.ir.DocumentContentReader;
import gate.creole.ir.FeatureReader;
import gate.creole.ir.IREngine;
import gate.creole.ir.IndexException;
import gate.creole.ir.IndexField;
import gate.creole.ir.IndexedCorpus;
import gate.event.CreoleEvent;
import gate.event.CreoleListener;
import gate.event.ProgressListener;
import gate.event.StatusListener;
import gate.persist.LuceneDataStoreImpl;
import gate.persist.PersistenceException;
import gate.security.Group;
import gate.security.SecurityException;
import gate.security.SecurityInfo;
import gate.security.User;
import gate.swing.XJFileChooser;
import gate.swing.XJMenuItem;
import gate.swing.XJPopupMenu;
import gate.util.Err;
import gate.util.ExtensionFileFilter;
import gate.util.Files;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;
import gate.util.NameBearer;
import gate.util.Out;
import gate.util.ant.packager.PackageGappTask;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

/**
 * Class used to store the GUI information about an open entity
 * (resource, controller, datastore). Such information will include icon
 * to be used for tree components, popup menu for right click events,
 * large and small views, etc.
 */
public class NameBearerHandle implements Handle, StatusListener,
                             ProgressListener, CreoleListener {

  public NameBearerHandle(NameBearer target, Window window) {
    this.target = target;
    this.window = window;
    actionPublishers = new ArrayList<ActionsPublisher>();

    sListenerProxy = new ProxyStatusListener();
    String iconName = null;
    if(target instanceof Resource) {
      rData = Gate.getCreoleRegister().get(target.getClass().getName());
      
      iconName = (String)((Resource)target).getFeatures().get("gate.gui.icon");

      if(iconName == null || MainFrame.getIcon(iconName) == null) {
        
        if(rData != null) {
          iconName = rData.getIcon();
          if(iconName == null) {
            if(target instanceof Controller)
              iconName = "application";
            else if(target instanceof LanguageResource)
              iconName = "lr";
            else if(target instanceof ProcessingResource) iconName = "pr";
          }          
        } else {
          iconName = "lr";
        }
      }
      
      if (rData != null) {
        tooltipText =
          "<HTML> <b>" + rData.getComment() + "</b><br>(<i>"
              + rData.getClassName() + "</i>)</HTML>";
      }      
    }
    else if(target instanceof DataStore) {
      iconName = ((DataStore)target).getIconName();
      tooltipText = ((DataStore)target).getComment();
    }    
    
    this.icon = MainFrame.getIcon(iconName);

    Gate.getCreoleRegister().addCreoleListener(this);

    if(target instanceof ActionsPublisher)
      actionPublishers.add((ActionsPublisher)target);

    buildStaticPopupItems();

    viewsBuilt = false;
  }// public DefaultResourceHandle(FeatureBearer res)

  public Icon getIcon() {
    return icon;
  }

  public void setIcon(Icon icon) {
    this.icon = icon;
  }

  public String getTitle() {
    return target == null ? null : target.getName();
  }

  /**
   * Returns <tt>true</tt> if the views have already been built for this
   * handle.
   * 
   * @return a <tt>boolean</tt> value.
   */
  public boolean viewsBuilt() {
    return viewsBuilt;
  }

  /**
   * Returns a GUI component to be used as a small viewer/editor, e.g.
   * below the main tree in the Gate GUI for the selected resource
   */
  public JComponent getSmallView() {
    if(!viewsBuilt) buildViews();
    return smallView;
  }

  /**
   * Returns the large view for this resource. This view will go into
   * the main display area.
   */
  public JComponent getLargeView() {
    if(!viewsBuilt) buildViews();
    return largeView;
  }

  public JPopupMenu getPopup() {
    JPopupMenu popup = new XJPopupMenu();
    // first add the static items
    Iterator<XJMenuItem> itemIter = staticPopupItems.iterator();
    while(itemIter.hasNext()) {
      JMenuItem anItem = (JMenuItem)itemIter.next();
      if(anItem == null)
        popup.addSeparator();
      else popup.add(anItem);
    }

    // next add the dynamic list from the target and its editors
    Iterator<ActionsPublisher> publishersIter = actionPublishers.iterator();
    while(publishersIter.hasNext()) {
      ActionsPublisher aPublisher = (ActionsPublisher)publishersIter.next();
      if(aPublisher.getActions() != null) {
        Iterator<Action> actionIter = aPublisher.getActions().iterator();
        while(actionIter.hasNext()) {
          Action anAction = (Action)actionIter.next();
          if(anAction == null)
            popup.addSeparator();
          else {
            popup.add(new XJMenuItem(anAction, sListenerProxy));
          }
        }
      }
    }

    return popup;
  }

  public String getTooltipText() {
    return tooltipText;
  }

  public void setTooltipText(String text) {
    this.tooltipText = text;
  }

  public Object getTarget() {
    return target;
  }

  public Action getCloseAction() {
    return new CloseAction();
  }

  public Action getCloseRecursivelyAction() {
    return new CloseRecursivelyAction();
  }
  
  protected void buildStaticPopupItems() {
    // build the static part of the popup
    staticPopupItems = new ArrayList<XJMenuItem>();

    if(target instanceof ProcessingResource && !(target instanceof Controller)) {
      // actions for PRs (but not Controllers)
      staticPopupItems.add(null);
      staticPopupItems.add(new XJMenuItem(new ReloadAction(), sListenerProxy));
    }
    else if(target instanceof LanguageResource) {
      // Language Resources
      staticPopupItems.add(null);
      if(target instanceof Document) {
        staticPopupItems.add(new XJMenuItem(new CreateCorpusForDocAction(),
                sListenerProxy));
      }
      if(target instanceof gate.TextualDocument) {
        XJMenuItem saveAsXmlItem = new XJMenuItem(new SaveAsXmlAction(),
                sListenerProxy);
        staticPopupItems.add(null);
        staticPopupItems.add(saveAsXmlItem);
      }
      else if(target instanceof Corpus) {
        corpusFiller = new CorpusFillerComponent();
        scfInputDialog = new SingleConcatenatedFileInputDialog();
        staticPopupItems.add(new XJMenuItem(new PopulateCorpusAction(),
                sListenerProxy));
        staticPopupItems.add(new XJMenuItem(
                new PopulateCorpusFromSingleConcatenatedFileAction(),
                sListenerProxy));
        staticPopupItems.add(null);
        staticPopupItems.add(new XJMenuItem(new SaveCorpusAsXmlAction(),
                sListenerProxy));
        // staticPopupItems.add(new XJMenuItem(new
        // SaveCorpusAsXmlAction(true), sListenerProxy));
        if(target instanceof IndexedCorpus) {
          IndexedCorpus ic = (IndexedCorpus)target;
          if(ic.getDataStore() != null
                  && ic.getDataStore() instanceof LuceneDataStoreImpl) {
            // do nothing
          }
          else {
            staticPopupItems.add(new XJMenuItem(new CreateIndexAction(),
                    sListenerProxy));
            staticPopupItems.add(new XJMenuItem(new OptimizeIndexAction(),
                    sListenerProxy));
            staticPopupItems.add(new XJMenuItem(new DeleteIndexAction(),
                    sListenerProxy));
          }
        }
      }
      if(((LanguageResource)target).getDataStore() != null) {
        // this item can be used only if the resource belongs to a
        // datastore
        staticPopupItems.add(new XJMenuItem(new SaveAction(), sListenerProxy));
      }
      if(!(target instanceof AnnotationSchema)) {
        staticPopupItems
                .add(new XJMenuItem(new SaveToAction(), sListenerProxy));
      }
    }

    if(target instanceof Controller) {
      // Applications
      staticPopupItems.add(null);
      
      if (target instanceof SerialAnalyserController) {
        staticPopupItems.add(new XJMenuItem(new MakeConditionalAction(), sListenerProxy));
      }
            
      staticPopupItems.add(new XJMenuItem(new DumpToFileAction(),
              sListenerProxy));
      staticPopupItems.add(new XJMenuItem(new ExportApplicationAction(),
              sListenerProxy));
    }
  }

  protected void buildViews() {
    viewsBuilt = true;
    fireStatusChanged("Building views...");

    // build the large views
    List<String> largeViewNames = Gate.getCreoleRegister().getLargeVRsForResource(
            target.getClass().getName());
    if(largeViewNames != null && !largeViewNames.isEmpty()) {
      largeView = new JTabbedPane(JTabbedPane.BOTTOM);
      Iterator<String> classNameIter = largeViewNames.iterator();
      while(classNameIter.hasNext()) {
        try {
          String className = (String)classNameIter.next();
          ResourceData rData = (ResourceData)Gate.getCreoleRegister().get(
                  className);
          FeatureMap params = Factory.newFeatureMap();
          FeatureMap features = Factory.newFeatureMap();
          Gate.setHiddenAttribute(features, true);
          VisualResource view = (VisualResource)Factory.createResource(
                  className, params, features);
          view.setTarget(target);
          view.setHandle(this);
          ((JTabbedPane)largeView).add((Component)view, rData.getName());
          // if view provide actions, add it to the list of action
          // publishers
          if(view instanceof ActionsPublisher)
            actionPublishers.add((ActionsPublisher)view);
        }
        catch(ResourceInstantiationException rie) {
          rie.printStackTrace(Err.getPrintWriter());
        }
      }
      if(largeViewNames.size() == 1) {
        largeView = (JComponent)((JTabbedPane)largeView).getComponentAt(0);
      }
      else {
        ((JTabbedPane)largeView).setSelectedIndex(0);
      }
    }

    // build the small views
    List<String> smallViewNames = Gate.getCreoleRegister().getSmallVRsForResource(
            target.getClass().getName());
    if(smallViewNames != null && !smallViewNames.isEmpty()) {
      smallView = new JTabbedPane(JTabbedPane.BOTTOM);
      Iterator<String> classNameIter = smallViewNames.iterator();
      while(classNameIter.hasNext()) {
        try {
          String className = (String)classNameIter.next();
          ResourceData rData = (ResourceData)Gate.getCreoleRegister().get(
                  className);
          FeatureMap params = Factory.newFeatureMap();
          FeatureMap features = Factory.newFeatureMap();
          Gate.setHiddenAttribute(features, true);
          VisualResource view = (VisualResource)Factory.createResource(
                  className, params, features);
          view.setTarget(target);
          view.setHandle(this);
          ((JTabbedPane)smallView).add((Component)view, rData.getName());
          if(view instanceof ActionsPublisher)
            actionPublishers.add((ActionsPublisher)view);
        }
        catch(ResourceInstantiationException rie) {
          rie.printStackTrace(Err.getPrintWriter());
        }
      }
      if(smallViewNames.size() == 1) {
        smallView = (JComponent)((JTabbedPane)smallView).getComponentAt(0);
      }
      else {
        ((JTabbedPane)smallView).setSelectedIndex(0);
      }
    }
    fireStatusChanged("Views built!");

    // Add the CTRL +F4 key & action combination to the resource
    JComponent largeView = this.getLargeView();
    if(largeView != null) {
      largeView.getActionMap().put("Close resource", new CloseAction());
      if(target instanceof Controller) {
        largeView.getActionMap().put("Close recursively",
                new CloseRecursivelyAction());
      }
      if(target instanceof gate.TextualDocument) {
        largeView.getActionMap().put("Save As XML", new SaveAsXmlAction());
      }// End if
    }// End if
  }// protected void buildViews

  public String toString() {
    return getTitle();
  }

  @SuppressWarnings("unchecked")
  public synchronized void removeProgressListener(ProgressListener l) {
    if(progressListeners != null && progressListeners.contains(l)) {
      Vector<ProgressListener> v = (Vector<ProgressListener>)progressListeners
              .clone();
      v.removeElement(l);
      progressListeners = v;
    }
  }// public synchronized void removeProgressListener(ProgressListener

  // l)

  @SuppressWarnings("unchecked")
  public synchronized void addProgressListener(ProgressListener l) {
    Vector<ProgressListener> v = progressListeners == null
            ? new Vector<ProgressListener>(2)
            : (Vector<ProgressListener>)progressListeners.clone();
    if(!v.contains(l)) {
      v.addElement(l);
      progressListeners = v;
    }
  }// public synchronized void addProgressListener(ProgressListener l)

  String tooltipText;

  NameBearer target;

  /**
   * Stores all the action providers for this resource. They will be
   * questioned when the getPopup() method is called.
   */
  protected List<ActionsPublisher> actionPublishers;

  /**
   * A list of menu items that constitute the static part of the popup.
   * Null values are used for separators.
   */
  protected List<XJMenuItem> staticPopupItems;

  /**
   * The top level GUI component this handle belongs to.
   */
  Window window;

  ResourceData rData;

  Icon icon;

  JComponent smallView;

  JComponent largeView;

  protected boolean viewsBuilt = false;

  /**
   * Component used to select the options for corpus populating
   */
  protected CorpusFillerComponent corpusFiller;

  protected SingleConcatenatedFileInputDialog scfInputDialog;

  StatusListener sListenerProxy;

  private transient Vector<ProgressListener> progressListeners;

  private transient Vector<StatusListener> statusListeners;

  class CloseAction extends AbstractAction {
    private static final long serialVersionUID = -89664884870963556L;

    public CloseAction() {
      super("Close");
      putValue(SHORT_DESCRIPTION, "Close this resource");
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control F4"));
    }

    public void actionPerformed(ActionEvent e) {
      if(target instanceof Controller) {
        // empty the controller of all its processing resources
        ((Controller)target).setPRs(Collections.emptyList());
        if(target instanceof ConditionalController) {
          ((ConditionalController)target).setRunningStrategies(Collections
                  .emptyList());
        }
      }
      if(target instanceof Resource) {
        Factory.deleteResource((Resource)target);
      }
      else if(target instanceof DataStore) {
        try {
          ((DataStore)target).close();
        }
        catch(PersistenceException pe) {
          JOptionPane.showMessageDialog(largeView != null
                  ? largeView
                  : smallView, "Error!\n" + pe.toString(), "GATE",
                  JOptionPane.ERROR_MESSAGE);
        }
      }
      statusListeners.clear();
      progressListeners.clear();
    }
  }

  class CloseRecursivelyAction extends AbstractAction {
    private static final long serialVersionUID = 6110698764173549317L;

    public CloseRecursivelyAction() {
      super("Close Recursively");
      putValue(SHORT_DESCRIPTION,
              "Close this application and recursively all contained resources");
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("shift F4"));
    }

    public void actionPerformed(ActionEvent e) {
      Factory.deleteResource((Resource)target);
      statusListeners.clear();
      progressListeners.clear();
    }
  }

  /**
   * Used to save a document as XML
   */
  class SaveAsXmlAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    public SaveAsXmlAction() {
      super("Save as XML...");
      putValue(SHORT_DESCRIPTION, "Saves this resource in GATE XML format");
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control S"));
    }// SaveAsXmlAction()

    public void actionPerformed(ActionEvent e) {
      Runnable runableAction = new Runnable() {
        public void run() {
          XJFileChooser fileChooser = MainFrame.getFileChooser();
          ExtensionFileFilter filter = new ExtensionFileFilter("XML files",
                  "xml", "gml");
          fileChooser.addChoosableFileFilter(filter);
          fileChooser.setMultiSelectionEnabled(false);
          fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
          fileChooser.setDialogTitle("Select document to save ...");

          gate.Document doc = (gate.Document)target;
          if(doc.getSourceUrl() != null) {
            String fileName = "";
            try {
              fileName = doc.getSourceUrl().toURI().getPath().trim();
            }
            catch(URISyntaxException e) {
              fileName = doc.getSourceUrl().getPath().trim();
            }
            if(fileName.equals("") || fileName.equals("/")) {
              if(doc.getNamedAnnotationSets().containsKey("Original markups")
                      && !doc.getAnnotations("Original markups").get("title")
                              .isEmpty()) {
                // use the title annotation if any
                try {
                  fileName = doc.getContent().getContent(
                          doc.getAnnotations("Original markups").get("title")
                                  .firstNode().getOffset(),
                          doc.getAnnotations("Original markups").get("title")
                                  .lastNode().getOffset()).toString();
                }
                catch(InvalidOffsetException e) {
                  e.printStackTrace();
                }
              }
              else {
                fileName = doc.getSourceUrl().toString();
              }
              // cleans the file name
              fileName = fileName.replaceAll("/", "_");
            }
            else {
              // replaces the extension with .xml
              fileName = fileName.replaceAll("\\.[a-zA-Z]{1,4}$", ".xml");
            }
            // cleans the file name
            fileName = fileName.replaceAll("[^/a-zA-Z0-9._-]", "_");
            fileName = fileName.replaceAll("__+", "_");
            // adds a .xml extension if not present
            if(!fileName.endsWith(".xml")) {
              fileName += ".xml";
            }
            File file = new File(fileName);
            fileChooser.ensureFileIsVisible(file);
            fileChooser.setSelectedFile(file);
          }

          int res = (getLargeView() != null) ? fileChooser
                  .showSaveDialog(getLargeView()) : (getSmallView() != null)
                  ? fileChooser.showSaveDialog(getSmallView())
                  : fileChooser.showSaveDialog(null);
          if(res == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if(selectedFile == null) return;
            long start = System.currentTimeMillis();
            NameBearerHandle.this.statusChanged("Saving as XML to "
                    + selectedFile.toString() + "...");
            try {
              MainFrame.lockGUI("Saving...");
              // Prepare to write into the xmlFile using the original
              // encoding
              // //////////////////////////////
              // String encoding =
              // ((gate.TextualDocument)target).getEncoding();

              // OutputStreamWriter writer = new OutputStreamWriter(
              // new FileOutputStream(selectedFile),
              // encoding);

              // Write (test the toXml() method)
              // This Action is added only when a gate.Document is
              // created.
              // So, is for sure that the resource is a gate.Document
              // writer.write(((gate.Document)target).toXml());
              // writer.flush();
              // writer.close();

              // write directly to the file using StAX
              DocumentStaxUtils.writeDocument((gate.Document)target,
                      selectedFile);
              ((gate.Document)target)
                      .setSourceUrl(selectedFile.toURI().toURL());
            }
            catch(Exception ex) {
              ex.printStackTrace(Out.getPrintWriter());
            }
            finally {
              MainFrame.unlockGUI();
            }
            long time = System.currentTimeMillis() - start;
            NameBearerHandle.this.statusChanged("Finished saving as xml into "
                    + " the file: " + selectedFile.toString() + " in "
                    + ((double)time) / 1000 + " s");
          }// End if
        }// End run()
      };// End Runnable
      Thread thread = new Thread(runableAction, "");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }// actionPerformed()
  }// SaveAsXmlAction

  /**
   * Saves a corpus as a set of xml files in a directory.
   */
  class SaveCorpusAsXmlAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    public SaveCorpusAsXmlAction() {
      super("Save as XML...");
      putValue(SHORT_DESCRIPTION, "Saves each document in GATE XML format");
    }// SaveAsXmlAction()

    public void actionPerformed(ActionEvent e) {
      Runnable runnable = new Runnable() {
        public void run() {
          try {
            // we need a directory
            XJFileChooser fileChooser = MainFrame.getFileChooser();
            fileChooser
                    .setDialogTitle("Select the directory that will contain the corpus");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if(fileChooser.showDialog(getLargeView() != null
                    ? getLargeView()
                    : getSmallView(), "Select") == JFileChooser.APPROVE_OPTION) {

              File dir = fileChooser.getSelectedFile();
              // create the top directory if needed
              if(!dir.exists()) {
                if(!dir.mkdirs()) {
                  JOptionPane.showMessageDialog(largeView != null
                          ? largeView
                          : smallView, "Could not create top directory!",
                          "GATE", JOptionPane.ERROR_MESSAGE);
                  return;
                }
              }

              MainFrame.lockGUI("Saving...");

              // iterate through all the docs and save each of them as
              // xml
              Corpus corpus = (Corpus)target;
              Iterator<Document> docIter = corpus.iterator();
              boolean overwriteAll = false;
              int docCnt = corpus.size();
              int currentDocIndex = 0;
              Set<String> usedFileNames = new HashSet<String>();
              while(docIter.hasNext()) {
                boolean docWasLoaded = corpus.isDocumentLoaded(currentDocIndex);
                Document currentDoc = (Document)docIter.next();
                URL sourceURL = currentDoc.getSourceUrl();
                String fileName = null;
                if(sourceURL != null) {
                  fileName = sourceURL.getPath();
                  fileName = Files.getLastPathComponent(fileName);
                }
                if(fileName == null || fileName.length() == 0) {
                  fileName = currentDoc.getName();
                }
                // makes sure that the filename does not contain any
                // forbidden character
                fileName = fileName.replaceAll("[\\/:\\*\\?\"<>|]", "_");
                if(fileName.toLowerCase().endsWith(".xml")) {
                 fileName = fileName.substring(0,fileName.length() - 4); 
                }
                if(usedFileNames.contains(fileName)){
                  // name clash -> add unique ID
                  String fileNameBase = fileName;
                  int  uniqId = 0;
                  fileName = fileNameBase + "-" + uniqId++;
                  while(usedFileNames.contains(fileName)) {
                    fileName = fileNameBase + "-" + uniqId++;
                  }                  
                }
                usedFileNames.add(fileName);
                if(!fileName.toLowerCase().endsWith(".xml"))
                  fileName += ".xml";
                File docFile = null;
                boolean nameOK = false;
                do {
                  docFile = new File(dir, fileName);
                  if(docFile.exists() && !overwriteAll) {
                    // ask the user if we can overwrite the file
                    Object[] options = new Object[] {"Yes", "All", "No",
                        "Cancel"};
                    MainFrame.unlockGUI();
                    int answer = JOptionPane.showOptionDialog(largeView != null
                            ? largeView
                            : smallView, "File " + docFile.getName()
                            + " already exists!\n" + "Overwrite?", "GATE",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, options,
                            options[2]);
                    MainFrame.lockGUI("Saving...");
                    switch(answer) {
                      case 0: {
                        nameOK = true;
                        break;
                      }
                      case 1: {
                        nameOK = true;
                        overwriteAll = true;
                        break;
                      }
                      case 2: {
                        // user said NO, allow them to provide an
                        // alternative name;
                        MainFrame.unlockGUI();
                        fileName = (String)JOptionPane.showInputDialog(
                                largeView != null ? largeView : smallView,
                                "Please provide an alternative file name",
                                "GATE", JOptionPane.QUESTION_MESSAGE, null,
                                null, fileName);
                        if(fileName == null) {
                          fireProcessFinished();
                          return;
                        }
                        MainFrame.lockGUI("Saving");
                        break;
                      }
                      case 3: {
                        // user gave up; return
                        fireProcessFinished();
                        return;
                      }
                    }

                  }
                  else {
                    nameOK = true;
                  }
                } while(!nameOK);
                // save the file
                try {
                  // for GATE XML format, use the direct StAX writer
                  DocumentStaxUtils.writeDocument(currentDoc, docFile);  
                }
                catch(Exception ioe) {
                  MainFrame.unlockGUI();
                  JOptionPane.showMessageDialog(largeView != null
                          ? largeView
                          : smallView, "Could not create write file:"
                          + ioe.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
                  ioe.printStackTrace(Err.getPrintWriter());
                  return;
                }

                fireStatusChanged(currentDoc.getName() + " saved");
                // close the doc if it wasn't already loaded
                if(!docWasLoaded) {
                  corpus.unloadDocument(currentDoc);
                  Factory.deleteResource(currentDoc);
                }

                fireProgressChanged(100 * currentDocIndex++ / docCnt);
              }// while(docIter.hasNext())
              fireStatusChanged("Corpus saved");
              fireProcessFinished();
            }// select directory
          }
          finally {
            MainFrame.unlockGUI();
          }
        }// public void run(){
      };// Runnable runnable = new Runnable()
      Thread thread = new Thread(Thread.currentThread().getThreadGroup(),
              runnable, "Corpus XML dumper");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();

    }// public void actionPerformed(ActionEvent e)
  }// class SaveCorpusAsXmlAction extends AbstractAction

  class MakeConditionalAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    public MakeConditionalAction() {
      super("Make Pipeline Conditional");
      putValue(SHORT_DESCRIPTION, "Convert to a Conditional Corpus Pipeline");
    }

    public void actionPerformed(ActionEvent e) {
      SerialAnalyserController existingController =
          (SerialAnalyserController)target;
      try {
        ConditionalSerialAnalyserController newController =
            (ConditionalSerialAnalyserController)Factory
                .createResource("gate.creole.ConditionalSerialAnalyserController");
        newController.getFeatures().putAll(existingController.getFeatures());
        newController.setName(existingController.getName());
        Iterator<?> it = existingController.getPRs().iterator();
        while(it.hasNext()) {
          newController.add((ProcessingResource)it.next());
        }
        existingController.setPRs(Collections.emptyList());
        Factory.deleteResource(existingController);
      } catch(Exception ex) {
        JOptionPane.showMessageDialog(getLargeView(),
            "Error!\n" + ex.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace(Err.getPrintWriter());
      }
    }
  }

  class SaveAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    public SaveAction() {
      super("Save to its Datastore");
      putValue(SHORT_DESCRIPTION, "Save back to its datastore");
    }

    public void actionPerformed(ActionEvent e) {
      Runnable runnable = new Runnable() {
        public void run() {
          DataStore ds = ((LanguageResource)target).getDataStore();
          if(ds != null) {
            try {
              MainFrame.lockGUI("Saving "
                      + ((LanguageResource)target).getName());
              StatusListener sListener = (StatusListener)gate.Gate
                      .getListeners().get("gate.event.StatusListener");
              if(sListener != null)
                sListener.statusChanged("Saving: "
                        + ((LanguageResource)target).getName());
              double timeBefore = System.currentTimeMillis();
              ((LanguageResource)target).getDataStore().sync(
                      (LanguageResource)target);
              double timeAfter = System.currentTimeMillis();
              if(sListener != null)
                sListener.statusChanged(((LanguageResource)target).getName()
                        + " saved in "
                        + NumberFormat.getInstance().format(
                                (timeAfter - timeBefore) / 1000) + " seconds");
            }
            catch(PersistenceException pe) {
              MainFrame.unlockGUI();
              JOptionPane.showMessageDialog(getLargeView(), "Save failed!\n "
                      + pe.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
            }
            catch(SecurityException se) {
              MainFrame.unlockGUI();
              JOptionPane.showMessageDialog(getLargeView(), "Save failed!\n "
                      + se.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
            }
            finally {
              MainFrame.unlockGUI();
            }
          }
          else {
            JOptionPane
                    .showMessageDialog(
                            getLargeView(),
                            "This resource has not been loaded from a datastore.\n"
                                    + "Please use the \"Save to Datastore...\" option.\n",
                            "GATE", JOptionPane.ERROR_MESSAGE);

          }
        }
      };
      new Thread(runnable).start();
    }// public void actionPerformed(ActionEvent e)
  }// class SaveAction

  class DumpToFileAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    public DumpToFileAction() {
      super("Save Application State");
      putValue(SHORT_DESCRIPTION,
              "Saves the data needed to recreate this application");
    }

    public void actionPerformed(ActionEvent ae) {
      final XJFileChooser fileChooser = MainFrame.getFileChooser();
      ExtensionFileFilter filter = new ExtensionFileFilter(
              "GATE Application files", "gapp");
      fileChooser.addChoosableFileFilter(filter);
      fileChooser
              .setDialogTitle("Select a file where to save the application "
                      + ((target instanceof CorpusController && ((CorpusController)target)
                              .getCorpus() != null) ? "WITH" : "WITHOUT")
                      + " corpus.");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fileChooser.setResource("application." + target.getName());

      if(fileChooser.showSaveDialog(largeView) == JFileChooser.APPROVE_OPTION) {
        final File file = fileChooser.getSelectedFile();
        Runnable runnable = new Runnable() {
          public void run() {
            try {
              Map<String, String> locations = fileChooser.getLocations();
              // When saving an application state, use paths relative to
              // GATE HOME for resources inside GATE HOME and warn about
              // them.
              gate.util.persistence.PersistenceManager.saveObjectToFile(target,
                      file, true, true);
              // save also the location of the application as last
              // application
              locations.put("lastapplication", file.getCanonicalPath());
              // add this application to the list of recent applications
              String list = locations.get("applications");
              if(list == null) {
                list = "";
              }
              list = list.replaceFirst("\\Q" + target.getName() + "\\E(;|$)",
                      "");
              list = target.getName() + ";" + list;
              locations.put("applications", list);
              fileChooser.setLocations(locations);
            }
            catch(Exception e) {
              JOptionPane.showMessageDialog(getLargeView(), "Error!\n"
                      + e.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
              e.printStackTrace(Err.getPrintWriter());
            }
          }
        };
        Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
      }
    }

  }

  class ExportApplicationAction extends AbstractAction {
    private Logger log = Logger.getLogger("gate.gui.ExportApplicationAction");

    private static final long serialVersionUID = 1L;

    public ExportApplicationAction() {
      super("Export for GATECloud.net");
      putValue(SHORT_DESCRIPTION,
              "Saves the resources of this application in a ZIP file");
    }

    /**
     * Build listener to receive log messages from Ant tasks and forward
     * them to the GATE status listener (i.e. the status bar). This
     * class implements Executable not because it is itself executed,
     * but in order to support interruption. If the user presses the
     * stop button in the GUI lock window this object will be
     * interrupted, and will throw an exception at the next call to
     * messageLogged, which has the effect of interrupting the currently
     * executing Ant task.
     */
    class ExporterBuildListener implements BuildListener, Executable {
      private boolean interrupted = false;

      public boolean isInterrupted() {
        return interrupted;
      }

      public void interrupt() {
        interrupted = true;
      }

      /**
       * Set status message appropriately when task completes or fails.
       */
      public void taskFinished(BuildEvent buildEvent) {
        if(buildEvent.getException() != null) {
          statusChanged("Error exporting application");
        }
        else {
          statusChanged("Export complete");
        }
      }

      /**
       * This is called for every log message (of any priority). If the
       * current process has been interrupted (the user pressed the stop
       * button) then we throw an exception to interrupt the currently
       * executing Ant task. Other than that, we simply pass INFO and
       * higher messages to the GATE status listener.
       */
      public void messageLogged(BuildEvent buildEvent) {
        // check for interruption
        if(interrupted) {
          interrupted = false;
          throw new BuildException("Export interrupted");
        }
        if(buildEvent.getPriority() <= Project.MSG_INFO) {
          statusChanged(buildEvent.getMessage());
        }
        // log the message to log4j for debugging purposes
        log.debug(buildEvent.getPriority() + ": " + buildEvent.getMessage());
      }

      // not interested in these events
      public void buildStarted(BuildEvent buildEvent) {
      }

      public void buildFinished(BuildEvent buildEvent) {
      }

      public void targetStarted(BuildEvent buildEvent) {
      }

      public void targetFinished(BuildEvent buildEvent) {
      }

      public void taskStarted(BuildEvent buildEvent) {
      }

      public void execute() {
        // do nothing, only here to match the interface
      }
    }

    public void actionPerformed(ActionEvent ae) {
      XJFileChooser fileChooser = MainFrame.getFileChooser();
      ExtensionFileFilter filter = new ExtensionFileFilter("ZIP file", "zip");
      fileChooser.addChoosableFileFilter(filter);
      fileChooser
              .setDialogTitle("Select a file where to save the application "
                      + ((target instanceof CorpusController && ((CorpusController)target)
                              .getCorpus() != null) ? "WITH" : "WITHOUT")
                      + " corpus.");
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fileChooser.setResource("application.zip." + target.getName());

      if(fileChooser.showSaveDialog(largeView) == JFileChooser.APPROVE_OPTION) {
        
        //ensure that the ZIP file we produce actually ends .zip
        File checkFile = fileChooser.getSelectedFile();        
        if (!checkFile.getName().toLowerCase().endsWith(".zip")) {
          checkFile = new File(checkFile.getParent(), checkFile.getName()+".zip");
        }
        
        final File targetZipFile = checkFile;
        
        Runnable runnable = new Runnable() {
          public void run() {
            try {
              // create and configure Ant Project
              Project project = new Project();
              ExporterBuildListener buildListener = new ExporterBuildListener();
              Gate.setExecutable(buildListener);
              project.addBuildListener(buildListener);
              project.init();
              MainFrame.lockGUI("Exporting application...");

              // create a temporary directory, and save the
              // application
              // in the normal way to that directory
              File temporaryDirectory = File.createTempFile("gapp-packager",
                      "", null);
              if(!temporaryDirectory.delete() || !temporaryDirectory.mkdir()) {
                throw new IOException(
                        "Unable to create temporary directory.\n"
                                + temporaryDirectory.getCanonicalPath());
              }
              // canonicalise (e.g. on Mac OS X java.io.tmpdir is
              // /var/folders/something, but /var is a symlink to
              // /private/var,
              // and this can confuse the relpaths which are based on
              // canonical
              // path strings)
              temporaryDirectory = temporaryDirectory.getCanonicalFile();
              File originalGapp = new File(temporaryDirectory,
                      "original.xgapp");
              File targetGapp = new File(temporaryDirectory,
                      "application.xgapp");

              // save the application in a gapp file
              // When exporting to cloud, the gapp file should not
              // contain
              // any paths relative to GATE HOME, but we still warn
              // about
              // resources under GATE HOME
              gate.util.persistence.PersistenceManager.saveObjectToFile(
                      target, originalGapp, false, true);

              // create instance of packager task and configure it
              PackageGappTask task = new PackageGappTask();
              task.setProject(project);
              task.setSrc(originalGapp);
              task.setDestFile(targetGapp);
              // sensible default settings
              task.setCopyPlugins(true);
              task.setExpandIvy(true);
              task.setCopyResourceDirs(true);
              task.setOnUnresolved(PackageGappTask.UnresolvedAction.recover);
              task.init();

              // run the task.
              task.perform();

              // create zip file using standard Ant zip task
              Zip zipTask = new Zip();
              zipTask.setProject(project);
              zipTask.setDestFile(targetZipFile);
              FileSet fs = new FileSet();
              fs.setProject(project);
              zipTask.addFileset(fs);
              fs.setDir(temporaryDirectory);
              // exclude the unpackaged gapp file from the zip
              fs.setExcludes("original.xgapp");
              zipTask.perform();

              // delete temporary files
              Delete deleteTask = new Delete();
              deleteTask.setProject(project);
              deleteTask.setDir(temporaryDirectory);
              deleteTask.perform();
            }
            catch(Exception e) {
              MainFrame.unlockGUI();
              JOptionPane.showMessageDialog(getLargeView(), "Error!\n"
                      + e.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
              e.printStackTrace(Err.getPrintWriter());
            }
            finally {
              MainFrame.unlockGUI();
              Gate.setExecutable(null);
            }
          }
        };
        Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
      }
    }

  }

  class SaveToAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    public SaveToAction() {
      super("Save to Datastore...");
      putValue(SHORT_DESCRIPTION, "Save this resource to a datastore");
    }

    public void actionPerformed(ActionEvent e) {
      Runnable runnable = new Runnable() {
        public void run() {
          try {
            DataStoreRegister dsReg = Gate.getDataStoreRegister();
            Map<String, DataStore> dsByName = new HashMap<String, DataStore>();
            Iterator<DataStore> dsIter = dsReg.iterator();
            while(dsIter.hasNext()) {
              DataStore oneDS = (DataStore)dsIter.next();
              String name;
              if((name = (String)oneDS.getName()) != null) {
              }
              else {
                name = oneDS.getStorageUrl();
                try {
                  URL tempURL = new URL(name);
                  name = tempURL.getFile();
                }
                catch(java.net.MalformedURLException ex) {
                  throw new GateRuntimeException();
                }
              }
              dsByName.put(name, oneDS);
            }
            List<String> dsNames = new ArrayList<String>(dsByName.keySet());
            if(dsNames.isEmpty()) {
              JOptionPane.showMessageDialog(getLargeView(),
                      "There are no open datastores!\n "
                              + "Please open a datastore first!", "GATE",
                      JOptionPane.ERROR_MESSAGE);

            }
            else {
              Object answer = JOptionPane.showInputDialog(getLargeView(),
                      "Select the datastore", "GATE",
                      JOptionPane.QUESTION_MESSAGE, null, dsNames.toArray(),
                      dsNames.get(0));
              if(answer == null) return;
              DataStore ds = (DataStore)dsByName.get(answer);
              if(ds == null) {
                Err.prln("The datastore does not exists. Saving procedure"
                        + " has FAILED! This should never happen again!");
                return;
              }// End if
              DataStore ownDS = ((LanguageResource)target).getDataStore();
              if(ds == ownDS) {
                MainFrame.lockGUI("Saving "
                        + ((LanguageResource)target).getName());

                StatusListener sListener = (StatusListener)gate.Gate
                        .getListeners().get("gate.event.StatusListener");
                if(sListener != null)
                  sListener.statusChanged("Saving: "
                          + ((LanguageResource)target).getName());
                double timeBefore = System.currentTimeMillis();
                ds.sync((LanguageResource)target);
                double timeAfter = System.currentTimeMillis();
                if(sListener != null)
                  sListener
                          .statusChanged(((LanguageResource)target).getName()
                                  + " saved in "
                                  + NumberFormat.getInstance().format(
                                          (timeAfter - timeBefore) / 1000)
                                  + " seconds");
              }
              else {
                FeatureMap securityData = (FeatureMap)DataStoreRegister
                        .getSecurityData(ds);
                SecurityInfo si = null;
                // check whether the datastore supports security data
                // serial ones do not for example
                if(securityData != null) {
                  // first get the type of access from the user
                  if(!AccessRightsDialog.showDialog(window)) return;
                  int accessType = AccessRightsDialog.getSelectedMode();
                  if(accessType < 0) return;
                  si = new SecurityInfo(accessType, (User)securityData
                          .get("user"), (Group)securityData.get("group"));
                }// if security info
                StatusListener sListener = (StatusListener)gate.Gate
                        .getListeners().get("gate.event.StatusListener");
                MainFrame.lockGUI("Saving "
                        + ((LanguageResource)target).getName());

                if(sListener != null)
                  sListener.statusChanged("Saving: "
                          + ((LanguageResource)target).getName());
                double timeBefore = System.currentTimeMillis();
                LanguageResource lr = ds.adopt((LanguageResource)target, si);
                ds.sync(lr);
                if(ds instanceof LuceneDataStoreImpl
                        && lr instanceof IndexedCorpus) {
                  Object persistanceID = lr.getLRPersistenceId();
                  String lrType = lr.getClass().getName();
                  String lrName = lr.getName();
                  Factory.deleteResource(lr);
                  FeatureMap params = Factory.newFeatureMap();
                  params.put(DataStore.DATASTORE_FEATURE_NAME, ds);
                  params.put(DataStore.LR_ID_FEATURE_NAME, persistanceID);
                  FeatureMap features = Factory.newFeatureMap();
                  try {
                    lr = (LanguageResource)Factory.createResource(lrType,
                            params, features, lrName);
                  }
                  catch(ResourceInstantiationException rie) {
                    throw new GateRuntimeException("Could not load the corpus",
                            rie);
                  }
                }

                double timeAfter = System.currentTimeMillis();
                if(sListener != null)
                  sListener
                          .statusChanged(((LanguageResource)target).getName()
                                  + " saved in "
                                  + NumberFormat.getInstance().format(
                                          (timeAfter - timeBefore) / 1000)
                                  + " seconds");

                // check whether the new LR is different from the
                // transient one and
                // if so, unload the transient LR, so the user realises
                // it is no longer valid. Don't do this in the adopt()
                // code itself
                // because the batch code might wish to keep the
                // transient
                // resource for some purpose.
                if(lr != target) {
                  Factory.deleteResource((LanguageResource)target);
                }
              }
            }
          }
          catch(PersistenceException pe) {
            MainFrame.unlockGUI();
            JOptionPane.showMessageDialog(getLargeView(), "Save failed!\n "
                    + pe.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
          }
          catch(gate.security.SecurityException se) {
            MainFrame.unlockGUI();
            JOptionPane.showMessageDialog(getLargeView(), "Save failed!\n "
                    + se.toString(), "GATE", JOptionPane.ERROR_MESSAGE);
          }
          finally {
            MainFrame.unlockGUI();
          }

        }
      };
      new Thread(runnable).start();
    }
  }// class SaveToAction extends AbstractAction

  class ReloadAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    ReloadAction() {
      super("Reinitialise");
      putValue(SHORT_DESCRIPTION, "Reloads this resource");
    }

    public void actionPerformed(ActionEvent e) {
      Runnable runnable = new Runnable() {
        public void run() {
          if(!(target instanceof ProcessingResource)) return;
          if(target instanceof Controller) return;
          try {
            long startTime = System.currentTimeMillis();
            fireStatusChanged("Reinitialising " + target.getName());
            Map<String, EventListener> listeners = new HashMap<String, EventListener>();
            StatusListener sListener = new StatusListener() {
              public void statusChanged(String text) {
                fireStatusChanged(text);
              }
            };
            listeners.put("gate.event.StatusListener", sListener);

            ProgressListener pListener = new ProgressListener() {
              public void progressChanged(int value) {
                fireProgressChanged(value);
              }

              public void processFinished() {
                fireProcessFinished();
              }
            };
            listeners.put("gate.event.ProgressListener", pListener);

            ProcessingResource res = (ProcessingResource)target;
            try {
              AbstractResource.setResourceListeners(res, listeners);
            }
            catch(Exception e) {
              e.printStackTrace(Err.getPrintWriter());
            }
            // show the progress indicator
            fireProgressChanged(0);
            // the actual reinitialisation
            res.reInit();
            try {
              AbstractResource.removeResourceListeners(res, listeners);
            }
            catch(Exception e) {
              e.printStackTrace(Err.getPrintWriter());
            }
            long endTime = System.currentTimeMillis();
            fireStatusChanged(target.getName()
                    + " reinitialised in "
                    + NumberFormat.getInstance().format(
                            (double)(endTime - startTime) / 1000) + " seconds");
            fireProcessFinished();
          }
          catch(ResourceInstantiationException rie) {
            fireStatusChanged("reinitialisation failed");
            rie.printStackTrace(Err.getPrintWriter());
            JOptionPane.showMessageDialog(getLargeView(), "Reload failed!\n "
                    + "See \"Messages\" tab for details!", "GATE",
                    JOptionPane.ERROR_MESSAGE);
            fireProcessFinished();
          }
        }// public void run()
      };
      Thread thread = new Thread(Thread.currentThread().getThreadGroup(),
              runnable, "DefaultResourceHandle1");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }// public void actionPerformed(ActionEvent e)

  }// class ReloadAction

  class PopulateCorpusAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    PopulateCorpusAction() {
      super("Populate");
      putValue(SHORT_DESCRIPTION,
              "Fills this corpus with documents from a directory");
    }

    public void actionPerformed(ActionEvent e) {
      corpusFiller.setExtensions(new ArrayList());
      corpusFiller.setEncoding("");
      final boolean answer = OkCancelDialog.showDialog(window, corpusFiller,
              "Select a directory and allowed extensions");
      Runnable runnable = new Runnable() {
        public void run() {
          if(answer) {
            long startTime = System.currentTimeMillis();
            URL url;
            try {
              url = new URL(corpusFiller.getUrlString());
              List extensions = corpusFiller.getExtensions();
              ExtensionFileFilter filter;
              if(extensions == null || extensions.isEmpty()) {
                filter = null;
              }
              else {
                filter = new ExtensionFileFilter();
                Iterator extIter = corpusFiller.getExtensions().iterator();
                while(extIter.hasNext()) {
                  filter.addExtension((String)extIter.next());
                }
              }
              String encoding = corpusFiller.getEncoding();
              if(encoding != null && encoding.trim().length() == 0) {
                encoding = null;
              }
              String mimeType = corpusFiller.getMimeType();
              if(mimeType != null && mimeType.trim().length() == 0) {
                mimeType = null;
              }
              ((Corpus)target).populate(url, filter, encoding, mimeType,
                      corpusFiller.isRecurseDirectories());
              if(((Corpus)target).getDataStore() != null) {
                ((LanguageResource)target).getDataStore().sync(
                        (LanguageResource)target);
              }

              long endTime = System.currentTimeMillis();
              fireStatusChanged("Corpus populated in "
                      + NumberFormat.getInstance().format(
                              (double)(endTime - startTime) / 1000)
                      + " seconds!");

            }
            catch(MalformedURLException mue) {
              JOptionPane.showMessageDialog(getLargeView(), "Invalid URL!\n "
                      + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              mue.printStackTrace(Err.getPrintWriter());
            }
            catch(IOException ioe) {
              JOptionPane.showMessageDialog(getLargeView(), "I/O error!\n "
                      + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              ioe.printStackTrace(Err.getPrintWriter());
            }
            catch(ResourceInstantiationException rie) {
              JOptionPane.showMessageDialog(getLargeView(),
                      "Could not create document!\n "
                              + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              rie.printStackTrace(Err.getPrintWriter());
            }
            catch(PersistenceException pe) {
              JOptionPane.showMessageDialog(getLargeView(),
                      "Corpus couldn't be synchronized!\n "
                              + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              pe.printStackTrace(Err.getPrintWriter());
            }
            catch(SecurityException pe) {
              JOptionPane.showMessageDialog(getLargeView(),
                      "Corpus couldn't be synchronized!\n "
                              + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              pe.printStackTrace(Err.getPrintWriter());
            }
          }
        }
      };
      Thread thread = new Thread(Thread.currentThread().getThreadGroup(),
              runnable, "PopulateCorpusAction");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  class PopulateCorpusFromSingleConcatenatedFileAction extends AbstractAction {
    private static final long serialVersionUID = 2342321851865139492L;

    PopulateCorpusFromSingleConcatenatedFileAction() {
      super("Populate from Single Concatenated File");
      putValue(SHORT_DESCRIPTION,
              "Fills this corpus by extracting multiple documents from a single file");
    }

    public void actionPerformed(ActionEvent e) {
      scfInputDialog.setEncoding("");
      final boolean answer = OkCancelDialog.showDialog(window, scfInputDialog,
              "Select a trecweb file");
      Runnable runnable = new Runnable() {
        public void run() {
          if(answer) {

            String message = null;
            // lets first check the parameters
            if(scfInputDialog.getUrlString().trim().length() == 0) {
              message = "file URL cannot be empty";
            }
            else if(scfInputDialog.getDocumentRootElement().trim().length() == 0) {
              message = "document root element cannot be empty";
            }

            // if something wrong say it to the user
            if(message != null) {
              JOptionPane.showMessageDialog(getLargeView(), message, "GATE",
                      JOptionPane.ERROR_MESSAGE);
              return;
            }

            // all parameters fine, proceed 
            long startTime = System.currentTimeMillis();
            URL url = null;
            try {
              url = new URL(scfInputDialog.getUrlString());
              ((Corpus)target).populate(url, scfInputDialog
                      .getDocumentRootElement(), scfInputDialog.getEncoding(),
                      scfInputDialog.getNumOfDocumentsToFetch(), scfInputDialog
                              .getDocumentNamePrefix(), scfInputDialog
                              .getDocumentType());
              if(((Corpus)target).getDataStore() != null) {
                ((LanguageResource)target).getDataStore().sync(
                        (LanguageResource)target);
              }

              long endTime = System.currentTimeMillis();
              fireStatusChanged("Corpus populated in "
                      + NumberFormat.getInstance().format(
                              (double)(endTime - startTime) / 1000)
                      + " seconds!");

            }
            catch(MalformedURLException mue) {
              JOptionPane.showMessageDialog(getLargeView(), "Invalid URL!\n "
                      + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              mue.printStackTrace(Err.getPrintWriter());
            }
            catch(IOException ioe) {
              JOptionPane.showMessageDialog(getLargeView(), "I/O error!\n "
                      + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              ioe.printStackTrace(Err.getPrintWriter());
            }
            catch(ResourceInstantiationException rie) {
              JOptionPane.showMessageDialog(getLargeView(),
                      "Could not create document!\n "
                              + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              rie.printStackTrace(Err.getPrintWriter());
            }
            catch(PersistenceException pe) {
              JOptionPane.showMessageDialog(getLargeView(),
                      "Corpus couldn't be synchronized!\n "
                              + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              pe.printStackTrace(Err.getPrintWriter());
            }
            catch(SecurityException pe) {
              JOptionPane.showMessageDialog(getLargeView(),
                      "Corpus couldn't be synchronized!\n "
                              + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              pe.printStackTrace(Err.getPrintWriter());
            }
          }
        }
      };
      Thread thread = new Thread(Thread.currentThread().getThreadGroup(),
              runnable, "PopulateCorpusFromSingleConcatenatedFileAction");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  class CreateIndexAction extends AbstractAction {
    private static final long serialVersionUID = -292879296310753260L;

    CreateIndexAction() {
      super("Index Corpus");
      putValue(SHORT_DESCRIPTION, "Create index with documents from the corpus");
      createIndexGui = new CreateIndexGUI();
    }

    public void actionPerformed(ActionEvent e) {
      boolean ok = OkCancelDialog.showDialog(largeView, createIndexGui,
              "Index \"" + target.getName() + "\" corpus");
      if(ok) {
        DefaultIndexDefinition did = new DefaultIndexDefinition();
        IREngine engine = createIndexGui.getIREngine();
        did.setIrEngineClassName(engine.getClass().getName());

        did.setIndexLocation(createIndexGui.getIndexLocation().toString());

        // add the content if wanted
        if(createIndexGui.getUseDocumentContent()) {
          did.addIndexField(new IndexField("body", new DocumentContentReader(),
                  false));
        }
        // add all the features
        Iterator featIter = createIndexGui.getFeaturesList().iterator();
        while(featIter.hasNext()) {
          String featureName = (String)featIter.next();
          did.addIndexField(new IndexField(featureName, new FeatureReader(
                  featureName), false));
        }

        ((IndexedCorpus)target).setIndexDefinition(did);

        Thread thread = new Thread(new Runnable() {
          public void run() {
            try {
              fireProgressChanged(1);
              fireStatusChanged("Indexing corpus...");
              long start = System.currentTimeMillis();
              ((IndexedCorpus)target).getIndexManager().deleteIndex();
              fireProgressChanged(10);
              ((IndexedCorpus)target).getIndexManager().createIndex();
              fireProgressChanged(100);
              fireProcessFinished();
              fireStatusChanged("Corpus indexed in "
                      + NumberFormat
                              .getInstance()
                              .format(
                                      (double)(System.currentTimeMillis() - start) / 1000)
                      + " seconds");
            }
            catch(IndexException ie) {
              JOptionPane.showMessageDialog(getLargeView() != null
                      ? getLargeView()
                      : getSmallView(), "Could not create index!\n "
                      + "See \"Messages\" tab for details!", "GATE",
                      JOptionPane.ERROR_MESSAGE);
              ie.printStackTrace(Err.getPrintWriter());
            }
            finally {
              fireProcessFinished();
            }
          }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
      }
    }

    CreateIndexGUI createIndexGui;
  }

  class OptimizeIndexAction extends AbstractAction {
    private static final long serialVersionUID = 261845730081082766L;

    OptimizeIndexAction() {
      super("Optimize Index");
      putValue(SHORT_DESCRIPTION, "Optimize existing index");
    }

    public boolean isEnabled() {
      return ((IndexedCorpus)target).getIndexDefinition() != null;
    }

    public void actionPerformed(ActionEvent e) {
      Thread thread = new Thread(new Runnable() {
        public void run() {
          try {
            fireProgressChanged(1);
            fireStatusChanged("Optimising index...");
            long start = System.currentTimeMillis();
            ((IndexedCorpus)target).getIndexManager().optimizeIndex();
            fireStatusChanged("Index optimised in "
                    + NumberFormat
                            .getInstance()
                            .format(
                                    (double)(System.currentTimeMillis() - start) / 1000)
                    + " seconds");
            fireProcessFinished();
          }
          catch(IndexException ie) {
            JOptionPane.showMessageDialog(getLargeView() != null
                    ? getLargeView()
                    : getSmallView(), "Errors during optimisation!", "GATE",
                    JOptionPane.PLAIN_MESSAGE);
            ie.printStackTrace(Err.getPrintWriter());
          }
          finally {
            fireProcessFinished();
          }
        }
      });
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  class DeleteIndexAction extends AbstractAction {
    private static final long serialVersionUID = 6121632107964572415L;

    DeleteIndexAction() {
      super("Delete Index");
      putValue(SHORT_DESCRIPTION, "Delete existing index");
    }

    public boolean isEnabled() {
      return ((IndexedCorpus)target).getIndexDefinition() != null;
    }

    public void actionPerformed(ActionEvent e) {
      int answer = JOptionPane.showOptionDialog(getLargeView() != null
              ? getLargeView()
              : getSmallView(), "Do you want to delete index?", "Gate",
              JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              null, null);
      if(answer == JOptionPane.YES_OPTION) {
        try {
          IndexedCorpus ic = (IndexedCorpus)target;
          if(ic.getIndexManager() != null) {
            ic.getIndexManager().deleteIndex();
            ic.getFeatures().remove(
                    GateConstants.CORPUS_INDEX_DEFINITION_FEATURE_KEY);
          }
          else {
            JOptionPane.showMessageDialog(getLargeView() != null
                    ? getLargeView()
                    : getSmallView(), "There is no index to delete!", "GATE",
                    JOptionPane.PLAIN_MESSAGE);
          }
        }
        catch(gate.creole.ir.IndexException ie) {
          ie.printStackTrace();
        }
      }
    }
  }

  class CreateCorpusForDocAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = -3698451324578510407L;

    public CreateCorpusForDocAction() {
      super("New Corpus with this Document");
    }

    public void actionPerformed(ActionEvent e) {
      try {
        Corpus corpus = Factory.newCorpus("Corpus for " + target.getName());
        corpus.add((Document)target);
      }
      catch(ResourceInstantiationException rie) {
        Err.println("Exception creating corpus");
        rie.printStackTrace(Err.getPrintWriter());
      }
    }
  }

  public void removeViews() {
    // delete all the VRs that were created
    if(largeView != null) {
      if(largeView instanceof VisualResource) {
        // we only had a view so no tabbed pane was used
        if(largeView instanceof ActionsPublisher)
          actionPublishers.remove(largeView);
        Factory.deleteResource((VisualResource)largeView);
      }
      else {
        Component vrs[] = ((JTabbedPane)largeView).getComponents();
        for(int i = 0; i < vrs.length; i++) {
          if(vrs[i] instanceof VisualResource) {
            if(vrs[i] instanceof ActionsPublisher)
              actionPublishers.remove(vrs[i]);
            Factory.deleteResource((VisualResource)vrs[i]);
          }
        }
      }
      largeView = null;
    }

    if(smallView != null) {
      if(smallView instanceof VisualResource) {
        // we only had a view so no tabbed pane was used
        if(smallView instanceof ActionsPublisher)
          actionPublishers.remove(smallView);
        Factory.deleteResource((VisualResource)smallView);
      }
      else {
        Component vrs[] = ((JTabbedPane)smallView).getComponents();
        for(int i = 0; i < vrs.length; i++) {
          if(vrs[i] instanceof VisualResource) {
            if(vrs[i] instanceof ActionsPublisher)
              actionPublishers.remove(vrs[i]);
            Factory.deleteResource((VisualResource)vrs[i]);
          }
        }
      }
      smallView = null;
    }
    viewsBuilt = false;
  }

  /**
   * Releases the memory, removes the listeners, cleans up. Will get
   * called when the target resource is unloaded from the system
   */
  public void cleanup() {

    removeViews();
    Gate.getCreoleRegister().removeCreoleListener(this);
    target = null;
  }

  class ProxyStatusListener implements StatusListener {
    public void statusChanged(String text) {
      fireStatusChanged(text);
    }
  }

  protected void fireProgressChanged(int e) {
    if(progressListeners != null) {
      Vector<ProgressListener> listeners = progressListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        listeners.elementAt(i).progressChanged(e);
      }
    }
  }// protected void fireProgressChanged(int e)

  protected void fireProcessFinished() {
    if(progressListeners != null) {
      Vector<ProgressListener> listeners = progressListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        listeners.elementAt(i).processFinished();
      }
    }
  }// protected void fireProcessFinished()

  @SuppressWarnings("unchecked")
  public synchronized void removeStatusListener(StatusListener l) {
    if(statusListeners != null && statusListeners.contains(l)) {
      Vector<StatusListener> v = (Vector<StatusListener>)statusListeners
              .clone();
      v.removeElement(l);
      statusListeners = v;
    }
  }// public synchronized void removeStatusListener(StatusListener l)

  @SuppressWarnings("unchecked")
  public synchronized void addStatusListener(StatusListener l) {
    Vector<StatusListener> v = statusListeners == null
            ? new Vector<StatusListener>(2)
            : (Vector<StatusListener>)statusListeners.clone();
    if(!v.contains(l)) {
      v.addElement(l);
      statusListeners = v;
    }
  }// public synchronized void addStatusListener(StatusListener l)

  protected void fireStatusChanged(String e) {
    if(statusListeners != null) {
      Vector<StatusListener> listeners = statusListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        listeners.elementAt(i).statusChanged(e);
      }
    }
  }

  public void statusChanged(String e) {
    fireStatusChanged(e);
  }

  public void progressChanged(int e) {
    fireProgressChanged(e);
  }

  public void processFinished() {
    fireProcessFinished();
  }

  public Window getWindow() {
    return window;
  }

  public void resourceLoaded(CreoleEvent e) {
  }

  public void resourceUnloaded(CreoleEvent e) {
  }

  public void resourceRenamed(Resource resource, String oldName, String newName) {
  }

  public void datastoreOpened(CreoleEvent e) {
  }

  public void datastoreCreated(CreoleEvent e) {
  }

  public void datastoreClosed(CreoleEvent e) {
    if(getTarget() == e.getDatastore()) cleanup();
  }
}// class DefaultResourceHandle
