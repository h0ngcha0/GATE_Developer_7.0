/*
 *  GazetteerList.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  borislav popov 02/2002
 *
 *  $Id: GazetteerList.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.creole.gazetteer;

import java.io.*;
import java.net.URL;
import java.util.*;

import gate.creole.ResourceInstantiationException;
import gate.util.BomStrippingInputStreamReader;
import gate.util.Files;


/** Gazetteer List provides the means for uploading, managing and
 *  storing the data in the gazetteer list files. */
public class GazetteerList extends gate.creole.AbstractLanguageResource
implements List {

  /** indicates list representation of the gazetteer list*/
  public final static int LIST_MODE = 0;

  /** indicates representation of the gaz list as a single string */
  public final static int STRING_MODE = 1;

  /** the url of this list */
  private URL url;

  /**the encoding of the list */
  private String encoding = "UTF-8";

  /** indicates the current mode
   *of the gazetteer list(e.g. STRING_MODE,LIST_MODE) */
  private int mode = 0 ;

  /** flag indicating whether the list has been modified after loading/storing */
  private boolean isModified = false;

  /** the entries of this list */
  private List entries = new ArrayList();

  /** the content of this list */
  private String content = null;

  /** the separator used to delimit feature name-value pairs in gazetteer lists */
  private String separator;

  /** create a new gazetteer list */
  public GazetteerList() {
  }

  /** @return true if the list has been modified after load/store  */
  public boolean isModified() {
    return isModified;
  }

  /**Sets the modified status of the current list
   * @param modified is modified flag   */
  public void setModified(boolean modified) {
    isModified = modified;
  }

  /** Retrieves the current mode of the gaz list
   *  @return the current mode   */
  public int getMode() { return mode; }

  /**Sets mode of the gazetteer list
   * @param m the mode to be set    */
  public void setMode(int m) {
    if (m!=mode) {
    switch (m){
        case LIST_MODE :{
          mode = m;
          updateContent(content);
          break;
        } // LIST_MODE
        case STRING_MODE:{
          content = this.toString();
          mode = m;
          break;
        } //STRING_MODE
        default:{
          throw new gate.util.GateRuntimeException("Invalid Mode ="+mode
          +"\nValid modes are:\nLIST_MODE = "+LIST_MODE
          +"\nSTRING_MODE = "+STRING_MODE);
        } // default
      } // switch
    } // only if different from the current
  } // setMode(int)


  /** Sets the encoding of the list
   *  @param encod the encoding to be set */
  public void setEncoding(String encod) {
    encoding = encod;
  }

  /** Gets the encoding of the list
   *  @return the encoding of the list*/
  public String getEncoding() {
    return encoding;
  }

  /**
   * Loads a gazetteer list
   * @throws ResourceInstantiationException when the resource cannot be created
   */
  public void load() throws ResourceInstantiationException {
    load(false);
  }

  /**
   * Loads a gazetteer list
   * @param isOrdered true if the feature maps used should be ordered
   * @throws ResourceInstantiationException when the resource cannot be created
   */
  public void load(boolean isOrdered) throws ResourceInstantiationException {
    try {
      if (null == url) {
        throw new ResourceInstantiationException("URL not specified (null).");
      }

      BufferedReader listReader;

      listReader = new BomStrippingInputStreamReader(
                              (url).openStream(), encoding);
      String line;
      while (null != (line = listReader.readLine())) {
        entries.add(new GazetteerNode(line, separator, isOrdered));
      } //while

      listReader.close();
    } catch (Exception x) {
      throw new ResourceInstantiationException(x.getClass()+":"+x.getMessage());
    }
    isModified = false;
  } // load ()

  /**
   * Stores the list to the specified url
   * @throws ResourceInstantiationException
   */
  public void store() throws ResourceInstantiationException{
    try {
      if (null == url) {
        throw new ResourceInstantiationException("URL not specified (null)");
      }

      URL tempUrl = url;
      if (-1 != url.getProtocol().indexOf("gate")) {
        tempUrl = gate.util.protocols.gate.Handler.class.getResource(
                      gate.util.Files.getResourcePath() + url.getPath()
                    );
      } // if gate:path url

      File fileo = Files.fileFromURL(tempUrl);

      fileo.delete();
      OutputStreamWriter listWriter  = new OutputStreamWriter(
                    new FileOutputStream(fileo), encoding);
//      BufferedWriter listWriter = new BufferedWriter(new FileWriter(fileo));
      Iterator iter = entries.iterator();
      while (iter.hasNext()) {
        listWriter.write(iter.next().toString());
        listWriter.write(13);
        listWriter.write(10);
      }
      listWriter.close();
    } catch (Exception x) {
      throw new ResourceInstantiationException(x.getClass()+":"+x.getMessage());
    }
    isModified = false;
  } // store()


  /**
   * Sets the URL of the list
   * @param theUrl the URL of the List
   */
  public void setURL(URL theUrl) {
    url = theUrl;
    isModified = true;
  }

  /**
   * Gets the URL of the list
   * @return the URL of the list
   */
  public URL getURL() {
    return url;
  }


  /**
   * @return the seperator
   */
  public String getSeparator() {
    return separator;
  }

  /**
   * @param separator the separator to set
   */
  public void setSeparator(String separator) {
    this.separator = separator;
  }


/*--------------implementation of java.util.List--------------------*/
  public int size() {
    return entries.size();
  }

  public boolean isEmpty() {
    return (0 == entries.size());
  }

  public boolean contains(Object o) {
    return entries.contains(o);
  } // contains()

  /**Gets an iterator over the list. It is not dangerous if the iterator is modified since there
  are no dependencies of entries to other members  */
  public Iterator iterator() {
    return entries.iterator();
  }

  public Object[] toArray() {
    return entries.toArray();
  }

  public Object[] toArray(Object[] a) {
    return entries.toArray(a);
  }

  public boolean add(Object o) {
    boolean result = false;
    if (o instanceof GazetteerNode) {
      result = entries.add(o);
    }
    isModified |= result;
    return result;
  } // add()

  public boolean remove(Object o) {
    boolean result = entries.remove(o);
    isModified |= result;
    return result;
  }

  public boolean containsAll(Collection c) {
    return entries.containsAll(c);
  }

  /**
   * Adds entire collection
   * @param c a collection to be addded
   * @return true if all the elements where Strings and all are sucessfully added
   */
  public boolean addAll(Collection c) {
    Iterator iter = c.iterator();
    Object o;
    boolean result = false;

    while (iter.hasNext()) {
      o = iter.next();
      if (o instanceof GazetteerNode) {
        result |= entries.add(o);
      }
    } // while
    isModified |= result;

    return result;
  } // addAll(Collection)

  public boolean addAll(int index, Collection c) {
    boolean result = entries.addAll(index,c);
    isModified |= result;
    return result;
  } //addAll(int,Collection)


  public boolean removeAll(Collection c) {
    boolean result = entries.removeAll(c);
    isModified |= result;
    return result;
  }

  public boolean retainAll(Collection c) {
    boolean result = entries.retainAll(c);
    isModified |= result;
    return result;
  }

  public void clear() {
    if (0 < entries.size())
      isModified = true;
    entries.clear();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((entries == null) ? 0 : entries.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    GazetteerList other = (GazetteerList)obj;
    if(entries == null) {
      if(other.entries != null) return false;
    } else if(!entries.equals(other.entries)) return false;
    return true;
  }

  public Object get(int index) {
    return entries.get(index);
  }

  public Object set(int index, Object element) {
    isModified=true;
    return entries.set(index,element);
  }

  public void add(int index, Object element) {
    isModified = true;
    entries.add(index,element);
  }

  public Object remove(int index) {
    int size = entries.size();
    Object result = entries.remove(index);
    isModified |= (size!=entries.size());
    return result;
  }

  public int indexOf(Object o) {
    return entries.indexOf(o);
  }

  public int lastIndexOf(Object o) {
    return entries.lastIndexOf(o);
  }

  public ListIterator listIterator() {
    return entries.listIterator();
  }

  public ListIterator listIterator(int index) {
    return entries.listIterator(index);
  }

  public List subList(int fromIndex, int toIndex) {
    return entries.subList(fromIndex,toIndex);
  }


  /** Retrieves the string representation of the gaz list
   *  according to its mode. If
   *  {@link #LIST_MODE} then all
   *  the entries are dumped sequentially to a string. If
   *  {@link #STRING_MODE} then
   *  the content (a string) of the gaz list is retrieved.
   *  @return the string representation of the gaz list*/
  public String toString() {
    String stres = null;
    switch (mode) {
      case LIST_MODE : {
        StringBuffer result = new StringBuffer();
        String entry = null;
        for(int i = 0; i < entries.size(); i++) {
          GazetteerNode node = (GazetteerNode)entries.get(i);
          entry = node.getEntry().trim();
          if(entry.length() > 0) {
            result.append(entry);
            Map featureMap = node.getFeatureMap();
            if(featureMap != null && (featureMap.size() > 0)) {
              result.append(node.featureMapToString(featureMap));
            }
            result.append("\n");
          }// if
        }// for
        stres = result.toString();
        break;
      }
      case STRING_MODE : {
        stres = content;
        break;
      }
      default: {
        throw new gate.util.GateRuntimeException("Invalid Mode ="+mode
        +"\nValid modes are:\nLIST_MODE = "+LIST_MODE
        +"\nSTRING_MODE = "+STRING_MODE);
      }
    } // switch
    return stres;
  }//toString()

  /** Updates the content of the gaz list with the given parameter.
   *  Depends on the mode of the gaz list.
   *  In the case of {@link #LIST_MODE}
   *  the new content is parsed and loaded as single nodes through the
   *  {@link java.util.List} interface. In the case of
   *  {@link #STRING_MODE} the new content
   *  is stored as a String and is not parsed.
   *  @param newContent the new content of the gazetteer list */
  public void updateContent(String newContent) {
    switch (mode) {
      case STRING_MODE : {
        content = newContent;
        break;
      }
      case LIST_MODE : {
        BufferedReader listReader;
        listReader = new BufferedReader(new StringReader(newContent));
        String line;
        List tempEntries = new ArrayList();
        try {
          while (null != (line = listReader.readLine())) {
            tempEntries.add(new GazetteerNode(line,separator));
          } //while
          listReader.close();
        } catch (IOException x) {
          /**should never be thrown*/
          throw new gate.util.LuckyException("IOException :"+x.getMessage());
        }

        isModified = !tempEntries.equals(entries);
        clear();
        entries = tempEntries;
        break;
      } // LIST_MODE
      default: {
        throw new gate.util.GateRuntimeException("Invalid Mode ="+mode
        +"\nValid modes are:\nLIST_MODE = "+LIST_MODE
        +"\nSTRING_MODE = "+STRING_MODE);
      }// default
    } // switch mode
  } // updateContent(String)


} // Class GazetteerList
