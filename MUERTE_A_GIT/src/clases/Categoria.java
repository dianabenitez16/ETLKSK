/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clases;

/**
 *
 * @author Juan Bogado
 */
public class Categoria {
    private String ID;
    private String referenciaExterna;
    private String nombre;

    public Categoria() {
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID.trim();
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna.trim();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre.trim();
    }
    
    
    public void imprimir(){
        System.out.print("ID: "+this.ID);
        System.out.print("\tRE: "+this.referenciaExterna);
        System.out.print("\tNO: "+this.nombre);
        System.out.println("");
    }
    
}
