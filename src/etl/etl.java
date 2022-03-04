/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etl;

import etl.archivos.aProductos;
import com.formdev.flatlaf.IntelliJTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import system.Consola;

/**
 *
 * @author Juan Bogado
 */
public class etl extends JFrame {
    public final static boolean DEBUG = false;
    
    public Properties configuracion;
    
    public static double JAVA_VERSION;
    public static int JAVA_MODEL;
    public boolean JAVA_VALID = true;
    public boolean DVY_VALID = true;
    
    public Connection conexion = null;
    public Statement sentencia = null;
    public ResultSet resultado = null;
    
    public Dimension windows;
    
    aProductos AProductos;

    /**
     * Creates new form mail
     */
    public etl() {
        initComponents();
        initVisuals();
        version();
        checkConnection();
        
        
        abrirAProductos();
    }
    
    private void checkConnection(){
        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            conexion = DriverManager.getConnection("jdbc:odbc:Discovery", "", "");
            if(DEBUG){
                Consola.out(Color.green, "Conectado!");
                System.out.println("Catalog: "+conexion.getCatalog());
                System.out.println("StringFunctions: "+conexion.getMetaData().getStringFunctions());
                System.out.println("SQLKeywords: "+conexion.getMetaData().getSQLKeywords());
                System.out.println("SearchStringEscape: "+conexion.getMetaData().getSearchStringEscape());
                System.out.println("CatalogTerm: "+conexion.getMetaData().getCatalogTerm());
                System.out.println("Catalog: "+conexion.getMetaData().getConnection().getCatalog());
                System.out.println("DatabaseProductName: "+conexion.getMetaData().getDatabaseProductName());
                System.out.println("SystemFunctions: "+conexion.getMetaData().getSystemFunctions());
                System.out.println("StringFunctions: "+conexion.getMetaData().getStringFunctions());
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(etl.class.getName()).log(Level.SEVERE, null, ex);
            eDiscovery.setText("Error");
            eDiscovery.setIcon(null);
            eDiscovery.setToolTipText(ex.getMessage());
        }
        
    }
    
    
    
    /**********************************************************************************************************/
    
    private void abrirAProductos(){
        if(AProductos == null){
            AProductos = new aProductos(this);
        }else{
            if(!AProductos.isDisplayable()){
                dp.remove(AProductos);
                AProductos = new aProductos(this);
            }
        }
        centrarVentana(AProductos);
    }
    
    public void initVisuals(){
        this.setLocationRelativeTo(null);
        //setJMenuBar(null);
        guiMenu.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "none");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth(), (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
    }
    
    private void version() {
        String version = System.getProperty("java.version");
        int pos = version.indexOf('.');
        pos = version.indexOf('.', pos+1);
        JAVA_VERSION = Double.parseDouble (version.substring (0, pos));
        JAVA_MODEL = Integer.parseInt(System.getProperty("sun.arch.data.model"));
        
        eJavaVersion.setText("JRE "+String.valueOf(JAVA_VERSION));
        eJavaArchitecture.setText("x"+String.valueOf(JAVA_MODEL));
        
        if(JAVA_VERSION > 1.8){
            eMensaje.setText("Version no compatible con ODBC.");
            JAVA_VALID = false;
        }else if(JAVA_VERSION < 1.7){
            eMensaje.setText("Version desactualizada de JRE.");
        }
        
        if(JAVA_MODEL !=32){
            eMensaje.setText("Arquitectura no compatible.");
            JAVA_VALID = false;
        }
        
        //JAVA_VALID = true; // debug
        
        if(JAVA_VALID){
            //menu.getMenu(0).setEnabled(true);
            //menu.getMenu(1).setEnabled(true);
            
            //if(DEBUG)
                //Consola.out(Color.green, "Version de java valido");
        }else{
            JOptionPane.showMessageDialog(this, "La version de java o la arquitectura no son válidos, instale unicamente JRE7x32", "Version no compatible: JRE"+JAVA_VERSION+" x"+JAVA_MODEL, JOptionPane.ERROR_MESSAGE);
        }
        
        if(DEBUG)
        System.out.println(System.getProperties());
        
    }
    
    
    
    /******/
    public void centrarVentana(JInternalFrame ventana){
        if(!ventana.isDisplayable())
            dp.add(ventana);
        
        windows = this.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        
        int x_screen = (int) screen.getHeight();
        int x = ( (int) screen.width / 2) - ventana.getWidth()/2;
        int y = ( (int) screen.height / 2) - ventana.getHeight()/2;
        
        
        ventana.setLocation(x,y);
        ventana.toFront();
        ventana.setVisible(true);
        try {
            ventana.setSelected(true);
        } catch (PropertyVetoException ex) {
            System.out.println("No se pudo seleccionar");
          
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dp = new javax.swing.JDesktopPane();
        estado = new javax.swing.JPanel();
        eMensaje = new javax.swing.JLabel();
        eJavaVersion = new javax.swing.JLabel();
        eDiscovery = new javax.swing.JLabel();
        eJavaArchitecture = new javax.swing.JLabel();
        guiMenu = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ETL: DISCV <> ODOO");

        dp.setBackground(new java.awt.Color(255, 255, 255));
        dp.setToolTipText("");

        javax.swing.GroupLayout dpLayout = new javax.swing.GroupLayout(dp);
        dp.setLayout(dpLayout);
        dpLayout.setHorizontalGroup(
            dpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1024, Short.MAX_VALUE)
        );
        dpLayout.setVerticalGroup(
            dpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 727, Short.MAX_VALUE)
        );

        eMensaje.setText("Analytix");

        eJavaVersion.setText("JRE 1.8");
        eJavaVersion.setPreferredSize(new java.awt.Dimension(40, 14));

        eDiscovery.setPreferredSize(new java.awt.Dimension(40, 14));

        eJavaArchitecture.setText("x64");
        eJavaArchitecture.setPreferredSize(new java.awt.Dimension(40, 14));

        javax.swing.GroupLayout estadoLayout = new javax.swing.GroupLayout(estado);
        estado.setLayout(estadoLayout);
        estadoLayout.setHorizontalGroup(
            estadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(estadoLayout.createSequentialGroup()
                .addComponent(eMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(eJavaVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(eJavaArchitecture, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(eDiscovery, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        estadoLayout.setVerticalGroup(
            estadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, estadoLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(estadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(eDiscovery, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(estadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(eMensaje)
                        .addComponent(eJavaVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(eJavaArchitecture, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        jMenu1.setText("Archivos");

        jMenuItem1.setText("Productos");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText("Clientes");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        guiMenu.add(jMenu1);

        jMenu2.setText("Edit");
        guiMenu.add(jMenu2);

        setJMenuBar(guiMenu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(dp)
            .addComponent(estado, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(dp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(estado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        abrirAProductos();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
         
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(etl.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(etl.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(etl.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(etl.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new etl().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDesktopPane dp;
    private javax.swing.JLabel eDiscovery;
    private javax.swing.JLabel eJavaArchitecture;
    private javax.swing.JLabel eJavaVersion;
    private javax.swing.JLabel eMensaje;
    private javax.swing.JPanel estado;
    private javax.swing.JMenuBar guiMenu;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    // End of variables declaration//GEN-END:variables
}
