package gate.creole.orthomatcher;

import java.util.HashMap;

import gate.Annotation;


/**
 * RULE #10: is one name the reverse of the other
 * reversing around prepositions only?
 * e.g. "Department of Defence" == "Defence Department"
 * Condition(s): case-sensitive match
 * Applied to: organisation annotations only
 */

public class MatchRule10 implements OrthoMatcherRule {

	OrthoMatcher orthomatcher;
		
	public MatchRule10(OrthoMatcher orthmatcher){
			this.orthomatcher=orthmatcher;
	}
	
	public boolean value(String s1, String s2) {
		
	    boolean result=false;
	  
		  String token = null;
	    String previous_token = null;
	    String next_token = null;
	    boolean invoke_rule=false;

	    if (orthomatcher.tokensLongAnnot.size() >= 3
	            && orthomatcher.tokensShortAnnot.size() >= 2) {

	      // first get the tokens before and after the preposition
	      int i = 0;
	      for (; i< orthomatcher.tokensLongAnnot.size(); i++) {
	        token = (String)
	        ((Annotation) orthomatcher.tokensLongAnnot.get(i)).getFeatures().get(orthomatcher.TOKEN_STRING_FEATURE_NAME);
	        if (orthomatcher.prepos.containsKey(token)) {
	          invoke_rule=true;
	          break;
	        }//if
	        previous_token = token;
	      }//while

	      if (! invoke_rule)
	        result = false;
	      else {
    	      if (i < orthomatcher.tokensLongAnnot.size()
    	              && previous_token != null) {
    	        next_token= (String)
    	        ((Annotation) orthomatcher.tokensLongAnnot.get(i++)).getFeatures().get(orthomatcher.TOKEN_STRING_FEATURE_NAME);
    	        
    	        String s21 = (String)
              ((Annotation) orthomatcher.tokensShortAnnot.get(0)).getFeatures().get(orthomatcher.TOKEN_STRING_FEATURE_NAME);
              String s22 = (String)
              ((Annotation) orthomatcher.tokensShortAnnot.get(1)).getFeatures().get(orthomatcher.TOKEN_STRING_FEATURE_NAME);
              // then compare (in reverse) with the first two tokens of s2
              if (OrthoMatcherHelper.straightCompare(next_token,(String) s21,orthomatcher.caseSensitive)
                      && OrthoMatcherHelper.straightCompare(previous_token, s22,orthomatcher.caseSensitive))
                result = true ;
    	        }
    	      else result = false;
	      }
	    }//if (tokensLongAnnot.countTokens() >= 3
	    
	    if (result) OrthoMatcherHelper.usedRule(10);
	    return result;
	}
	
  public String getId(){
    return "MatchRule10";
  }
}
