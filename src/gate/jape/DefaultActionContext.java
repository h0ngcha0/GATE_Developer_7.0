/*
 *  DefaultActionContext.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  $Id: $
 *
 */

package gate.jape;

import gate.Controller;
import gate.Corpus;
import gate.FeatureMap;
import gate.ProcessingResource;

/**
 * Default implementation for an action context.<br>
 * Note: A JAPE RHS should only ever use the methods defined in
 * the ActionContext interface, the additional methods implemented here
 * are for use by the Transducer only.
 * 
 * @author Johann Petrak
 */
public class DefaultActionContext implements ActionContext {
  protected Corpus corpus;
  protected FeatureMap prfeatures;
  protected String prname;
  protected Controller controller;
  protected boolean endPhaseSupported;
  protected boolean phaseEnded = false;
  protected ProcessingResource pr;

  public DefaultActionContext() {}

  public void setCorpus(Corpus corpus) {
    this.corpus = corpus;
  }
  public void setPRFeatures(FeatureMap features) {
    this.prfeatures = features;
  }

  public void setPRName(String name) {
    this.prname = name;
  }
  
  public void setPR(ProcessingResource pr) {
    this.pr = pr;
  }
  
  public Corpus getCorpus() {
    return corpus;
  }

  public FeatureMap getPRFeatures() {
    return prfeatures;
  }
  
  public String getPRName() {
    return prname;
  }

  public void setController(Controller c) {
    controller = c;
  }

  public Controller getController() {
    return controller;
  }

  public boolean endPhase() {
    phaseEnded = true;
    // all transducers using this ActionContext implementation support
    // ending a phase. If another implementation does not support it,
    // use a different ActionContext implementation (e.g. a subclass of this)
    // or change the way singalling this is implemented.
    return true;
  }


  public boolean isPhaseEnded() {
    return phaseEnded;
  }

  public void setPhaseEnded(boolean isended) {
    phaseEnded = isended;
  }
  
  public boolean isPREnabled() {
    return gate.Utils.isEnabled(controller, pr);
  }

}
