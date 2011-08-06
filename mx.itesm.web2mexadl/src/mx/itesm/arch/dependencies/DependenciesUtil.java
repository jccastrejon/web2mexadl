package mx.itesm.arch.dependencies;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dependencies Utility methods.
 * 
 * @author jccastrejon
 * 
 */
public class DependenciesUtil {

    /**
     * Class logger.
     */
    private static Logger logger = Logger.getLogger(DependenciesUtil.class.getName());

    /**
     * Export a graphic representation of the Classes dependencies list.
     * 
     * @param dependencies
     *            Class dependencies.
     * @param includeExternal
     *            Should the external dependencies be exported.
     * @param imageFile
     *            Image File.
     * @param internalPackages
     *            Project's internal packages.
     * @param exportCommands
     *            Commands to be executed during export process.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static void exportDependenciesToSVG(final List<ClassDependencies> dependencies,
            final boolean includeExternal, final File imageFile, final Map<String, Set<String>> internalPackages,
            final ExportCommand... exportCommands) throws IOException {
        File dotFile;
        int processCode;
        Process process;
        String fileName;
        String className;
        String dotCommand;
        String exportCommand;
        FileWriter fileWriter;
        String currentPackageName;
        Set<String> dotComponents;
        StringBuilder dotDescription;
        Set<String> internalComponents;
        Map<String, Set<String>> externalPackages;
        Map<String, Set<String>> internalDotPackages;

        // Validate arguments
        if ((imageFile == null) || (!imageFile.getAbsolutePath().endsWith(".svg"))) {
            throw new IllegalArgumentException("Not a svg file: " + imageFile.getAbsolutePath());
        }

        // Build dot file
        fileName = imageFile.getName().substring(0, imageFile.getName().indexOf('.'));
        dotFile = new File(imageFile.getParent() + "/" + fileName + ".dot");

        // Simple Dependencies
        externalPackages = new HashMap<String, Set<String>>();
        dotDescription = new StringBuilder("digraph \"" + fileName
                + "\" {\n\tcompound=\"true\";rankdir=\"TB\";\n\tnode[shape=box, fontsize=8, height=.1, width=.1];\n");

        // Add Export Commands description
        for (ExportCommand command : exportCommands) {
            dotDescription.append(command.getDescription());
        }

        // Add internal and external dependencies
        for (ClassDependencies dependency : dependencies) {
            className = DependenciesUtil.getDotValidName(dependency.getClassName());

            // Append each result of the registered export commands.
            if (exportCommands != null) {
                for (ExportCommand command : exportCommands) {
                    exportCommand = command.execute(dependency);

                    // Append only if it's a valid result
                    if (exportCommand != null) {
                        dotDescription.append(exportCommand);
                    }
                }
            }

            // Add internal dependencies
            if (dependency.getInternalDependencies() != null) {
                for (String internalDependency : dependency.getInternalDependencies()) {
                    dotDescription.append("\t" + className + " -> "
                            + DependenciesUtil.getDotValidName(internalDependency) + ";\n");
                }
            }

            // Add external dependencies, also group them by packages
            if ((includeExternal) && (dependency.getInternalDependencies() != null)) {
                for (String externalDependency : dependency.getExternalDependencies()) {
                    dotDescription.append("\t" + className + " -> "
                            + DependenciesUtil.getDotValidName(externalDependency) + ";\n");
                    currentPackageName = externalDependency.substring(0, externalDependency.lastIndexOf('.'));

                    if (!externalPackages.containsKey(currentPackageName)) {
                        externalPackages.put(currentPackageName, new HashSet<String>());
                    }

                    externalPackages.get(currentPackageName).add(DependenciesUtil.getDotValidName(externalDependency));
                }
            }
        }

        // Group internal packages
        internalDotPackages = new HashMap<String, Set<String>>();
        for (String internalPackage : internalPackages.keySet()) {
            internalComponents = internalPackages.get(internalPackage);
            dotComponents = new HashSet<String>(internalComponents.size());
            for (String component : internalComponents) {
                dotComponents.add(DependenciesUtil.getDotValidName(component));
            }
            internalDotPackages.put(internalPackage, dotComponents);
        }

        // Internal dependencies
        DependenciesUtil.addClustersToDotDescription(internalDotPackages, dotDescription);

        // External dependencies
        if (includeExternal) {
            DependenciesUtil.addClustersToDotDescription(externalPackages, dotDescription);
        }

        // End of dot description
        dotDescription.append("}");

        // Save dot file
        fileWriter = new FileWriter(dotFile, false);
        fileWriter.write(dotDescription.toString());
        fileWriter.close();

        // Execute dot command
        try {
            dotCommand = "dot -Tsvg " + dotFile.getAbsolutePath() + " -o " + imageFile.getAbsolutePath();
            process = Runtime.getRuntime().exec(dotCommand);
            processCode = process.waitFor();
            dotFile.delete();

            if (processCode != 0) {
                throw new RuntimeException("An error ocurred while executing: " + dotCommand);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error creating image file: " + imageFile.getAbsolutePath(), e);
        }
    }

    /**
     * Get a reference to a directory only if the specified path points to a
     * valid directory, that is, it exists, it's indeed a directory, and can be
     * read. If provided with an invalid path it will throw a
     * IllegalArgumentException.
     * 
     * @param path
     *            Directory path.
     * @return File reference.
     */
    public static File getDirectory(final String path) {
        File returnValue;

        if (path == null) {
            throw new IllegalArgumentException("Directory must not be null");
        } else {
            returnValue = new File(path);
        }

        if (!returnValue.exists()) {
            throw new IllegalArgumentException("Directory " + path + " doesn't exist");
        } else if (!returnValue.isDirectory()) {
            throw new IllegalArgumentException("Path " + path + " is not a directory");
        } else if (!returnValue.canRead()) {
            throw new IllegalArgumentException("Directory " + path + " cannot be read");
        }

        return returnValue;
    }

    /**
     * Get all the Classes names in a directory, considering also its
     * subdirectories.
     * 
     * @param directory
     *            Current directory.
     * @param rootDirectory
     *            Root directory where the analysis started.
     * @param dependencyCommands
     *            DependencyCommands to be executed during the analysis.
     * @return List with the Classes names.
     */
    public static List<String> getClassesInDirectory(final File directory, final File rootDirectory,
            final DependencyCommand... dependencyCommands) {
        File currentFile;
        File[] directoryFiles;
        List<String> innerFiles;
        List<String> returnValue;
        List<String> extraFileTypes;

        // Add the valid file types for the specified dependency commands.
        extraFileTypes = new ArrayList<String>();
        if (dependencyCommands != null) {
            for (DependencyCommand dependencyCommand : dependencyCommands) {
                extraFileTypes.addAll(Arrays.asList(dependencyCommand.getValidFileTypes()));
            }
        }

        returnValue = new ArrayList<String>();
        directoryFiles = directory.listFiles(new DependencyFileFilter(extraFileTypes));
        for (int i = 0; i < directoryFiles.length; i++) {
            currentFile = directoryFiles[i];

            if (currentFile.isDirectory()) {
                // Recover subdirectory classes
                innerFiles = DependenciesUtil.getClassesInDirectory(currentFile, rootDirectory, dependencyCommands);
                returnValue.addAll(innerFiles);
            } else {
                returnValue.add(DependenciesUtil.getClassNameFromPath(currentFile.getAbsolutePath(),
                        rootDirectory.getAbsolutePath()));
            }
        }

        return returnValue;
    }

    /**
     * Get a Class Name from a Class File Path.
     * 
     * @param rootPath
     *            Rooth Path containing the Class File Path.
     * @param path
     *            Class File Path.
     * @return Class Name.
     */
    public static String getClassNameFromPath(final String path, final String rootPath) {
        String returnValue;

        if (path == null) {
            throw new IllegalArgumentException("Invalid path");
        }

        // {rootAbsolutePath}/{classPath}
        returnValue = path.substring(path.indexOf(rootPath) + rootPath.length() + 1);
        if (returnValue.indexOf(".class") > 0) {
            returnValue = returnValue.substring(0, returnValue.indexOf(".class")).replace('/', '.');
        }

        return returnValue;
    }

    /**
     * Get a Class File Path from a Class Name.
     * 
     * @param className
     *            Class Name.
     * @param rootPath
     *            Rooth Path containing the Class File Path.
     * @return Class File Path.
     */
    public static String getPathFromClassName(final String className, final String rootPath) {
        return rootPath + "/" + className.replace('.', '/') + ".class";
    }

    /**
     * Get the WAR family name from the specified WAR File Path.
     * 
     * @param path
     *            Path containing the WAR File Path.
     * @return WAR File Name.
     */
    public static String getWarFamilyNameFromPath(final String path) {
        int index;
        String returnValue;

        if (!path.endsWith(".war")) {
            throw new IllegalArgumentException("Invalid war path: " + path);
        }

        if (path.lastIndexOf('/') > 0) {
            index = path.lastIndexOf('/') + 1;
        } else {
            index = 0;
        }

        // FileName
        returnValue = path.substring(index, path.lastIndexOf('.')).toLowerCase();

        // System family name
        index = 0;
        for (char character : returnValue.toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                index++;
            } else {
                break;
            }
        }
        returnValue = returnValue.substring(0, index);

        return returnValue;
    }

    /**
     * Determine if a dependency is valid for a given class. That is, it's not
     * part of the java.* packages, it's not the same class, and it's not an
     * inner class defined in the same class.
     * 
     * @param className
     *            Class to be analyzed.
     * @param dependency
     *            Dependency to check.
     * @return true if the specified dependency is indeed a dependency of the
     *         specified class, false otherwise.
     */
    public static boolean isValidDependency(final String className, final String dependency) {
        boolean returnValue;
        int innerClassIndex;
        int classNameInnerClassIndex;
        String declaringClass;

        if ((className == null) || (dependency == null)) {
            throw new IllegalArgumentException("Invalid dependency: " + dependency + " for class: " + className);
        }

        innerClassIndex = dependency.indexOf('$');
        classNameInnerClassIndex = className.indexOf('$');
        returnValue = true;
        // Leave out java language classes
        if (dependency.startsWith("java")) {
            returnValue = false;
        }

        // Leave out self-references
        else if (dependency.equals(className)) {
            returnValue = false;
        }

        // Leave out inner classes defined in this class
        if (innerClassIndex > 0) {
            declaringClass = dependency.substring(0, innerClassIndex);
            // Dependency with declaring class
            if (declaringClass.equals(className)) {
                returnValue = false;
            }

            // Dependency with other inner classes defined in the same class.
            if (classNameInnerClassIndex > 0) {
                if (className.substring(0, classNameInnerClassIndex).equals(declaringClass)) {
                    returnValue = false;
                }
            }
        }

        return returnValue;
    }

    /**
     * Add the specified clusters to the dot description.
     * 
     * @param clusters
     *            Clusters.
     * @param dotDescription
     *            dot Description.
     */
    private static void addClustersToDotDescription(final Map<String, Set<String>> clusters,
            final StringBuilder dotDescription) {
        boolean clusterDependencyAdded;
        String previousClusterDependency;

        previousClusterDependency = null;
        if ((clusters != null) && (!clusters.isEmpty())) {
            for (String packageName : DependenciesUtil.sortClustersKeys(clusters)) {
                dotDescription.append("\tsubgraph \"cluster_" + packageName + "\" {\n");
                dotDescription.append("\t\trankdir=\"TB\";fontsize=8;label = \"" + packageName + "\";\n");

                dotDescription.append("\t\t");

                clusterDependencyAdded = false;
                for (String packageDependency : clusters.get(packageName)) {
                    dotDescription.append(packageDependency + ";");

                    if (!clusterDependencyAdded) {
                        clusterDependencyAdded = true;
                        if (previousClusterDependency == null) {
                            previousClusterDependency = packageDependency;
                        } else {
                            dotDescription.append("\n\t" + previousClusterDependency + " -> " + packageDependency
                                    + "[lhead=\"cluster_" + packageName + "\", style=\"invis\"];");
                            previousClusterDependency = packageDependency;
                        }
                    }
                }

                dotDescription.append("\n\t}\n");
            }
        }
    }

    /**
     * Get a valid class name for a dot node.
     * 
     * @param className
     *            Class Name.
     * @return Valid Class Name.
     */
    public static String getDotValidName(final String className) {
        String returnValue;
        int classNameIndex;

        classNameIndex = -1;
        if (className.indexOf('/') > 0) {
            classNameIndex = className.lastIndexOf("/");
        } else if (className.indexOf('.') > 0) {
            classNameIndex = className.lastIndexOf('.');
        }
        returnValue = "\"" + className.substring((classNameIndex + 1), className.length()) + "\"";

        return returnValue;
    }

    /**
     * Get the project internal packages by analyzing the dependencies.
     * 
     * @param dependencies
     *            Class Dependencies.
     * @param validTypes
     *            Valid file types.
     * @return Project packages with components that belong to that package.
     */
    public static Map<String, Set<String>> getInternalPackages(final List<ClassDependencies> dependencies,
            final String[] validTypes) {
        String otherPackageName;
        boolean fileDependencies;
        String currentPackageName;
        Set<String> currentPackage;
        Map<String, Set<String>> returnValue;

        returnValue = new HashMap<String, Set<String>>();
        for (ClassDependencies dependency : dependencies) {
            fileDependencies = false;
            currentPackageName = dependency.getPackageName();

            if (!returnValue.containsKey(currentPackageName)) {
                returnValue.put(currentPackageName, new HashSet<String>());
            }

            currentPackage = returnValue.get(currentPackageName);

            for (ClassDependencies otherDependency : dependencies) {
                if (otherDependency.getClassName().startsWith(currentPackageName)) {
                    otherPackageName = otherDependency.getClassName().replace(currentPackageName, "");
                    if (otherPackageName.lastIndexOf('/') <= 0) {
                        // Check if the dependencies in this package contain
                        // files with one of the valid file types
                        for (String currentDependency : currentPackage) {
                            for (String validType : validTypes) {
                                if (currentDependency.endsWith(validType)) {
                                    fileDependencies = true;
                                    break;
                                }
                            }

                            if (fileDependencies) {
                                break;
                            }
                        }

                        // If the dependencies contain valid file types, this
                        // new dependency should also be a valid file type
                        if (fileDependencies) {
                            for (String validType : validTypes) {
                                if (otherDependency.getClassName().endsWith(validType)) {
                                    currentPackage.add(otherDependency.getClassName());
                                    break;
                                }
                            }
                        } else {
                            currentPackage.add(otherDependency.getClassName());
                        }
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Sort cluster keys by their packages size.
     * 
     * @param clusters
     *            Package cluster.
     * @return Sorted cluster keys.
     */
    private static List<String> sortClustersKeys(final Map<String, Set<String>> clusters) {
        List<String> returnValue;

        returnValue = new ArrayList<String>(clusters.keySet());
        Collections.sort(returnValue, new Comparator<String>() {
            public int compare(final String first, final String second) {
                return new Integer(clusters.get(first).size()).compareTo(clusters.get(second).size());
            }
        });

        return returnValue;
    }
}