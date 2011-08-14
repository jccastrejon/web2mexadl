package mx.itesm.web2mexadl.mvc;

import java.io.File;

import mx.itesm.web2mexadl.mvc.MvcAnalyzer;

import junit.framework.TestCase;

/**
 * 
 * @author jccastrejon
 * 
 */
public class MvcAnalyzerTest extends TestCase {

    public void testClassifyClassesInWar() throws Exception {
        MvcAnalyzer.classifyClassesinWar(new File("./tst/petclinic-0.1.0.war"), false, new File("./tst/test.svg"));
    }
}
