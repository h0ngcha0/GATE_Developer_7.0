/*
 *  SystemData.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Hamish Cunningham, 9/Nov/2000
 *
 *  $Id: SystemData.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.config;

import java.util.*;

import gate.Factory;
import gate.FeatureMap;
import gate.creole.ResourceInstantiationException;
import gate.util.GateSaxException;
import gate.util.Strings;


/** This class represents and instantiates systems during
  * config data parsing.
  */
class SystemData
{
  /** Debug flag */
  protected static final boolean DEBUG = false;

  /** Default constructor. */
  SystemData() {
  } // default constructor

  /** The list of PRs */
  List prList = new ArrayList();

  /** The list of LRs */
  List lrList = new ArrayList();

  /** The name of the SYSTEM */
  String systemName = new String("name not set");

  /** The type name of the SYSTEM's controller */
  String controllerTypeName = new String("controller type name not set");

  /** Create a Controller; called when all the system data
    * is present.
    */
  void createSystem() throws GateSaxException
  {
    // create the controller
    if(controllerTypeName.equalsIgnoreCase("none")){
      //no controller required, bail
      return;
    }
    try {
      FeatureMap controllerParams = Factory.newFeatureMap();
      Collection controller = (Collection)
        Factory.createResource(controllerTypeName, controllerParams);
      controller.addAll(prList);
    } catch(ResourceInstantiationException e) {
      throw new GateSaxException(
        "Couldn't create controller for SYSTEM: " +
        systemName + "; problem was: " + Strings.getNl() + e
      );
    }
  } // createSystem()

} // class SystemData
