package gate.creole;

/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 13 May 2002
 *
 *  $Id: OntologyAwareTransducer.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

import gate.Resource;

/**
 * An ontology aware JAPE transducer. Adds the {@link #ontology} member to the
 * {@link Transducer} class.
 * @deprecated Ontology related functionality is now implemented in the 
 * standard JAPE Transducer {@link gate.creole.Transducer}.
 */
public class OntologyAwareTransducer extends Transducer {

  /**
   * The ontology that will be available on the RHS of JAPE rules.
   */
  private gate.creole.ontology.Ontology ontology;

  /**
   * Gets the ontology used by this transducer.
   * @return an {@link gate.creole.ontology.Ontology} value.
   */
  public gate.creole.ontology.Ontology getOntology() {
    return ontology;
  }

  /**
   * Sets the ontology used by this transducer.
   * @param ontology an {@link gate.creole.ontology.Ontology} value.
   */
  public void setOntology(gate.creole.ontology.Ontology ontology) {
    this.ontology = ontology;
  }

  public Resource init() throws ResourceInstantiationException {
    Resource res = super.init();
    batch.setOntology(ontology);
    return res;
  }
}