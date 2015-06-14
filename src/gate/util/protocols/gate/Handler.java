package gate.util.protocols.gate;

import java.io.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.Iterator;
import java.util.Map;
import gate.GateConstants;
import gate.util.Files;


/**
 * The handler for the "gate://" URLs.
 * All this class does is to transparently transform a "gate://" URL into
 * an URL of the corresponding type and forward all requests through it.
 */
public class Handler extends URLStreamHandler {

  protected URLConnection openConnection(URL u) throws java.io.IOException {
    URL actualURL = Handler.class.getResource(
                      Files.getResourcePath() + u.getPath()
                    );
    if(actualURL == null){
      System.out.println(
        "WARNING: Starting with the GATE v.3 release the gate:// URLs have " +
        "been deprecated. All resources used by processing resources are now " +
        "external to the GATE distribution.\n" +
        "You should rebuild your application!\n" + 
        "The system will try to guess the location but " +
        "there are no guarantees:");
      //try to fix the problem if caused by the externalisation of GATE 
      //resources
      File pluginHome = new File(
              System.getProperty(GateConstants.GATE_HOME_PROPERTY_NAME), 
              "plugins");
      Iterator pathIter = conversionMap.keySet().iterator();
      while(pathIter.hasNext()){
        String aPath = (String)pathIter.next();
        if(u.getPath().startsWith(aPath)){
          String oldPath = u.getPath();
          String newPath = oldPath.replaceFirst(aPath, 
                  (String)conversionMap.get(aPath));
          actualURL = new File(pluginHome, newPath).toURI().toURL();
          System.out.println(u.toExternalForm() + " ---> " + 
                  actualURL.toExternalForm() + "\n");
          return actualURL.openConnection();
        }
      }
    }
    if(actualURL == null) throw new FileNotFoundException(u.toExternalForm());
    return actualURL.openConnection();
  }
  
  static Map conversionMap;
  static{
    conversionMap = new HashMap();
    conversionMap.put("/creole/BengaliNE", "ANNIE/resources/BengaliNE");
    conversionMap.put("/creole/chunker/VP", "ANNIE/resources/VP/");
    conversionMap.put("/creole/gazeteer", "ANNIE/resources/gazetteer/");
    conversionMap.put("/creole/heptag", "ANNIE/resources/heptag/");
    conversionMap.put("/creole/morph", "Tools/resources/morph/");
    conversionMap.put("/creole/namematcher", "ANNIE/resources/othomatcher/");
    conversionMap.put("/creole/ontology", "Ontology_Tools/resources/");
    conversionMap.put("/creole/splitter", "ANNIE/resources/sentenceSplitter/");
    conversionMap.put("/creole/tokeniser", "ANNIE/resources/tokeniser/");
    conversionMap.put("/creole/transducer/NE", "ANNIE/resources/NE/");
  }
}
