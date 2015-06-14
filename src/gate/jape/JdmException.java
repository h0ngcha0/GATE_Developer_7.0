/*
 *  JdmException.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 * 
 *  Kalina Bontcheva, 23/02/2000
 *
 *  $Id: JdmException.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 *  Description:  This is JDM aimed at repeating the functionality of GDM
 */

package gate.jape;

/**
  * THIS CLASS SHOULDN'T BE HERE. Please let's all ignore it, and maybe
  * it will go away.
  */
public class JdmException extends gate.util.GateException {

  /** Debug flag */
  private static final boolean DEBUG = false;

	public JdmException() {
  	super();
  }

  public JdmException(String s) {
  	super(s);
  }
}
