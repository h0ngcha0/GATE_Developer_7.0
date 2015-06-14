/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 08/05/2008
 *
 *  $Id: RealtimeCorpusController.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */

package gate.creole;

import gate.Document;
import gate.Executable;
import gate.Factory;
import gate.LanguageAnalyser;
import gate.ProcessingResource;
import gate.Resource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.util.Err;
import gate.util.Out;
import gate.util.profile.Profiler;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

/**
 * A custom GATE controller that interrupts the execution over a document when a
 * specified amount of time has elapsed. It also ignores all errors/exceptions 
 * that may occur during execution and simply carries on with the next document
 * when that happens.
 */
@CreoleResource(name = "Real-Time Corpus Pipeline",
    comment = "A serial controller for PR pipelines over corpora which "
        + "limits the run time of each PR.",
    icon = "application-realtime",
    helpURL = "http://gate.ac.uk/userguide/sec:creole-model:applications")
public class RealtimeCorpusController extends SerialAnalyserController {
	
  private final static boolean DEBUG = false;
  
  /**
   * Shared logger object.
   */
  private static final Logger logger = Logger.getLogger(
          RealtimeCorpusController.class);

  /** Profiler to track PR execute time */
  protected Profiler prof;
  protected HashMap<String,Long> timeMap;
  
  /**
   * An executor service used to execute the PRs over the document .
   */
  protected ExecutorService threadSource;
  
  /**
   * The tread currently running the document processing.
   */
  protected volatile Thread currentWorkingThread;
  
  public RealtimeCorpusController(){
    super();
    if(DEBUG) {
      prof = new Profiler();
      prof.enableGCCalling(false);
      prof.printToSystemOut(true);
      timeMap = new HashMap<String,Long>();
    }
  }
  
  protected class DocRunner implements Callable<Object>{
    
    public DocRunner(Document document) {
      this.document = document;
    }
    
    public Object call() {
      try {
        // save a reference to the executor thread
        currentWorkingThread = Thread.currentThread();
        // run the system over the current document
        // set the doc and corpus
        for(int j = 0; j < prList.size(); j++) {
          ((LanguageAnalyser)prList.get(j)).setDocument(document);
          ((LanguageAnalyser)prList.get(j)).setCorpus(corpus);
        }
        interrupted = false;
        // execute the PRs
        // check all the PRs have the right parameters
        checkParameters();
        if(DEBUG) {
          prof.initRun("Execute controller [" + getName() + "]");
        }

        // execute all PRs in sequence
        interrupted = false;
        for(int j = 0; j < prList.size(); j++) {
          if(isInterrupted())
            throw new ExecutionInterruptedException("The execution of the "
                    + getName() + " application has been abruptly interrupted!");

          if(Thread.currentThread().isInterrupted()) {
            Err.println("Execution on document " + document.getName()
                    + " has been stopped");
            break;
          }

          try {
            runComponent(j);
          }
          catch(Throwable e) {
            if(!Thread.currentThread().isInterrupted()) throw e;

            Err.println("Execution on document " + document.getName()
                    + " has been stopped");
            break;
          }

          if(DEBUG) {
            prof.checkPoint("~Execute PR ["
                    + ((ProcessingResource)prList.get(j)).getName() + "]");
            Long timeOfPR = timeMap.get(((ProcessingResource)prList.get(j))
                    .getName());
            if(timeOfPR == null)
              timeMap.put(((ProcessingResource)prList.get(j)).getName(),
                      new Long(prof.getLastDuration()));
            else timeMap.put(((ProcessingResource)prList.get(j)).getName(),
                    new Long(timeOfPR.longValue() + prof.getLastDuration()));
            Out.println("Time taken so far by "
                    + ((ProcessingResource)prList.get(j)).getName()
                    + ": "
                    + timeMap.get(((ProcessingResource)prList.get(j)).getName()));
          }
        }
      }
      catch(ThreadDeath td) {
        // special case as we need to re-throw this one
        Err.prln("Execution on document " + document.getName()
                + " has been stopped");
        throw (td);
      }
      catch(Throwable cause) {
        Err.prln("Execution on document " + document.getName()
                + " has caused an error:\n=========================");
        cause.printStackTrace(Err.getPrintWriter());
        Err.prln("=========================\nError ignored...\n");
      }
      finally {
        // remove the reference to the thread, as we're now done
        currentWorkingThread = null;
        // unset the doc and corpus
        for(int j = 0; j < prList.size(); j++) {
          ((LanguageAnalyser)prList.get(j)).setDocument(null);
          ((LanguageAnalyser)prList.get(j)).setCorpus(null);
        }

        if(DEBUG) {
          prof.checkPoint("Execute controller [" + getName() + "] finished");
        }
      }
      
      return null;
    }
    private Document document;
  }
  
  @Override
  public void cleanup() {
    threadSource.shutdownNow();
    super.cleanup();
  }

  @Override
  public Resource init() throws ResourceInstantiationException {
    // we normally require 2 threads: one to execute the PRs and another one to
    // to execute the job stoppers. More threads are created as required.  We
    // use a custom ThreadFactory that returns daemon threads so we don't block
    // GATE from exiting if this controller has not been properly disposed of.
    threadSource = Executors.newSingleThreadExecutor(new ThreadFactory() {
      private ThreadFactory dtf = Executors.defaultThreadFactory();
      public Thread newThread(Runnable r) {
        Thread t = dtf.newThread(r);
        t.setDaemon(true);
        return t;
      }
    });
    return super.init();
  }

  /** Run the Processing Resources in sequence. */
  @SuppressWarnings("deprecation")
  public void executeImpl() throws ExecutionException{
    interrupted = false;
    if(corpus == null) throw new ExecutionException(
      "(SerialAnalyserController) \"" + getName() + "\":\n" +
      "The corpus supplied for execution was null!");
    //iterate through the documents in the corpus
    for(int i = 0; i < corpus.size(); i++){
      if(isInterrupted()) throw new ExecutionInterruptedException(
        "The execution of the " + getName() +
        " application has been abruptly interrupted!");

      boolean docWasLoaded = corpus.isDocumentLoaded(i);
      Document doc = (Document)corpus.get(i);
      // start the execution, in the separate thread
      Future<?> docRunnerFuture = threadSource.submit(new DocRunner(doc));
      // how long have we already waited 
      long waitSoFar = 0;
      // check if we should use graceful stop first 
      if (graceful != -1 && (timeout == -1 || graceful < timeout )) {
        try {
          docRunnerFuture.get(graceful, TimeUnit.MILLISECONDS);
        } catch(TimeoutException e) {
          // we waited the graceful period, and the task did not finish
          // -> interrupt the job (nicely)
          waitSoFar += graceful;
          logger.info("Execution timeout, attempting to gracefully stop worker thread...");
          // interrupt the working thread - we can't cancel the future as
          // that would cause future get() calls to fail immediately with
          // a CancellationException
          Thread t = currentWorkingThread;
          if(t != null) {
            t.interrupt();
          }
          for(int j = 0; j < prList.size(); j++){
            ((Executable)prList.get(j)).interrupt();
          }
          // next check scheduled for 
          // - half-time between graceful and timeout, or
          // - graceful-and-a-half (if no timeout)
          long waitTime = (timeout != -1) ? 
                          (timeout - graceful) / 2 : 
                          (graceful / 2);
          try {
            docRunnerFuture.get(waitTime, TimeUnit.MILLISECONDS);
          } catch(TimeoutException e1) {
            // the mid point has been reached: try nullify
            waitSoFar += waitTime;
            logger.info("Execution timeout, attempting to induce exception in order to stop worker thread...");
            for(int j = 0; j < prList.size(); j++){
              ((LanguageAnalyser)prList.get(j)).setDocument(null);
              ((LanguageAnalyser)prList.get(j)).setCorpus(null);
            }            
          } catch(InterruptedException e1) {
            // the current thread (not the execution thread!) was interrupted
            // throw it forward
            Thread.currentThread().interrupt();
          } catch(java.util.concurrent.ExecutionException e2) {
            throw new ExecutionException(e2);
          }
        } catch(java.util.concurrent.ExecutionException e) {
          throw new ExecutionException(e);
        } catch(InterruptedException e) {
          // the current thread (not the execution thread!) was interrupted
          // throw it forward
          Thread.currentThread().interrupt();
        }
      }
      // wait before we call stop()
      if(timeout != -1) {
        long waitTime = timeout - waitSoFar;
        if(waitTime > 0) {
          try {
            docRunnerFuture.get(waitTime, TimeUnit.MILLISECONDS);
          } catch(TimeoutException e) {
            // we're out of time: stop the thread
            logger.info("Execution timeout, worker thread will be forcibly terminated!");
            // using a volatile variable instead of synchronisation
            Thread theThread = currentWorkingThread;
            if(theThread != null) theThread.stop();
          } catch(InterruptedException e) {
            // the current thread (not the execution thread!) was interrupted
            // throw it forward
            Thread.currentThread().interrupt();
          } catch(java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(e);
          }
        } else {
          // stop now!
          logger.info("Execution timeout, worker thread will be forcibly terminated!");
          // using a volatile variable instead of synchronisation
          Thread theThread = currentWorkingThread;
          if(theThread != null) theThread.stop();
        }
      }
      
      // at this point we finished execution (one way or another)
      if(!docWasLoaded){
        //trigger saving
        getCorpus().unloadDocument(doc);
        //close the previously unloaded Doc
        Factory.deleteResource(doc);
      }
    }
  }
  
  
  /**
   * The timeout in milliseconds before execution on a document is 
   * forcibly stopped (forcibly stopping execution may result in memory leaks 
   * and/or unexpected behaviour).   
   */
  protected Long timeout;

  /**
   * Gets the timeout in milliseconds before execution on a document is 
   * forcibly stopped (forcibly stopping execution may result in memory leaks 
   * and/or unexpected behaviour).
   */
  public Long getTimeout() {
    return timeout;
  }
  
  
  /**
   * Sets the timeout in milliseconds before execution on a document is 
   * forcibly stopped (forcibly stopping execution may result in memory leaks 
   * and/or unexpected behaviour).
   * @param timeout in milliseconds before execution is forcibly stopped
   */
  @CreoleParameter(defaultValue = "60000",
      comment = "Timeout in milliseconds before execution on a document is forcibly stopped (forcibly stopping execution may result in memory leaks and/or unexpected behaviour)")
  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }
  
  /**
   * The timeout in milliseconds before execution on a document is 
   * gracefully stopped. Defaults to -1 which disables this functionality and 
   * relies, as previously, on forcibly stopping execution.
   */
  protected Long graceful;

  /**
   * Gets the timeout in milliseconds before execution on a document is 
   * gracefully stopped. Defaults to -1 which disables this functionality and 
   * relies, as previously, on forcibly stopping execution.
   */
  public Long getGracefulTimeout() {
    return graceful;
  }

  /**
   * Sets the timeout in milliseconds before execution on a document is 
   * gracefully stopped. Defaults to -1 which disables this functionality and 
   * relies, as previously, on forcibly stopping execution.
   * @param graceful timeout in milliseconds before execution is gracefully stopped
   */
  @CreoleParameter(defaultValue = "-1",
      comment = "Timeout in milliseconds before execution on a document is gracefully stopped. Defaults to -1 which disables this functionality and relies, as previously, on forcibly stoping execution.")
  public void setGracefulTimeout(Long graceful) {
    this.graceful = graceful;
  }

  /**
   * Sleep time in milliseconds while waiting for worker thread to finish.
   */
  private static final int POLL_INTERVAL = 50;
}
