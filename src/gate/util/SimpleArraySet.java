/*
 *  SimpleArraySet.java
 *
 *  Copyright (c) 2001, The University of Sheffield.
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *   D.Ognyanoff, 5/Nov/2001
 *
 *  $Id: SimpleArraySet.java 8055 2007-01-23 19:20:43Z kwilliams $
 */


package gate.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


/**
 * A specific *partial* implementation of the Set interface used for
 * high performance and memory reduction on small sets. Used in
 * gate.fsm.State, for example
 */
public class SimpleArraySet<T> implements Serializable, Iterable<T>
{
  /**
   * The array storing the elements
   */
  Object[] theArray = null;

  public int size()
  {
      return theArray == null ? 0 : theArray.length;
  }

  public Collection asCollection()
  {
      if (theArray == null) return new ArrayList();
      return Arrays.asList(theArray);
  }

  public boolean add(T tr)
  {
    if (theArray == null)
    {
      theArray = new Object[1];
      theArray[0] = tr;
    } else {
      int newsz = theArray.length+1;
      int index = java.util.Arrays.binarySearch(theArray, tr);
      if (index < 0)
      {
        index = ~index;
        Object[] temp = new Object[newsz];
        int i;
        for (i = 0; i < index; i++)
        {
          temp[i] = theArray[i]; theArray[i] = null;
        }
        for (i = index+1; i<newsz; i++)
        {
          temp[i] = theArray[i-1]; theArray[i-1] = null;
        }
        temp[index] = tr;
        theArray = temp;
      } else {
        theArray[index] = tr;
      }
    } // if initially empty
    return true;
  } // add

  /**
   * iterator
   */
  public java.util.Iterator<T> iterator()
  {
    if (theArray == null)
      return new java.util.Iterator<T>()
        {
          public boolean hasNext() {return false;}
          public T next() { return null; }
          public void remove() {}
        };
    else
      return new java.util.Iterator<T>()
        {
          int count = 0;
          public boolean hasNext()
          {
            if (theArray == null)
              throw new RuntimeException("");
            return count < theArray.length;
          }
          public T next() {
            if (theArray == null)
              throw new RuntimeException("");
            return (T) theArray[count++];
          }
          public void remove() {}
        }; // anonymous iterator
  } // iterator

} // SimpleArraySet
