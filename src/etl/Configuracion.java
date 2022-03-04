/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Juan Bogado
 */
public class Configuracion {
    public static FileInputStream fisConfiguracion = null;
    public static OutputStream osConfiguracion = null;
    public static File archivo;
    
    public static String confdir =  "conf";
    public static String subdir =  "/";
    public static String confext =  ".cfg";
    
    
    public static void loadProperties(Properties prop, String file){
        try {
            fisConfiguracion = new FileInputStream(confdir+subdir+file+confext);
            prop.load(fisConfiguracion);
            fisConfiguracion.close();
            
        } catch (FileNotFoundException ex) {
            System.out.println("No existe archivo de configuracion, se crea.");
            newProperties(prop, file);
        } catch (IOException ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
            newProperties(prop, file);
        } catch (NullPointerException ex) {
            System.out.println("El archivo de configuracion de "+file+" esta vacio.");
            newProperties(prop, file);
        } catch (Exception ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void newProperties(Properties prop, String file){
        try {
            archivo = new File(confdir+subdir);
            archivo.mkdirs();
            
            osConfiguracion = new FileOutputStream(confdir+subdir+file+confext);
            
            switch (file) {
                case "productos":
                    prop.setProperty("deposito", "1");
                    prop.setProperty("cantidadMinima", "1");
                    prop.setProperty("anhos", "20,21,22,23,24,25");
                    prop.setProperty("procedencias", "");
                    prop.setProperty("tipos", "");
                    prop.setProperty("nombres", "");
                    prop.setProperty("colores", "");
                    prop.setProperty("sexos", "");
                    prop.setProperty("tamanhos", "");
                    prop.setProperty("descripcion", "");
                    break;
                default:
                    break;
            }
            
            prop.store(osConfiguracion, null);
            osConfiguracion.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    public static void saveProperties(Properties prop, String file){
        try {
            archivo = new File(confdir+subdir);
            archivo.mkdirs();
            
            osConfiguracion = new FileOutputStream(confdir+subdir+file+confext);
            
            
            prop.store(osConfiguracion, null);
            osConfiguracion.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
