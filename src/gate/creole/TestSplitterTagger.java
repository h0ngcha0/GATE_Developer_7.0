/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 17/05/01
 *
 *  $Id: TestSplitterTagger.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */
package gate.creole;

import java.net.URL;
import java.util.Iterator;

import junit.framework.*;

import gate.*;
import gate.corpora.TestDocument;
import gate.creole.splitter.SentenceSplitter;
import gate.creole.tokeniser.DefaultTokeniser;
import gate.util.GateException;

/**
 * Test code for the SentenceSplitter and the POS tagger.
 */
public class TestSplitterTagger extends TestCase{

/** Construction */
  public TestSplitterTagger(String name) { super(name); }

  /** Fixture set up */
  public void setUp() throws GateException {
  } // setUp

  /** Put things back as they should be after running tests
    * (reinitialise the CREOLE register).
    */
  public void tearDown() throws Exception {
  } // tearDown

  /** Test suite routine for the test runner */
  public static Test suite() {
    return new TestSuite(TestSplitterTagger.class);
  } // suite



  public void testSplitterTagger() throws Exception{
    //get a document
    Document doc = Factory.newDocument(
      new URL(TestDocument.getTestServerName() + "tests/doc0.html")
    );

    //tokenise the document
    //create a tokeniser
    FeatureMap params = Factory.newFeatureMap();
    DefaultTokeniser tokeniser = (DefaultTokeniser) Factory.createResource(
                          "gate.creole.tokeniser.DefaultTokeniser", params);
    //runtime stuff
    tokeniser.setDocument(doc);
    tokeniser.setAnnotationSetName("testAS");
    tokeniser.execute();


    //create a splitter
    params = Factory.newFeatureMap();
    SentenceSplitter splitter = (SentenceSplitter) Factory.createResource(
                          "gate.creole.splitter.SentenceSplitter", params);

    //runtime stuff
    splitter.setDocument(doc);
    splitter.setOutputASName("testAS");
    splitter.setInputASName("testAS");
    splitter.execute();
    assertTrue(!doc.getAnnotations("testAS").
      get(ANNIEConstants.SENTENCE_ANNOTATION_TYPE).isEmpty());

    //now check the tagger
    //create a tagger
    params = Factory.newFeatureMap();
    POSTagger tagger = (POSTagger) Factory.createResource(
                          "gate.creole.POSTagger", params);

    //runtime stuff
    tagger.setDocument(doc);
    tagger.setInputASName("testAS");
    tagger.execute();
    Iterator<Annotation> tokIter =doc.getAnnotations("testAS").
      get(ANNIEConstants.TOKEN_ANNOTATION_TYPE).iterator();
    while(tokIter.hasNext()){
      Annotation token = tokIter.next();
      String kind = (String)token.getFeatures().
        get(ANNIEConstants.TOKEN_KIND_FEATURE_NAME);
      if(kind.equals(ANNIEConstants.TOKEN_KIND_FEATURE_NAME))
        assertNotNull(token.getFeatures().
          get(ANNIEConstants.TOKEN_CATEGORY_FEATURE_NAME));
    }
  }
}