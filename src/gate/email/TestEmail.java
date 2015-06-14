/*
 *  TestEmail.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Cristian URSU,  7/Aug/2000
 *
 *  $Id: TestEmail.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.email;

import java.util.Map;

import junit.framework.*;

import gate.Gate;
//import org.w3c.www.mime.*;


/**
  * Test class for Email facilities
  */
public class TestEmail extends TestCase
{
  /** Debug flag */
  private static final boolean DEBUG = false;

  /** Construction */
  public TestEmail(String name) { super(name); }

  /** Fixture set up */
  public void setUp() {
  } // setUp

  /** A test */
  public void testUnpackMarkup() throws Exception{
    // create the markupElementsMap map
    Map markupElementsMap = null;
    gate.Document doc = null;
//    Gate.init();
    doc = gate.Factory.newDocument(Gate.getUrl("tests/email/test.eml"), "ISO-8859-1");

    // get a document format that deals with e-mails
    gate.DocumentFormat docFormat = gate.DocumentFormat.getDocumentFormat(
      doc, doc.getSourceUrl()
    );
    assertTrue( "Bad document Format was produced.EmailDocumentFormat was expected",
            docFormat instanceof gate.corpora.EmailDocumentFormat
          );

    docFormat.unpackMarkup (doc,"DocumentContent");
    // Verfy if all annotations from the default annotation set are consistent
    gate.corpora.TestDocument.verifyNodeIdConsistency(doc);

  } // testUnpackMarkup()

  public static void main(String[] args) {
    try{
      Gate.init();
      TestEmail testEmail = new TestEmail("");
      testEmail.testUnpackMarkup();

    }catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
    * final test
    */
  public void testEmail(){
    EmailDocumentHandler emailDocumentHandler = new EmailDocumentHandler();
    emailDocumentHandler.testSelf();
  }// testEmail

  /** Test suite routine for the test runner */
  public static Test suite() {
    return new TestSuite(TestEmail.class);
  } // suite

} // class TestEmail
