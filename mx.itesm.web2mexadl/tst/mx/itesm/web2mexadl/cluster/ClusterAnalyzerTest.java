package mx.itesm.web2mexadl.cluster;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * 
 * @author jccastrejon
 * 
 */
public class ClusterAnalyzerTest extends TestCase {
    public void testClassifyClassesInDirectory() throws IOException {
        ClusterAnalyzer.classifyClassesInWar(new File("./tst/petclinic-0.1.0.war"), new File("./tst/test.svg"));
    }
}
