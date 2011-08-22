package mx.itesm.web2mexadl.cluster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mx.itesm.web2mexadl.dependencies.ClassDependencies;
import mx.itesm.web2mexadl.dependencies.DependencyAnalyzer;
import mx.itesm.web2mexadl.mvc.MvcDependencyCommand;
import mx.itesm.web2mexadl.util.Util;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.weka.WekaClusterer;
import weka.clusterers.EM;

/**
 * 
 * @author jccastrejon
 * 
 */
public class ClusterAnalyzer {

    /**
     * 
     * @param path
     * @param outputFile
     * @throws IOException
     */
    public static void classifyClassesInDirectory(final File path, final File outputFile) throws IOException {
        Dataset[] clusters;
        List<ClassDependencies> dependencies;

        dependencies = DependencyAnalyzer.getDirectoryDependencies(path.getAbsolutePath(), new MvcDependencyCommand());
        clusters = ClusterAnalyzer.generateClusters(dependencies);
        System.out.println(clusters);
    }

    /**
     * 
     * @param path
     * @param outputFile
     * @throws IOException
     */
    public static void classifyClassesInWar(final File file, final File outputFile) throws IOException {
        Dataset[] clusters;
        List<ClassDependencies> dependencies;

        // Classify each class in the specified war
        dependencies = DependencyAnalyzer.getWarDependencies(file.getAbsolutePath(), new MvcDependencyCommand());

        // Remove the WEB-INF.classes prefix
        for (ClassDependencies dependency : dependencies) {
            dependency.setClassName(dependency.getClassName().replace("WEB-INF.classes.", ""));
            dependency.setPackageName(dependency.getPackageName().replace("WEB-INF.classes.", ""));
        }

        clusters = ClusterAnalyzer.generateClusters(dependencies);
        System.out.println(clusters);
    }

    /**
     * 
     * @param dependencies
     */
    private static Dataset[] generateClusters(final List<ClassDependencies> dependencies) {
        Dataset dataset;
        double[] values;
        boolean valueFound;
        Clusterer clusterer;
        String[] typeValues;
        int instanceTypeIndex;
        String[] suffixValues;
        Dataset[] returnValue;
        String[] externalApiValues;
        Map<String, String[]> externalApiPackages;

        // Valid suffixes to look for in the class names
        suffixValues = Util.getPropertyValues(Util.Variable.Suffix.getVariableName());

        // Valid file types to look for in the component names
        typeValues = Util.getPropertyValues(Util.Variable.Type.getVariableName());

        // Valid external api packages to look for in the classes dependencies
        externalApiValues = Util.getPropertyValues(Util.Variable.ExternalAPI.getVariableName());
        externalApiPackages = new HashMap<String, String[]>(externalApiValues.length);
        for (int i = 0; i < externalApiValues.length; i++) {
            if (!externalApiValues[i].equals("none")) {
                externalApiPackages.put(externalApiValues[i],
                        Util.getPropertyValues("externalApi." + externalApiValues[i] + ".packages"));
            }
        }

        // Get instances data
        returnValue = null;
        dataset = new DefaultDataset();
        values = new double[Util.Variable.values().length];
        for (ClassDependencies classDependencies : dependencies) {

            // Type
            instanceTypeIndex = 0;
            for (int i = 0; i < typeValues.length; i++) {
                String validType = typeValues[i];
                if (classDependencies.getClassName().endsWith("." + validType)) {
                    instanceTypeIndex = i;
                    break;
                }
            }
            values[Util.Variable.Type.ordinal()] = instanceTypeIndex;

            // ExternalAPI
            valueFound = false;
            externalApi: for (int i = 0; i < externalApiValues.length; i++) {
                String externalApi = externalApiValues[i];

                if (externalApi.equals("none")) {
                    continue;
                }

                // Check if any of the class' external dependencies match with
                // one of the key external dependencies
                if (classDependencies.getExternalDependencies() != null) {
                    for (String externalDependency : classDependencies.getExternalDependencies()) {
                        for (String externalPackage : externalApiPackages.get(externalApi)) {
                            if (externalDependency.toLowerCase().startsWith(externalPackage)) {
                                valueFound = true;
                                values[Util.Variable.ExternalAPI.ordinal()] = i;
                                break externalApi;
                            }
                        }
                    }
                }
            }

            // No key external dependency found
            if (!valueFound) {
                values[Util.Variable.ExternalAPI.ordinal()] = externalApiValues.length;
            }

            // Suffix
            valueFound = false;
            for (int i = 0; i < suffixValues.length; i++) {
                String suffix = suffixValues[i];
                if (classDependencies.getClassName().toLowerCase().endsWith(suffix)) {
                    valueFound = true;
                    values[Util.Variable.Suffix.ordinal()] = i;
                    break;
                }
            }

            // No key suffix found
            if (!valueFound) {
                values[Util.Variable.Suffix.ordinal()] = suffixValues.length;
            }

            // Save instance data
            dataset.add(new DenseInstance(values, classDependencies.getPackageName() + "."
                    + classDependencies.getClassName()));
        }

        // Generate clusters
        clusterer = new WekaClusterer(new EM());
        returnValue = clusterer.cluster(dataset);

        return returnValue;
    }
}