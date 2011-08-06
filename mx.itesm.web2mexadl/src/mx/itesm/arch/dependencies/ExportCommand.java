package mx.itesm.arch.dependencies;

/**
 * Command to execute during the graphic export process.
 * 
 * @author jccastrejon
 * 
 */
public interface ExportCommand {

    /**
     * Execute an action during the graphic export of the specified class'
     * dependencies.
     * 
     * @param classDependencies
     *            Class' Dependencies.
     */
    public String execute(final ClassDependencies classDependencies);

    /**
     * Get the description of this export command.
     * 
     * @return Command description.
     */
    public String getDescription();
}
