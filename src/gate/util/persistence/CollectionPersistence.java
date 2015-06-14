/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 26/10/2001
 *
 *  $Id: CollectionPersistence.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */
package gate.util.persistence;

import java.util.*;

import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.Err;


public class CollectionPersistence implements Persistence {

  /**
   * Populates this Persistence with the data that needs to be stored from the
   * original source object.
   */
  public void extractDataFromSource(Object source)throws PersistenceException{
    if(! (source instanceof Collection)){
      throw new UnsupportedOperationException(
                getClass().getName() + " can only be used for " +
                Collection.class.getName() +
                " objects!\n" + source.getClass().getName() +
                " is not a " + Collection.class.getName());
    }
    collectionType = source.getClass();

    Collection coll = (Collection)source;

    //get the values in the iterator's order
    localList = new ArrayList(coll.size());
    Iterator elemIter = coll.iterator();
    while(elemIter.hasNext()){
      localList.add(PersistenceManager.
                    getPersistentRepresentation(elemIter.next()));
    }
  }

  /**
   * Creates a new object from the data contained. This new object is supposed
   * to be a copy for the original object used as source for data extraction.
   */
  public Object createObject()throws PersistenceException,
                                     ResourceInstantiationException{
    List<String> exceptionsOccurred = new ArrayList<String>();
    //let's try to create a collection of the same type as the original
    Collection result = null;
    try{
      result = (Collection)collectionType.newInstance();
    }catch(Exception e){
      // ignore - if we can't create a collection of the original type
      // for any reason, just create an ArrayList as a fallback.  The
      // main use for this class is to persist parameter values for
      // GATE resources, and GATE can convert an ArrayList to any type
      // required by a resource parameter.
    }
    if(result == null) result = new ArrayList(localList.size());

    //now we have the collection let's populate it
    for(Object local : localList) {
      try {
        result.add(PersistenceManager.getTransientRepresentation(local));
      }
      catch(PersistenceException pe) {
        exceptionsOccurred.add(pe.getMessage());
        pe.printStackTrace(Err.getPrintWriter());
      }
      catch(ResourceInstantiationException rie) {
        exceptionsOccurred.add(rie.getMessage());
        rie.printStackTrace(Err.getPrintWriter());
      }
    }

    if(exceptionsOccurred.size() > 0) {
      throw new PersistenceException("Some resources cannot be restored:\n" +
        Arrays.toString(exceptionsOccurred
        .toArray(new String[exceptionsOccurred.size()]))
        .replaceAll("[\\]\\[]", "").replaceAll(", ", "\n"));
    }

    return result;
  }


  protected List localList;
  protected Class collectionType;
  static final long serialVersionUID = 7908364068699089834L;
}