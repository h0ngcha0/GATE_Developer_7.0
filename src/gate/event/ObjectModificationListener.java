/*
 *  ObjectModificationListener.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Marin Dimitrov, 21/Sep/2001
 *
 */

package gate.event;


public interface ObjectModificationListener extends GateListener {

  public void objectCreated(ObjectModificationEvent e);

  public void objectModified(ObjectModificationEvent e);

  public void objectDeleted(ObjectModificationEvent e);
}