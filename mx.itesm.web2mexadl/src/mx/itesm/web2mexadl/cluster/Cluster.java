package mx.itesm.web2mexadl.cluster;

import java.awt.Color;

/**
 * 
 * @author jccastrejon
 * 
 */
public class Cluster {
    /**
     * RGB color associated to the layer, used in the graphic export.
     */
    private Color color;

    /**
     * 
     */
    private String style;

    /**
     * 
     * @param color
     */
    public Cluster(final Color color) {
        this.color = color;
        this.style = "filled";
    }

    public Color getColor() {
        return color;
    }

    public String getHexColor() {
        return "#" + Integer.toHexString(this.color.getRGB() & 0x00ffffff).toUpperCase();
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
