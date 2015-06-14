/*
 *  TestJape2.java (Java Annotation Patterns Engine)
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Hamish Cunningham, 23/02/2000
 *
 *  $Id: TestJape2.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 *  Description: Test class for JAPE.
 */

package gate.jape.functest;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import gate.*;
import gate.annotation.AnnotationSetImpl;
import gate.creole.ResourceInstantiationException;
import gate.util.Err;
import gate.util.Out;

/**
  * Second test harness for JAPE.
  * Uses the Sheffield Tokeniser and Gazetteer, and must be run
  * from the gate directory.
  * @author Hamish Cunningham
  */
public class TestJape2 {

  /** Debug flag */
  private static final boolean DEBUG = false;

  /** How much noise to make. */
  static private boolean verbose = false;


  /** Take a list of text files and a collection name, and
    * call tokeniser/gazetteer/jape on them, creating the
    * collection.
    */
  static public void main(String[] args) {

    // turn debug output on/off
    //Debug.setDebug(true);
    //Debug.setDebug(AnnotationSet.class, true);
    //Debug.setDebug(BasicPatternElement.class, true);
    //Debug.setDebug(ComplexPatternElement.class, true);
    //Debug.setDebug(ConstraintGroup.class, true);
    //Debug.setDebug(SinglePhaseTransducer.class, true);

    // variables to parse the command line options into
    String collName = null;
    String japeName = null;
    ArrayList fileNames = null;

    // process options
    for(int i=0; i<args.length; i++) {
      if(args[i].equals("-c") && ++i < args.length) // -c = coll name
        collName = args[i];
      else if(args[i].equals("-j") && ++i < args.length) // -j: .jape name
        japeName = args[i];
      else if(args[i].equals("-v")) // -v = verbose
        verbose = true;
      else { // a list of files
        fileNames = new ArrayList();
        do {
          fileNames.add(args[i++]);
        } while(i < args.length);
      }
    } // for each arg

    // did they give valid options?
    message("checking options");
    if(collName == null || japeName == null || fileNames == null)
      usage("you must supply collection, transducer and file names");

    // create a collection and run the tokeniser
    message("creating coll, tokenising and gazetteering");
    Corpus coll = null;
    try {
      coll = tokAndGaz(collName, fileNames);
    } catch(ResourceInstantiationException e) {
      usage("couldn't open collection: " + e);
    }
/*
    // run the parser test
    message("parsing the .jape file (or deserialising the .ser file)");
    Batch batch = null;
    try { batch = new Batch(japeName);
    } catch(JapeException e) {
      usage("can't create transducer " + e.getMessage());
    }
*/
    /*Transducer transducer = parseJape(japeName);
    //Out.println(transducer);
    if(transducer == null)
      System.exit(1);*/

    // test the transducers from the parser
/*
    message("running the transducer");
    try { batch.transduce(coll); } catch(JapeException e) {
      usage("couldn't run transducer " + e.getMessage());
    }
    //runTransducer(transducer, coll);
    //Out.println(transducer);

    message("done\n\r");
    //System.exit(0);
*/
  } // main


  /**
    * Create a collection and put tokenised and gazetteered docs in it.
    */
  static public Corpus tokAndGaz(String collName, ArrayList fileNames)
  throws ResourceInstantiationException {

    // create or overwrite the collection
    Corpus collection = null;
    File collDir = new File(collName);
    collection = Factory.newCorpus(
      collDir.getAbsolutePath()
    );

    // add all the documents
    for(Iterator i = fileNames.iterator(); i.hasNext(); ) {
      String fname = (String) i.next();

      File f = new File(fname);
      FeatureMap attrs = Factory.newFeatureMap();
      Document doc = null;

      try {
        AnnotationSet annots = new AnnotationSetImpl(doc);
        collection.add(
          Factory.newDocument(f.getAbsolutePath())
        );
      } catch(ResourceInstantiationException e) {
        e.printStackTrace();
      }

      /*
      // Tokenise the document
      Tokeniser tokeniser = new Tokeniser(doc, Tokeniser.HMM);
      try { tokeniser.hmmTokenSequence(); }
      catch(sheffield.creole.tokeniser.ParseException ex) {
        ex.printStackTrace();
        return null;
      } catch (CreoleException ex) {
        ex.printStackTrace();
        return null;
      }

      // Gazetteer the document
      gate.creole.Annotator gazetteer = new GazetteerAnnotator();
      gazetteer.annotate(doc, null);
      */
    } // for each doc name

    // return the annotated collection
    return collection;

  } //tokAndGaz


  /**
    * Must be run from the gate directory.
    * Parse the .jape file.
    */
    /*
    static public Transducer parseJape(String japeName) {
    Transducer transducer = null;

    if(japeName.endsWith(".ser")) { // it's compiled already
      message("deserialising " + japeName);
      File f = new File(japeName);
      if(! f.exists())
        Out.println(japeName + " not found");

      try {
        FileInputStream fis = new FileInputStream(f.getPath());
        ObjectInputStream ois = new ObjectInputStream(fis);
        transducer = (Transducer) ois.readObject();
        ois.close();
      } catch (Exception ex) {
        Err.println(
          "Can't read from " + f.getName() + ": " + ex.toString()
        );
      }
    } else { // parse it
      message("parsing " + japeName);
      try {
        ParseCpsl cpslParser = new ParseCpsl(japeName);
        transducer = cpslParser.MultiPhaseTransducer();
      } catch(IOException e) {
        e.printStackTrace();
      } catch(gate.jape.parser.ParseException ee) {
        Err.println("Error parsing transducer: " + ee.getMessage());
      }
    }

    return transducer;
  } // parseJape


  static public void runTransducer(
    Transducer transducer, Corpus coll
  ) {

    try {
      Document doc = coll.firstDocument();
      do {
        message("doing document " + doc.getId());
        transducer.transduce(doc);
        // Out.println(transducer.toString());
      } while( (doc = coll.nextDocument()) != null );
    } catch(JdmException e) {
      e.printStackTrace();
    } catch(JapeException e) {
      e.printStackTrace();
    }
  } // runTransducer
  */

  /** You got something wrong, dumbo. */
  public static void usage(String errorMessage) {
    String usageMessage =
      "usage: java gate.jape.TestJape2.main [-v] " +
        "-j JapePatternFile -c CollectionName FileName(s)";

    Err.println(errorMessage);
    Err.println(usageMessage);
    //System.exit(1);

  } // usage


  /** Hello? Anybody there?? */
  public static void message(String mess) {
    if(verbose) Out.println("TestJape2: " + mess);
  } // message

} // class TestJape2


// $Log$
// Revision 1.13  2005/01/11 13:51:36  ian
// Updating copyrights to 1998-2005 in preparation for v3.0
//
// Revision 1.12  2004/07/21 17:10:08  akshay
// Changed copyright from 1998-2001 to 1998-2004
//
// Revision 1.11  2004/03/25 13:01:14  valyt
// Imports optimisation throughout the Java sources
// (to get rid of annoying warnings in Eclipse)
//
// Revision 1.10  2001/09/13 12:09:50  kalina
// Removed completely the use of jgl.objectspace.Array and such.
// Instead all sources now use the new Collections, typically ArrayList.
// I ran the tests and I ran some documents and compared with keys.
// JAPE seems to work well (that's where it all was). If there are problems
// maybe look at those new structures first.
//
// Revision 1.9  2001/02/08 13:46:06  valyt
// Added full Unicode support for the gazetteer and Jape
// converted the gazetteer files to UTF-8
//
// Revision 1.8  2001/01/30 14:18:02  hamish
// fixed some hard-coded paths
//
// Revision 1.7  2000/11/08 16:35:04  hamish
// formatting
//
// Revision 1.6  2000/10/26 10:45:31  oana
// Modified in the code style
//
// Revision 1.5  2000/10/23 21:50:42  hamish
// cleaned up exception handling in gate.creole and added
// ResourceInstantiationException;
//
// changed Factory.newDocument(URL u) to use the new instantiation
// facilities;
//
// added COMMENT to resource metadata / ResourceData;
//
// changed Document and DocumentImpl to follow beans style, and moved
// constructor logic to init(); changed all the Factory newDocument methods to
// use the new resource creation stuff;
//
// added builtin document and corpus metadata to creole/creole.xml (copied from
// gate.ac.uk/tests/creole.xml);
//
// changed Corpus to the new style too;
//
// removed CreoleRegister.init()
//
// Revision 1.4  2000/10/18 13:26:48  hamish
// Factory.createResource now working, with a utility method that uses reflection (via java.beans.Introspector) to set properties on a resource from the
//     parameter list fed to createResource.
//     resources may now have both an interface and a class; they are indexed by interface type; the class is used to instantiate them
//     moved createResource from CR to Factory
//     removed Transients; use Factory instead
//
// Revision 1.3  2000/10/16 16:44:34  oana
// Changed the comment of DEBUG variable
//
// Revision 1.2  2000/10/10 15:36:37  oana
// Changed System.out in Out and System.err in Err;
// Added the DEBUG variable seted on false;
// Added in the header the licence;
//
// Revision 1.1  2000/02/23 13:46:12  hamish
// added
//
// Revision 1.1.1.1  1999/02/03 16:23:03  hamish
// added gate2
//
// Revision 1.9  1998/10/29 12:13:55  hamish
// reorganised to use Batch
//
// Revision 1.8  1998/10/01 16:06:41  hamish
// new appelt transduction style, replacing buggy version
//
// Revision 1.7  1998/09/26 09:19:21  hamish
// added cloning of PE macros
//
// Revision 1.6  1998/09/23 12:48:03  hamish
// negation added; noncontiguous BPEs disallowed
//
// Revision 1.5  1998/09/17 12:53:09  hamish
// fixed for new tok; new construction pattern
//
// Revision 1.4  1998/09/17 10:24:05  hamish
// added options support, and Appelt-style rule application
//
// Revision 1.3  1998/08/19 20:21:46  hamish
// new RHS assignment expression stuff added
//
// Revision 1.2  1998/08/18 14:37:45  hamish
// added some messages
//
// Revision 1.1  1998/08/18 12:43:11  hamish
// fixed SPT bug, not advancing newPosition
