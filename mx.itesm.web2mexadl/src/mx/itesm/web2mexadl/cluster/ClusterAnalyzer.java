package mx.itesm.web2mexadl.cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mx.itesm.web2mexadl.dependencies.ClassDependencies;
import mx.itesm.web2mexadl.dependencies.DependenciesUtil;
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

    public static void classifyClassesInWar(final File file, final File outputFile) throws Exception {
        Dataset[] clusters;
        List<ClassDependencies> dependencies;
        Map<String, Set<String>> internalPackages;

        // Classify each class in the specified war
        dependencies = DependencyAnalyzer.getWarDependencies(file.getAbsolutePath(), new MvcDependencyCommand());

        // Remove the WEB-INF.classes prefix
        for (ClassDependencies dependency : dependencies) {
            dependency.setClassName(dependency.getClassName().replace("WEB-INF.classes.", ""));
            dependency.setPackageName(dependency.getPackageName().replace("WEB-INF.classes.", ""));
        }

        internalPackages = DependenciesUtil.getInternalPackages(dependencies,
                Util.getPropertyValues(Util.Variable.Type.getVariableName()));

        clusters = ClusterAnalyzer.generateClusters(dependencies);
        ClusterAnalyzer.generateArchitecture(clusters, internalPackages);
    }

    /**
     * 
     * @param path
     * @param outputFile
     * @throws IOException
     */
    public static void classifyClassesInDirectory(final File path, final File outputFile) throws IOException {
        Dataset[] clusters;
        List<ClassDependencies> dependencies;
        Map<String, Set<String>> internalPackages;

        // Classify each class in the specified path
        dependencies = DependencyAnalyzer.getDirectoryDependencies(path.getAbsolutePath(), new MvcDependencyCommand());
        internalPackages = DependenciesUtil.getInternalPackages(dependencies,
                Util.getPropertyValues(Util.Variable.Type.getVariableName()));

        clusters = ClusterAnalyzer.generateClusters(dependencies);
        ClusterAnalyzer.generateArchitecture(clusters, internalPackages);
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
            dataset.add(new DenseInstance(values, classDependencies.getClassName()));
        }

        // Generate clusters
        clusterer = new WekaClusterer(new EM());
        returnValue = clusterer.cluster(dataset);

        return returnValue;
    }

    /**
     * 
     * @param clusters
     * @param internalPackages
     */
    private static void generateArchitecture(final Dataset[] clusters, final Map<String, Set<String>> internalPackages) {
        int maxCount;
        int clusterIndex;
        int[] clustersCounts;
        List<Integer> clusterCountsList;
        Set<String> currentPackageContent;
        HashMap<String, Integer> clusterClasses;
        Map<String, Integer> packagesClassification;

        // Get the clusters assignments
        clusterIndex = -1;
        clusterClasses = new HashMap<String, Integer>();
        for (Dataset cluster : clusters) {
            clusterIndex++;
            for (Object clazz : cluster.classes()) {
                clusterClasses.put(clazz.toString(), clusterIndex);
            }
        }

        clustersCounts = new int[clusters.length];
        packagesClassification = new HashMap<String, Integer>(internalPackages.size());
        for (String currentPackage : internalPackages.keySet()) {
            currentPackageContent = internalPackages.get(currentPackage);
            for (String component : currentPackageContent) {
                // Check if this component was assigned to any cluster
                if (clusterClasses.keySet().contains(component)) {
                    clustersCounts[clusterClasses.get(component)]++;
                }
            }

            // Get the package cluster according to the most common cluster
            // between the package contents
            clusterCountsList = new ArrayList<Integer>(clustersCounts.length);
            for (int i : clustersCounts) {
                clusterCountsList.add(i);
            }
            maxCount = Collections.max(clusterCountsList);
            for (int i = 0; i < clustersCounts.length; i++) {
                if (clustersCounts[i] == maxCount) {
                    packagesClassification.put(currentPackage, i);
                    break;
                }
            }

            System.out.println(currentPackage + ": " + packagesClassification.get(currentPackage));
        }
    }
}