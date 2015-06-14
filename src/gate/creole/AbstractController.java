/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 27 Sep 2001
 *
 *  $I$
 */
package gate.creole;

import gate.Controller;
import gate.Gate;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.metadata.CreoleResource;
import gate.event.ControllerEvent;
import gate.event.ControllerListener;
import gate.event.ProgressListener;
import gate.event.StatusListener;
import gate.util.Benchmark;
import gate.util.Benchmarkable;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

@CreoleResource(icon = "application")
public abstract class AbstractController extends AbstractResource 
       implements Controller, ProcessingResource, Benchmarkable {

  /**
   * Benchmark ID of this resource.
   */
  protected String benchmarkID;

  /**
   * Shared featureMap
   */
  protected Map benchmarkFeatures = new HashMap();

  // executable code
  /**
   * Execute this controller. This implementation takes care of informing any
   * {@link ControllerAwarePR}s of the start and end of execution, and
   * delegates to the {@link #executeImpl()} method to do the real work.
   * Subclasses should override {@link #executeImpl()} rather than this method.
   */
  public void execute() throws ExecutionException {

    // inform ControllerAware PRs that execution has started
    for(ControllerAwarePR pr : getControllerAwarePRs()) {
      pr.controllerExecutionStarted(this);
    }
    Throwable thrown = null;
    try {
      if(Benchmark.isBenchmarkingEnabled()) {
        // write a start marker to the benchmark log for this
        // controller as a whole
        Benchmark.startPoint(getBenchmarkId());
      }
      // do the real work
      this.executeImpl();
    }
    catch(Throwable t) {
      thrown = t;
    }
    finally {
      if(thrown == null) {
        // successfully completed
        for(ControllerAwarePR pr : getControllerAwarePRs()) {
          pr.controllerExecutionFinished(this);
        }
      }
      else {
        // aborted
        for(ControllerAwarePR pr : getControllerAwarePRs()) {
          pr.controllerExecutionAborted(this, thrown);
        }

        // rethrow the aborting exception or error
        if(thrown instanceof Error) {
          throw (Error)thrown;
        }
        else if(thrown instanceof RuntimeException) {
          throw (RuntimeException)thrown;
        }
        else if(thrown instanceof ExecutionException) {
          throw (ExecutionException)thrown;
        }
        else {
          // we have a checked exception that isn't one executeImpl can
          // throw. This shouldn't be possible, but just in case...
          throw new UndeclaredThrowableException(thrown);
        }
      }
    }

  }

  /**
   * Get the set of PRs from this controller that implement
   * {@link ControllerAwarePR}. If there are no such PRs in this controller, an
   * empty set is returned. This implementation simply filters the collection
   * returned by {@link Controller#getPRs()}, override this method if your
   * subclass admits a more efficient implementation.
   */
  protected Set<ControllerAwarePR> getControllerAwarePRs() {
    Set<ControllerAwarePR> returnSet = null;
    for(Object pr : getPRs()) {
      if(pr instanceof ControllerAwarePR) {
        if(returnSet == null) {
          returnSet = new HashSet<ControllerAwarePR>();
        }
        returnSet.add((ControllerAwarePR)pr);
      }
    }

    if(returnSet == null) {
      // optimization - don't waste time creating a new set in the most
      // common case where there are no Controller aware PRs
      return Collections.emptySet();
    }
    else {
      return returnSet;
    }
  }

  /**
   * Executes the PRs in this controller, according to the execution strategy of
   * the particular controller type (simple pipeline, parallel execution,
   * once-per-document in a corpus, etc.). Subclasses should override this
   * method, allowing the default {@link #execute()} method to handle sending
   * notifications to controller aware PRs.
   */
  protected void executeImpl() throws ExecutionException {
    throw new ExecutionException("Controller " + getClass()
      + " hasn't overriden the executeImpl() method");
  }

  /** Initialise this resource, and return it. */
  public Resource init() throws ResourceInstantiationException {
    return this;
  }

  /* (non-Javadoc)
   * @see gate.ProcessingResource#reInit()
   */
  public void reInit() throws ResourceInstantiationException {
    init();
  }

  /** Clears the internal data of the resource, when it gets released * */
  public void cleanup() {
  }

  /**
   * Populates this controller from a collection of {@link ProcessingResource}s
   * (optional operation).
   * 
   * Controllers that are serializable must implement this method needed by GATE
   * to restore their contents.
   * 
   * @throws UnsupportedOperationException
   *           if the <tt>setPRs</tt> method is not supported by this
   *           controller.
   */
  public void setPRs(Collection PRs) {
  }

  /**
   * Notifies all the PRs in this controller that they should stop their
   * execution as soon as possible.
   */
  public synchronized void interrupt() {
    interrupted = true;
    Iterator prIter = getPRs().iterator();
    while(prIter.hasNext()) {
      ((ProcessingResource)prIter.next()).interrupt();
    }
  }

  public synchronized boolean isInterrupted() {
    return interrupted;
  }

  // events code
  /**
   * Removes a {@link gate.event.StatusListener} from the list of listeners for
   * this processing resource
   */
  public synchronized void removeStatusListener(StatusListener l) {
    if(statusListeners != null && statusListeners.contains(l)) {
      Vector v = (Vector)statusListeners.clone();
      v.removeElement(l);
      statusListeners = v;
    }
  }

  /**
   * Adds a {@link gate.event.StatusListener} to the list of listeners for this
   * processing resource
   */
  public synchronized void addStatusListener(StatusListener l) {
    Vector v =
      statusListeners == null ? new Vector(2) : (Vector)statusListeners.clone();
    if(!v.contains(l)) {
      v.addElement(l);
      statusListeners = v;
    }
  }

  /**
   * Notifies all the {@link gate.event.StatusListener}s of a change of status.
   * 
   * @param e
   *          the message describing the status change
   */
  protected void fireStatusChanged(String e) {
    if(statusListeners != null) {
      Vector listeners = statusListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        ((StatusListener)listeners.elementAt(i)).statusChanged(e);
      }
    }
  }

  /**
   * Adds a {@link gate.event.ProgressListener} to the list of listeners for
   * this processing resource.
   */
  public synchronized void addProgressListener(ProgressListener l) {
    Vector v =
      progressListeners == null ? new Vector(2) : (Vector)progressListeners
        .clone();
    if(!v.contains(l)) {
      v.addElement(l);
      progressListeners = v;
    }
  }

  /**
   * Removes a {@link gate.event.ProgressListener} from the list of listeners
   * for this processing resource.
   */
  public synchronized void removeProgressListener(ProgressListener l) {
    if(progressListeners != null && progressListeners.contains(l)) {
      Vector v = (Vector)progressListeners.clone();
      v.removeElement(l);
      progressListeners = v;
    }
  }

  /**
   * Notifies all the {@link gate.event.ProgressListener}s of a progress change
   * event.
   * 
   * @param e
   *          the new value of execution completion
   */
  protected void fireProgressChanged(int e) {
    if(progressListeners != null) {
      Vector listeners = progressListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        ((ProgressListener)listeners.elementAt(i)).progressChanged(e);
      }
    }
  }

  /**
   * Notifies all the {@link gate.event.ProgressListener}s of a progress
   * finished.
   */
  protected void fireProcessFinished() {
    if(progressListeners != null) {
      Vector listeners = progressListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        ((ProgressListener)listeners.elementAt(i)).processFinished();
      }
    }
  }

  /**
   * A progress listener used to convert a 0..100 interval into a smaller one
   */
  protected class IntervalProgressListener implements ProgressListener {
    public IntervalProgressListener(int start, int end) {
      this.start = start;
      this.end = end;
    }

    public void progressChanged(int i) {
      fireProgressChanged(start + (end - start) * i / 100);
    }

    public void processFinished() {
      fireProgressChanged(end);
    }

    int start;
    int end;
  }// CustomProgressListener

  /**
   * A simple status listener used to forward the events upstream.
   */
  protected class InternalStatusListener implements StatusListener {
    public void statusChanged(String message) {
      fireStatusChanged(message);
    }
  }

  /**
   * Checks whether all the contained PRs have all the required runtime
   * parameters set.
   * 
   * @return a {@link List} of {@link ProcessingResource}s that have required
   *         parameters with null values if they exist <tt>null</tt>
   *         otherwise.
   * @throws {@link ResourceInstantiationException}
   *           if problems occur while inspecting the parameters for one of the
   *           resources. These will normally be introspection problems and are
   *           usually caused by the lack of a parameter or of the read accessor
   *           for a parameter.
   */
  public List getOffendingPocessingResources()
    throws ResourceInstantiationException {
    // take all the contained PRs
    ArrayList badPRs = new ArrayList(getPRs());
    // remove the ones that no parameters problems
    Iterator prIter = getPRs().iterator();
    while(prIter.hasNext()) {
      ProcessingResource pr = (ProcessingResource)prIter.next();
      ResourceData rData =
        (ResourceData)Gate.getCreoleRegister().get(pr.getClass().getName());
      if(AbstractResource.checkParameterValues(pr, rData.getParameterList()
        .getRuntimeParameters())) {
        badPRs.remove(pr);
      }
    }
    return badPRs.isEmpty() ? null : badPRs;
  }

  public synchronized void removeControllerListener(ControllerListener l) {
    if(controllerListeners != null && controllerListeners.contains(l)) {
      Vector v = (Vector)controllerListeners.clone();
      v.removeElement(l);
      controllerListeners = v;
    }
  }

  public synchronized void addControllerListener(ControllerListener l) {
    Vector v =
      controllerListeners == null ? new Vector(2) : (Vector)controllerListeners
        .clone();
    if(!v.contains(l)) {
      v.addElement(l);
      controllerListeners = v;
    }
  }

  /**
   * The list of {@link gate.event.StatusListener}s registered with this
   * resource
   */
  private transient Vector statusListeners;

  /**
   * The list of {@link gate.event.ProgressListener}s registered with this
   * resource
   */
  private transient Vector progressListeners;

  /**
   * The list of {@link gate.event.ControllerListener}s registered with this
   * resource
   */
  private transient Vector controllerListeners;

  protected boolean interrupted = false;

  protected void fireResourceAdded(ControllerEvent e) {
    if(controllerListeners != null) {
      Vector listeners = controllerListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        ((ControllerListener)listeners.elementAt(i)).resourceAdded(e);
      }
    }
  }

  protected void fireResourceRemoved(ControllerEvent e) {
    if(controllerListeners != null) {
      Vector listeners = controllerListeners;
      int count = listeners.size();
      for(int i = 0; i < count; i++) {
        ((ControllerListener)listeners.elementAt(i)).resourceRemoved(e);
      }
    }
  }
  
  /**
   * Sets the benchmark ID of this controller.
   */
  public void setBenchmarkId(String benchmarkID) {
    this.benchmarkID = benchmarkID;
  }

  /**
   * Returns the benchmark ID of this controller.
   */
  public String getBenchmarkId() {
    if(benchmarkID == null) {
      benchmarkID = getName().replaceAll("[ ]+", "_");
    }
    return benchmarkID;
  }
}
