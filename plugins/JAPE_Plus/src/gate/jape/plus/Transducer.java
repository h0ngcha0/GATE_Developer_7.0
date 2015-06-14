/*
 *  Tansducer.java
 *  
 *  Copyright (c) 2009 - 2011, Valentin Tablan.
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 17 Aug 2009
 *
 *  $Id$
 */
package gate.jape.plus;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.event.AnnotationSetEvent;
import gate.event.AnnotationSetListener;
import gate.event.ProgressListener;
import gate.event.StatusListener;
import gate.fsm.FSM;
import gate.gui.ActionsPublisher;
import gate.gui.MainFrame;
import gate.jape.MultiPhaseTransducer;
import gate.jape.SinglePhaseTransducer;
import gate.jape.constraint.AnnotationAccessor;
import gate.jape.constraint.ConstraintPredicate;
import gate.jape.parser.ParseCpsl;
import gate.jape.parser.ParseException;
import gate.util.Err;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;
import gate.jape.DefaultActionContext;
import gate.creole.ControllerAwarePR;
import gate.creole.ontology.Ontology;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.ontotext.jape.pda.FSMPDA;


/**
 * A JAPE-Plus transducer (with a {@link LanguageAnalyser} interface.
 */
@CreoleResource(name = "JAPE-Plus Transducer", 
    comment = "An optimised, JAPE-compatible transducer.")
public class Transducer extends AbstractLanguageAnalyser 
    implements ControllerAwarePR, ProgressListener /*, ActionsPublisher */ {

  private static final long serialVersionUID = 4194243737624821476L;

  /**
   * A comparator for annotations based on start offset and inverse length.
   */
  protected class AnnotationComparator implements Comparator<Annotation>{

    public int compare(Annotation a0, Annotation a1) {
      long start0 = a0.getStartNode().getOffset();
      long start1 = a1.getStartNode().getOffset();
      if(start0 < start1) {
        return -1;
      } else if(start0 > start1) {
        return 1;
      } else {
        long end0 = a0.getEndNode().getOffset();
        long end1 = a1.getEndNode().getOffset();
        if(end0 > end1) {
          return -1;
        } else if(end0 < end1) { return 1; }
        return 0;
      }
    }
    
  }
  
  
  /**
   * A listener for the input annotation set, which invalidates the pre-built
   * lists of sorted annotation when they change due to the execution of one of
   * the phases.
   */
  protected class AnnSetListener implements AnnotationSetListener{

    /* (non-Javadoc)
     * @see gate.event.AnnotationSetListener#annotationAdded(gate.event.AnnotationSetEvent)
     */
    @Override
    public void annotationAdded(AnnotationSetEvent e) {
      changedTypes.add(e.getAnnotation().getType());
    }

    /* (non-Javadoc)
     * @see gate.event.AnnotationSetListener#annotationRemoved(gate.event.AnnotationSetEvent)
     */
    @Override
    public void annotationRemoved(AnnotationSetEvent e) {
      changedTypes.add(e.getAnnotation().getType());
    }
  }
  
  
  protected class SerialiseTransducerAction extends AbstractAction {
    public SerialiseTransducerAction() {
      super("Serialize Transducer");
      putValue(SHORT_DESCRIPTION, 
          "Save this JAPE Plus Transducer as a binary file");
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
      Runnable runnable = new Runnable() {
        public void run() {
          JFileChooser fileChooser = MainFrame.getFileChooser();
          fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
          fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
          fileChooser.setMultiSelectionEnabled(false);
          if(fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
              MainFrame.lockGUI("Serialising JAPE Plus Transducer...");
              FileOutputStream out = new FileOutputStream(file);
              ObjectOutputStream s = new ObjectOutputStream(out);
              //TODO
              // collect all class objects
              @SuppressWarnings("unchecked")
              Class<? extends SPTBase>[] sptClasses = 
                  new Class[singlePhaseTransducers.length];
              for(int i = 0; i < singlePhaseTransducers.length; i++) {
//                singlePhaseTransducers[i].getClass();
              }
//              s.writeObject();
              s.flush();
              s.close();
              out.close();
            } catch(IOException ioe) {
              JOptionPane.showMessageDialog(MainFrame.getInstance(), "Error!\n" + ioe.toString(),
                      "GATE", JOptionPane.ERROR_MESSAGE);
              ioe.printStackTrace(Err.getPrintWriter());
            } finally {
              MainFrame.unlockGUI();
            }
          }
        }
      };
      Thread thread = new Thread(runnable, "Transducer Serialisation");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }
  
  protected static class SinglePhaseTransducerPDA extends SinglePhaseTransducer {

    public SinglePhaseTransducerPDA(String name) {
      super(name);
    }

    @Override
    protected FSMPDA createFSM() {
      return new FSMPDA(this);
    }
    
    
  }
  
  public URL getGrammarURL() {
    return grammarURL;
  }

  @CreoleParameter(
      comment="URL for the data from which this transducer should be built.", suffixes="jape")
  public void setGrammarURL(URL source) {
    this.grammarURL = source;
  }
  
  /**
   * The source from which this transducer is created.
   */
  protected URL grammarURL;
  
  /**
   * List of class names for any custom
   * {@link gate.jape.constraint.ConstraintPredicate}.
   */
  protected List<String> operators = null;
  
  /**
   * List of class names for any custom
   * {@link gate.jape.constraint.AnnotationAccessor}s.
   */
  protected List<String> annotationAccessors = null;
  
  
  
  protected String encoding;
  
  protected String inputASName;
  
  protected String outputASName;

  protected DefaultActionContext actionContext;
  
  protected List<Action> actions;
  
  /**
   * Instance of {@link AnnotationComparator} used for sorting annots for the
   * phases.
   */
  protected AnnotationComparator annotationComparator;
  
  /**
   * The sets of annotations (of a given type) that have already been sorted.
   */
  protected Map<String, Annotation[]> sortedAnnotations;
  
  /**
   * A set of annotation types that were modified during the latest execution of
   * a pahse.
   */
  protected Set<String> changedTypes;
  
  /**
   * The listener that keeps track of the annotation types that have changed.
   */
  protected AnnotationSetListener inputASListener;
  
  /**
   * The list of phases used in this transducer.
   */
  protected transient SPTBase[] singlePhaseTransducers;
  
  /**
   * The index in {@link #singlePhaseTransducers} for the SPT currently being
   * executed, if any, -1 otherwise.
   */
  protected int currentSptIndex = -1;
  
  /**
   * Gets the list of class names for any custom boolean operators.
   * Classes must implement {@link gate.jape.constraint.ConstraintPredicate}.
   */
  public List<String> getOperators() {
    return operators;
  }

  /**
   * Sets the list of class names for any custom boolean operators.
   * Classes must implement {@link gate.jape.constraint.ConstraintPredicate}.
   */
  @Optional
  @CreoleParameter(
    comment = "Class names that implement gate.jape.constraint.ConstraintPredicate."
  )
  public void setOperators(List<String> operators) {
    this.operators = operators;
  }
  
  /**
   * Gets the list of class names for any custom
   * {@link gate.jape.constraint.AnnotationAccessor}s.
   */
  public List<String> getAnnotationAccessors() {
    return annotationAccessors;
  }

  /**
   * Sets the list of class names for any custom
   * {@link gate.jape.constraint.AnnotationAccessor}s.
   */
  @Optional
  @CreoleParameter(
    comment = "Class names that implement gate.jape.constraint.AnnotationAccessor."
  )
  public void setAnnotationAccessors(List<String> annotationAccessors) {
    this.annotationAccessors = annotationAccessors;
  }  
  
  public String getEncoding() {
    return encoding;
  }

  @CreoleParameter(defaultValue="UTF-8", 
          comment="The encoding used for the input .jape files.")
  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  
  /**
   * 
   */
  public Transducer() {
    sortedAnnotations = new HashMap<String, Annotation[]>();
    changedTypes = new HashSet<String>();
    inputASListener = new AnnSetListener();
    annotationComparator = new AnnotationComparator();
    
    actions = new ArrayList<Action>();
    actions.add(new SerialiseTransducerAction());
  }

 
  
  @Override
  public Resource init() throws ResourceInstantiationException {
    super.init();
    initCustomConstraints();
    
    try {
      parseJape();
    } catch(IOException e) {
      throw new ResourceInstantiationException(e);
    } catch(ParseException e) {
      throw new ResourceInstantiationException(e);
    }
    actionContext = new DefaultActionContext();
    return this;
  }

  /**
   * Loads any custom operators and annotation accessors into the ConstraintFactory.
   * @throws ResourceInstantiationException
   */
  protected void initCustomConstraints() throws ResourceInstantiationException {
    //Load operators
    if (operators != null) {
      for(String opName : operators) {
        Class<? extends ConstraintPredicate> clazz = null;
        try {
          clazz = Class.forName(opName, true, Gate.getClassLoader())
                        .asSubclass(ConstraintPredicate.class);
        }
        catch(ClassNotFoundException e) {
          //if couldn't find it that way, try with current thread class loader
          try {
            clazz = Class.forName(opName, true,
                Thread.currentThread().getContextClassLoader())
                  .asSubclass(ConstraintPredicate.class);
          }
          catch(ClassNotFoundException e1) {
            throw new ResourceInstantiationException("Cannot load class for operator: " + opName, e1);
          }
        }
        catch(ClassCastException cce) {
          throw new ResourceInstantiationException("Operator class '" + opName + "' must implement ConstraintPredicate");
        }

        //instantiate an instance of the class so can get the operator string
        try {
          ConstraintPredicate predicate = clazz.newInstance();
          String opSymbol = predicate.getOperator();
          //now store it in ConstraintFactory
          Factory.getConstraintFactory().addOperator(opSymbol, clazz);
        }
        catch(Exception e) {
          throw new ResourceInstantiationException("Cannot instantiate class for operator: " + opName, e);
        }
      }
    }

    //Load annotationAccessors
    if (annotationAccessors != null) {
      for(String accessorName : annotationAccessors) {
        Class<? extends AnnotationAccessor> clazz = null;
        try {
          clazz = Class.forName(accessorName, true, Gate.getClassLoader())
                     .asSubclass(AnnotationAccessor.class);
        }
        catch(ClassNotFoundException e) {
          //if couldn't find it that way, try with current thread class loader
          try {
            clazz = Class.forName(accessorName, true,
                Thread.currentThread().getContextClassLoader())
                   .asSubclass(AnnotationAccessor.class);
          }
          catch(ClassNotFoundException e1) {
            throw new ResourceInstantiationException("Cannot load class for accessor: " + accessorName, e1);
          }
        }
        catch(ClassCastException cce) {
          throw new ResourceInstantiationException("Operator class '" + accessorName + "' must implement AnnotationAccessor");
        }

        //instantiate an instance of the class so can get the meta-property name string
        try {
          AnnotationAccessor aa = clazz.newInstance();
          String accSymbol = (String)aa.getKey();
          //now store it in ConstraintFactory
          Factory.getConstraintFactory().addMetaProperty(accSymbol, clazz);
        }
        catch(Exception e) {
          throw new ResourceInstantiationException("Cannot instantiate class for accessor: " + accessorName, e);
        }

      }
    }
  }

  protected void parseJape() throws IOException, ParseException, ResourceInstantiationException{
		ParseCpsl parser = Factory.newJapeParser(grammarURL, encoding);
		parser.setSptClass(SinglePhaseTransducerPDA.class);

    StatusListener listener = new StatusListener(){
      public void statusChanged(String text){
        fireStatusChanged(text);
      }
    };
    parser.addStatusListener(listener);
    MultiPhaseTransducer intermediate =  parser.MultiPhaseTransducer();
    parser.removeStatusListener(listener);
    
    singlePhaseTransducers = new SPTBase[intermediate.getPhases().size()];
    SPTBuilder builder = new SPTBuilder();
    for(int i = 0; i < intermediate.getPhases().size(); i++){
      singlePhaseTransducers[i] = builder.buildSPT(
          (SinglePhaseTransducer)intermediate.getPhases().get(i));
      singlePhaseTransducers[i].addProgressListener(this);
    }
  }
  
  @Override
  public void cleanup() {
    super.cleanup();
    for(SPTBase aSpt : singlePhaseTransducers){
      aSpt.removeProgressListener(this);
      aSpt.cleanup();
    }
  }
  
  @Override
  public void execute() throws ExecutionException {
  	if (singlePhaseTransducers == null) {
  		throw new IllegalStateException("init() was not called.");
  	}
  	interrupted = false;
    AnnotationSet inputAs = (inputASName == null || inputASName.length() == 0) ?
            document.getAnnotations() : document.getAnnotations(inputASName);
    fireProgressChanged(0);
    try {
      inputAs.addAnnotationSetListener(inputASListener);
      sortedAnnotations.clear();
      for(currentSptIndex = 0; currentSptIndex < singlePhaseTransducers.length; 
          currentSptIndex++){
        SPTBase aSpt = singlePhaseTransducers[currentSptIndex];
        changedTypes.clear();
        aSpt.setCorpus(corpus);
        aSpt.setDocument(document);
        aSpt.setInputASName(inputASName);
        aSpt.setOutputASName(outputASName);
        aSpt.setOwner(this);
        actionContext.setCorpus(corpus);
        actionContext.setPR(this);
        actionContext.setPRFeatures(features);
        aSpt.setActionContext(actionContext);
        aSpt.setOntology(ontology);
        aSpt.execute();
        aSpt.setCorpus(null);
        aSpt.setDocument(null);
        aSpt.setInputASName(null);
        aSpt.setOutputASName(null);
        aSpt.setOwner(null);
        for(String type : changedTypes) sortedAnnotations.remove(type);
        changedTypes.clear();
      }
    } finally {
      inputAs.removeAnnotationSetListener(inputASListener);
      currentSptIndex = -1;
      fireProcessFinished();
    }
  }

  
  @Override
  public void progressChanged(int i) {
    // event coming from one of our SPTs
    if(currentSptIndex >= 0) {
      fireProgressChanged((currentSptIndex * 100 + i) / singlePhaseTransducers.length);
    }
  }

  @Override
  public void processFinished() {
    // ignore
  }

  /**
   * Get the set of annotations, of a given type, sorted by start offset and
   * inverse length, obtained from the input annotation set of the current 
   * document.
   * 
   * @param type the type of annotations requested. 
   * @return an array of {@link Annotation} values.
   */
  public Annotation[] getSortedAnnotations(String type){
    Annotation[] annots = sortedAnnotations.get(type);
    if(annots == null){
      //not calculated yet
      AnnotationSet inputAS = 
        (inputASName == null || inputASName.trim().length() == 0) ?
        document.getAnnotations() : document.getAnnotations(inputASName);
      ArrayList<Annotation> annOfType = new ArrayList<Annotation>(
              inputAS.get(type));
      Collections.sort(annOfType, annotationComparator);
      annots = annOfType.toArray(new Annotation[annOfType.size()]);
      sortedAnnotations.put(type, annots);
    }
    return annots;
  }
  
  /**
   * @return the inputASName
   */
  public String getInputASName() {
    return inputASName;
  }

  /**
   * @param inputASName the inputASName to set
   */
  @CreoleParameter(comment="The name of the input annotation set.")
  @Optional
  @RunTime
  public void setInputASName(String inputASName) {
    this.inputASName = inputASName;
  }

  /**
   * @return the outputASName
   */
  public String getOutputASName() {
    return outputASName;
  }

  /**
   * @param outputASName the outputASName to set
   */
  @CreoleParameter(comment="The name of the output annotation set.")
  @Optional
  @RunTime
  public void setOutputASName(String outputASName) {
    this.outputASName = outputASName;
  }

  @CreoleParameter(comment="The ontology LR to be used by this transducer")
  @Optional
  @RunTime
  public void setOntology(Ontology onto) {
    ontology = onto;
  }

  public Ontology getOntology() {
    return ontology;
  }

  protected Ontology ontology = null;

  // methods implementing ControllerAwarePR
  @Override
  public void controllerExecutionStarted(Controller c)
    throws ExecutionException {
    actionContext.setController(c);
    actionContext.setCorpus(corpus);
    actionContext.setPRFeatures(features);
    actionContext.setPRName(this.getName());
    actionContext.setPR(this);
    for(SPTBase aSpt : singlePhaseTransducers){
      aSpt.runControllerExecutionStartedBlock(actionContext,c,ontology);
    }
  }
  
  @Override
  public void controllerExecutionFinished(Controller c)
    throws ExecutionException {
    for(SPTBase aSpt : singlePhaseTransducers){
      // ontologies not supported yet, pass null
      Ontology o = null;
      aSpt.runControllerExecutionFinishedBlock(actionContext,c,ontology);
    }
    actionContext.setCorpus(null);
    actionContext.setController(null);
    actionContext.setPR(null);
  }

  @Override
  public void controllerExecutionAborted(Controller c, Throwable t)
    throws ExecutionException {
    for(SPTBase aSpt : singlePhaseTransducers){
      // ontologies not supported yet, pass null
      Ontology o = null;
      aSpt.runControllerExecutionAbortedBlock(actionContext,c,t,ontology);
    }
    actionContext.setCorpus(null);
    actionContext.setController(null);
    actionContext.setPR(null);
  }

  
  /**
   * This is testing code used during development.
   * TODO: delete it!
   */
  public static void main(String[] args){
    try {
      Gate.init();
      MainFrame.getInstance().setVisible(true);
      Gate.getCreoleRegister().registerDirectories(new File(".").toURI().toURL());
      File session = Gate.getUserSessionFile();
      if(session == null) session = new File(System.getProperty("user.home") + 
              ".gate.session");
      if(session.exists()) PersistenceManager.loadObjectFromFile(session);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
