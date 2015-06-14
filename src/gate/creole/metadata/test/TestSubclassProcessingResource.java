package gate.creole.metadata.test;

import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.*;
import java.util.List;
import java.net.URL;

@CreoleResource(name = "Subclass PR")
public class TestSubclassProcessingResource extends TestSuperclassProcessingResource {

  // override firstParameter to not be runtime and add a default
  @RunTime(false)
  @CreoleParameter(defaultValue = "default/value")
  public void setFirstParameter(URL value) {
    super.setFirstParameter(value);
  }

  // hide thirdParameter
  @HiddenCreoleParameter
  public void setThirdParameter(Integer value) {
    super.setThirdParameter(value);
  }

  // hide corpus parameter from LanguageAnalyser
  @HiddenCreoleParameter
  public void setCorpus(gate.Corpus c) {
    super.setCorpus(c);
  }

  @CreoleParameter
  public void setFourthParameter(Boolean value) {
  }
}
