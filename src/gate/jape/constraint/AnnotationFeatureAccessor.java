/*
 *  AnnotationFeatureAccessor - transducer class
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Eric Sword, 03/09/08
 *
 *  $Id$
 */
package gate.jape.constraint;

import gate.Annotation;
import gate.AnnotationSet;

/**
 * Accessor that returns a named feature value.
 *
 * @version $Revision$
 * @author esword
 */
public class AnnotationFeatureAccessor implements AnnotationAccessor {

  protected String featureName;

  public AnnotationFeatureAccessor() {
  }

  public AnnotationFeatureAccessor(Object key) {
    setKey(key);
  }

  /**
   * Obtain a named feature
   */
  public Object getValue(Annotation annot, AnnotationSet context) {

    if(featureName == null || featureName.length() == 00)
      throw new IllegalStateException("setKey has not been called with "
              + "the featureName or key was empty");

    if(annot == null || annot.getFeatures() == null) return null;

    return annot.getFeatures().get(featureName);
  }

  @Override
  public int hashCode() {
    return 37 + (featureName != null ? featureName.hashCode() : 0);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) return false;
    if(obj == this) return true;
    if(!(this.getClass().equals(obj.getClass()))) return false;

    AnnotationFeatureAccessor a = (AnnotationFeatureAccessor)obj;

    if(featureName != a.getKey() && featureName != null
            && !a.equals(a.getKey())) return false;

    return true;
  }

  @Override
  public String toString() {
    return featureName;
  }

  public void setKey(Object key) {
    if(key != null) featureName = key.toString();
  }

  public Object getKey() {
    return featureName;
  }
}
