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
 *  $Id: ControllerPersistence.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */
package gate.util.persistence;

import java.util.*;

import gate.Controller;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;

public class ControllerPersistence extends ResourcePersistence {
  /**
   * Populates this Persistence with the data that needs to be stored from the
   * original source object.
   */
  public void extractDataFromSource(Object source)throws PersistenceException{
    if(! (source instanceof Controller)){
      throw new UnsupportedOperationException(
                getClass().getName() + " can only be used for " +
                Controller.class.getName() +
                " objects!\n" + source.getClass().getName() +
                " is not a " + Controller.class.getName());
    }
    Controller controller = (Controller)source;

    super.extractDataFromSource(source);
    prList = new ArrayList(controller.getPRs().size());
    Iterator prIter = controller.getPRs().iterator();

    while(prIter.hasNext()){
      ((List)prList).add(prIter.next());
    }
    prList = PersistenceManager.getPersistentRepresentation(prList);
  }

  /**
   * Creates a new object from the data contained. This new object is supposed
   * to be a copy for the original object used as source for data extraction.
   */
  public Object createObject()throws PersistenceException,
                                     ResourceInstantiationException{

    Controller controller = (Controller)super.createObject();

    if(controller.getPRs().isEmpty()){
      prList = PersistenceManager.getTransientRepresentation(prList);
      controller.setPRs((Collection)prList);
    }

    return controller;
  }

  protected Object prList;
  static final long serialVersionUID = 727852357092819439L;
}