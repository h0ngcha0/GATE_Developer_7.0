/*
 *  GateFormatXmlDocumentHandler.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Cristian URSU,  22 Nov 2000
 *
 *  $Id: GateFormatXmlDocumentHandler.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.xml;

import java.lang.reflect.Constructor;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import gate.*;
import gate.corpora.DocumentContentImpl;
import gate.corpora.DocumentImpl;
import gate.event.StatusListener;
import gate.util.*;

/**
 * Implements the behaviour of the XML reader. This is the reader for
 * Gate Xml documents saved with DocumentImplementation.toXml() method.
 * 
 * @deprecated GATE format XML documents are now handled by
 *             {@link gate.corpora.DocumentStaxUtils}.
 */
public class GateFormatXmlDocumentHandler extends DefaultHandler {
  /** Debug flag */
  private static final boolean DEBUG = false;

  /**
   * This is used to capture all data within two tags before calling the
   * actual characters method
   */
  private StringBuffer contentBuffer = new StringBuffer("");

  /** This is a variable that shows if characters have been read */
  private boolean readCharacterStatus = false;

  /**
   * An OLD GATE XML format is the one in which Annotations IDs are not
   * present
   */
  private static final int OLD = 1;

  /**
   * A NEW GATE XML format is the one in which Annotations IDs are
   * present
   */
  private static final int NEW = 2;

  /**
   * This value signifies that the document being read can be either OLD
   * or NEW
   */
  private static final int UNDEFINED = 0;

  /**
   * In the beginning we don't know the type of GATE XML format that we
   * read. We need to be able to read both types, but not a mixture of
   * them
   */
  private int gateXmlFormatType = UNDEFINED;

  /**
   * A Set recording every annotation ID read from the XML file. It is
   * used to check the consistency of the annotations being read. At the
   * end we need the maximum ID in order to set the annotation ID
   * generator on the document. This is why we need a TreeSet.
   */
  private TreeSet annotationIdSet = new TreeSet();

  /*********************************************************************
   * Instead of creating a new Class object for every Feature object we
   * store them in a map with a String as a key.
   ********************************************************************/
  private Map classCache = new HashMap();

  /**
   */
  public GateFormatXmlDocumentHandler(gate.Document aDocument) {
    // This string contains the plain text (the text without markup)
    tmpDocContent = new StringBuffer(aDocument.getContent().size().intValue());

    // Colector is used later to transform all custom objects into
    // annotation
    // objects
    colector = new LinkedList();

    // The Gate document
    doc = aDocument;
    currentAnnotationSet = doc.getAnnotations();
  }// GateFormatXmlDocumentHandler

  /**
   * This method is called when the SAX parser encounts the beginning of
   * the XML document.
   */
  public void startDocument() throws org.xml.sax.SAXException {
  }// startDocument

  /**
   * This method is called when the SAX parser encounts the end of the
   * XML document. Here we set the content of the gate Document to be
   * the one generated inside this class (tmpDocContent). After that we
   * use the colector to generate all the annotation reffering this new
   * gate document.
   */
  public void endDocument() throws org.xml.sax.SAXException {

    // replace the document content with the one without markups
    doc.setContent(new DocumentContentImpl(tmpDocContent.toString()));
    // long docSize = doc.getContent().size().longValue();

    // If annotations were present in the NEW GATE XML document format,
    // set the document generator to start from th next MAX Annot ID
    // value
    if(gateXmlFormatType == NEW && !annotationIdSet.isEmpty()) {
      // Because annotationIdSet is a TreeSet its elements are already
      // sorted.
      // The last element will contain the maximum value
      Integer maxAnnotID = (Integer)annotationIdSet.last();
      // Set the document generator to start from the maxAnnotID value
      ((DocumentImpl)doc).setNextAnnotationId(maxAnnotID.intValue() + 1);
      // Dispose of the annotationIdSet
      annotationIdSet = null;
    }// fi

    // fire the status listener
    fireStatusChangedEvent("Total elements: " + elements);

  }// endDocument

  /**
   * This method is called when the SAX parser encounts the beginning of
   * an XML element.
   */
  public void startElement(String uri, String qName, String elemName,
          Attributes atts) throws SAXException {

    // call characterActions
    if(readCharacterStatus) {
      readCharacterStatus = false;
      charactersAction(new String(contentBuffer).toCharArray(), 0,
              contentBuffer.length());
    }

    // Inform the progress listener to fire only if no of elements
    // processed
    // so far is a multiple of ELEMENTS_RATE
    if((++elements % ELEMENTS_RATE) == 0)
      fireStatusChangedEvent("Processed elements : " + elements);

    // Set the curent element being processed
    currentElementStack.add(elemName);

    if("AnnotationSet".equals(elemName)) processAnnotationSetElement(atts);

    if("Annotation".equals(elemName)) processAnnotationElement(atts);

    if("Feature".equals(elemName)) processFeatureElement(atts);

    if("Name".equals(elemName)) processNameElement(atts);

    if("Value".equals(elemName)) processValueElement(atts);

    if("Node".equals(elemName)) processNodeElement(atts);
  }// startElement

  /**
   * This method is called when the SAX parser encounts the end of an
   * XML element.
   */
  public void endElement(String uri, String qName, String elemName)
          throws SAXException {

    // call characterActions
    if(readCharacterStatus) {
      readCharacterStatus = false;
      charactersAction(new String(contentBuffer).toCharArray(), 0,
              contentBuffer.length());
    }

    currentElementStack.pop();
    // Deal with Annotation
    if("Annotation".equals(elemName)) {
      if(currentFeatureMap == null)
        currentFeatureMap = Factory.newFeatureMap();
      currentAnnot.setFM(currentFeatureMap);
      colector.add(currentAnnot);
      // Reset current Annot and current featue map
      currentAnnot = null;
      currentFeatureMap = null;
      return;
    }// End if
    // Deal with Value
    if("Value".equals(elemName)
            && "Feature".equals((String)currentElementStack.peek())) {
      // If the Value tag was empty, then an empty string will be
      // created.
      if(currentFeatureValue == null) currentFeatureValue = "";
    }// End if
    // Deal with Feature
    if("Feature".equals(elemName)) {
      if(currentFeatureName == null) {
        // Cannot add the (key,value) pair to the map
        // One of them is null something was wrong in the XML file.
        throw new GateSaxException(
                "A feature name was empty."
                        + "The annotation that cause it is "
                        + currentAnnot
                        + ".Please check the document with a text editor before trying again.");
      }
      else {
        if(currentFeatureMap == null) {
          // The XMl file was somehow altered and a start Feature wasn't
          // found.
          throw new GateSaxException(
                  "Document not consistent. A start"
                          + " feature element is missing. "
                          + "The annotation that cause it is "
                          + currentAnnot
                          + "Please check the document with a text editor before trying again.");
        }// End if
        // Create the appropiate feature name and values
        // If those object cannot be created, their string
        // representation will
        // be used.
        currentFeatureMap.put(createFeatKey(), createFeatValue());
        // currentFeatureMap.put(currentFeatureName,currentFeatureValue);
        // Reset current key
        currentFeatureKeyClassName = null;
        currentFeatureKeyItemClassName = null;
        currentFeatureName = null;
        // Reset current value
        currentFeatureValueClassName = null;
        currentFeatureValueItemClassName = null;
        currentFeatureValue = null;
      }// End if
      // Reset the Name & Value pair.
      currentFeatureName = null;
      currentFeatureValue = null;
      return;
    }// End if
    // Deal GateDocumentFeatures
    if("GateDocumentFeatures".equals(elemName)) {
      if(currentFeatureMap == null)
        currentFeatureMap = Factory.newFeatureMap();
      doc.setFeatures(currentFeatureMap);
      currentFeatureMap = null;
      return;
    }// End if

    // Deal with AnnotationSet
    if("AnnotationSet".equals(elemName)) {
      // Create and add annotations to the currentAnnotationSet
      Iterator iterator = colector.iterator();
      while(iterator.hasNext()) {
        AnnotationObject annot = (AnnotationObject)iterator.next();
        // Clear the annot from the colector
        iterator.remove();

        // Create a new annotation and add it to the annotation set
        try {

          // This is the result of a code-fix.The XML writter has been
          // modified
          // to serialize the annotation ID.In order to keep backward
          // compatibility
          // with previously saved documents we had to keep the old
          // code(where the id
          // is not added) in place.
          // If the document presents a mixture of the two formats, then
          // error is signaled

          // Check if the Annotation ID is present or not
          if(annot.getId() == null) {
            // Annotation without ID. We assume the OLD format.

            // If we previously detected a NEW format, then we have a
            // mixture of the two
            if(gateXmlFormatType == NEW)
            // Signal the error to the user
              throw new GateSaxException(
                      "Found an annotation without ID while "
                              + "previous annotations had one."
                              + "The NEW GATE XML document format requires"
                              + " all annotations to have an UNIQUE ID."
                              + " The offending annotation was of [type="
                              + annot.getElemName() + ", startOffset="
                              + annot.getStart() + ", endOffset="
                              + annot.getEnd() + "]");

            // We are reading OLD format document
            gateXmlFormatType = OLD;
            currentAnnotationSet.add(annot.getStart(), annot.getEnd(), annot
                    .getElemName(), annot.getFM());
          }
          else {
            // Annotation with ID. We assume the NEW format

            // If we previously detected an OLD format, then it means we
            // have a mixture of the two
            if(gateXmlFormatType == OLD)
            // Signal the error to the user
              throw new GateSaxException(
                      "Found an annotation with ID while "
                              + "previous annotations didn't have one."
                              + "The OLD GATE XML"
                              + "document format requires all annotations NOT to have an ID."
                              + " The offending annotation was of [Id="
                              + annot.getId() + ", type=" + annot.getElemName()
                              + ", startOffset=" + annot.getStart()
                              + ", endOffset=" + annot.getEnd() + "]");

            gateXmlFormatType = NEW;
            // Test for the unicity of the annotation ID being used
            // If the ID is not Unique, the method will throw an
            // exception
            testAnnotationIdUnicity(annot.getId());

            // Add the annotation
            currentAnnotationSet.add(annot.getId(), annot.getStart(), annot
                    .getEnd(), annot.getElemName(), annot.getFM());
          }
        }
        catch(gate.util.InvalidOffsetException e) {
          throw new GateSaxException(e);
        }// End try
      }// End while
      // The colector is empty and ready for the next AnnotationSet
      return;
    }// End if

  }// endElement

  /**
   * This method is called when the SAX parser encounts text in the XML
   * doc. Here we calculate the end indices for all the elements present
   * inside the stack and update with the new values.
   */
  public void characters(char[] text, int start, int length)
          throws SAXException {
    if(!readCharacterStatus) {
      contentBuffer = new StringBuffer(new String(text, start, length));
    }
    else {
      contentBuffer.append(new String(text, start, length));
    }
    readCharacterStatus = true;
  }

  /**
   * This method is called when all characters between specific tags
   * have been read completely
   */
  public void charactersAction(char[] text, int start, int length)
          throws SAXException {
    // Create a string object based on the reported text
    String content = new String(text, start, length);
    if("TextWithNodes".equals((String)currentElementStack.peek())) {
      processTextOfTextWithNodesElement(content);
      return;
    }// End if
    if("Name".equals((String)currentElementStack.peek())) {
      processTextOfNameElement(content);
      return;
    }// End if
    if("Value".equals((String)currentElementStack.peek())) {
      // if (currentFeatureName != null &&
      // "string".equals(currentFeatureName) &&
      // currentAnnot!= null &&
      // "Token".equals(currentAnnot.getElemName()) &&
      // currentAnnot.getEnd().longValue() == 1063)
      // System.out.println("Content=" + content + " start="+ start + "
      // length=" + length);
      processTextOfValueElement(content);
      return;
    }// End if
  }// characters

  /**
   * This method is called when the SAX parser encounts white spaces
   */
  public void ignorableWhitespace(char ch[], int start, int length)
          throws SAXException {
  }// ignorableWhitespace

  /**
   * Error method.We deal with this exception inside SimpleErrorHandler
   * class
   */
  public void error(SAXParseException ex) throws SAXException {
    // deal with a SAXParseException
    // see SimpleErrorhandler class
    _seh.error(ex);
  }// error

  /**
   * FatalError method.
   */
  public void fatalError(SAXParseException ex) throws SAXException {
    // deal with a SAXParseException
    // see SimpleErrorhandler class
    _seh.fatalError(ex);
  }// fatalError

  /**
   * Warning method comment.
   */
  public void warning(SAXParseException ex) throws SAXException {
    // deal with a SAXParseException
    // see SimpleErrorhandler class
    _seh.warning(ex);
  }// warning

  // Custom methods section

  /** This method deals with a AnnotationSet element. */
  private void processAnnotationSetElement(Attributes atts) {
    if(atts != null) {
      for(int i = 0; i < atts.getLength(); i++) {
        // Extract name and value
        String attName = atts.getLocalName(i);
        String attValue = atts.getValue(i);
        if("Name".equals(attName))
          currentAnnotationSet = doc.getAnnotations(attValue);
      }// End for
    }// End if
  }// processAnnotationSetElement

  /** This method deals with the start of a Name element */
  private void processNameElement(Attributes atts) {
    if(atts == null) return;
    currentFeatureKeyClassName = atts.getValue("className");
    currentFeatureKeyItemClassName = atts.getValue("itemClassName");
  }// End processNameElement();

  /** This method deals with the start of a Value element */
  private void processValueElement(Attributes atts) {
    if(atts == null) return;
    currentFeatureValueClassName = atts.getValue("className");
    currentFeatureValueItemClassName = atts.getValue("itemClassName");
  }// End processValueElement();

  /** This method deals with a Annotation element. */
  private void processAnnotationElement(Attributes atts) {
    if(atts != null) {
      currentAnnot = new AnnotationObject();
      for(int i = 0; i < atts.getLength(); i++) {
        // Extract name and value
        String attName = atts.getLocalName(i);
        String attValue = atts.getValue(i);

        if("Id".equals(attName)) currentAnnot.setId(new Integer(attValue));

        if("Type".equals(attName)) currentAnnot.setElemName(attValue);

        try {
          if("StartNode".equals(attName)) {
            Integer id = new Integer(attValue);
            Long offset = (Long)id2Offset.get(id);
            if(offset == null) {
              throw new GateRuntimeException("Couldn't found Node with id = "
                      + id + ".It was specified in annot " + currentAnnot
                      + " as a start node!"
                      + "Check the document with a text editor or something"
                      + " before trying again.");

            }
            else currentAnnot.setStart(offset);
          }// Endif
          if("EndNode".equals(attName)) {
            Integer id = new Integer(attValue);
            Long offset = (Long)id2Offset.get(id);
            if(offset == null) {
              throw new GateRuntimeException("Couldn't found Node with id = "
                      + id + ".It was specified in annot " + currentAnnot
                      + " as a end node!"
                      + "Check the document with a text editor or something"
                      + " before trying again.");
            }
            else currentAnnot.setEnd(offset);
          }// End if
        }
        catch(NumberFormatException e) {
          throw new GateRuntimeException("Offsets problems.Couldn't create"
                  + " Integers from" + " id[" + attValue + "]) in annot "
                  + currentAnnot
                  + "Check the document with a text editor or something,"
                  + " before trying again");
        }// End try
      }// End For
    }// End if
  }// processAnnotationElement

  /** This method deals with a Features element. */
  private void processFeatureElement(Attributes atts) {
    // The first time feature is calle it will create a features map.
    if(currentFeatureMap == null) currentFeatureMap = Factory.newFeatureMap();
  }// processFeatureElement

  /** This method deals with a Node element. */
  private void processNodeElement(Attributes atts) {
    if(atts != null) {
      for(int i = 0; i < atts.getLength(); i++) {
        // Extract name and value
        String attName = atts.getLocalName(i);
        String attValue = atts.getValue(i);
        // System.out.println("Node : " + attName + "=" +attValue);
        if("id".equals(attName)) {
          try {
            Integer id = new Integer(attValue);
            id2Offset.put(id, new Long(tmpDocContent.length()));
          }
          catch(NumberFormatException e) {
            throw new GateRuntimeException("Coudn't create a node from "
                    + attValue + " Expected an integer.");
          }// End try
        }// End if
      }// End for
    }// End if
  }// processNodeElement();

  /** This method deals with a Text belonging to TextWithNodes element. */
  private void processTextOfTextWithNodesElement(String text) {
    text = recoverNewLineSequence(text);
    tmpDocContent.append(text);
  }// processTextOfTextWithNodesElement

  /** Restore new line as in the original document if needed */
  private String recoverNewLineSequence(String text) {
    String result = text;

    // check for new line
    if(text.indexOf('\n') != -1) {
      String newLineType = (String)doc.getFeatures().get(
              GateConstants.DOCUMENT_NEW_LINE_TYPE);

      if("LF".equalsIgnoreCase(newLineType)) {
        newLineType = null;
      }

      // exit with the same text if the change isn't necessary
      if(newLineType == null) return result;

      String newLine = "\n";
      if("CRLF".equalsIgnoreCase(newLineType)) {
        newLine = "\r\n";
      }
      if("CR".equalsIgnoreCase(newLineType)) {
        newLine = "\r";
      }
      if("LFCR".equalsIgnoreCase(newLineType)) {
        newLine = "\n\r";
      }

      StringBuffer buff = new StringBuffer(text);
      int index = text.lastIndexOf('\n');
      while(index != -1) {
        buff.replace(index, index + 1, newLine);
        index = text.lastIndexOf('\n', index - 1);
      } // while
      result = buff.toString();
    } // if

    return result;
  } // recoverNewLineSequence(String text)

  /** This method deals with a Text belonging to Name element. */
  private void processTextOfNameElement(String text) throws GateSaxException {
    if(currentFeatureMap == null)
      throw new GateSaxException(
              "GATE xml format processing error:"
                      + " Found a Name element that is not enclosed into a Feature one while"
                      + " analyzing the annotation "
                      + currentAnnot
                      + "Please check the document with a text editor or something before"
                      + " trying again.");
    else {
      // In the entities case, characters() gets called separately for
      // each
      // entity so the text needs to be appended.
      if(currentFeatureName == null)
        currentFeatureName = text;
      else currentFeatureName = currentFeatureName + text;
    }// End If
  }// processTextOfNameElement();

  /** This method deals with a Text belonging to Value element. */
  private void processTextOfValueElement(String text) throws GateSaxException {
    if(currentFeatureMap == null)
      throw new GateSaxException(
              "GATE xml format processing error:"
                      + " Found a Value element that is not enclosed into a Feature one while"
                      + " analyzing the annotation "
                      + currentAnnot
                      + "Please check the document with a text editor or something before"
                      + " trying again.");
    else {
      // In the entities case, characters() gets called separately for
      // each
      // entity so the text needs to be appended.
      if(currentFeatureValue == null)
        currentFeatureValue = text;
      else currentFeatureValue = currentFeatureValue + text;
    }// End If
  }// processTextOfValueElement();

  /**
   * Creates a feature key using this information:
   * currentFeatureKeyClassName, currentFeatureKeyItemClassName,
   * currentFeatureName. See createFeatObject() method for more details.
   */
  private Object createFeatKey() {
    return createFeatObject(currentFeatureKeyClassName,
            currentFeatureKeyItemClassName, currentFeatureName);
  }// createFeatKey()

  /**
   * Creates a feature value using this information:
   * currentFeatureValueClassName, currentFeatureValueItemClassName,
   * currentFeatureValue. See createFeatObject() method for more
   * details.
   */
  private Object createFeatValue() {
    return createFeatObject(currentFeatureValueClassName,
            currentFeatureValueItemClassName, currentFeatureValue);
  }// createFeatValue()

  /**
   * This method tries to reconstruct an object given its class name and
   * its string representation. If the object is a Collection then the
   * items from its string representation must be separated by a ";". In
   * that case, the currentFeatureValueItemClassName is used to create
   * items belonging to this class.
   * 
   * @param aFeatClassName represents the name of the class of the feat
   *          object being created. If it is null then the
   *          javaLang.String will be used as default.
   * @param aFeatItemClassName is it used only if aFeatClassName is a
   *          collection.If it is null then java.lang.String will be
   *          used as default;
   * @param aFeatStringRepresentation sais it all
   * @return an Object created from aFeatClassName and its
   *         aFeatStringRepresentation. If not possible, then
   *         aFeatStringRepresentation is returned.
   * @throws GateRuntimeException If it can't create an item, that does
   *           not comply with its class definition, to add to the
   *           collection.
   */
  private Object createFeatObject(String aFeatClassName,
          String aFeatItemClassName, String aFeatStringRepresentation) {
    // If the string rep is null then the object will be null;
    if(aFeatStringRepresentation == null) return null;
    if(aFeatClassName == null) aFeatClassName = "java.lang.String";
    if(aFeatItemClassName == null) aFeatItemClassName = "java.lang.String";
    Class currentFeatClass = null;
    // look in the cache for existing
    // Class objects instead of recreating them
    currentFeatClass = (Class)classCache.get(aFeatClassName);
    if(currentFeatClass == null) {
      try {
        currentFeatClass = Gate.getClassLoader().loadClass(aFeatClassName);
      }
      catch(ClassNotFoundException cnfex) {
        return aFeatStringRepresentation;
      }// End try
      classCache.put(aFeatClassName, currentFeatClass);
    }
    if(java.util.Collection.class.isAssignableFrom(currentFeatClass)) {
      Class itemClass = null;
      Collection featObject = null;
      try {
        featObject = (Collection)currentFeatClass.newInstance();
        try {
          itemClass = Gate.getClassLoader().loadClass(aFeatItemClassName);
        }
        catch(ClassNotFoundException cnfex) {
          Out
                  .prln("Warning: Item class " + aFeatItemClassName
                          + " not found."
                          + "Adding items as Strings to the feature called \""
                          + currentFeatureName + "\" in the annotation "
                          + currentAnnot);
          itemClass = java.lang.String.class;
        }// End try
        // Let's detect if itemClass takes a constructor with a String
        // as param
        Class[] paramsArray = new Class[1];
        paramsArray[0] = java.lang.String.class;
        Constructor itemConstructor = null;
        boolean addItemAsString = false;
        try {
          itemConstructor = itemClass.getConstructor(paramsArray);
        }
        catch(NoSuchMethodException nsme) {
          addItemAsString = true;
        }
        catch(SecurityException se) {
          addItemAsString = true;
        }// End try
        StringTokenizer strTok = new StringTokenizer(aFeatStringRepresentation,
                ";");
        Object[] params = new Object[1];
        Object itemObj = null;
        while(strTok.hasMoreTokens()) {
          String itemStrRep = strTok.nextToken();
          if(addItemAsString)
            featObject.add(itemStrRep);
          else {
            params[0] = itemStrRep;
            try {
              itemObj = itemConstructor.newInstance(params);
            }
            catch(Exception e) {
              throw new GateRuntimeException("An item(" + itemStrRep
                      + ")  does not comply with its class" + " definition("
                      + aFeatItemClassName + ").Happened while tried to"
                      + " add feature: " + aFeatStringRepresentation
                      + " to the annotation " + currentAnnot);
            }// End try
            featObject.add(itemObj);
          }// End if
        }// End while
      }
      catch(InstantiationException instex) {
        return aFeatStringRepresentation;
      }
      catch(IllegalAccessException iae) {
        return aFeatStringRepresentation;
      }// End try
      return featObject;
    }// End if
    // If currentfeatClass is not a Collection,test to see if
    // it has a constructor that takes a String as param
    Class[] params = new Class[1];
    params[0] = java.lang.String.class;
    try {
      Constructor featConstr = currentFeatClass.getConstructor(params);
      Object[] featConstrParams = new Object[1];
      featConstrParams[0] = aFeatStringRepresentation;
      Object featObject = featConstr.newInstance(featConstrParams);
      return featObject;
    }
    catch(Exception e) {
      return aFeatStringRepresentation;
    }// End try
  }// createFeatObject()

  /**
   * This method tests if the Annotation ID has been used previously (in
   * which case will rase an exception) and also adds the ID being
   * tested to the annotationIdSet
   * 
   * @param anAnnotId An Integer representing an annotation ID to be
   *          tested
   * @throws GateSaxException if there is already an annotation wit the
   *           same ID
   */
  private void testAnnotationIdUnicity(Integer anAnnotId)
          throws GateSaxException {

    if(annotationIdSet.contains(anAnnotId))
      throw new GateSaxException("Found two or possibly more annotations with"
              + " the same ID! The offending ID was " + anAnnotId);
    else annotationIdSet.add(anAnnotId);
  }// End of testAnnotationIdUnicity()

  /**
   * This method is called when the SAX parser encounts a comment It
   * works only if the XmlDocumentHandler implements a
   * com.sun.parser.LexicalEventListener
   */
  public void comment(String text) throws SAXException {
  }// comment

  /**
   * This method is called when the SAX parser encounts a start of a
   * CDATA section It works only if the XmlDocumentHandler implements a
   * com.sun.parser.LexicalEventListener
   */
  public void startCDATA() throws SAXException {
  }// startCDATA

  /**
   * This method is called when the SAX parser encounts the end of a
   * CDATA section. It works only if the XmlDocumentHandler implements a
   * com.sun.parser.LexicalEventListener
   */
  public void endCDATA() throws SAXException {
  }// endCDATA

  /**
   * This method is called when the SAX parser encounts a parsed Entity
   * It works only if the XmlDocumentHandler implements a
   * com.sun.parser.LexicalEventListener
   */
  public void startParsedEntity(String name) throws SAXException {
  }// startParsedEntity

  /**
   * This method is called when the SAX parser encounts a parsed entity
   * and informs the application if that entity was parsed or not It's
   * working only if the CustomDocumentHandler implements a
   * com.sun.parser.LexicalEventListener
   */
  public void endParsedEntity(String name, boolean included)
          throws SAXException {
  }// endParsedEntity

  // StatusReporter Implementation

  /**
   * This methos is called when a listener is registered with this class
   */
  public void addStatusListener(StatusListener listener) {
    myStatusListeners.add(listener);
  }// addStatusListener

  /**
   * This methos is called when a listener is removed
   */
  public void removeStatusListener(StatusListener listener) {
    myStatusListeners.remove(listener);
  }// removeStatusListener

  /**
   * This methos is called whenever we need to inform the listener about
   * an event.
   */
  protected void fireStatusChangedEvent(String text) {
    Iterator listenersIter = myStatusListeners.iterator();
    while(listenersIter.hasNext())
      ((StatusListener)listenersIter.next()).statusChanged(text);
  }// fireStatusChangedEvent

  // XmlDocumentHandler member data

  /**
   * This constant indicates when to fire the status listener. This
   * listener will add an overhead and we don't want a big overhead. It
   * will be callled from ELEMENTS_RATE to ELEMENTS_RATE
   */
  final static int ELEMENTS_RATE = 128;

  /** This object indicates what to do when the parser encounts an error */
  private SimpleErrorHandler _seh = new SimpleErrorHandler();

  /** The content of the XML document, without any tag */
  private StringBuffer tmpDocContent = new StringBuffer("");

  /** A gate document */
  private gate.Document doc = null;

  /** Listeners for status report */
  protected List myStatusListeners = new LinkedList();

  /**
   * This reports the the number of elements that have beed processed so
   * far
   */
  private int elements = 0;

  /**
   * We need a colection to retain all the CustomObjects that will be
   * transformed into annotation over the gate document... At the end of
   * every annotation set read the objects in the colector are
   * transformed into annotations...
   */
  private List colector = null;

  /**
   * Maps nodes Ids to their offset in the document text. Those offsets
   * will be used when creating annotations
   */
  private Map id2Offset = new TreeMap();

  /** Holds the current element read. */
  private Stack currentElementStack = new Stack();

  /**
   * This inner objects maps an annotation object. When an annotation
   * from the xml document was read this structure is filled out
   */
  private AnnotationObject currentAnnot = null;

  /** A map holding current annotation's features */
  private FeatureMap currentFeatureMap = null;

  /** A key of the current feature */
  private String currentFeatureName = null;

  /** The value of the current feature */
  private String currentFeatureValue = null;

  /** The class name of the key in the current feature */
  private String currentFeatureKeyClassName = null;

  /**
   * If the key is a collection then we need to know the class name of
   * the items present in this collection. The next field holds just
   * that.
   */
  private String currentFeatureKeyItemClassName = null;

  /** The class name for the value in the current feature */
  private String currentFeatureValueClassName = null;

  /**
   * If the value is a collection then we need to know the class name of
   * the items present in this collection. The next field holds just
   * that.
   */
  private String currentFeatureValueItemClassName = null;

  /**
   * the current annotation set that is being created and filled with
   * annotations
   */
  private AnnotationSet currentAnnotationSet = null;

  /** An inner class modeling the information contained by an annotation. */
  class AnnotationObject {
    /** Constructor */
    public AnnotationObject() {
    }// AnnotationObject

    /** Accesor for the annotation type modeled here as ElemName */
    public String getElemName() {
      return elemName;
    }// getElemName

    /** Accesor for the feature map */
    public FeatureMap getFM() {
      return fm;
    }// getFM()

    /** Accesor for the start ofset */
    public Long getStart() {
      return start;
    }// getStart()

    /** Accesor for the end offset */
    public Long getEnd() {
      return end;
    }// getEnd()

    /** Mutator for the annotation type */
    public void setElemName(String anElemName) {
      elemName = anElemName;
    }// setElemName();

    /** Mutator for the feature map */
    public void setFM(FeatureMap aFm) {
      fm = aFm;
    }// setFM();

    /** Mutator for the start offset */
    public void setStart(Long aStart) {
      start = aStart;
    }// setStart();

    /** Mutator for the end offset */
    public void setEnd(Long anEnd) {
      end = anEnd;
    }// setEnd();

    /** Accesor for the id */
    public Integer getId() {
      return id;
    }// End of getId()

    /** Mutator for the id */
    public void setId(Integer anId) {
      id = anId;
    }// End of setId()

    public String toString() {
      return " [id =" + id + " type=" + elemName + " startNode=" + start
              + " endNode=" + end + " features=" + fm + "] ";
    }

    // Data fields
    private String elemName = null;

    private FeatureMap fm = null;

    private Long start = null;

    private Long end = null;

    private Integer id = null;
  } // AnnotationObject
}// GateFormatXmlDocumentHandler

