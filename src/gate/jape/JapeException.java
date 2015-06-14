/*
 *  JapeException.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Hamish Cunningham, 23/02/2000
 *
 *  $Id: JapeException.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.jape;

import gate.util.GateException;

/** Superclass of all JAPE exceptions. */
public class JapeException extends GateException {

  /** Debug flag */
  private static final boolean DEBUG = false;

  public JapeException(Throwable cause) {
    super(cause);
  }
  
  public JapeException(String message) {
    super(message);
  }
  
  public JapeException(String message, Throwable cause) {
    super(message, cause);
  }

  public JapeException() {
    super();
  }
  
  /**
   * The location of the error.
   */
  String location = null;
  
  void setLocation(String location) {
    this.location = location;
  }
  
  public String getMessage() {
    if(location != null) {
      return super.getMessage() + " (at " + location + ")";
    }
    else {
      return super.getMessage();
    }
  }

} // class JapeException
