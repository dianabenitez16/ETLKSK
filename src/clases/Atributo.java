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
public class Atributo {
    private Integer ID;
    private String nombre;
    private AtributoValor valor;
    private AtributoValor[] valores;

    public Atributo() {
    }

    
    
    public Integer getID() {
        return ID;
    }

    public void setID(Integer id) {
        this.ID = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public AtributoValor getValor() {
        return valor;
    }

    public void setValor(AtributoValor valor) {
        this.valor = valor;
    }

    public AtributoValor[] getValores() {
        return valores;
    }

    public void setValores(AtributoValor[] valores) {
        this.valores = valores;
    }
    
    
    
}

