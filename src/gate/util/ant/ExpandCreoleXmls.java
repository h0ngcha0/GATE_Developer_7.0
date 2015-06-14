package gate.util.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import gate.Gate;
import gate.creole.CreoleAnnotationHandler;
import gate.creole.metadata.CreoleResource;
import gate.util.GateException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Ant task to take a bunch of creole.xml files, process the
 * {@link CreoleResource} annotations on their resources, and write the
 * augmented XML to a target directory.
 */
public class ExpandCreoleXmls extends Task {

  private static boolean gateInited = false;
  
  private List<FileSet> srcFiles = new ArrayList<FileSet>();
  
  private File toDir;
  
  private SAXBuilder builder = new SAXBuilder();
  
  private XMLOutputter outputter = new XMLOutputter();
  
  public void addFileset(FileSet fs) {
    srcFiles.add(fs);
  }
  
  public void setTodir(File toDir) {
    this.toDir = toDir;
  }
  
  @Override
  public void execute() throws BuildException {
    if(toDir == null) {
      throw new BuildException("Please specify a destination directory using todir", getLocation());
    }
    if(toDir.isFile()) {
      throw new BuildException("Destination already exists and is not a directory", getLocation());
    }

    if(!gateInited) {
      try {
        Gate.init();
      }
      catch(GateException e) {
        throw new BuildException("Error initialising GATE", e, getLocation());
      }
    }
    for(FileSet fs : srcFiles) {
      DirectoryScanner ds = fs.getDirectoryScanner(getProject());
      for(String f : ds.getIncludedFiles()) {
        File creoleFile = new File(ds.getBasedir(), f);
        try {
          File plugin = creoleFile.getParentFile();
          File destFile = new File(toDir, f);
          File destPlugin = destFile.getParentFile();

          log("Expanding " + creoleFile + " to " + destFile, Project.MSG_VERBOSE);
          Gate.addKnownPlugin(plugin.toURI().toURL());
          CreoleAnnotationHandler annotationHandler = new CreoleAnnotationHandler(creoleFile.toURI().toURL());
          Document creoleDoc = builder.build(creoleFile);
          annotationHandler.addJarsToClassLoader(creoleDoc);
          annotationHandler.createResourceElementsForDirInfo(creoleDoc);
          annotationHandler.processAnnotations(creoleDoc);
          
          destPlugin.mkdirs();
          FileOutputStream fos = new FileOutputStream(destFile);
          try {
            outputter.output(creoleDoc, fos);
          }
          finally {
            fos.close();
          }
        }
        catch(Exception e) {
          log("Error processing " + creoleFile + ", skipped", Project.MSG_WARN);
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          log(sw.toString(), Project.MSG_VERBOSE);
        }
      }
    }
  }

  public void setGateHome(File gateHome) {
    Gate.setGateHome(gateHome);
  }
  
  public void setPluginsHome(File pluginsHome) {
    Gate.setPluginsHome(pluginsHome);
  }

}
