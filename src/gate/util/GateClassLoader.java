/*
 *  GateClassLoader.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 * 
 *  Kalina Bontcheva, 1998
 *
 *  Revised by Hamish for 1.2 style and URL/Jar loading, June 2000
 *
 *  $Id: GateClassLoader.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.util;

import java.net.URL;
import java.net.URLClassLoader;

/** GATE's class loader, which allows loading of classes over the net.
  * A list of URLs is searched, which should point at .jar files or
  * to directories containing class file hierarchies.
  * The class loader is unusual in supporting reloading of classes, which
  * is useful for CREOLE developers who want to recompile modules without
  * relaunching GATE.
  * The loader is also used for creating JAPE RHS action classes.
  */
public class GateClassLoader extends URLClassLoader {

  /** Debug flag */
  private static final boolean DEBUG = false;

  /** Default construction - use an empty URL list. */
  public GateClassLoader() { super(new URL[0]); }

  /** Chaining constructor. */
  public GateClassLoader(ClassLoader parent) { super(new URL[0], parent); }

  /** Default construction with URLs list. */
  public GateClassLoader(URL[] urls) { super(urls); }

  /** Chaining constructor with URLs list. */
  public GateClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  } // Chaining constructor with URLs list.

  /** Appends the specified URL to the list of URLs to search for classes
    * and resources.
    */
  @Override
  public void addURL(URL url) { super.addURL(url); }

  /** Delegate loading to the super class (loadClass has protected
    * access there).
    */
  @Override
  public synchronized Class<?> loadClass(String name, boolean resolve)
  throws ClassNotFoundException {
    return super.loadClass(name, resolve);
  } // loadClass(name, resolve)

  /** Forward a call to super.defineClass, which is protected and final
    * in super. This is used by JAPE and the Jdk compiler class.
    */
  public synchronized Class<?> defineGateClass(String name, byte[] bytes, int offset, int len)
  {
    return super.defineClass(name, bytes, offset, len);
  } // defineGateClass(name, bytes, offset, len);

  /** Forward a call to super.resolveClass, which is protected and final
    * in super. This is used by JAPE and the Jdk compiler class
    */
  public synchronized void resolveGateClass(Class<?> c) { super.resolveClass(c); }

  /**
   * Given a fully qualified class name, this method returns the instance of Class if it is already loaded using the ClassLoader
   * or it returns null.
   */
  public synchronized Class<?> findExistingClass(String name) {
	  return findLoadedClass(name);
  }
  
  /** Reload a class. This works on the assumption that all classes that
    * we are asked to reload will have been loaded by a GateClassLoader
    * and not the system class loader. If this is not the case, this
    * method will simply return the previously loaded class (because of
    * the delegation chaining model of class loaders in JDK1.2 and above).
    * <P>
    * The method works by avoiding the normal chaining behaviour of
    * class loaders by creating a star-shaped group of parallel loaders.
    * Each of these chains of the system class loader, but as long as
    * the class that we wish to reload wan't loaded by the system loader,
    * it will not be present in a new loader of this type.
    * <P>
    * An implication is that reloaded classes must always be instantiated
    * via the class returned from this method.
    */
  public synchronized Class<?> reloadClass(String name) throws ClassNotFoundException {
    Class<?> theClass = null;

    // if the class isn't already present in this class loader
    // we can just load it
    theClass = findLoadedClass(name);
    if(theClass == null)
      return loadClass(name);

    // if there's a cached loader, try that
    if(cachedReloader != null) {

      // if the cached loader hasn't already loaded this file, then ask it to
      theClass = cachedReloader.findLoadedClass(name);
      if(theClass == null)
        return cachedReloader.loadClass(name);
    }

    // create a new reloader and cache it
    cachedReloader = new GateClassLoader(getURLs());

    // ask the new reloader to load the class
    return cachedReloader.loadClass(name, true);

  } // reloadClass(String name)

  /** A cache used by the reloadClass method to store the last new
    * loader that we created.
    */
  private static GateClassLoader cachedReloader = null;

} // GateClassLoader
