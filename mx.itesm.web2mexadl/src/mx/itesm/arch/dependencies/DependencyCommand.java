package mx.itesm.arch.dependencies;

/**
 * Command to execute during the dependency analysis process.
 * 
 * @author jccastrejon
 * 
 */
public interface DependencyCommand {

    /**
     * Execute an action during the dependency analysis process.
     * 
     * @param fileName
     *            Name of the file being analyzed.
     * @return Name of the dependency to be added to the dependency analysis
     *         process, or <em>null</em> if the specified file is not valid for
     *         this command.
     */
    public String execute(final String fileName);

    /**
     * Get the valid file types for this command.
     * 
     * @return Array containing the command's valid file types.
     */
    public String[] getValidFileTypes();
}
