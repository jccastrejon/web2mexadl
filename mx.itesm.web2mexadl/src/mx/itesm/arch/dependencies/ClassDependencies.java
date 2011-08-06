package mx.itesm.arch.dependencies;

import java.util.List;

/**
 * Holds a class' dependency data. That is, the dependencies with other classes
 * in the same project, <em>internal dependencies</em>, and the dependencies
 * with external projects, <em>external dependencies</em>. To uniquely identify
 * a class, it's package and class name are also stored here.
 * 
 * @author jccastrejon
 * 
 */
public class ClassDependencies {

    /**
     * Package Name.
     */
    private String packageName;

    /**
     * Class name.
     */
    private String className;

    /**
     * Dependencies with internal classes (same project).
     */
    private List<String> internalDependencies;

    /**
     * Dependencies with external classes (libraries).
     */
    private List<String> externalDependencies;

    /**
     * Full constructor.
     * 
     * @param className
     *            Class name.
     * @param internalDependencies
     *            Dependencies with internal classes (same project).
     * @param externalDependencies
     *            Dependencies with external classes (libraries).
     */
    public ClassDependencies(final String className, final List<String> internalDependencies,
            final List<String> externalDependencies) {
        this.className = className;
        this.internalDependencies = internalDependencies;
        this.externalDependencies = externalDependencies;

        if (className.lastIndexOf('/') == 0) {
            this.packageName = "/";
        } else if (className.lastIndexOf('/') > 0) {
            this.packageName = className.substring(0, className.lastIndexOf('/'));
        } else if (className.lastIndexOf('.') > 0) {
            this.packageName = className.substring(0, className.lastIndexOf('.'));
        } else {
            this.packageName = "/";
        }
    }

    @Override
    public String toString() {
        return "{Class: " + this.className + " - Internal: " + this.internalDependencies + " - External: "
                + this.externalDependencies + "}";
    }

    /**
     * @return the internalDependencies
     */
    public List<String> getInternalDependencies() {
        return internalDependencies;
    }

    /**
     * @param internalDependencies
     *            the internalDependencies to set
     */
    public void setInternalDependencies(List<String> internalDependencies) {
        this.internalDependencies = internalDependencies;
    }

    /**
     * @return the externalDependencies
     */
    public List<String> getExternalDependencies() {
        return externalDependencies;
    }

    /**
     * @param externalDependencies
     *            the externalDependencies to set
     */
    public void setExternalDependencies(List<String> externalDependencies) {
        this.externalDependencies = externalDependencies;
    }

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className
     *            the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the packageName
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @param packageName
     *            the packageName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
