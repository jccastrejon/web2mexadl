package mx.itesm.web2mexadl.cluster;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    /**
     * 
     * @param file
     * @param includeExternal
     * @param outputFile
     * @throws Exception
     */
    public static void classifyClassesInWar(final File file, final boolean includeExternal, final File outputFile)
            throws Exception {
        Dataset[] clusters;
        Map<String, Cluster> returnValue;
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
        returnValue = ClusterAnalyzer.generateArchitecture(clusters, internalPackages);

        if (outputFile != null) {
            DependenciesUtil.exportDependenciesToSVG(dependencies, includeExternal, outputFile, internalPackages,
                    new ClusterExportCommand(returnValue));
        }
    }

    /**
     * 
     * @param path
     * @param outputFile
     * @throws IOException
     */
    public static void classifyClassesInDirectory(final File path, final boolean includeExternal, final File outputFile)
            throws IOException {
        Dataset[] clusters;
        Map<String, Cluster> returnValue;
        List<ClassDependencies> dependencies;
        Map<String, Set<String>> internalPackages;

        // Classify each class in the specified path
        dependencies = DependencyAnalyzer.getDirectoryDependencies(path.getAbsolutePath(), new MvcDependencyCommand());
        internalPackages = DependenciesUtil.getInternalPackages(dependencies,
                Util.getPropertyValues(Util.Variable.Type.getVariableName()));

        clusters = ClusterAnalyzer.generateClusters(dependencies);
        returnValue = ClusterAnalyzer.generateArchitecture(clusters, internalPackages);

        if (outputFile != null) {
            DependenciesUtil.exportDependenciesToSVG(dependencies, includeExternal, outputFile, internalPackages,
                    new ClusterExportCommand(returnValue));
        }
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
     * @param clustersData
     * @param internalPackages
     */
    private static Map<String, Cluster> generateArchitecture(final Dataset[] clustersData,
            final Map<String, Set<String>> internalPackages) {
        int maxCount;
        int clusterIndex;
        int[] clustersCounts;
        Cluster[] clusters;
        List<Integer> clusterCountsList;
        Map<String, Cluster> returnValue;
        Set<String> currentPackageContent;
        StringBuilder[] implementationPackages;
        HashMap<String, Integer> clusterClasses;
        Map<String, Integer> packagesClassification;

        // Get the clusters assignments
        clusterIndex = -1;
        clusterClasses = new HashMap<String, Integer>();
        for (Dataset cluster : clustersData) {
            clusterIndex++;
            for (Object clazz : cluster.classes()) {
                clusterClasses.put(clazz.toString(), clusterIndex);
            }
        }

        // Initialize clusters
        clusters = new Cluster[clustersData.length];
        for (int i = 0; i < clustersData.length; i++) {
            clusters[i] = new Cluster(ClusterAnalyzer.getRandomColor());
        }

        // Initialize implementation packages
        implementationPackages = new StringBuilder[clustersData.length];
        for (int i = 0; i < implementationPackages.length; i++) {
            implementationPackages[i] = new StringBuilder();
        }

        // Classify packages
        clustersCounts = new int[clustersData.length];
        returnValue = new HashMap<String, Cluster>();
        packagesClassification = new HashMap<String, Integer>(internalPackages.size());
        for (String currentPackage : internalPackages.keySet()) {
            currentPackageContent = internalPackages.get(currentPackage);
            for (String component : currentPackageContent) {
                // Check if this component was assigned to any cluster
                if (clusterClasses.keySet().contains(component)) {
                    clustersCounts[clusterClasses.get(component)]++;
                    returnValue.put(component, clusters[clusterClasses.get(component)]);
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
                    Util.addImplementationPackage(implementationPackages[i], currentPackage);
                    break;
                }
            }
        }

        return returnValue;
    }

    /**
     * Based on:
     * http://stackoverflow.com/questions/43044/algorithm-to-randomly-generate
     * -an-aesthetically-pleasing-color-palette
     * 
     * @return
     */
    private static Color getRandomColor() {
        int red;
        int blue;
        int green;
        Random random;
        Color mixColor;
        Color returnValue;

        mixColor = new Color(255, 255, 255);
        random = new Random();
        red = random.nextInt(256);
        green = random.nextInt(256);
        blue = random.nextInt(256);

        // mix the color
        red = (red + mixColor.getRed()) / 2;
        green = (green + mixColor.getGreen()) / 2;
        blue = (blue + mixColor.getBlue()) / 2;

        returnValue = new Color(red, green, blue);
        return returnValue;
    }
}