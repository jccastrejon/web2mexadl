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
package mx.itesm.web2mexadl.util;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import mx.itesm.web2mexadl.mvc.MvcAnalyzer;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.SerializationHelper;

/**
 * Contains utility methods used in the analysis of web applications.
 * 
 * @author jccastrejon
 * 
 */
public class Util {

    /**
     * Template for main implementation packages.
     */
    public static final String MAIN_IMPLEMENTATION = "<javaimplementation:mainClass xsi:type='javaimplementation:JavaClassFile'>"
            + "<javaimplementation:javaClassName xsi:type='javaimplementation:JavaClassName'>PACKAGE..**</javaimplementation:javaClassName>"
            + "</javaimplementation:mainClass>";

    /**
     * Template for auxiliary implementation packages.
     */
    public static final String AUX_IMPLEMENTATION = "<javaimplementation:auxClass xsi:type='javaimplementation:JavaClassFile'>"
            + "<javaimplementation:javaClassName xsi:type='javaimplementation:JavaClassName'>PACKAGE..**</javaimplementation:javaClassName>"
            + "</javaimplementation:auxClass>";

    /**
     * Path to the properties file containing the model's variables.
     */
    private final static String PROPERTIES_FILE_PATH = "/mvc-variables-grails-play-struts-roo.properties";

    /**
     * Path to the file containing the model's classifier.
     */
    private final static String CLASSIFIER_FILE_PATH = "/mvc-classifier-grails-play-struts-roo.model";

    /**
     * Class logger.
     */
    private static Logger logger = Logger.getLogger(MvcAnalyzer.class.getName());

    /**
     * Properties file containing the variables data.
     */
    public static Properties classifierVariables;

    /**
     * MVC Classifier.
     */
    public static Classifier classifier;

    /**
     * Random Variables used in the Uncertainty model.
     * 
     * @author jccastrejon
     * 
     */
    public enum Variable {
        Type("variable.type"), ExternalAPI("variable.externalApi"), Suffix("variable.suffix");

        /**
         * Variable property name.
         */
        private String variableName;

        /**
         * Variable attribute.
         */
        private Attribute attribute;

        /**
         * Constructor that specifies the Variable's property name. The
         * attribute's values are read from the
         * <em>MvcAnalyzer.classifierVariables</em> properties.
         * 
         * @param variableName
         *            Variable Name.
         */
        private Variable(final String variableName) {
            String[] propertyValues;
            FastVector valuesVector;

            // Load property values
            propertyValues = Util.getPropertyValues(variableName);
            valuesVector = new FastVector(propertyValues.length);
            for (String propertyValue : propertyValues) {
                valuesVector.addElement(propertyValue);
            }

            this.variableName = variableName;
            this.attribute = new Attribute(this.toString(), valuesVector, this.ordinal());
        }

        /**
         * Get the variables property name.
         * 
         * @return Property name.
         */
        public String getVariableName() {
            return this.variableName;
        }

        /**
         * Get the variable's attribute.
         * 
         * @return Attribute.
         */
        public Attribute getAttribute() {
            return this.attribute;
        }
    };

    /**
     * Initialize properties file.
     */
    static {
        Util.classifierVariables = new Properties();

        try {
            Util.classifierVariables.load(MvcAnalyzer.class.getResourceAsStream(Util.PROPERTIES_FILE_PATH));

            Util.classifier = (Classifier) SerializationHelper.read(MvcAnalyzer.class
                    .getResourceAsStream(Util.CLASSIFIER_FILE_PATH));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Properties file: " + Util.PROPERTIES_FILE_PATH + " could not be read", e);
        }
    }

    /**
     * Get a property's values, specified in the MvcAnalyzer.classifierVariables
     * properties file.
     * 
     * @param propertyName
     *            Property name.
     * @return Property's values.
     */
    public static String[] getPropertyValues(final String propertyName) {
        String[] returnValue;

        if ((propertyName == null) || (!Util.classifierVariables.containsKey(propertyName))) {
            throw new IllegalArgumentException("Invalid property: " + propertyName);
        }

        // Make sure all the values are in lower case
        returnValue = Util.classifierVariables.getProperty(propertyName).split(",");
        for (int i = 0; i < returnValue.length; i++) {
            returnValue[i] = returnValue[i].toLowerCase();
        }

        return returnValue;
    }

    /**
     * Add an implementation package (either main or auxiliary) info.
     * 
     * @param currentPackages
     * @param currentPackage
     */
    public static void addImplementationPackage(final StringBuilder currentPackages, final String currentPackage) {
        if ((currentPackage.indexOf('.') > 0) && (currentPackage.indexOf('/') < 0)) {
            if (currentPackages.length() == 0) {
                currentPackages.append(Util.MAIN_IMPLEMENTATION.replace("PACKAGE", currentPackage)).append("\n");
            } else {
                currentPackages.append(Util.AUX_IMPLEMENTATION.replace("PACKAGE", currentPackage)).append("\n");
            }
        }
    }
}
