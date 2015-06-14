/*
 *  Group.java
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
 *  $Id: Group.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.security;

import java.util.List;

import gate.persist.PersistenceException;


public interface Group {

  public static final int OBJECT_CHANGE_NAME        = 1001;
  public static final int OBJECT_CHANGE_ADDUSER     = 1002;
  public static final int OBJECT_CHANGE_REMOVEUSER  = 1003;

  /** --- */
  public Long getID();

  /** --- */
  public String getName();

  /** --- */
  public List getUsers();

  /** --- */
  public void setName(String newName, Session s)
    throws PersistenceException,SecurityException;

  /** --- */
  public void addUser(Long userID, Session s)
    throws PersistenceException,SecurityException;

  /** --- */
  public void addUser(User usr, Session s)
    throws PersistenceException,SecurityException;

  /** --- */
  public void removeUser(Long userID, Session s)
    throws PersistenceException,SecurityException;

  /** --- */
  public void removeUser(User usr, Session s)
    throws PersistenceException,SecurityException;

}
