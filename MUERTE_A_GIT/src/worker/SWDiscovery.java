/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worker;

import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import system.Consola;
import system.JColor;

/**
 *
 * @author juan.bogado
 */
public class SWDiscovery {
    private final static boolean DEBUG = true;
    
    public Consultar consultar;
    
    boolean conectado;
    boolean finalizado;
    public JLabel estado;
    
    Connection conexion = null;
    Statement sentencia = null;
    ResultSet resultado = null;
    String query;
    
    public Integer largo;
    public Integer ancho;
    
    String ultimoMensaje;

    public SWDiscovery(JLabel estado) {
        this.estado = estado;
        this.ultimoMensaje = "";
        
    }
    
    public void consultar(String query){
        consultar = new Consultar(query);
    }
    
    public void print(String mensaje){
        if(!ultimoMensaje.equals(mensaje)){
            ultimoMensaje = mensaje;
            Color color;
            Boolean error = false;

            if(conectado){
                if(finalizado){
                    color = JColor.green;
                }else{
                    color = JColor.blue;
                }
            }else{
                color = JColor.red;
                error = true;
            }

            estado.setText(mensaje);
            estado.setForeground(color);

            if(DEBUG){
                if(error){
                    Consola.out(color, mensaje+ "\n"+query);
                }else{
                    Consola.out(color, mensaje);
                }
            }
        }
    }

    
    public class Consultar extends SwingWorker<Object[][], String> {
       
        List<List<String>> registros;
        List<String> registro;
        
        public Object[][] datatypes;
        public String[][] encabezado;
        public String tabla;
        public String proceso;
        
        public Consultar(String consulta){
            query = consulta;
            System.out.println("QUERY: "+query);
            Integer indexFrom = query.indexOf("FROM");
            Integer indexTablaIni = indexFrom + 5;
            Integer indexTablaFin = (query.indexOf(" ",indexTablaIni)<0?query.length():query.indexOf(" ",indexTablaIni));
            tabla = query.substring(indexTablaIni, indexTablaFin).trim();
            
            
            estado.setText("");
            estado.setForeground(JColor.blue);
            
            conectado = false;
            finalizado = false;
            largo = 0;
            ancho = 0;
        }
        
        @Override
        protected Object[][] doInBackground(){
            publish("Conectando...");
            try{
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
                conexion = DriverManager.getConnection("jdbc:odbc:Discovery", "", "");
                conectado = true;
                publish("Consultando...");
                
                // Obteniendo cantidad de registros
                
                sentencia = conexion.createStatement();
                resultado = sentencia.executeQuery(query);
                while(resultado.next()){
                    largo++;
                }
                
                // Hacinedo conuslta principal
                sentencia = conexion.createStatement();
                resultado = sentencia.executeQuery(query);
                ancho = resultado.getMetaData().getColumnCount();
                
                encabezado = new String[1][ancho];
                datatypes = new Object[largo][ancho];
                                
                for (int i = 1; i <= ancho; i++) {
                    encabezado[0][i-1] = resultado.getMetaData().getColumnLabel(i);
                }
                
                while (resultado.next()) {
                    for (int i = 0; i < ancho; i++) {
                        datatypes[resultado.getRow()-1][i] = resultado.getObject(i+1);
                    }
                    publish("Procesando consulta "+ resultado.getRow()*100/largo + "%");
                    //publish("Procesando registro "+ resultado.getRow());
                }
                
                resultado.close();
                sentencia.close();
                conexion.close();
                
                finalizado = true;
                publish("Encontrado "+largo+" registro(s).");
            }catch (ClassNotFoundException | SQLException e){
                conectado = false;
                publish("Error al acceder a los datos.");
                Consola.out(JColor.red,"Error al acceder a los datos.");
                System.out.println(e);
            }catch (Exception e){
                publish("Error desconocido.");
                Consola.out(JColor.red,"Error desconocido.");
                System.out.println(e);
            }
                
            return datatypes;
        }
        
        @Override
        protected void process(List<String> publish){
            print(publish.get(publish.size()-1));
        }

        /*
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String clase = getClass().getName().substring(getClass().getName().lastIndexOf(".")+1, getClass().getName().length()).toUpperCase();
            String source = evt.getSource().toString().substring(evt.getSource().toString().lastIndexOf(".")+1, evt.getSource().toString().indexOf("@"));
            String value = evt.getNewValue().toString();
            evt.setPropagationId(tabla);

            Consola.out(JColor.ORANGE,"[CLASE]: "+clase+ " [SOURCE]: "+source+ " [VALUE]:"+value);
        }
        */
        public String getQuery(){
            return query;
        }


    }

}
