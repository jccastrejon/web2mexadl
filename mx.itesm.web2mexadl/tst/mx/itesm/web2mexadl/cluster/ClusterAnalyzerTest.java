package mx.itesm.web2mexadl.cluster;

import java.io.File;

import junit.framework.TestCase;

/**
 * 
 * @author jccastrejon
 * 
 */
public class ClusterAnalyzerTest extends TestCase {
    public void testClassifyClassesInDirectory() throws Exception {
        ClusterAnalyzer.classifyClassesInWar(new File("./tst/petclinic-0.1.0.war"), false, new File("./tst/testCluster.svg"));
    }
}
