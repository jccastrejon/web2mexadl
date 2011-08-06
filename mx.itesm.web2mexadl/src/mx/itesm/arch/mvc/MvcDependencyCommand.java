package mx.itesm.arch.mvc;

import mx.itesm.arch.dependencies.DependencyCommand;

/**
 * Dependency Command that adds the project's web pages and configuration files
 * to the dependency analysis.
 * 
 * @author jccastrejon
 * 
 */
public class MvcDependencyCommand implements DependencyCommand {

    /**
     * Valid components to be analyzed.
     */
    private static String[] VALID_TYPES = MvcAnalyzer.getPropertyValues(MvcAnalyzer.Variable.Type.getVariableName());

    @Override
    public String execute(String fileName) {
        String returnValue;

        returnValue = null;
        for (String validType : MvcDependencyCommand.VALID_TYPES) {
            if (fileName.endsWith(validType)) {
                returnValue = fileName;
                if (returnValue.indexOf('/') < 0) {
                    returnValue = "/" + returnValue;
                }

                break;
            }
        }

        return returnValue;
    }

    @Override
    public String[] getValidFileTypes() {
        return MvcDependencyCommand.VALID_TYPES;
    }
}