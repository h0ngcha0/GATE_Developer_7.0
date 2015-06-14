package gate.util;

/**
 * A ClassLoader that supports class reloading.
 * It maintains a list of URLs which are searched for classes not found in the
 * system classloader.
 * URLs can be loaded and unloaded. Loading the same URL twice will cause the
 * jar file or directory pointed by the URL to be reloaded.
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ReloadingClassLoader extends ClassLoader {

  /**
   * Constructs a ReloadingClassLoader using a custom class loader as parent.
   *
   * @param parent the parent class loader. The parent class loader should give
   * access to the system classes at the least (in order to load a new class
   * access to {@link java.lang.Object} is required).
   */
  public ReloadingClassLoader(ClassLoader parent){
    this.parent = parent;
    loaders = new HashMap();
  }


  /**
   * Constructs a ReloadingClassLoader using the System Class Loader as a
   * parent.
   */
  public ReloadingClassLoader() {
    this(ClassLoader.getSystemClassLoader());
  }

  /**
   * Registers an URL as a location where class files can be found.
   * If the URL was already registered the the classes found at the location
   * will be reloaded.
   * @param url the URL pointing to a jar file or to a directory containing
   * class files.
   */
  public void load(URL url){
    LocationClassLoader loader = new LocationClassLoader(url);
    loaders.put(url, loader);
  }

  /**
   * Removes a registered URL.
   * @param url the URl to be unloaded.
   */
  public void unload(URL url){
    loaders.remove(url);
  }

  /**
   * Loads the class with the specified name.  It searches for classes in the
   * following order:
   * <ol>
   *   <li>the parent classloader</li>
   *   <li>all the locations registered with this class loader</li>
   * </ol>
   *
   * @param  name The name of the class
   * @param  resolve If <tt>true</tt> then resolve the class
   * @return  The resulting <tt>Class</tt> object
   * @throws  ClassNotFoundException If the class could not be found
   */
  protected synchronized Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException{
    Class c = null;
    //check with the parent (most classes are fixed)
    if(parent != null){
      try {
        c = parent.loadClass(name);
      }catch (ClassNotFoundException cnfe) {}
    }

    if(c == null){
      //Check all the loaders for the class
      Iterator loaderIter = loaders.values().iterator();
      while (c == null && loaderIter.hasNext()) {
        LocationClassLoader aLoader = (LocationClassLoader) loaderIter.next();
        try {
          c = aLoader.loadClass(name, false);
        } catch (ClassNotFoundException e) {}
      }
    }
    if(c == null) throw new ClassNotFoundException(name);
    if (resolve) resolveClass(c);
    return c;
  }

  /**
   * A ClassLoader that loads classes from a location specified by an URL.
   */
  protected class LocationClassLoader extends URLClassLoader {

    /**
     * Constructs a LocationClassLoader for a specified URL.
     * Uses the same parent classloader as the enclosing ReloadingClassLoader.
     * @param location the URL to be searched for class files.
     */
    public LocationClassLoader(URL location) {
      super(new URL[]{location}, null);
      this.location = location;
      classCache = new HashMap();
    }

    /**
     * Loads the class with the specified name. It will search first the parent
     * class loader, then an internal cache for classes already loaded and then
     * the registered URL.
     *
     * @param  name The name of the class
     * @param  resolve If <tt>true</tt> then resolve the class
     * @return  The resulting <tt>Class</tt> object
     *
     * @throws  ClassNotFoundException If the class could not be found
     */
    protected synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException{
      Class c = null;
      //search the parent first
      if(parent != null){
        try {
          c = parent.loadClass(name);
        }catch (ClassNotFoundException cnfe) {}
      }
      //search the cache
      if(c == null){
        c = (Class) classCache.get(name);
      }
      //search the registered location
      if (c == null) {
        //this will trow ClassNotFoundException if necessary
        c = findClass(name);
        //save the class for future searches
        classCache.put(name, c);
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }

    /**
     * A cache for classes already found and loaded.
     */
    protected Map classCache;
    /**
     * The location to be searched for new classes.
     */
    protected URL location;
  }//protected class LocationClassLoader


  /**
   * Map that contains the {@link LocationClassLoader} for each registered URL.
   */
  protected Map loaders;

  /**
   * The parent class loader.
   */
  protected ClassLoader parent;
}