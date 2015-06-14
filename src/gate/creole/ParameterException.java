/*
 *  ParameterException.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Hamish Cunningham, 14/Nov/2000
 *
 *  $Id: ParameterException.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.creole;

import gate.util.GateException;

/** This exception indicates failure to set a resource parameter.
  */
public class ParameterException extends GateException {

  /** Debug flag */
  private static final boolean DEBUG = false;

  public ParameterException() {
    super();
  }

  public ParameterException(String s) {
    super(s);
  }

  public ParameterException(Exception e) {
    super(e.toString());
  }
} // ParameterException
