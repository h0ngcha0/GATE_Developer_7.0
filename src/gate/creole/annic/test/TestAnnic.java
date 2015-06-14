/*
 *  TestAnnic.java
 *
 *  Niraj Aswani, 19/March/07
 *
 *  $Id: TestAnnic.html,v 1.0 2007/03/19 16:22:01 niraj Exp $
 */
package gate.creole.annic.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import gate.creole.annic.Constants;
import gate.creole.annic.Hit;
import gate.creole.annic.Parser;
import gate.creole.annic.lucene.LuceneSearcher;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import gate.*;
import gate.creole.splitter.SentenceSplitter;

/**
 * A class to test ANNIC Functionalities.
 * 
 * @author niraj
 */
public class TestAnnic extends TestCase {

  /**
   * Corpus to index
   */
  private Corpus testCorpus;

  /**
   * ANNIC Home
   */
  private File annicHome;

  /**
   * Index URL
   */
  private File indexURL;

  /**
   * Constructor
   * 
   * @param dummy
   */
  public TestAnnic(String dummy) {
    super(dummy);
  }

  /**
   * This method sets up the parameters for the files to be testes It
   * initialises the Tokenizer and sets up the other parameters for the
   * morph program
   */
  protected void setUp() throws Exception {
    super.setUp();
    indexURL = new File(File.createTempFile("abc", "abc").getParentFile(),
            "test-index");
    if(!indexURL.exists() || !indexURL.isDirectory()) indexURL.mkdir();
  }

  /**
   * Called when tests ends
   */
  protected void tearDown() throws Exception {
    // clean up
    super.tearDown();
  }

  /**
   * Testing the annic indexing functionalities
   * 
   * @throws Exception
   */
  public void testAnnicIndexing() throws Exception {
    // lets create a corpus
    testCorpus = Factory.newCorpus("TestAnnic");

    File directory = new File(new File(new File(new File(new File(new File(Gate
            .getGateHome(), "src"), "gate"), "resources"), "gate.ac.uk"),
            "tests"), "annic");
    File[] files = directory.listFiles();
    for(int i = 0; i < files.length; i++) {
      if(files[i].isFile()) {
        Document doc = Factory.newDocument(files[i].toURI().toURL(), 
                "ISO-8859-1");
        testCorpus.add(doc);
      }
    }

    AnnicIndexing annicPR = new AnnicIndexing();
    SentenceSplitter splitter = (SentenceSplitter)Factory
            .createResource("gate.creole.splitter.SentenceSplitter");
    splitter.setInputASName("Key");
    splitter.setOutputASName("Key");
    for(int i = 0; i < testCorpus.size(); i++) {
      splitter.setDocument((Document)testCorpus.get(i));
      splitter.execute();
    }

    // index
    annicPR.setAnnotationSetName("Key");
    annicPR.setBaseTokenAnnotationType("Token");
    annicPR.setCorpus(testCorpus);
    annicPR.setIndexUnitAnnotationType("Sentence");
    annicPR.setIndexOutputDirectoryLocation(indexURL.toURI().toURL());
    annicPR.execute();
    Factory.deleteResource(testCorpus);

  }

  /**
   * Testing annic searching functionalities.
   * 
   * @throws Exception
   */
  public void testSearcher() throws Exception {
    LuceneSearcher searcher = new LuceneSearcher();
    HashMap parameters = new HashMap();
    ArrayList indexLocations = new ArrayList();
    indexLocations.add(indexURL.getAbsolutePath());
    parameters.put(Constants.INDEX_LOCATIONS, indexLocations);
    parameters.put(Constants.CONTEXT_WINDOW, new Integer(5));
    String query = "{Person}";
    boolean success = searcher.search(query, parameters);
    int noOfHits = searcher.next(-1).length;
    assertEquals(12, noOfHits);

    query = "{Organization}({Token})*3{Person}";
    success = searcher.search(query, parameters);
    noOfHits = searcher.next(-1).length;
    assertEquals(noOfHits, 0);

    query = "{Organization}({Token})*3 (\"up\" | \"down\") ({Token})*3 ({Money} | {Percent})";
    success = searcher.search(query, parameters);
    Hit toExport[] = searcher.next(-1);
    assertEquals(toExport.length, 0);

    String xmlRepresentation = Parser.toXML(toExport);
    // and then read it back
    toExport = Parser.fromXML(xmlRepresentation);
    xmlRepresentation = Parser.toXML(toExport);
  }

  /**
   * A Method that is called from gate.TestGate to invoke the testing
   * methods.
   */
  public static Test suite() {
    return new TestSuite(TestAnnic.class);
  }
}
