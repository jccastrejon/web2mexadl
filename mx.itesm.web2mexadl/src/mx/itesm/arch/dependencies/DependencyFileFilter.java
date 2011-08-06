package mx.itesm.arch.dependencies;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

/**
 * File Filter used in the dependency analysis of directories.
 * 
 * @author jccastrejon
 * 
 */
public class DependencyFileFilter implements FilenameFilter {

    /**
     * File types besides .class to be included in the dependency analysis.
     */
    private List<String> extraFileTypes;

    /**
     * Default constructor.
     */
    public DependencyFileFilter() {
    }

    /**
     * Full constructor that specifies additional extra types besides .class to
     * be included in the dependency analysis.
     * 
     * @param extraFileTypes
     *            Extra file types.
     */
    public DependencyFileFilter(final List<String> extraFileTypes) {
        this.extraFileTypes = extraFileTypes;
        this.extraFileTypes.remove("java");
    }

    @Override
    public boolean accept(final File dir, final String name) {
        String extension;
        File file;
        boolean returnValue;

        returnValue = false;
        file = new File(dir.getAbsolutePath() + "/" + name);
        if (file.isDirectory()) {
            returnValue = true;
        } else {
            extension = this.getExtensionName(file);
            if (extension != null) {
                if (this.extraFileTypes != null) {
                    for (String fileType : extraFileTypes) {
                        if (fileType.equalsIgnoreCase(extension)) {
                            returnValue = true;
                            break;
                        }
                    }
                }

                if (!returnValue) {
                    if (extension.equals("class")) {
                        returnValue = true;
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Get a file extension from a file.
     * 
     * @param file
     *            File.
     * @return File's extension.
     */
    public String getExtensionName(final File file) {
        String returnValue = null;
        String name = file.getName();
        int i = name.lastIndexOf('.');

        if (i > 0 && i < name.length() - 1) {
            returnValue = name.substring(i + 1).toLowerCase();
        }
        return returnValue;
    }

    /**
     * @return the extraFileTypes
     */
    public List<String> getExtraFileTypes() {
        return extraFileTypes;
    }

    /**
     * @param extraFileTypes
     *            the extraFileTypes to set
     */
    public void setExtraFileTypes(List<String> extraFileTypes) {
        this.extraFileTypes = extraFileTypes;
    }
}
