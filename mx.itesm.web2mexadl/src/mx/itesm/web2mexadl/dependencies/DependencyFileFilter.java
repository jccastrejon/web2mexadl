/*
 * Copyright 2011 jccastrejon
 *  
 * This file is part of Web2MexADL.
 *
 * Web2MexADL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Web2MexADL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Web2MexADL.  If not, see <http://www.gnu.org/licenses/>.
 */
package mx.itesm.web2mexadl.dependencies;

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
