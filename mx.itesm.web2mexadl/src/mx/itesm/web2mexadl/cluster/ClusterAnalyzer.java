package mx.itesm.web2mexadl.cluster;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.itesm.web2mexadl.dependencies.ClassDependencies;
import mx.itesm.web2mexadl.dependencies.DependenciesUtil;
import mx.itesm.web2mexadl.dependencies.DependencyAnalyzer;
import mx.itesm.web2mexadl.mvc.MvcAnalyzer;
import mx.itesm.web2mexadl.mvc.MvcDependencyCommand;
import mx.itesm.web2mexadl.util.Util;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.tools.weka.WekaClusterer;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import weka.clusterers.EM;

/**
 * 
 * @author jccastrejon
 * 
 */
public class ClusterAnalyzer {

    /**
     * Class logger.
     */
    private static Logger logger = Logger.getLogger(ClusterAnalyzer.class.getName());

    /**
     * xADL Types namespace.
     */
    public static final Namespace XADL_TYPES_NAMESPACE = Namespace.getNamespace("types",
            "http://www.ics.uci.edu/pub/arch/xArch/types.xsd");

    /**
     * 
     */
    private static XPath linksPath;

    /**
     * 
     */
    private static XPath componentsPath;

    /**
     * 
     */
    private static XPath connectorsPath;

    /**
     * 
     */
    private static XPath componentTypesPath;

    /**
     * 
     */
    private static SAXBuilder saxBuilder = new SAXBuilder();

    static {
        try {
            ClusterAnalyzer.componentsPath = XPath.newInstance("/instance:xArch/types:archStructure/types:component");
            ClusterAnalyzer.connectorsPath = XPath.newInstance("/instance:xArch/types:archStructure/types:connector");
            ClusterAnalyzer.linksPath = XPath.newInstance("/instance:xArch/types:archStructure/types:link");
            ClusterAnalyzer.componentTypesPath = XPath
                    .newInstance("/instance:xArch/types:archTypes/types:componentType");
        } catch (Exception e) {
            ClusterAnalyzer.logger.log(Level.WARNING, "Error while xpath instances: ", e);
        }
    }

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
        returnValue = ClusterAnalyzer.generateArchitecture(clusters, internalPackages, outputFile.getParentFile());

        if (outputFile != null) {
            DependenciesUtil.exportDependenciesToSVG(dependencies, includeExternal, outputFile, internalPackages,
                    new ClusterExportCommand(returnValue));
        }
    }

    /**
     * 
     * @param path
     * @param includeExternal
     * @param outputFile
     * @throws Exception
     */
    public static void classifyClassesInDirectory(final File path, final boolean includeExternal, final File outputFile)
            throws Exception {
        Dataset[] clusters;
        Map<String, Cluster> returnValue;
        List<ClassDependencies> dependencies;
        Map<String, Set<String>> internalPackages;

        // Classify each class in the specified path
        dependencies = DependencyAnalyzer.getDirectoryDependencies(path.getAbsolutePath(), new MvcDependencyCommand());
        internalPackages = DependenciesUtil.getInternalPackages(dependencies,
                Util.getPropertyValues(Util.Variable.Type.getVariableName()));

        clusters = ClusterAnalyzer.generateClusters(dependencies);
        returnValue = ClusterAnalyzer.generateArchitecture(clusters, internalPackages, outputFile.getParentFile());

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
     * @param outputDir
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    private static Map<String, Cluster> generateArchitecture(final Dataset[] clustersData,
            final Map<String, Set<String>> internalPackages, final File outputDir) throws IOException, JDOMException {
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

        ClusterAnalyzer.exportToMexADL(outputDir, implementationPackages);

        return returnValue;
    }

    /**
     * 
     * @param outputDir
     * @param implementationPackages
     * @throws IOException
     * @throws JDOMException
     */
    @SuppressWarnings("unchecked")
    public static void exportToMexADL(final File outputDir, final StringBuilder... implementationPackages)
            throws IOException, JDOMException {
        Document document;
        String identifier;
        List<Element> links;
        XMLOutputter outputter;
        List<Element> components;
        List<Element> connectors;
        List<Element> componentTypes;
        List<Element> validComponents;

        document = ClusterAnalyzer.saxBuilder.build(MvcAnalyzer.class
                .getResourceAsStream("/mx/itesm/web2mexadl/templates/ClusterTemplate.xml"));
        components = (List<Element>) ClusterAnalyzer.componentsPath.selectNodes(document);
        connectors = (List<Element>) ClusterAnalyzer.connectorsPath.selectNodes(document);
        links = (List<Element>) ClusterAnalyzer.linksPath.selectNodes(document);
        componentTypes = (List<Element>) ClusterAnalyzer.componentTypesPath.selectNodes(document);

        // Identify the clusters specified in the implementationPackages
        validComponents = new ArrayList<Element>(implementationPackages.length);
        for (int i = 0; i < implementationPackages.length; i++) {
            for (Element component : components) {
                if (component.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue()
                        .equals("Cluster_" + i)) {
                    validComponents.add(component);
                }
            }
        }

        // Remove the unused clusters and their associated connectors and links
        for (Element component : components) {
            if (!validComponents.contains(component)) {
                identifier = component.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue();
                identifier = identifier.substring(identifier.indexOf('_') + 1);

                // Remove component
                component.detach();

                // Remove component type
                for (Element type : componentTypes) {
                    if (type.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue()
                            .equals("Cluster_" + identifier + "Type")) {
                        type.detach();
                        break;
                    }
                }

                // Remove connector
                for (Element connector : connectors) {
                    if (connector.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue()
                            .equals("Connector_" + identifier)) {
                        connector.detach();
                        break;
                    }
                }

                // Remove links
                for (Element link : links) {
                    // Input link
                    if (link.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue()
                            .equals("in" + identifier)) {
                        link.detach();
                    }

                    // Output from this component to other ones
                    else if (link.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue()
                            .startsWith("out" + identifier)) {
                        link.detach();
                    }

                    // Output from other components to this one
                    else if (link.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue()
                            .startsWith("out")
                            && link.getChild("description", ClusterAnalyzer.XADL_TYPES_NAMESPACE).getValue()
                                    .endsWith("-" + identifier)) {
                        link.detach();
                    }
                }

            }
        }

        // Write architecture document
        outputter = new XMLOutputter();
        outputter.output(document, new FileWriter(new File(outputDir, "clusteredArchitecture.xml")));
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

        // Base color (white)
        mixColor = new Color(255, 255, 255);
        random = new Random();
        red = random.nextInt(256);
        green = random.nextInt(256);
        blue = random.nextInt(256);

        // Mix new color with base one
        red = (red + mixColor.getRed()) / 2;
        green = (green + mixColor.getGreen()) / 2;
        blue = (blue + mixColor.getBlue()) / 2;

        returnValue = new Color(red, green, blue);
        return returnValue;
    }
}