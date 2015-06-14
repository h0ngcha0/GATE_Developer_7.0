package gate.jape.functest;

import java.util.Calendar;
import org.apache.log4j.Logger;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Executes all tests on given JAPE Transducer.
 * <p/>
 * See {@link BaseJapeTests#transducerType}
 *
 */
public class TestTransducer extends TestCase {
    private static final Logger logger = Logger.getLogger(TestTransducer.class);
    private static Calendar executionTime; 
    
    public static Test suite() {
	TestSuite tests = new TestSuite("All JAPE tests");
	tests.addTest(TestJape.suite());
	tests.addTest(TestConstraints.suite());
	
	Test suite = new TestSetup(tests) {
	    protected void setUp() {
		executionTime = Calendar.getInstance();
	    }
	    
	    protected void tearDown() {
		long executedIn = Calendar.getInstance().getTimeInMillis() - executionTime.getTimeInMillis();
		logger.info("Test suite executed in: " + executedIn/1000  + " second(s).");
	    }
	};
	
	return suite;
    }

    public static void main(String... args) {
	junit.textui.TestRunner.run(TestTransducer.suite());
    }
}
