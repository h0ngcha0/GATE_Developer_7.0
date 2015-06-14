/*
 *  DefaultIndexDefinition.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Rosen Marinov, 19/Apr/2002
 *
 */

package gate.creole.ir;

import java.util.*;

public class DefaultIndexDefinition implements IndexDefinition{

  /** List of IndexField - objects for indexing */
  private List fields;

  /** Location (path) of the index store directory */
  private String location;

  /* Niraj */
  // Annic Specific Changes
  private ArrayList featuresToExclude;
  private String annotationSet;
  private String baseTokenAnnotationType;
  /* End */


//  /**  Type of index see GateConstants.java*/
//  private int indexType;

  /**  Sets the location of index
   * @param location - index directory path
   */
  public void setIndexLocation(String location){
    this.location = location;
  }
  /** @return String  path of index store directory*/
  public String getIndexLocation(){
    return location;
  }


  /* Niraj */
  // Annic specific changes
  public void setFeaturesToExclude(ArrayList featuresToExclude) {
    this.featuresToExclude = featuresToExclude;
  }

  public ArrayList getFeaturesToExclude() {
    return featuresToExclude;
  }

  public void setAnnotationSetName(String annotSetName) {
    this.annotationSet = annotSetName;
  }

  public String getAnnotationSetName() {
    return this.annotationSet;
  }

  public void setBaseTokenAnnotationType(String baseTokenAnnotationType) {
    this.baseTokenAnnotationType = baseTokenAnnotationType;
  }

  public String getBaseTokenAnnotationType() {
    return this.baseTokenAnnotationType;
  }
  /* End */


//  /**  @return int index type*/
//  public int getIndexType(){
//    return indexType;
//  }
//
//  /**  Sets the index type.
//   *  @param type - index type
//   */
//  public void setIndexType(int type){
//    this.indexType = type;
//  }

  /**  @return Iterator of IndexFields, fileds for indexing. */
  public Iterator getIndexFields(){
    return fields.iterator();
  }

  /**  Add new IndexField object to fields list.*/
  public void addIndexField(IndexField fld){
    if (fields==null){
      fields = new Vector();
    }
    fields.add(fld);
  }

  /**
   * Sets the fully qualified class name for the IR engine to be used.
   * @param irEngineClassName a String.
   */
  public void setIrEngineClassName(String irEngineClassName) {
    this.irEngineClassName = irEngineClassName;
  }

  /**
   * Gets the fully qualified class name for the IR engine to be used.
   * @return a String.
   */
  public String getIrEngineClassName() {
    return irEngineClassName;
  }

  /**Serialisation ID*/
  static final long serialVersionUID = 2925395897153647322L;
  private String irEngineClassName;
}
