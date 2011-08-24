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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mx.itesm.web2mexadl.dependencies.ClassDependencies;
import mx.itesm.web2mexadl.dependencies.DependenciesUtil;
import mx.itesm.web2mexadl.dependencies.ExportCommand;

/**
 * Export command that adds the Clusters data to the graphic export process.
 * 
 * @author jccastrejon
 * 
 */
public class ClusterExportCommand implements ExportCommand {

    /**
     * 
     */
    Map<String, Cluster> classifications;

    /**
     * 
     * @param classifications
     */
    public ClusterExportCommand(final Map<String, Cluster> classifications) {
        this.classifications = classifications;
    }

    @Override
    public String execute(final ClassDependencies classDependencies) {
        String returnValue;
        Cluster classCluster;

        returnValue = null;
        if (classifications != null) {
            classCluster = classifications.get(classDependencies.getClassName());

            if (classCluster != null) {
                returnValue = "\n\t" + DependenciesUtil.getDotValidName(classDependencies.getClassName())
                        + " [color=\"" + classCluster.getHexColor() + "\",style=\"" + classCluster.getStyle()
                        + "\"];\n";
            }
        }

        return returnValue;
    }

    @Override
    public String getDescription() {
        int index;
        Set<Cluster> clusters;
        StringBuilder returnValue;

        returnValue = new StringBuilder();
        clusters = new HashSet<Cluster>();
        for (Cluster cluster : this.classifications.values()) {
            clusters.add(cluster);
        }

        index = -1;
        for (Cluster cluster : clusters) {
            index++;
            returnValue.append("\n\tCluster_" + index + " [label=\"Cluster_" + index + "\",color=\""
                    + cluster.getHexColor() + "\",style=\"" + cluster.getStyle() + "\"];");
        }

        // Create clusters description
        returnValue.append("\n\tsubgraph clusterWebClusters {\n\trankdir=\"TB\";fontsize=\"8\"; label=\"Clusters\";");
        returnValue.append("color=\"#CCFFFF\"; style=\"bold\";\n\t");
        for (int i = 0; i < clusters.size(); i++) {
            returnValue.append("Cluster_" + i + "; ");
        }

        // Group descriptions
        returnValue.append("\n");
        for (int i = 0; i < clusters.size(); i++) {
            returnValue.append("Cluster_" + i + " -> ");
        }
        returnValue.replace(returnValue.lastIndexOf("->"), returnValue.lastIndexOf("->") + 3, "");
        returnValue.append(" [style=\"invis\"];\n");

        returnValue.append("}");
        return returnValue.toString();
    }

}
