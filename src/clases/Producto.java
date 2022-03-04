/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clases;

import java.util.List;

/**
 *
 * @author Juan Bogado
 */
public class Producto {
    private Integer ID;
    private String codigoDISCV;
    private String referenciaInterna;
    private Categoria[] categorias;
    private Tamanho tamanho;
    private Website website;
    private Boolean publicado;
    private Boolean activo;
    
    private String nombre;
    private Double precioVenta;
    private Double precioCosto;
    private Double stockTotal;
    private Double stockSucursal;
    
    private String anho;
    private String procedencia;
    private String tipo;
    private String modelo;
    private String color;
    private String sexo;

    public Producto() {
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer id) {
        this.ID = id;
    }

    public String getCodigoDISCV() {
        return codigoDISCV;
    }

    public void setCodigoDISCV(String codigoDISCV) {
        this.codigoDISCV = codigoDISCV;
    }
    
    public String getReferenciaInterna() {
        return referenciaInterna;
    }

    public void setReferenciaInterna(String referenciaInterna) {
        this.referenciaInterna = referenciaInterna.trim();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(Double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Double getPrecioCosto() {
        return precioCosto;
    }

    public void setPrecioCosto(Double precioCosto) {
        this.precioCosto = precioCosto;
    }

    public Double getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(Double stockTotal) {
        this.stockTotal = stockTotal;
    }

    public Double getStockSucursal() {
        return stockSucursal;
    }

    public void setStockSucursal(Double stockSucursal) {
        this.stockSucursal = stockSucursal;
    }

    public String getAnho() {
        return anho;
    }

    public void setAnho(String anho) {
        this.anho = anho;
    }

    public String getProcedencia() {
        return procedencia;
    }

    public void setProcedencia(String procedencia) {
        this.procedencia = procedencia;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public Categoria[] getCategorias() {
        return categorias;
    }

    public void setCategorias(Categoria[] categorias) {
        this.categorias = categorias;
    }

    public Website getWebsite() {
        return website;
    }

    public void setWebsite(Website website) {
        this.website = website;
    }

    public Boolean getPublicado() {
        return publicado;
    }

    public void setPublicado(Boolean publicado) {
        this.publicado = publicado;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    public Object[] getArrayCategorias(){
        Object[] resultado = new Object[this.categorias.length];
        for (int i = 0; i < categorias.length; i++) {
            resultado[i] = categorias[i].getID();
            System.out.println(" CATEGORIAS " +categorias[i].getID() );
        }
        return resultado;
    }
    /*
    public Object[] getArrayTamanho(){
        Object[] resultados = new Object[this.tamanho.length];
        for (int i = 0; i < tamanho.length; i++) {
            resultados[i] = tamanho[i].getID();
        }
        return resultados;
    }*/
    
    public void imprimir(){
        System.out.println("");
        System.out.println("ID: "+this.getID());
        System.out.println("default_code: "+this.getReferenciaInterna());
        System.out.println("name: "+this.getNombre());
        System.out.println("list_price: "+this.getPrecioVenta());
        System.out.println("standard_price: "+this.getPrecioCosto());
        System.out.println("is_published: "+this.getPublicado());
        if(this.getCategorias() != null){
            for (Categoria categoria : this.getCategorias()) {
                System.out.println("public_categ_ids: "+categoria.getID());
            }
    
        System.out.println("website_id: "+this.getWebsite().getID());
    }
    
    }}
