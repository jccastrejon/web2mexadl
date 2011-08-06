package mx.itesm.arch.dependencies;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;

/**
 * Recover Classes dependencies.
 * 
 * @author jccastrejon
 * 
 */
public class DependencyAnalyzer {

    /**
     * Recover the dependencies from each Java class within the specified
     * directory.
     * 
     * @param path
     *            Directory path.
     * @param dependencyCommands
     *            DependencyCommands to be executed during the analysis.
     * @return Dependencies for each class within the directory.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static List<ClassDependencies> getDirectoryDependencies(final String path,
            final DependencyCommand... dependencyCommands) throws IOException {
        File directory;
        String[] validTypes;
        boolean extraFileType;
        List<String> internalClasses;
        InputStream classInputStream;
        List<ClassDependencies> returnValue;

        // Get classes in directory
        returnValue = new ArrayList<ClassDependencies>();
        directory = DependenciesUtil.getDirectory(path);
        internalClasses = DependenciesUtil.getClassesInDirectory(directory, directory, dependencyCommands);

        // Get classes dependencies
        for (String className : internalClasses) {
            extraFileType = false;
            if (dependencyCommands != null) {
                for (DependencyCommand dependencyCommand : dependencyCommands) {
                    validTypes = dependencyCommand.getValidFileTypes();

                    if (validTypes != null) {
                        for (String validType : validTypes) {
                            if (className.endsWith("." + validType)) {
                                extraFileType = true;
                                break;
                            }
                        }

                        if (extraFileType) {
                            break;
                        }
                    }
                }
            }

            if (!extraFileType) {
                classInputStream = new FileInputStream(DependenciesUtil.getPathFromClassName(className,
                        directory.getAbsolutePath()));
                returnValue.add(DependencyAnalyzer.getClassSortedDependencies(className, classInputStream,
                        internalClasses, path));
            } else {
                returnValue.add(new ClassDependencies(className, null, null));
            }
        }

        return returnValue;
    }

    /**
     * Recover the dependencies from each Java class within the specified WAR
     * file, along with the classes in JAR files that belong to the same
     * project.
     * 
     * @param file
     *            Path to the JAR file.
     * @param dependencyCommands
     *            DependencyCommands to be executed during the analysis.
     * @return Dependencies for each class within the JAR file.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static List<ClassDependencies> getWarDependencies(final String file,
            final DependencyCommand... dependencyCommands) throws IOException {
        File warFile;
        String warName;
        ZipFile zipFile;
        ZipEntry zipEntry;
        List<ClassDependencies> returnValue;
        Enumeration<? extends ZipEntry> zipEntries;

        // .class files in the WAR file
        returnValue = DependencyAnalyzer.getJarDependencies(file, dependencyCommands);

        // JAR files that belong to the same project
        warFile = new File(file);
        zipFile = new ZipFile(file);
        zipEntries = zipFile.entries();
        warName = DependenciesUtil.getWarFamilyNameFromPath(file);
        while (zipEntries.hasMoreElements()) {
            zipEntry = zipEntries.nextElement();

            if ((!zipEntry.isDirectory()) && zipEntry.getName().endsWith(".jar")) {
                // Consider only JAR files that have a similar name than that of
                // the WAR file
                if (zipEntry.getName().toLowerCase().contains(warName)) {
                    returnValue.addAll(DependencyAnalyzer.getJarDependencies(
                            warFile.getAbsolutePath() + "/" + zipEntry.getName(), zipFile.getInputStream(zipEntry),
                            dependencyCommands));
                }
            }
        }

        return returnValue;
    }

    /**
     * Recover the dependencies from each Java class within the specified JAR
     * file.
     * 
     * @param file
     *            Path to the JAR file.
     * @param dependencyCommands
     *            DependencyCommands to be executed during the analysis.
     * @return Dependencies for each class within the JAR file.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static List<ClassDependencies> getJarDependencies(final String file,
            final DependencyCommand... dependencyCommands) throws IOException {
        File fileRef;
        JarFile jarFile;
        JarEntry jarEntry;
        List<String> internalClasses;
        List<ClassDependencies> returnValue;
        Enumeration<? extends JarEntry> jarEntries;

        fileRef = new File(file);
        jarFile = new JarFile(fileRef);
        internalClasses = new ArrayList<String>();
        returnValue = new ArrayList<ClassDependencies>();

        // Dependencies for each class file
        jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            jarEntry = jarEntries.nextElement();
            DependencyAnalyzer.analyzeJarEntry(jarEntry, internalClasses, fileRef, jarFile.getInputStream(jarEntry),
                    returnValue, dependencyCommands);
        }

        return returnValue;
    }

    /**
     * Recover the dependencies from each Java class within the specified JAR
     * file. The specified Input Stream is used to read the file contents.
     * 
     * @param file
     *            Path to the JAR file.
     * @param inputStream
     *            Input Stream to the JAR file.
     * @param dependencyCommands
     *            DependencyCommands to be executed during the analysis.
     * @return Dependencies for each class within the JAR file.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static List<ClassDependencies> getJarDependencies(final String file, final InputStream inputStream,
            final DependencyCommand... dependencyCommands) throws IOException {
        File fileRef;
        JarEntry jarEntry;
        List<String> internalClasses;
        JarInputStream jarInputStream;
        List<ClassDependencies> returnValue;

        fileRef = new File(file);
        internalClasses = new ArrayList<String>();
        returnValue = new ArrayList<ClassDependencies>();

        // Get internal classes
        jarInputStream = new JarInputStream(inputStream);
        jarEntry = jarInputStream.getNextJarEntry();
        while (jarEntry != null) {
            DependencyAnalyzer.analyzeJarEntry(jarEntry, internalClasses, fileRef, jarInputStream, returnValue,
                    dependencyCommands);
            jarEntry = jarInputStream.getNextJarEntry();
        }

        return returnValue;
    }

    /**
     * Recover the dependencies for the file referenced by the specified Jar
     * Entry. If the referenced file is a Class, its dependencies are added to
     * the classesDependencies list.
     * 
     * @param jarEntry
     *            Jar Entry.
     * @param internalClasses
     *            Class' internal classes.
     * @param fileRef
     *            File reference to the JAR file.
     * @param inputStream
     *            Input Stream to the JAR file.
     * @param classesDependencies
     *            List of the classes dependencies found so far in the JAR file.
     * @param dependencyCommands
     *            DependencyCommands to be executed during the analysis.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    private static void analyzeJarEntry(final JarEntry jarEntry, final List<String> internalClasses,
            final File fileRef, final InputStream inputStream, final List<ClassDependencies> classesDependencies,
            final DependencyCommand... dependencyCommands) throws IOException {
        String className;
        ClassDependencies dependencies;

        if (!jarEntry.isDirectory()) {
            // Classes
            if (jarEntry.getName().endsWith(".class")) {
                // Get internal classes
                internalClasses.add(DependenciesUtil.getClassNameFromPath(
                        fileRef.getParent() + "/" + jarEntry.getName(), fileRef.getParent()));

                // Get dependencies
                className = DependenciesUtil.getClassNameFromPath(fileRef.getParent() + "/" + jarEntry.getName(),
                        fileRef.getParent());
                dependencies = DependencyAnalyzer.getClassSortedDependencies(className, inputStream, internalClasses,
                        fileRef.getParent());
                classesDependencies.add(dependencies);
            }

            // Web pages, configuration files
            for (DependencyCommand dependencyCommand : dependencyCommands) {
                className = dependencyCommand.execute(jarEntry.getName());
                if (className != null) {
                    classesDependencies.add(new ClassDependencies(className, null, null));
                }
            }
        }
    }

    /**
     * Recover the dependencies for the specified Class with no special grouping
     * criteria.
     * 
     * @param clazz
     *            Class to analyze.
     * @return Unsorted Class' dependencies.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static Set<String> getClassUnsortedDependencies(final Class<?> clazz) throws IOException {
        return DependencyAnalyzer.getClassUnsortedDependencies(clazz.getResourceAsStream("/"
                + clazz.getName().replace('.', '/') + ".class"));
    }

    /**
     * Recover the dependencies for the specified Class with no special grouping
     * criteria.
     * 
     * @param fileStream
     *            IputStream to the required class file.
     * @return Unsorted Class' dependencies.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static Set<String> getClassUnsortedDependencies(final InputStream fileStream) throws IOException {
        Set<String> returnValue;
        DependencyVisitor dependencyVisitor;

        // Recover all dependencies
        dependencyVisitor = new DependencyVisitor();
        new ClassReader(fileStream).accept(dependencyVisitor, ClassReader.SKIP_DEBUG);
        returnValue = dependencyVisitor.getDependencies();

        return returnValue;
    }

    /**
     * Recover the dependencies for the specified Class, grouped by
     * <em>internal</em> (Same Project) and <em>external</em> (Libraries)
     * dependencies.
     * 
     * @param clazz
     *            Class to analyze.
     * @param internalClasses
     *            List of classes that belong to the same project as the class
     *            being analyzed.
     * @return Class' dependencies.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static ClassDependencies getClassSortedDependencies(final Class<?> clazz, final List<String> internalClasses)
            throws IOException {
        String classDirectory;
        ClassDependencies returnValue;

        classDirectory = clazz.getResource(clazz.getName()).getPath();
        returnValue = DependencyAnalyzer.getClassSortedDependencies(clazz.getName(),
                clazz.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class"), internalClasses,
                classDirectory);

        return returnValue;
    }

    /**
     * Recover the dependencies for the specified Class, grouped by
     * <em>internal</em> (Same Project) and <em>external</em> (Libraries)
     * dependencies.
     * 
     * @param className
     *            Class name.
     * @param fileStream
     *            IputStream to the required class file.
     * @param internalClasses
     *            List of classes that belong to the same project as the class
     *            being analyzed.
     * @param rootPath
     *            Root Path that contains the project classes.
     * @return Class' dependencies.
     * @throws IOException
     *             If an I/O error has occurred.
     */
    public static ClassDependencies getClassSortedDependencies(final String className, final InputStream fileStream,
            final List<String> internalClasses, final String rootPath) throws IOException {
        Set<String> dependencies;
        boolean isInternalDependency;
        List<String> internalDependencies;
        List<String> externalDependencies;

        // Recover all dependencies
        dependencies = DependencyAnalyzer.getClassUnsortedDependencies(fileStream);

        // Separate internal - external dependencies
        internalDependencies = new ArrayList<String>();
        externalDependencies = new ArrayList<String>();
        for (String dependency : dependencies) {

            if (!DependenciesUtil.isValidDependency(className, dependency)) {
                continue;
            }

            // Internal
            isInternalDependency = false;
            for (String internalClass : internalClasses) {
                if (dependency.equals(internalClass)) {
                    internalDependencies.add(dependency);
                    isInternalDependency = true;
                    break;
                }
            }

            // External
            if (!isInternalDependency) {
                externalDependencies.add(dependency);
            }
        }

        return new ClassDependencies(className, internalDependencies, externalDependencies);
    }
}
