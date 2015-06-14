/*
 *  RhsAction.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Hamish, 30/7/98
 *
 *  $Id: RhsAction.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.jape;
import java.io.Serializable;
import java.util.Map;

import gate.AnnotationSet;
import gate.Document;
import gate.creole.ontology.Ontology;

/** An interface that defines what the action classes created
  * for RightHandSides look like.
  */
public interface RhsAction extends Serializable {

  /**
   * Fires the RHS action for a particular LHS match.
   * @param doc the document the RHS action will be run on
   * @param bindings A map containing the matching results from the LHS in 
   * the form label(String) -> matched annotations (AnnotationSet)
   * @param annotations copy of the outputAS value provided for backward
   * compatibility
   * @param inputAS the input annotation set
   * @param outputAS the output annotation set
   * @param ontology
   * @throws JapeException
   */
  public void doit(Document doc, Map<String, AnnotationSet> bindings,
                   AnnotationSet annotations,
                   AnnotationSet inputAS, AnnotationSet outputAS,
                   Ontology ontology)
              throws JapeException;

  public void setActionContext(ActionContext actionContext);
  public ActionContext getActionContext();

} // RhsAction
