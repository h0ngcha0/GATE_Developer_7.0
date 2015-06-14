package gate.creole.orthomatcher.SampleOrthoMatcher;

import static gate.creole.ANNIEConstants.ANNOTATION_COREF_FEATURE_NAME;
import static gate.creole.ANNIEConstants.LOOKUP_ANNOTATION_TYPE;
import static gate.creole.ANNIEConstants.PERSON_GENDER_FEATURE_NAME;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ExecutionException;
import gate.creole.orthomatcher.AnnotationOrthography;
import gate.creole.orthomatcher.OrthoMatcherHelper;
import gate.util.Err;
import gate.util.InvalidOffsetException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/*
 * This sample orthography shows you how to create your own orthography.
 * Those methods that you do not need to change can use the code from the BasicAnnotationOrthography.
 * This sample othography copies the behavior of the default one - BasicAnnotationOrthography.
 */
public class SampleAnnotationOrthography implements gate.creole.orthomatcher.AnnotationOrthography {
    
  private final String personType;
  private final AnnotationOrthography defaultOrthography;
  private final boolean extLists;
  
  public SampleAnnotationOrthography(String personType, boolean extLists,
      AnnotationOrthography defaultOrthography) {
    this.personType = personType;
    this.defaultOrthography = defaultOrthography;
    this.extLists = extLists;
  }

  public String getStringForAnnotation(Annotation a, gate.Document d)
      throws ExecutionException {

    return defaultOrthography.getStringForAnnotation(a,d);
  }
  
  public String stripPersonTitle (String annotString, Annotation annot, Document doc, Map<Integer, List<Annotation>> tokensMap, HashMap normalizedTokensMap,AnnotationSet nameAllAnnots)
    throws ExecutionException {
        return defaultOrthography.stripPersonTitle(annotString,annot,doc,tokensMap,normalizedTokensMap,nameAllAnnots);
    }
  
  public boolean matchedAlready(Annotation annot1, Annotation annot2,List matchesDocFeature,AnnotationSet nameAllAnnots) {
        return defaultOrthography.matchedAlready(annot1,annot2,matchesDocFeature,nameAllAnnots);
    }

    public void updateMatches(Annotation newAnnot, Annotation prevAnnot,List matchesDocFeature,AnnotationSet nameAllAnnots) {
             defaultOrthography.updateMatches(newAnnot, prevAnnot,matchesDocFeature,nameAllAnnots);
    } 
    
    public HashSet buildTables(AnnotationSet nameAllAnnots) {

      return defaultOrthography.buildTables(nameAllAnnots);
    }

  public boolean allNonStopTokensInOtherAnnot(ArrayList<Annotation> arg0,
      ArrayList<Annotation> arg1, String arg2, boolean arg3) {
    
    return defaultOrthography.allNonStopTokensInOtherAnnot(arg0, arg1, arg2, arg3);
  }

  public boolean fuzzyMatch(String arg1,
      String arg2) {
    
    return defaultOrthography.fuzzyMatch(arg1, arg2);
  }

  public Annotation updateMatches(Annotation newAnnot, String annotString,HashMap processedAnnots,AnnotationSet nameAllAnnots,List matchesDocFeature) {
    
    return defaultOrthography.updateMatches(newAnnot, annotString, processedAnnots,nameAllAnnots,matchesDocFeature);
  }

  public boolean isUnknownGender(String arg0) {
    
    return defaultOrthography.isUnknownGender(arg0);
  }

}
