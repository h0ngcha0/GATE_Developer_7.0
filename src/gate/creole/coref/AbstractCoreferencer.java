/*
 *  AbstractCoreferencer.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  $Id: AbstractCoreferencer.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.creole.coref;

import java.util.*;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.util.GateRuntimeException;
import gate.util.SimpleFeatureMapImpl;

public abstract class AbstractCoreferencer extends AbstractLanguageAnalyser
    implements ProcessingResource{

  public static final String COREF_DOCUMENT_PARAMETER_NAME = "document";

  public static final String COREF_ANN_SET_PARAMETER_NAME = "annotationSetName";

  public static final String COREF_TYPE_FEATURE_NAME = "ENTITY_MENTION_TYPE";
  public static final String COREF_ANTECEDENT_FEATURE_NAME = "antecedent_offset";

  /** --- */
  private static final boolean DEBUG = false;

  public String coreferenceType;

  /** --- */
  public AbstractCoreferencer(String type) {
    this.coreferenceType = type;
  }


  /** Initialise this resource, and return it. */
  public Resource init() throws ResourceInstantiationException {

    Resource result = super.init();

    return result;
  } // init()


  /**
   * Reinitialises the processing resource. After calling this method the
   * resource should be in the state it is after calling init.
   * If the resource depends on external resources (such as rules files) then
   * the resource will re-read those resources. If the data used to create
   * the resource has changed since the resource has been created then the
   * resource will change too after calling reInit().
  */
  public void reInit() throws ResourceInstantiationException {
    init();
  } // reInit()

  /** Set the document to run on. */
  public void setDocument(Document newDocument) {
    super.setDocument(newDocument);
  }

  /** --- */
  public abstract void setAnnotationSetName(String annotationSetName);

  /** --- */
  public abstract String getAnnotationSetName();

  /** --- */
  protected void generateCorefChains(HashMap ana2ant)
      throws GateRuntimeException{

    String asName = getAnnotationSetName();
    AnnotationSet outputSet = null;

    if (null == asName || asName.equals("")) {
      outputSet = getDocument().getAnnotations();
    }
    else {
      outputSet = getDocument().getAnnotations(asName);
    }

    //3. generate new annotations
    Iterator it = ana2ant.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry currLink = (Map.Entry)it.next();
      Annotation anaphor = (Annotation)currLink.getKey();
      Annotation antecedent = (Annotation)currLink.getValue();

      if (DEBUG) {
        AnnotationSet corefSet = getDocument().getAnnotations("COREF");
        Long antOffset = new Long(0);

        if (null != antecedent) {
          antOffset = antecedent.getStartNode().getOffset();
        }

        FeatureMap features = new SimpleFeatureMapImpl();
        features.put("antecedent",antOffset);
        corefSet.add(anaphor.getStartNode(),anaphor.getEndNode(),"COREF",features);
      }

      //do we have antecedent?
      if (null == antecedent) {
        continue;
      }

      //get the ortho-matches of the antecedent
      List matches = (List)antecedent.getFeatures().
        get(ANNOTATION_COREF_FEATURE_NAME);
      if (null == matches) {
        matches = new ArrayList();
        matches.add(antecedent.getId());
        antecedent.getFeatures().
          put(ANNOTATION_COREF_FEATURE_NAME,matches);
        //check if the document has a list of matches
        //if yes, simply add the new list to it
        //if not, create it and add the list of matches to it
        if (document.getFeatures().containsKey(
            DOCUMENT_COREF_FEATURE_NAME)) {
          Map matchesMap = (Map) document.getFeatures().get(
                                DOCUMENT_COREF_FEATURE_NAME);
          List matchesList = (List) matchesMap.get(getAnnotationSetName());
          if (matchesList == null) {
            matchesList = new ArrayList();
            matchesMap.put(getAnnotationSetName(), matchesList);
          }
          matchesList.add(matches);
        } else {
          Map matchesMap = new HashMap();
            List matchesList = new ArrayList();
            matchesMap.put(getAnnotationSetName(), matchesList);
            matchesList.add(matches);
        }//if else
      }//if matches == null

      FeatureMap features = new SimpleFeatureMapImpl();
      features.put(COREF_TYPE_FEATURE_NAME, coreferenceType);
      features.put(ANNOTATION_COREF_FEATURE_NAME, matches);
      features.put(COREF_ANTECEDENT_FEATURE_NAME,
                   antecedent.getStartNode().getOffset());

      Integer annID = outputSet.add(anaphor.getStartNode(),
                                    anaphor.getEndNode(),
                                    antecedent.getType(),
                                    features);
      matches.add(annID);
    }
  }

}
