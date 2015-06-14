/*
 * MappingDefinition.java
 *
 * Copyright (c) 2002, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 2, June1991.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 * borislav popov 02/2002
 *
 */
package gate.creole.gazetteer;


import java.io.*;
import java.net.URL;
import java.util.*;

import gate.creole.ResourceInstantiationException;
import gate.util.BomStrippingInputStreamReader;
import gate.util.Files;

/** Represents a mapping definition which maps gazetteer lists to ontology classes */
public class MappingDefinition extends gate.creole.AbstractLanguageResource
                              implements List<MappingNode> {

  private static final long serialVersionUID = 3617291212063848503L;

  /** the default encoding of the mapping */
  private final static String ENCODING = "UTF-8";

  /** the list of nodes */
  private List<MappingNode> nodes = new ArrayList<MappingNode>();

  /** the url of the mapping definition */
  private URL url;

  /** set of gaz lists */
  private Set<String> lists = new HashSet<String>();

  /** mapping between a list and a node */
  private Map<String, MappingNode> nodesByList = new HashMap<String, MappingNode>();


  /** Creates a new mapping definition */
  public MappingDefinition() {
  }

  /**Gets the urls from this definition
   * @return a list of all the ontology urls present in this mapping def   */
  public List<String> getUrls() {
    Set<String> result = new HashSet<String>();
    for ( int i = 0 ; i < nodes.size() ; i++ ) {
      result.add(nodes.get(i).getOntologyID());
    } // for
    return new ArrayList<String>(result);
  }  // getUrls()

  /** Gets the url of this definition
   *  @return the url of the definition */
  public URL getURL() {
    return url;
  }

  /** Sets the url of this definition
   *  @param aUrl the url of the definition*/
  public void setURL(URL aUrl) {
    url = aUrl;
  }

  /**Loads the mapping definition
   * @throws ResourceInstantiationException if load fails.
   */
  public void load() throws ResourceInstantiationException,InvalidFormatException {
    if (null == url) {
      throw new ResourceInstantiationException("URL not set (null).");
    }
    try {
      BufferedReader mapReader =
      new BomStrippingInputStreamReader((url).openStream(), ENCODING);

      String line;
      MappingNode node;
      while (null != (line = mapReader.readLine())) {
        if (0 != line.trim().length()) {
          node = new MappingNode(line);
          this.add(node);
        } // if
      } //while

      mapReader.close();

    } catch (InvalidFormatException ife){
      throw new InvalidFormatException(url,"on load");
    } catch (IOException ioe) {
      throw new ResourceInstantiationException(ioe);
    }


  } // load();

  /**
   * Stores the mapping definition
   * @throws ResourceInstantiationException if store fails.
   */
  public void store()throws ResourceInstantiationException{
    if (null == url) {
      throw new ResourceInstantiationException("URL not set (null).");
    }
    try {
    File fileo = Files.fileFromURL(url);
    fileo.delete();
    BufferedWriter mapWriter = new BufferedWriter(new FileWriter(fileo));
    for (int index = 0 ; index < nodes.size() ; index++) {
      mapWriter.write(nodes.get(index).toString());
      mapWriter.newLine();
    }
    mapWriter.close();
    } catch (IOException ioe) {
      throw new ResourceInstantiationException(ioe);
    }
  } //store();

  /**
   * Gets the gaz lists.
   * @return set of the gazetteer lists
   */
  public Set<String> getLists() {
    return new HashSet<String>(lists);
  }

  /**
   * Gets node by list
   * @param list a gazetteer list filename
   * @return the mapping node that matches the list
   */
  public MappingNode getNodeByList(String list) {
    return nodesByList.get(list);
  }

  /*---implementation of interface java.util.List---*/

  public int size() {
    return nodes.size();
  }

  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  public boolean contains(Object o) {
    return nodes.contains(o);
  }

  public Iterator iterator() {
    return new SafeIterator();
  }

  public Object[] toArray() {
    return nodes.toArray();
  }

  public Object[] toArray(Object[] a) {
    return nodes.toArray(a);
  }

  /**
   * adds a new node, only if its list is new and uniquely mapped to this node.
   * @param o a node
   * @return true if the list of node is not already mapped with another node.
   */
  public boolean add(MappingNode o) {
    boolean result = false;
    if (o instanceof MappingNode) {
      String list = ((MappingNode)o).getList();
      if (!nodesByList.containsKey(list)) {
        result = nodes.add((MappingNode)o);
        nodesByList.put(list,(MappingNode)o);
        lists.add(list);
      } // if unique
    } // if a linear node
    return result;
  } // add()

  /**
   * adds a new node at the specified position, only if its list is new and uniquely mapped to this node.
   * @param o a node
   * @param index position in the list
   */
  public void add(int index,MappingNode o) {
    if (o instanceof MappingNode) {
      String list = ((MappingNode)o).getList();
      if (!nodesByList.containsKey(list)) {
        nodes.add(index,(MappingNode)o);
        nodesByList.put(list,(MappingNode)o);
        lists.add(list);
      } // if unique
    } // if a linear node
  } // add()

  public MappingNode set(int index, MappingNode o) {
    throw new UnsupportedOperationException("this method has not been implemented");
  }

  public MappingNode get(int index){
    return nodes.get(index);
  }

  public boolean remove(Object o) {
    boolean result = false;
    if (o instanceof MappingNode) {
      result = nodes.remove(o);
      String list = ((MappingNode)o).getList();
      lists.remove(list);
      nodesByList.remove(list);
    } // if linear node
    return result;
  }// remove

  public MappingNode remove(int index) {
    MappingNode result = null;
    result = nodes.remove(index);
    if (null!=result) {
      String list = ((MappingNode)result).getList();
      lists.remove(list);
      nodesByList.remove(list);
    }
    return result;
  }

  public boolean containsAll(Collection c) {
    return nodes.containsAll(c);
  }

  public boolean addAll(Collection c) {
    boolean result = false;
    Iterator iter = c.iterator();
    Object o;
    while (iter.hasNext()) {
      o = iter.next();
      if (o instanceof MappingNode)  {
        result |= add((MappingNode)o);
      } // instance of MappingNode
    } // while
    return result;
  } // addAll(Collection)

  public boolean addAll(int index,Collection c) {
    int size = nodes.size();
    Iterator iter = c.iterator();
    Object o;
    while (iter.hasNext()) {
      o = iter.next();
      if (o instanceof MappingNode)  {
        add(index++, (MappingNode)o);
      } // instance of MappingNode
    } // while
    return (size!=nodes.size());
  }//addAll(int,Collection)

  public boolean removeAll(Collection c) {
    boolean result = false;
    Iterator iter = c.iterator();
    Object o;
    while (iter.hasNext()) {
      o = iter.next();
      result |= remove(o);
    }
    return result;
  }// removeAll()


  public boolean retainAll(Collection c) {
    int aprioriSize = nodes.size();
    List scrap = new ArrayList();

    MappingNode node;
    for (int index = 0; index < nodes.size(); index++) {
      node = (MappingNode) nodes.get(index);
      if (c.contains(node)) {
        scrap.add(node);
      }
    } //for

    removeAll(scrap);

    return (aprioriSize != nodes.size());
  }


  public void clear() {
    nodes.clear();
    lists.clear();
    nodesByList.clear();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((lists == null) ? 0 : lists.hashCode());
    result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
    result =
            prime * result
                    + ((nodesByList == null) ? 0 : nodesByList.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    MappingDefinition other = (MappingDefinition)obj;
    if(lists == null) {
      if(other.lists != null) return false;
    } else if(!lists.equals(other.lists)) return false;
    if(nodes == null) {
      if(other.nodes != null) return false;
    } else if(!nodes.equals(other.nodes)) return false;
    if(nodesByList == null) {
      if(other.nodesByList != null) return false;
    } else if(!nodesByList.equals(other.nodesByList)) return false;
    return true;
  }
  
  public List subList(int i1, int i2) {
    return nodes.subList(i1,i2);
  }

  public ListIterator listIterator(int index) {
    throw new UnsupportedOperationException("this method is not implemented");
  }
  public ListIterator listIterator() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  public int lastIndexOf(Object o) {
    return nodes.lastIndexOf(o);
  }

  public int indexOf(Object o) {
    return nodes.indexOf(o);
  }

 /*---end of implementation of interface java.util.List---*/

 /*-----------internal classes -------------*/

  /**Provides means for safe iteration over
   * the entries of the Mapping Definition  */
  private class SafeIterator implements Iterator {
    private int index = 0;
    private boolean removeCalled = false;

    public boolean hasNext() {
      return (index < nodes.size());
    }

    public Object next() {
      removeCalled = false;
      return nodes.get(index++);
    }

    public void remove() {
      if (!removeCalled && index > 0  ) {
        index--;
        MappingDefinition.this.remove(nodes.get(index));
      }// if possible remove
      removeCalled = true;
    } // remove


  } // class SafeIterator

} // class MappingDefinition