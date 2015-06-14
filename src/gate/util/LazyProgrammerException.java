/*
 *  LazyProgrammerException.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  Hamish Cunningham, 14/Feb/00
 *
 *  $Id: LazyProgrammerException.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.util;

/** What to throw in a method that hasn't been implemented yet. 
  * Yes, there are good reasons never to throw RuntimeExceptions
  * and thereby sidestep Java's exception checking mechanism. But
  * we're so lazy we don't care. And anyway, none of these are
  * ever supposed to make it into released versions (who are we
  * kidding?).
  */
public class LazyProgrammerException extends RuntimeException {

  /** Debug flag */
  private static final boolean DEBUG = false;

  /** In a fit of complete laziness we didn't even document this
    * class properly.
    */
  public LazyProgrammerException() {
    super(defaultMessage);
  }

  /** In a fit of complete laziness we didn't even document this
    * class properly.
    */
  public LazyProgrammerException(String s) {
    super(s + defaultMessage);
  }

  /** In a fit of complete laziness we didn't even document this
    * class properly.
    */
  static String defaultMessage = 
    " It was Valentin's fault. I never touched it.";

} // LazyProgrammerException
