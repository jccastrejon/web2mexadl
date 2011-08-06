package mx.itesm.arch.mvc;

import weka.core.Attribute;
import weka.core.FastVector;

/**
 * MVC Layer.
 * 
 * @author jccastrejon
 * 
 */
public enum Layer {
    Model("#B1FF3D", "filled") {
        @Override
        public boolean isValidRelation(final Layer layer) {
            boolean returnValue;

            // The Model layer should only depend on itself
            returnValue = true;
            if (layer != Model) {
                returnValue = false;
            }

            return returnValue;
        }
    },
    View("#3399FF", "filled") {
        @Override
        public boolean isValidRelation(final Layer layer) {
            boolean returnValue;

            // The View layer shouldn't depend directly on the Model, the
            // Controller should provide the required beans, of if the model
            // classes are used directly on the view, the class names shouldn't
            // be directly exposed
            returnValue = true;
            if (layer == Model) {
                returnValue = false;
            }

            return returnValue;
        }
    },
    Controller("#A3A3A3", "filled") {
        @Override
        public boolean isValidRelation(final Layer layer) {
            // The Controller layer is the intermediary between all layers
            return true;
        }
    },
    InvalidModel("#B1FF3D", "rounded"), InvalidView("#3399FF", "rounded"), InvalidController("#A3A3A3", "rounded");

    /**
     * Layer style used in the graphic export.
     */
    private String style;

    /**
     * RGB color associated to the layer, used in the graphic export.
     */
    private String rgbColor;

    /**
     * Variable attribute.
     */
    public static Attribute attribute;

    static {
        FastVector valuesVector;

        // Initialize the attribute's data
        valuesVector = new FastVector(Layer.values().length);
        for (Layer layer : Layer.values()) {
            valuesVector.addElement(layer.toString());
        }

        attribute = new Attribute("Layer", valuesVector, MvcAnalyzer.Variable.values().length + 1);
    }

    /**
     * Check if a relation with the given layer is valid or not in a MVC design.
     * 
     * @param layer
     *            Layer to compare with.
     * @return <em>true</em> if the relation is valid, <em>false</em> otherwise.
     */
    public boolean isValidRelation(final Layer layer) {
        return true;
    }

    /**
     * Full constructor that specifies the color to be used in the graphic
     * export.
     * 
     * @param rgbColor
     *            RGB color.
     * @param style
     *            Layer style.
     * 
     */
    private Layer(final String rgbColor, final String style) {
        this.rgbColor = rgbColor;
        this.style = style;
    }

    /**
     * Get the Layer's RGB color.
     * 
     * @return RGB color.
     */
    public String getRgbColor() {
        return this.rgbColor;
    }

    /**
     * Get the Layer's style.
     * 
     * @return Layer's style.
     */
    public String getStyle() {
        return this.style;
    }
}
