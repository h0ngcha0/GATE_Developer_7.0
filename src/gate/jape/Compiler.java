/*
 *  Compiler.java - compile .jape files
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
 *  $Id: Compiler.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.jape;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import gate.Factory;
import gate.jape.parser.ParseCpsl;
import gate.util.Err;
import gate.util.Out;

/**
  * Compiler for JAPE files.
  */
public class Compiler {

  /** Debug flag */
  private static final boolean DEBUG = false;

  /** How much noise to make. */
  static private boolean verbose = false;

  static String defaultEncoding = "UTF-8";

  /** Take a list of .jape files names and compile them to .ser.
    * Also recognises a -v option which makes it chatty.
    */
  static public void main(String[] args) {

    // process options
    int argsIndex = 0;
    while(args[argsIndex].toCharArray()[0] == '-')
      if(args[argsIndex++].equals("-v"))
        verbose = true;

    // construct list of the files
    ArrayList fileNames = new ArrayList();
    for( ; argsIndex<args.length; argsIndex++)
      fileNames.add(args[argsIndex]);

    // compile the files
    compile(fileNames);

    message("done");
  } // main

  /** The main compile method, taking a file name. */
  static public void compile(String japeFileName, String encoding) {
    // parse
    message("parsing " + japeFileName);
    Transducer transducer = null;
    try {
      transducer = parseJape(japeFileName, encoding);
    } catch(JapeException e) {
      emessage("couldn't compile " + japeFileName + ": " + e);
      return;
    }

    // save
    message("saving " + japeFileName);
    try {
      saveJape(japeFileName, transducer);
    } catch (JapeException e) {
      emessage("couldn't save " + japeFileName + ": " + e);
    }

    message("finished " + japeFileName);
  } // compile(String japeFileName)

  /** The main compile method, taking a list of file names. */
  static public void compile(ArrayList fileNames) {
    // for each file, compile and save
    for(Iterator i = fileNames.iterator(); i.hasNext(); )
      compile((String) i.next(), defaultEncoding);
  } // compile

  /** Parse a .jape and return a transducer, or throw exception. */
  static public Transducer parseJape(String japeFileName, String encoding)
  throws JapeException {
    Transducer transducer = null;

    try {
      ParseCpsl cpslParser = Factory.newJapeParser(new File(japeFileName).toURI().toURL(),
                                           encoding);
      transducer = cpslParser.MultiPhaseTransducer();
    } catch(gate.jape.parser.ParseException e) {
      throw(new JapeException(e.toString()));
    } catch(IOException e) {
      throw(new JapeException(e.toString()));
    }

    return transducer;
  } // parseJape

  /** Save a .jape, or throw exception. */
  static public void saveJape(String japeFileName, Transducer transducer)
  throws JapeException {
    String saveName = japeNameToSaveName(japeFileName);

    try {
      FileOutputStream fos = new FileOutputStream(saveName);
      ObjectOutputStream oos = new ObjectOutputStream (fos);
      oos.writeObject(transducer);
      oos.close();
    } catch (IOException e) {
      throw(new JapeException(e.toString()));
    }
  } // saveJape

  /** Convert a .jape file name to a .ser file name. */
  static String japeNameToSaveName(String japeFileName) {
    String base = japeFileName;
    if(japeFileName.endsWith(".jape") || japeFileName.endsWith(".JAPE"))
      base = japeFileName.substring(0, japeFileName.length() - 5);
    return base + ".ser";
  } // japeNameToSaveName

  /** Hello? Anybody there?? */
  public static void message(String mess) {
    if(verbose) Out.println("JAPE compiler: " + mess);
  } // message

  /** Ooops. */
  public static void emessage(String mess) {
    Err.println("JAPE compiler error: " + mess);
  } // emessage

} // class Compiler


// $Log$
// Revision 1.11  2005/06/21 14:09:51  valyt
// Ken Williams's patch for Factory and JAPE tranducers
//
// Revision 1.10  2005/01/11 13:51:36  ian
// Updating copyrights to 1998-2005 in preparation for v3.0
//
// Revision 1.9  2004/07/21 17:10:07  akshay
// Changed copyright from 1998-2001 to 1998-2004
//
// Revision 1.8  2004/03/25 13:01:14  valyt
// Imports optimisation throughout the Java sources
// (to get rid of annoying warnings in Eclipse)
//
// Revision 1.7  2001/09/13 12:09:49  kalina
// Removed completely the use of jgl.objectspace.Array and such.
// Instead all sources now use the new Collections, typically ArrayList.
// I ran the tests and I ran some documents and compared with keys.
// JAPE seems to work well (that's where it all was). If there are problems
// maybe look at those new structures first.
//
// Revision 1.6  2001/02/08 13:46:06  valyt
// Added full Unicode support for the gazetteer and Jape
// converted the gazetteer files to UTF-8
//
// Revision 1.5  2000/11/08 16:35:02  hamish
// formatting
//
// Revision 1.4  2000/10/26 10:45:30  oana
// Modified in the code style
//
// Revision 1.3  2000/10/16 16:44:33  oana
// Changed the comment of DEBUG variable
//
// Revision 1.2  2000/10/10 15:36:35  oana
// Changed System.out in Out and System.err in Err;
// Added the DEBUG variable seted on false;
// Added in the header the licence;
//
// Revision 1.1  2000/02/23 13:46:04  hamish
// added
//
// Revision 1.1.1.1  1999/02/03 16:23:01  hamish
// added gate2
//
// Revision 1.3  1998/10/29 12:07:27  hamish
// added compile method taking a file name
//
// Revision 1.2  1998/09/21 16:19:27  hamish
// don't catch *all* exceptions!
//
// Revision 1.1  1998/09/18 15:07:41  hamish
// a functioning compiler in two shakes of a rats tail
