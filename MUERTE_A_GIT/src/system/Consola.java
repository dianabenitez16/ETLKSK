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
public class Consola {
    public static void out(Color color,String str){      
        String bg, fg;
        if (color == Color.GREEN){
            fg = "32";
            bg = "42";
        }else if(color == Color.RED){
            fg = "31";
            bg = "41";
        }else if(color == JColor.green){
            fg = "32";
            bg = "42";
        }else if(color == JColor.red){
            fg = "31";
            bg = "41";
        }else if(color == JColor.blue){
            fg = "34";
            bg = "44";
        }else if(color == Color.YELLOW){
            fg = "33";
            bg = "43";
        }else if(color == Color.BLUE){
            fg = "34";
            bg = "44";
        }else if(color == Color.MAGENTA){
            fg = "35";
            bg = "45";
        }else if(color == Color.CYAN){
            fg = "36";
            bg = "46";
        }else if(color == Color.WHITE){
            fg = "37";
            bg = "47";
        }else{
            fg = "30";
            bg = "40";
        }
        
        System.out.println((char)27 + "["+bg+";"+fg+"m"+ str + (char)27 + "[0m");
    }
    
    public static void out(String str){      
        System.out.println("   "+str);
    }
}
