/*
 *  User.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Marin Dimitrov, 19/Sep/2001
 *
 *  $Id: User.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.security;

import java.util.List;

import gate.persist.PersistenceException;


public interface User {

  /** subtype for ObjectModificationEvent of type OBJECT_MODIFIED
   *  @see gate.event.ObjectModificationEvent
   *  the event is sent when the name of the user is changed
   *  */
  public static final int OBJECT_CHANGE_NAME        = 1001;


  /** returns the ID of the user
   *  user IDs are uniques in the same
   *  data store
   *  */
  public Long getID();

  /** returns the name of the user
   *  user names are unique in the
   *  same data store */
  public String getName();

  /** returns a list with the groups that the
   *  user is member of  */
  public List getGroups();

  /** changes user name
   *  Only members of the ADMIN group have sufficient privileges.
   *  fires ObjectModificationEvent  */
  public void setName(String newName, Session s)
    throws PersistenceException,SecurityException;

  /** changes user password
   *  Only members of the ADMIN group and the user himself
   *  have sufficient privileges */
  public void setPassword(String newPass, Session s)
    throws PersistenceException,SecurityException;
}
