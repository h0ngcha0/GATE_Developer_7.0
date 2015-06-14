/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 25/10/2001
 *
 *  $Id: ResourcePersistence.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */
package gate.util.persistence;

import java.util.*;

import gate.*;
import gate.creole.*;
import gate.persist.PersistenceException;
import gate.util.NameBearer;

/**
 * Holds the data needed to serialise and recreate a {@link Resource}.
 * This data is considered to be: the resource class name, the resource name,
 * the resource features and the resource initialistion parameters.
 */
class ResourcePersistence implements Persistence{

  public void extractDataFromSource(Object source) throws PersistenceException{
    if(! (source instanceof Resource)){
      throw new UnsupportedOperationException(
                getClass().getName() + " can only be used for " +
                Resource.class.getName() +
                " objects!\n" + source.getClass().getName() +
                " is not a " + Resource.class.getName());
    }
    Resource res = (Resource)source;
    resourceType = res.getClass().getName();
    if(res instanceof NameBearer) resourceName = ((NameBearer)res).getName();

    ResourceData rData = (ResourceData)
                         Gate.getCreoleRegister().get(resourceType);
    if(rData == null) throw new PersistenceException(
                                "Could not find CREOLE data for " +
                                resourceType);
    ParameterList params = rData.getParameterList();
    try{
      //get the values for the init time parameters
      initParams = Factory.newFeatureMap();
      //this is a list of lists
      Iterator parDisjIter = ((List)params.getInitimeParameters()).iterator();
      while(parDisjIter.hasNext()){
        Iterator parIter = ((List)parDisjIter.next()).iterator();
        while(parIter.hasNext()){
          Parameter parameter = (Parameter)parIter.next();
          String parName = parameter.getName();
          Object parValue = res.getParameterValue(parName);
          ((Map)initParams).put(parName, parValue);
        }
      }
      initParams = PersistenceManager.getPersistentRepresentation(initParams);

      //get the features
      if(res.getFeatures() != null){
        features = Factory.newFeatureMap();
        ((Map)features).putAll(res.getFeatures());
        features = PersistenceManager.getPersistentRepresentation(features);
      }
    }catch(ResourceInstantiationException rie){
      throw new PersistenceException(rie);
    }
  }


  public Object createObject()throws PersistenceException,
                                     ResourceInstantiationException {
    if(initParams != null)
      initParams = PersistenceManager.getTransientRepresentation(initParams);
    if(features != null)
      features = PersistenceManager.getTransientRepresentation(features);
    Resource res = Factory.createResource(resourceType, (FeatureMap)initParams,
                                          (FeatureMap)features,resourceName);
    return res;
  }

  protected String resourceType;
  protected String resourceName;
  protected Object initParams;
  protected Object features;
  static final long serialVersionUID = -3196664486112887875L;
}