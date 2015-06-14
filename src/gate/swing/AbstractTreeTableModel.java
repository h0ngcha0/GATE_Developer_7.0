/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 13/02/2001
 *
 *  $Id: AbstractTreeTableModel.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */
package gate.swing;

import javax.swing.event.*;
import javax.swing.tree.TreePath;

/**
 * An abstract implementation of the TreeTableModel interface. Its main purpose
 * is handling the list of listeners.
 */
public abstract class AbstractTreeTableModel implements TreeTableModel {
  /**
   * The root of the tree.
   */
  protected Object root;

  /**
   * The list of listeners.
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor for a tree-table containing only one node: the root.
   */
  public AbstractTreeTableModel(Object root) {
      this.root = root;
  }

  //
  // Default implmentations for methods in the TreeModel interface.
  //

  /**
   * Default implementation. Gets the root of the tree.
   */
  public Object getRoot() {
      return root;
  }

  /**
   * Is this node a leaf?
   */
  public boolean isLeaf(Object node) {
      return getChildCount(node) == 0;
  }

  public void valueForPathChanged(TreePath path, Object newValue) {}

  /**
   * This method is not called by the current implementation of JTree.
   * Implemented only for completion.
   */
  public int getIndexOfChild(Object parent, Object child) {
    for (int i = 0; i < getChildCount(parent); i++){
      if (getChild(parent, i).equals(child)){
        return i;
      }
    }
    return -1;
  }

  /**
   * Registers a new {@link javax.swing.event.TreeModelListener} with this
   * model.
   */
  public void addTreeModelListener(TreeModelListener l) {
    listenerList.add(TreeModelListener.class, l);
  }

  /**
   * Removes a {@link javax.swing.event.TreeModelListener} from the list of
   * listeners registered with this model.
   */
  public void removeTreeModelListener(TreeModelListener l) {
    listenerList.remove(TreeModelListener.class, l);
  }

  /**
   * Notify all listeners that have registered interest for
   * notification on this event type.  The event instance
   * is lazily created using the parameters passed into
   * the fire method.
   * @see EventListenerList
   */
  protected void fireTreeNodesChanged(Object source, Object[] path,
                                      int[] childIndices,
                                      Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path,
                                              childIndices, children);
        ((TreeModelListener)listeners[i+1]).treeNodesChanged(e);
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for
   * notification on this event type.  The event instance
   * is lazily created using the parameters passed into
   * the fire method.
   * @see EventListenerList
   */
  protected void fireTreeNodesInserted(Object source, Object[] path,
                                      int[] childIndices,
                                      Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path,
                                              childIndices, children);
        ((TreeModelListener)listeners[i+1]).treeNodesInserted(e);
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for
   * notification on this event type.  The event instance
   * is lazily created using the parameters passed into
   * the fire method.
   * @see EventListenerList
   */
  protected void fireTreeNodesRemoved(Object source, Object[] path,
                                      int[] childIndices,
                                      Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path,
                                              childIndices, children);
        ((TreeModelListener)listeners[i+1]).treeNodesRemoved(e);
      }
    }
  }

  /**
   * Notify all listeners that have registered interest for
   * notification on this event type.  The event instance
   * is lazily created using the parameters passed into
   * the fire method.
   * @see EventListenerList
   */
  protected void fireTreeStructureChanged(Object source, Object[] path,
                                      int[] childIndices,
                                      Object[] children) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    TreeModelEvent e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==TreeModelListener.class) {
        // Lazily create the event:
        if (e == null) e = new TreeModelEvent(source, path,
                                              childIndices, children);
        ((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
      }
    }
  }

  /**
   * Default implementation. Does nothing.
   */
  public void setValueAt(Object aValue, Object node, int column){}

  abstract public Class getColumnClass(int column);
  abstract public boolean isCellEditable(Object node, int column);
  abstract public Object getChild(Object parent, int index);
  abstract public int getChildCount(Object parent);
  abstract public int getColumnCount();
  abstract public String getColumnName(int column);
  abstract public Object getValueAt(Object node, int column);

}
