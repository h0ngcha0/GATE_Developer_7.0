/*
 *  Utils.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution annotationSet file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Johann Petrak, 2010-02-05
 *
 *  $Id: Main.java 12006 2009-12-01 17:24:28Z thomas_heitz $
 */

package gate;

import gate.annotation.AnnotationSetImpl;
import gate.annotation.ImmutableAnnotationSetImpl;
import gate.creole.ConditionalSerialController;
import gate.creole.RunningStrategy;
import gate.util.GateRuntimeException;
import gate.util.OffsetComparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Various utility methods to make often-needed tasks more easy and
 * using up less code.  In Java code (or JAPE grammars) you may wish to
 * <code>import static gate.Utils.*</code> to access these methods without
 * having to qualify them with a class name.  In Groovy code, this class can be
 * used as a category to inject each utility method into the class of its first
 * argument, e.g.
 * <pre>
 * Document doc = // ...
 * Annotation ann = // ...
 * use(gate.Utils) {
 *   println "Annotation has ${ann.length()} characters"
 *   println "and covers the string \"${doc.stringFor(ann)}\""
 * }
 * </pre>
 *
 * @author Johann Petrak, Ian Roberts
 */
public class Utils {
  /**
   * Return the length of the document content covered by an Annotation as an
   * int -- if the content is too long for an int, the method will throw
   * a GateRuntimeException. Use getLengthLong(SimpleAnnotation ann) if
   * this situation could occur.
   * @param ann the annotation for which to determine the length
   * @return the length of the document content covered by this annotation.
   */
  public static int length(SimpleAnnotation ann) {
    long len = lengthLong(ann);
    if (len > java.lang.Integer.MAX_VALUE) {
      throw new GateRuntimeException(
              "Length of annotation too big to be returned as an int: "+len);
    } else {
      return (int)len;
    }
  }

  /**
   * Return the length of the document content covered by an Annotation as a
   * long.
   * @param ann the annotation for which to determine the length
   * @return the length of the document content covered by this annotation.
   */
  public static long lengthLong(SimpleAnnotation ann) {
    return ann.getEndNode().getOffset() -
       ann.getStartNode().getOffset();
  }

  /**
   * Return the length of the document as an
   * int -- if the content is too long for an int, the method will throw a
   * GateRuntimeException. Use getLengthLong(Document doc) if
   * this situation could occur.
   * @param doc the document for which to determine the length
   * @return the length of the document content.
   */
  public static int length(Document doc) {
    long len = doc.getContent().size();
    if (len > java.lang.Integer.MAX_VALUE) {
      throw new GateRuntimeException(
              "Length of document too big to be returned as an int: "+len);
    } else {
      return (int)len;
    }
  }

  /**
   * Return the length of the document as a long.
   * @param doc the document for which to determine the length
   * @return the length of the document content.
   */
  public static long lengthLong(Document doc) {
    return doc.getContent().size();
  }

  /**
   * Return the DocumentContent corresponding to the annotation.
   * <p>
   * Note: the DocumentContent object returned will also contain the
   * original content which can be accessed using the getOriginalContent()
   * method.
   * @param doc the document from which to extract the content
   * @param ann the annotation for which to return the content.
   * @return a DocumentContent representing the content spanned by the annotation.
   */
  public static DocumentContent contentFor(
          SimpleDocument doc, SimpleAnnotation ann) {
    try {
      return doc.getContent().getContent(
              ann.getStartNode().getOffset(),
              ann.getEndNode().getOffset());
    } catch(gate.util.InvalidOffsetException ex) {
      throw new GateRuntimeException(ex.getMessage());
    }
  }

  /**
   * Return the document text as a String corresponding to the annotation.
   * @param doc the document from which to extract the document text
   * @param ann the annotation for which to return the text.
   * @return a String representing the text content spanned by the annotation.
   */
  public static String stringFor(
          Document doc, SimpleAnnotation ann) {
    try {
      return doc.getContent().getContent(
              ann.getStartNode().getOffset(),
              ann.getEndNode().getOffset()).toString();
    } catch(gate.util.InvalidOffsetException ex) {
      throw new GateRuntimeException(ex.getMessage());
    }
  }

  
  /**
   * Return the cleaned document text as a String corresponding to the annotation.
   * (Delete leading and trailing whitespace; normalize 
   * internal whitespace to single spaces.)
   * @param doc the document from which to extract the document text
   * @param ann the annotation for which to return the text.
   * @return a String representing the text content spanned by the annotation.
   */
  public static String cleanStringFor(Document doc, SimpleAnnotation ann) {
    return cleanString(stringFor(doc, ann));
  }
  
  /**
   * Returns the document text between the provided offsets.
   * @param doc the document from which to extract the document text
   * @param start the start offset 
   * @param end the end offset
   * @return document text between the provided offsets
   */
  public static String stringFor(
          Document doc, Long start, Long end) {
    try {
      return doc.getContent().getContent(
              start,
              end).toString();
    } catch(gate.util.InvalidOffsetException ex) {
      throw new GateRuntimeException(ex.getMessage());
    }
  }

  
  /**
   * Return the cleaned document text between the provided offsets.
   * (Delete leading and trailing whitespace; normalize 
   * internal whitespace to single spaces.)
   * @param doc the document from which to extract the document text
   * @param start the start offset 
   * @param end the end offset
   * @return document text between the provided offsets
   */
  public static String cleanStringFor(Document doc, Long start, Long end) {
    return cleanString(stringFor(doc, start, end));
  }
  
  /**
   * Return the DocumentContent covered by the given annotation set.
   * <p>
   * Note: the DocumentContent object returned will also contain the
   * original content which can be accessed using the getOriginalContent()
   * method.
   * @param doc the document from which to extract the content
   * @param anns the annotation set for which to return the content.
   * @return a DocumentContent representing the content spanned by the
   * annotation set.
   */
  public static DocumentContent contentFor(
          SimpleDocument doc, AnnotationSet anns) {
    try {
      return doc.getContent().getContent(
              anns.firstNode().getOffset(),
              anns.lastNode().getOffset());
    } catch(gate.util.InvalidOffsetException ex) {
      throw new GateRuntimeException(ex.getMessage());
    }
  }

  /**
   * Return the document text as a String covered by the given annotation set.
   * @param doc the document from which to extract the document text
   * @param anns the annotation set for which to return the text.
   * @return a String representing the text content spanned by the annotation
   * set.
   */
  public static String stringFor(
          Document doc, AnnotationSet anns) {
    try {
      return doc.getContent().getContent(
              anns.firstNode().getOffset(),
              anns.lastNode().getOffset()).toString();
    } catch(gate.util.InvalidOffsetException ex) {
      throw new GateRuntimeException(ex.getMessage());
    }
  }

  /**
   * Return the cleaned document text as a String covered by the given annotation set.
   * (Delete leading and trailing whitespace; normalize 
   * internal whitespace to single spaces.)
   * @param doc the document from which to extract the document text
   * @param anns the annotation set for which to return the text.
   * @return a String representing the text content spanned by the annotation
   * set.
   */
  public static String cleanStringFor(Document doc, AnnotationSet anns) {
    return cleanString(stringFor(doc, anns));
  }
  
  /**
   * Return a cleaned version of the input String. (Delete leading and trailing
   * whitespace; normalize internal whitespace to single spaces; return an
   * empty String if the input contains nothing but whitespace, but null
   * if the input is null.)
   * @return a cleaned version of the input String.
   */
  public static String cleanString(String input) {
    if (input == null) {
      return null;
    }
    // implied else
    return input.replaceAll("\\s+", " ").trim();
  }
  
  /**
   * Get the start offset of an annotation.
   */
  public static Long start(SimpleAnnotation a) {
    return (a.getStartNode() == null) ? null : a.getStartNode().getOffset();
  }

  /**
   * Get the start offset of an annotation set.
   */
  public static Long start(AnnotationSet as) {
    return (as.firstNode() == null) ? null : as.firstNode().getOffset();
  }

  /**
   * Get the start offset of a document (i.e. 0L).
   */
  public static Long start(SimpleDocument d) {
    return Long.valueOf(0L);
  }

  /**
   * Get the end offset of an annotation.
   */
  public static Long end(SimpleAnnotation a) {
    return (a.getEndNode() == null) ? null : a.getEndNode().getOffset();
  }

  /**
   * Get the end offset of an annotation set.
   */
  public static Long end(AnnotationSet as) {
    return (as.lastNode() == null) ? null : as.lastNode().getOffset();
  }

  /**
   * Get the end offset of a document.
   */
  public static Long end(SimpleDocument d) {
    return d.getContent().size();
  }

  /**
   * Return a the subset of annotations from the given annotation set
   * that start exactly at the given offset.
   *
   * @param annotationSet the set of annotations from which to select
   * @param atOffset the offset where the annoation to be returned should start
   * @return an annotation set containing all the annotations from the original
   * set that start at the given offset
   */
  public static AnnotationSet getAnnotationsAtOffset(
          AnnotationSet annotationSet, Long atOffset) {
    // this returns all annotations that start at this atOffset OR AFTER!
    AnnotationSet tmp = annotationSet.get(atOffset);
    // so lets filter ...
    AnnotationSet ret = new AnnotationSetImpl(annotationSet.getDocument());
    Iterator<Annotation> it = tmp.iterator();
    while(it.hasNext()) {
      Annotation ann = it.next();
      if(ann.getStartNode().getOffset().equals(atOffset)) {
        ret.add(ann);
      }
    }
    return ret;
  }

  /**
   * Get all the annotations from the source annotation set that lie within
   * the range of the containing annotation.
   * 
   * @param sourceAnnotationSet the annotation set from which to select
   * @param containingAnnotation the annotation whose range must contain the
   * selected annotations
   * @return the AnnotationSet containing all annotations fully contained in
   * the offset range of the containingAnnotation
   */
  public static AnnotationSet getContainedAnnotations(
    AnnotationSet sourceAnnotationSet,
    Annotation containingAnnotation) {
    return getContainedAnnotations(sourceAnnotationSet,containingAnnotation,"");
  }

  /**
   * Get all the annotations of type targetType
   * from the source annotation set that lie within
   * the range of the containing annotation.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param containingAnnotation the annotation whose range must contain the
   * @param targetType the type the selected annotations must have. If the
   * empty string, no filtering on type is done.
   * @return the AnnotationSet containing all annotations fully contained in
   * the offset range of the containingAnnotation
   */
  public static AnnotationSet getContainedAnnotations(
    AnnotationSet sourceAnnotationSet,
    Annotation containingAnnotation,
    String targetType) {
    if(targetType.equals("")) {
      return sourceAnnotationSet.getContained(
        containingAnnotation.getStartNode().getOffset(),
        containingAnnotation.getEndNode().getOffset());
    } else {
      return sourceAnnotationSet.getContained(
        containingAnnotation.getStartNode().getOffset(),
        containingAnnotation.getEndNode().getOffset()).get(targetType);
    }
  }

  /**
   * Get all the annotations from the source annotation set that lie within
   * the range of the containing annotation set, i.e. within the offset range
   * between the start of the first annotation in the containing set and the
   * end of the last annotation in the annotation set. If the containing
   * annotation set is empty, an empty set is returned.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param containingAnnotationSet the annotation set whose range must contain
   * the selected annotations
   * @return the AnnotationSet containing all annotations fully contained in
   * the offset range of the containingAnnotationSet
   */
  public static AnnotationSet getContainedAnnotations(
    AnnotationSet sourceAnnotationSet,
    AnnotationSet containingAnnotationSet) {
    return getContainedAnnotations(sourceAnnotationSet,containingAnnotationSet,"");
  }

  /**
   * Get all the annotations from the source annotation set with a type equal to
   * targetType that lie within
   * the range of the containing annotation set, i.e. within the offset range
   * between the start of the first annotation in the containing set and the
   * end of the last annotation in the annotation set. If the containing
   * annotation set is empty, an empty set is returned.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param containingAnnotationSet the annotation set whose range must contain
   * the selected annotations
   * @param targetType the type the selected annotations must have
   * @return the AnnotationSet containing all annotations fully contained in
   * the offset range of the containingAnnotationSet
   */
  public static AnnotationSet getContainedAnnotations(
    AnnotationSet sourceAnnotationSet,
    AnnotationSet containingAnnotationSet,
    String targetType) {
    if(containingAnnotationSet.isEmpty() || sourceAnnotationSet.isEmpty()) {
      return Factory.createImmutableAnnotationSet(sourceAnnotationSet.getDocument(), null);
    }
    if(targetType.equals("")) {
      return sourceAnnotationSet.getContained(
        containingAnnotationSet.firstNode().getOffset(),
        containingAnnotationSet.lastNode().getOffset());
    } else {
      return sourceAnnotationSet.getContained(
        containingAnnotationSet.firstNode().getOffset(),
        containingAnnotationSet.lastNode().getOffset()).get(targetType);
    }
  }

  
  /**
   * Get all the annotations from the source annotation set that cover
   * the range of the specified annotation.
   * 
   * @param sourceAnnotationSet the annotation set from which to select
   * @param coveredAnnotation the annotation whose range must equal or lie within
   * the selected annotations
   * @return the AnnotationSet containing all annotations that fully cover
   * the offset range of the coveredAnnotation
   */
  public static AnnotationSet getCoveringAnnotations(
    AnnotationSet sourceAnnotationSet,
    Annotation coveredAnnotation) {
    return getCoveringAnnotations(sourceAnnotationSet,coveredAnnotation,"");
  }

  /**
   * Get all the annotations of type targetType
   * from the source annotation set that cover
   * the range of the specified annotation.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param coveredAnnotation the annotation whose range must be covered
   * @param targetType the type the selected annotations must have. If the
   * empty string, no filtering on type is done.
   * @return the AnnotationSet containing all annotations that fully cover
   * the offset range of the coveredAnnotation
   */
  public static AnnotationSet getCoveringAnnotations(
    AnnotationSet sourceAnnotationSet,
    Annotation coveredAnnotation,
    String targetType) {
    return sourceAnnotationSet.getCovering(targetType,
        coveredAnnotation.getStartNode().getOffset(),
        coveredAnnotation.getEndNode().getOffset());
  }

  /**
   * Get all the annotations from the source annotation set that cover
   * the range of the specified annotation set. If the covered
   * annotation set is empty, an empty set is returned.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param coveredAnnotationSet the annotation set whose range must be covered by
   * the selected annotations
   * @return the AnnotationSet containing all annotations that fully cover
   * the offset range of the containingAnnotationSet
   */
  public static AnnotationSet getCoveringAnnotations(
    AnnotationSet sourceAnnotationSet,
    AnnotationSet coveredAnnotationSet) {
    return getCoveringAnnotations(sourceAnnotationSet,coveredAnnotationSet,"");
  }

  /**
   * Get all the annotations from the source annotation set with a type equal to
   * targetType that cover
   * the range of the specified annotation set. If the specified
   * annotation set is empty, an empty set is returned.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param coveredAnnotationSet the annotation set whose range must
   * be covered by the selected annotations
   * @param targetType the type the selected annotations must have
   * @return the AnnotationSet containing all annotations that fully cover
   * the offset range of the containingAnnotationSet
   */
  public static AnnotationSet getCoveringAnnotations(
    AnnotationSet sourceAnnotationSet,
    AnnotationSet coveredAnnotationSet,
    String targetType) {
    if(coveredAnnotationSet.isEmpty() || sourceAnnotationSet.isEmpty()) {
      return Factory.createImmutableAnnotationSet(sourceAnnotationSet.getDocument(), null);
    }
    return sourceAnnotationSet.getCovering(targetType,
        coveredAnnotationSet.firstNode().getOffset(),
        coveredAnnotationSet.lastNode().getOffset());
  }

  
  
  
  /**
   * Get all the annotations from the source annotation set that
   * partly or totally overlap
   * the range of the specified annotation.
   * 
   * @param sourceAnnotationSet the annotation set from which to select
   * @param overlappedAnnotation the annotation whose range the selected
   * annotations must overlap
   * @return the AnnotationSet containing all annotations that fully cover
   * the offset range of the coveredAnnotation
   */
  public static AnnotationSet getOverlappingAnnotations(
    AnnotationSet sourceAnnotationSet,
    Annotation overlappedAnnotation) {
    return getOverlappingAnnotations(sourceAnnotationSet,overlappedAnnotation,"");
  }

  /**
   * Get all the annotations of type targetType
   * from the source annotation set that partly or totally overlap
   * the range of the specified annotation.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param overlappedAnnotation the annotation whose range the selected
   * annotations must overlap
   * @param targetType the type the selected annotations must have. If the
   * empty string, no filtering on type is done.
   * @return the AnnotationSet containing all annotations that fully cover
   * the offset range of the coveredAnnotation
   */
  public static AnnotationSet getOverlappingAnnotations(
    AnnotationSet sourceAnnotationSet,
    Annotation overlappedAnnotation,
    String targetType) {
    
    
    if ( (targetType == null) || targetType.isEmpty()) {
      return sourceAnnotationSet.get(overlappedAnnotation.getStartNode().getOffset(),
          overlappedAnnotation.getEndNode().getOffset());
    }
    
    return sourceAnnotationSet.get(targetType,
        overlappedAnnotation.getStartNode().getOffset(),
        overlappedAnnotation.getEndNode().getOffset());
  }

  /**
   * Get all the annotations from the source annotation set that overlap
   * the range of the specified annotation set. If the overlapped
   * annotation set is empty, an empty set is returned.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param overlappedAnnotationSet the annotation set whose range must
   * be overlapped by the selected annotations
   * @return the AnnotationSet containing all annotations that fully cover
   * the offset range of the containingAnnotationSet
   */
  public static AnnotationSet getOverlappingAnnotations(
    AnnotationSet sourceAnnotationSet,
    AnnotationSet overlappedAnnotationSet) {
    return getOverlappingAnnotations(sourceAnnotationSet,overlappedAnnotationSet,"");
  }

  
  /**
   * Get all the annotations from the source annotation set with a type equal to
   * targetType that partly or completely overlap the range of the specified 
   * annotation set. If the specified annotation set is empty, an empty 
   * set is returned.
   *
   * @param sourceAnnotationSet the annotation set from which to select
   * @param overlappedAnnotationSet the annotation set whose range must
   * be overlapped by the selected annotations
   * @param targetType the type the selected annotations must have
   * @return the AnnotationSet containing all annotations that partly or fully
   * overlap the offset range of the containingAnnotationSet
   */
  public static AnnotationSet getOverlappingAnnotations(
    AnnotationSet sourceAnnotationSet,
    AnnotationSet overlappedAnnotationSet,
    String targetType) {
    if(overlappedAnnotationSet.isEmpty() || sourceAnnotationSet.isEmpty()) {
      return Factory.createImmutableAnnotationSet(sourceAnnotationSet.getDocument(), null);
    }
    
    if ( (targetType == null) || targetType.isEmpty()) {
      return sourceAnnotationSet.get(overlappedAnnotationSet.firstNode().getOffset(),
          overlappedAnnotationSet.lastNode().getOffset());
    }
    
    return sourceAnnotationSet.get(targetType,
        overlappedAnnotationSet.firstNode().getOffset(),
        overlappedAnnotationSet.lastNode().getOffset());
  }


  /**
   * Return a List containing the annotations in the given annotation set, in
   * document order (i.e. increasing order of start offset).
   *
   * @param as the annotation set
   * @return a list containing the annotations from <code>as</code> in document
   * order.
   */
  public static List<Annotation> inDocumentOrder(AnnotationSet as) {
    List<Annotation> ret = new ArrayList<Annotation>();
    if(as != null) {
      ret.addAll(as);
      Collections.sort(ret, OFFSET_COMPARATOR);
    }
    return ret;
  }

  /**
   * A single instance of {@link OffsetComparator} that can be used by any code
   * that requires one.
   */
  public static final OffsetComparator OFFSET_COMPARATOR =
          new OffsetComparator();

  /**
   * Create a feature map from an array of values.  The array must have an even
   * number of items, alternating keys and values i.e. [key1, value1, key2,
   * value2, ...].
   *
   * @param values an even number of items, alternating keys and values.
   * @return a feature map containing the given items.
   */
  public static FeatureMap featureMap(Object... values) {
    FeatureMap fm = Factory.newFeatureMap();
    if(values != null) {
      for(int i = 0; i < values.length; i++) {
        fm.put(values[i], values[++i]);
      }
    }
    return fm;
  }

  /**
   * Create a feature map from an existing map (typically one that does not
   * itself implement FeatureMap).
   *
   * @param map the map to convert.
   * @return a new FeatureMap containing the same mappings as the source map.
   */
  public static FeatureMap toFeatureMap(Map<?,?> map) {
    FeatureMap fm = Factory.newFeatureMap();
    fm.putAll(map);
    return fm;
  }
  
  /** 
   * This method can be used to check if a ProcessingResource has 
   * a chance to be run in the given controller with the current settings.
   * <p>
   * That means that for a non-conditional controller, the method will return
   * true if the PR is part of the controller. For a conditional controller,
   * the method will return true if it is part of the controller and at least
   * once (if the same PR is contained multiple times) it is not disabled.
   * 
   * @param controller 
   * @param pr
   * @return true or false depending on the conditions explained above.
   */
  public static boolean isEnabled(Controller controller, ProcessingResource pr) {
    Collection<ProcessingResource> prs = controller.getPRs();
    if(!prs.contains(pr)) {
      return false;
    }
    if(controller instanceof ConditionalSerialController) {
      Collection<RunningStrategy> rss = 
        ((ConditionalSerialController)controller).getRunningStrategies();
      for(RunningStrategy rs : rss) {
        // if we find at least one occurrence of the PR that is not disabled
        // return true
        if(rs.getPR().equals(pr) && 
           rs.getRunMode() != RunningStrategy.RUN_NEVER) {
          return true;
        }
      }
      // if we get here, no occurrence of the PR has found or none that
      // is not disabled, so return false
      return false;
    }
    return true;
  }
  
  /**
   * Return the running strategy of the PR in the controller, if the controller
   * is a conditional controller. If the controller is not a conditional 
   * controller, null is returned. If the controller is a conditional controller
   * and the PR is contained multiple times, the running strategy for the 
   * first occurrence the is found is returned.
   * 
   * @param controller
   * @param pr
   * @return A RunningStrategy object or null
   */
  public static RunningStrategy getRunningStrategy(Controller controller,
          ProcessingResource pr) {
    if(controller instanceof ConditionalSerialController) {
      Collection<RunningStrategy> rss = 
        ((ConditionalSerialController)controller).getRunningStrategies();
      for(RunningStrategy rs : rss) {
        if(rs.getPR() == pr) {
          return rs;
        }
      } 
    }
    return null;
  }
  
  /**
   * Issue a message to the log but only if the same message has not
   * been logged already in the same GATE session.
   * This is intended for explanations or warnings that should not be 
   * repeated every time the same situation occurs.
   * 
   * @param logger - the logger instance to use
   * @param level  - the severity level for the message
   * @param message - the message itself
   */
  public static void logOnce (Logger logger, Level level, String message) {
    if(!alreadyLoggedMessages.contains(message)) { 
      logger.log(level, message);
      alreadyLoggedMessages.add(message);
    }
  }

  /**
   * Check if a message has already been logged or shown. This does not log
   * or show anything but only stores the message as one that has been shown
   * already if necessary and returns if the message has been shown or not.
   *
   * @param message - the message that should only be logged or shown once
   * @return - true if the message has already been logged or checked with
   * this method.
   *
   */
   public static boolean isLoggedOnce(String message) {
     boolean isThere = alreadyLoggedMessages.contains(message);
     if(!isThere) {
       alreadyLoggedMessages.add(message);
     }
     return isThere;
   }

  private static final Set<String> alreadyLoggedMessages = 
    Collections.synchronizedSet(new HashSet<String>());

}
