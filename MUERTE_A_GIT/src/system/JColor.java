/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

import java.awt.Color;

/**
 *
 * @author juan.bogado
 */
public class JColor extends Color{
    public static Color green = new Color(0,100,50);
    public static Color blue = new Color(0,75,150);
    public static Color red = new Color(150,0,0);
    
    public JColor(int r, int g, int b) {
        super(r, g, b);
    }
    
}
