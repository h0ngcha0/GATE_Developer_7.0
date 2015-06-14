/*
 *  JdmAttribute.java
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
 *  $Id: JdmAttribute.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 *  Description:  This is JDM aimed at repeating the functionality of GDM
 */

package gate.jape;

import java.io.Serializable;

/**
  * THIS CLASS SHOULDN'T BE HERE. Please let's all ignore it, and maybe
  * it will go away.
  * <P>
  * Implements the TIPSTER and GDM API for attributes.
  * Test code in <code>testAttributes</code> class. <P>
  * The JdmAttribute class would accept all java serialisable classes, all
  * jdm classes and also all user-defined classes provided they implement
  * the Serializable interface. This restriction is necessary  since Jdm
  * uses Java serialisation to ensure object persistency. However, making
  * classes serialisable is usually quite straightforward. <P>
  * @author Kalina Bontcheva
*/
public class JdmAttribute implements Serializable {

  /** Debug flag */
  private static final boolean DEBUG = false;

  private String name;
  private Object value;

  protected JdmAttribute() {
  }

  /** throws JdmException when the value isn't one of the types we know
    * how to store, i.e., a serialisable or Jdm class.
    */
  public JdmAttribute(String name, Object value) {
    this.name = name; this.value = value;
  }

  /** throws JdmException when the value isn't one of the types we know
    * how to store, i.e., a serialisable or Jdm class.
    */
  public JdmAttribute(JdmAttribute jdmAttr) {
  	String name = jdmAttr.getName();
    Object value = jdmAttr.getValue();
  }

  public String getName() {
  	return name;
  }

  public Object getValue() {
  	return value;
  }

  public String getValueType() {
  	return value.getClass().getName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    JdmAttribute other = (JdmAttribute)obj;
    if(name == null) {
      if(other.name != null) return false;
    } else if(!name.equals(other.name)) return false;
    if(value == null) {
      if(other.value != null) return false;
    } else if(!value.equals(other.value)) return false;
    return true;
  }

  public String toString() {
         return "JdmAttr: name=" + name + "; value=" + value.toString();

  }

} // class JdmAttribute
