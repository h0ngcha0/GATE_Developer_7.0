/*
 *  NodeImpl.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 24.01.2000
 *
 *  $Id: NodeImpl.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package  gate.annotation;

import gate.Node;

/** Provides an implementation for the interface gate.Node.
 *
 */
public class NodeImpl implements Node, Comparable
{
  /** Debug flag
   */
  private static final boolean DEBUG = false;

  /** Freeze the serialization UID. */
  static final long serialVersionUID = -8240414984367916298L;

  /** Construction from id. Creates an unrooted node.
   */
  public NodeImpl (Integer id) {
    this.id = id;
    offset = null;
  } // Node()

  /** Construction from id and offset.
   *
   * @param id the Id of the new node
   * @param offset the (temporal) offset of the Node; Should be <b>null</b>
   *     for non-anchored nodes.
   */
  public NodeImpl (Integer id, Long offset) {
      this.id = id;
      this.offset = offset;
  } // Node(id, offset)

  /** Returns the Id of the Node.
   */
  public Integer getId () { return  id; }

  /** Offset (will be null when the node is not anchored)
   */
  public Long getOffset () { return  offset; }

  /** String representation
   */
  public String toString() {
    return "NodeImpl: id=" + id + "; offset=" + offset;
  } // toString()

  /** Ordering
   */
  public int compareTo(Object o) throws ClassCastException {
    Node other = (Node) o;
    return id.compareTo(other.getId());
  } // compareTo

  /** To allow AnnotationSet to revise offsets during editing
   */
  void setOffset(Long offset) { this.offset = offset; }

  /**
   * The id of this node (used for persistency)
   *
   */
  Integer id;
  /**
   * The offset of this node
   *
   */
  Long offset;
}
