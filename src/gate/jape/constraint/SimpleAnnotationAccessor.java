/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Eric Sword, 09/03/08
 *
 *  $Id$
 */
package gate.jape.constraint;

import gate.*;

/**
 * Accessor that returns the annotation itself
 *
 * @version $Revision$
 * @author esword
 */
public class SimpleAnnotationAccessor extends MetaPropertyAccessor {

  public Object getValue(Annotation annot, AnnotationSet context) {
    return annot;
  }

  @Override
  public Object getKey() {
    return null;
  }
}
