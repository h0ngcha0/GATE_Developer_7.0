/*
 *  MutableBoolean.java - A mutable wrapper for int, so you can return
 *                       integer values via a method parameter
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Hamish Cunningham, 30/09/98
 *
 *  $Id: MutableBoolean.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */


package gate.jape;


/**
  * A mutable wrapper for bool, so you can return
  * bool values via a method parameter. If public data members bother you
  * I suggest you get a hobby, or have more sex or something.
  */
public class MutableBoolean
{
  /** Debug flag */
  private static final boolean DEBUG = false;

  public boolean value = false;

} // class MutableBoolean

