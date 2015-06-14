package gate.creole.orthomatcher;

import java.util.HashMap;

/**
 * RULE #2: if the two names are listed as equivalent in the
 * lookup table (alias) then they match
 * Condition(s): -
 * Applied to: all name annotations
 */
public class MatchRule2 implements OrthoMatcherRule {

  OrthoMatcher orthomatcher;
	
	public MatchRule2(OrthoMatcher orthmatcher){
		this.orthomatcher=orthmatcher;
	}
	
	public boolean value(String s1, String s2) {

	  boolean result=false;
	  
	    if (orthomatcher.alias.containsKey(s1) && orthomatcher.alias.containsKey(s2)) {
	      if (orthomatcher.alias.get(s1).toString().equals(orthomatcher.alias.get(s2).toString())) {
	        if (orthomatcher.log.isDebugEnabled()) {
	          orthomatcher.log.debug("rule 2 matched " + s1 + " to " + s2);
	        }
	        result=true;
	      }
	    }

	    if(result) OrthoMatcherHelper.usedRule(2);
	    
	    return result;
	  }
	
  public String getId(){
    return "MatchRule2";
  }
}
