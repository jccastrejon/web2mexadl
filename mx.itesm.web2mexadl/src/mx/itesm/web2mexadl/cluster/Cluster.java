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
package mx.itesm.web2mexadl.cluster;

import java.awt.Color;

/**
 * This class represents a Cluster in a software architecture.
 * 
 * @author jccastrejon
 * 
 */
public class Cluster {
    /**
     * RGB color associated to the cluster, used in the graphic export.
     */
    private Color color;

    /**
     * Style associated to the cluster, used in the graphic export.
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

    // Getters - Setters

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
