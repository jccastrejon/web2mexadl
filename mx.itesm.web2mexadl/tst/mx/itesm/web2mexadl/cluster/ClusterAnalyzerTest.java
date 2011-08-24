/*
 * Copyright 2011 jccastrejon
 *  
 * This file is part of Web2MexADL.
 *
 * Web2MexADL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Web2MexADL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Web2MexADL.  If not, see <http://www.gnu.org/licenses/>.
 */
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
