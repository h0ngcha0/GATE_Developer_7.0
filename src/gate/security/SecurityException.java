/*
 *  SecurityException.java
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
 *  $Id: SecurityException.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.security;

import gate.util.GateException;



/** This exception indicates security violation.
  */
public class SecurityException extends GateException {
  /** Debug flag */
  private static final boolean DEBUG = false;

  /** Default construction */
  public SecurityException() { super(); }

  /** Construction from string */
  public SecurityException(String s) { super(s); }

  /** Construction from exception */
  public SecurityException(Exception e) { super(e.toString()); }

} // PersistenceException
