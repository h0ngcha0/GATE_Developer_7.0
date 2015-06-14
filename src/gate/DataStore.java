/*
 *  DataStore.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Hamish Cunningham, 11/Feb/2000
 *
 *  $Id: DataStore.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate;

import java.util.List;

import gate.event.DatastoreListener;
import gate.persist.PersistenceException;
import gate.security.*;
import gate.security.SecurityException;
import gate.util.FeatureBearer;
import gate.util.NameBearer;

/** Models all sorts of data storage.
  */
public interface DataStore extends FeatureBearer, NameBearer {

  public static final String DATASTORE_FEATURE_NAME = "DataStore";
  public static final String LR_ID_FEATURE_NAME = "LRPersistenceId";


  /** Set the URL as string for the underlying storage mechanism. */
  public void setStorageUrl(String storageUrl) throws PersistenceException;

  /** Get the URL as String for the underlying storage mechanism. */
  public String getStorageUrl();

  /**
   * Create a new data store. <B>NOTE:</B> for some data stores
   * creation is an system administrator task; in such cases this
   * method will throw an UnsupportedOperationException.
   */
  public void create()
  throws PersistenceException, UnsupportedOperationException;

  /** Open a connection to the data store. */
  public void open() throws PersistenceException;

  /** Close the data store. */
  public void close() throws PersistenceException;

  /**
   * Delete the data store. <B>NOTE:</B> for some data stores
   * deletion is an system administrator task; in such cases this
   * method will throw an UnsupportedOperationException.
   */
  public void delete()
  throws PersistenceException, UnsupportedOperationException;

  /**
   * Delete a resource from the data store.
   * @param lrId a data-store specific unique identifier for the resource
   * @param lrClassName class name of the type of resource
   */
  public void delete(String lrClassName, Object lrId)
  throws PersistenceException,SecurityException;

  /**
   * Save: synchonise the in-memory image of the LR with the persistent
   * image.
   */
  public void sync(LanguageResource lr)
  throws PersistenceException,SecurityException;

  /**
   * Set method for the autosaving behaviour of the data store.
   * <B>NOTE:</B> many types of datastore have no auto-save function,
   * in which case this will throw an UnsupportedOperationException.
   */
  public void setAutoSaving(boolean autoSaving)
  throws UnsupportedOperationException,PersistenceException;

  /** Get the autosaving behaviour of the LR. */
  public boolean isAutoSaving();

  /** Adopt a resource for persistence. */
  public LanguageResource adopt(LanguageResource lr, SecurityInfo secInfo)
  throws PersistenceException, gate.security.SecurityException;

  /**
   * Get a resource from the persistent store.
   * <B>Don't use this method - use Factory.createResource with
   * DataStore and DataStoreInstanceId parameters set instead.</B>
   */
  LanguageResource getLr(String lrClassName, Object lrId)
  throws PersistenceException,SecurityException;

  /** Get a list of the types of LR that are present in the data store. */
  public List getLrTypes() throws PersistenceException;

  /** Get a list of the IDs of LRs of a particular type that are present. */
  public List getLrIds(String lrType) throws PersistenceException;

  /** Get a list of the names of LRs of a particular type that are present. */
  public List getLrNames(String lrType) throws PersistenceException;

  /** Get a list of LRs that satisfy some set or restrictions */
  public List findLrIds(List constraints) throws PersistenceException;

  /**
   *  Get a list of LRs that satisfy some set or restrictions and are
   *  of a particular type
   */
  public List findLrIds(List constraints, String lrType) throws PersistenceException;

  /** Get the name of an LR from its ID. */
  public String getLrName(Object lrId) throws PersistenceException;

  /**
   * Registers a new {@link gate.event.DatastoreListener} with this datastore
   */
  public void addDatastoreListener(DatastoreListener l);

  /**
   * Removes a a previously registered {@link gate.event.DatastoreListener}
   * from the list listeners for this datastore
   */
  public void removeDatastoreListener(DatastoreListener l);

  /**
   * Returns the name of the icon to be used when this datastore is displayed
   * in the GUI
   */
  public String getIconName();

  /**
   * Returns the comment displayed by the GUI for this DataStore
   */
  public String getComment();


  /**
   * Checks if the user (identified by the sessionID)
   *  has read access to the LR
   */
  public boolean canReadLR(Object lrID)
    throws PersistenceException, gate.security.SecurityException;

  /**
   * Checks if the user (identified by the sessionID)
   * has write access to the LR
   */
  public boolean canWriteLR(Object lrID)
    throws PersistenceException, gate.security.SecurityException;

  /** get security information for LR . */
  public SecurityInfo getSecurityInfo(LanguageResource lr)
    throws PersistenceException;

  /** set security information for LR . */
  public void setSecurityInfo(LanguageResource lr,SecurityInfo si)
    throws PersistenceException, gate.security.SecurityException;

  /** identify user using this datastore */
  public void setSession(Session s)
    throws gate.security.SecurityException;

  /** identify user using this datastore */
  public Session getSession(Session s)
    throws gate.security.SecurityException;

  /**
   * Try to acquire exlusive lock on a resource from the persistent store.
   * Always call unlockLR() when the lock is no longer needed
   */
  public boolean lockLr(LanguageResource lr)
  throws PersistenceException,SecurityException;

  /**
   * Releases the exlusive lock on a resource from the persistent store.
   */
  public void unlockLr(LanguageResource lr)
  throws PersistenceException,SecurityException;


} // interface DataStore
