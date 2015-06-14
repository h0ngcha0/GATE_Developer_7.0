/*
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 26/Feb/2002
 *
 *  $Id: TestReload.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.util;

import java.net.URL;

import junit.framework.*;


public class TestReload extends TestCase{
  /** Construction */
  public TestReload(String name) { super(name); }

 /** Fixture set up */
  public void setUp() {
  } // setUp

  /** Test suite routine for the test runner */
  public static Test suite() {
    return new TestSuite(TestReload.class);
  } // suite

 /** Reload */
  public void testReload() throws Exception {
    ReloadingClassLoader loader = new ReloadingClassLoader();
    //load first version
    //it looks that Java doesn't like the jar in jar situation so we'll have to
    //load the jar archives directly from the website.
    URL url = new URL("http://gate.ac.uk/tests/first.jar");
    loader.load(url);
    //try the class
    Class c = loader.loadClass("loader.Scratch", true);
    String firstResult = c.newInstance().toString();

    //unload first version
    loader.unload(url);

    //try to get an error
    try{
      c = loader.loadClass("loader.Scratch", true);
      Assert.assertTrue("Class was found after being unloaded!", false);
    }catch(ClassNotFoundException cnfe){
      if(DEBUG) System.out.println("OK: got exception");
    }

    //load second version
    url = new URL("http://gate.ac.uk/tests/second.jar");
    loader.load(url);

    //try the class
    c = loader.loadClass("loader.Scratch", true);
    String secondResult = c.newInstance().toString();

    //check the results are different
    Assert.assertTrue("Got same result from different versions of the class",
                      !firstResult.equals(secondResult));
  }

  public void testUnload() throws Exception {
    ReloadingClassLoader loader = new ReloadingClassLoader();
    //load first version
//    URL url = Gate.class.getResource(Files.getResourcePath() +
//                                     "/gate.ac.uk/tests/first.jar");
    URL url = new URL("http://gate.ac.uk/tests/first.jar");

    loader.load(url);
    //try the class
    Class c = loader.loadClass("loader.Scratch", true);
    String firstResult = c.newInstance().toString();

    //unload first version
    loader.unload(url);

    //try to get an error
    try{
      c = loader.loadClass("loader.Scratch", true);
      Assert.assertTrue("Class was found after being unloaded!", false);
    }catch(ClassNotFoundException cnfe){
      if(DEBUG) System.out.println("OK: got exception");
    }
  }

  public void doNottestCache() throws Exception {
    ReloadingClassLoader loader = new ReloadingClassLoader();
    long timeFresh = 0;
    long startTime;
    long endTime;
    //load fresh class 100 times
    URL url = new URL("http://gate.ac.uk/tests/first.jar");
    for(int i = 0; i< 100; i++){
      loader.load(url);
      startTime = System.currentTimeMillis();
      //load the class
      Class c = loader.loadClass("loader.Scratch", true);
      endTime = System.currentTimeMillis();
      timeFresh += endTime - startTime;
      loader.unload(url);
    }

    //load cached classes 100 times
    loader.load(url);
    //load the class
    Class c = loader.loadClass("loader.Scratch", true);
    long timeCache = 0;
    for(int i = 0; i< 100; i++){
      startTime = System.currentTimeMillis();
      //load the class
      c = loader.loadClass("loader.Scratch", true);
      endTime = System.currentTimeMillis();
      timeCache += endTime - startTime;
    }
    Assert.assertTrue("Cached classes load slower than fresh ones!",
                      timeCache < timeFresh);
  }


  /** Debug flag */
  private static final boolean DEBUG = false;
}