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
public class Tamanho {
    private Integer IdValor; //product_attribute_value_id [(8) XS]
    private Integer IdAtributo; //attribute_id [(1) Tamaño]
    private Integer IdTemplateValor; //id [(85) XS ] - Asociado a un template de producto
    private Integer IdTemplateAtributo; //attribute_line_id [(55) Tamaño] - Asociado a un template de producto
    private String nombre;
    
    public Tamanho() {
    }

        
    public Integer getIdValor() {
        return IdValor;
    }

    public void setIdValor(Integer IdValor) {
        this.IdValor = IdValor;
    }

    public Integer getIdAtributo() {
        return IdAtributo;
    }

    public void setIdAtributo(Integer IdAtributo) {
        this.IdAtributo = IdAtributo;
    }

    public Integer getIdTemplateAtributo() {
        return IdTemplateAtributo;
    }

    public void setIdTemplateAtributo(Integer IdTemplateAtributo) {
        this.IdTemplateAtributo = IdTemplateAtributo;
    }

    public Integer getIdTemplateValor() {
        return IdTemplateValor;
    }

    public void setIdTemplateValor(Integer IdTemplateValor) {
        this.IdTemplateValor = IdTemplateValor;
    }


    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre.trim();
    }
    
    public void imprimir(){
        System.out.print("ID: "+this.getIdValor());
        System.out.print("\tNO: "+this.nombre);
        System.out.println("");
    }
    
}
