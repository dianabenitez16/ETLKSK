/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etl.archivos;

import clases.Categoria;
import clases.Producto;
import clases.Tamanho;
import clases.Website;
import etl.Configuracion;
import etl.etl;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import system.Consola;
import system.JColor;
import worker.SWDiscovery;
import worker.SWDiscovery.Consultar;

/**
 *
 * @author Juan Bogado
 */
public class aProductos extends javax.swing.JInternalFrame implements PropertyChangeListener{
    public Properties configuracion;
    
    private XmlRpcClient odooCliente;
    private XmlRpcClientConfigImpl odooConfigCommon;
    private XmlRpcClientConfigImpl odooConfigObject;
    private Boolean odooBandera;
    private Integer odooID;
    private HashMap odooRespuesta;
    private List<Object> odooRegistros;
    private Producto[] odooProductos;
    private Website[] odooWebsites;
    private Categoria[] odooCategorias;
    private Tamanho[] odooTamanho;
    private Integer odooUID;
    private String odooURL, odooDB, odooUser, odooPassword;
    
    private Producto[] discvProductos;
    
    private List<Producto> odooUpdateProductos;
    private List<Producto> odooInsertProductos;
    private List<Producto> odooDeleteProductos;
    private List<Producto> odooNoneProductos;
    
    private HashMap odooWriteAttributes;
    
    
    private Categoria categoria;
    private Tamanho tamanho;
    
    
    SWDiscovery SWDVY;
    String query;
    
    String talonariosfactura;
    String talonariosncr;
    
    Integer cantidadMinima;
    Integer deposito;
    
    File maestroProductos;
    File maestroCategorias;
    
    private Categoria categorias[];
    private Tamanho tamanhos[];
    public aProductos(etl etl) {
        initComponents();
        SWDVY = new SWDiscovery(eMensaje);
        loadConfig();
        initListeners();
        odooStart();
        odooLogin();
        

        //FOR DEBUG
        odooModeloListarT();
    }
    
    private boolean loadConfig(){
        configuracion = new Properties();
        try{
            String error = "";
            Configuracion.loadProperties(configuracion, "productos");
            
            tDeposito.setText(configuracion.getProperty("deposito"));
            tCantidadMinima.setText(configuracion.getProperty("cantidadMinima"));
            lAnho.setText(configuracion.getProperty("anhos"));
            lProcedencia.setText(configuracion.getProperty("procedencias"));
            lTipo.setText(configuracion.getProperty("tipos"));
            lNombre.setText(configuracion.getProperty("nombres"));
            lColor.setText(configuracion.getProperty("colores"));
            lSexo.setText(configuracion.getProperty("sexos"));
            lTamanho.setText(configuracion.getProperty("tamanhos"));
            
            
            error += lAnho.getText().trim() == null ? "anhos": "";
                        
            if(error.isEmpty()){
                return true;
            }else{
                JOptionPane.showMessageDialog(this, "Error al cargar archivo de configuracion de Hechauka, verifique la variable "+error+".");
                return false;
            }
            
            
        }catch (Exception ex){
            System.out.println("Error al cargar configuracion.");
            Logger.getLogger(Configuracion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    private void initListeners(){
        tOdooTestModeloInsertRefenciaExterna.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) { 
                if (tOdooTestModeloInsertRefenciaExterna.getText().length() >= 2 ) 
                    e.consume(); 
            }
        });
    }
    
    public void extraerDatos(){
        limpiar(false);
                
        // Se establecen los parametros del query (Cantidad minima y Desosito)
        cantidadMinima = 0;
        if(!tCantidadMinima.getText().trim().isEmpty()){
            cantidadMinima = Integer.valueOf(tCantidadMinima.getText().trim());
        }
        deposito = 0;
        if(!tDeposito.getText().trim().isEmpty()){
            deposito = Integer.valueOf(tDeposito.getText().trim());
        }
        
        query =   "select itm_cod as Codigo, itm_des as Descripcion, itm_pr1 as Venta, itm_pr4 as Costo, itm_act as StockTotal, ppd_act as StockSucursal "
                + "from productos inner join existencias_por_deposito on itm_cod = ppd_itm "
                + "where ppd_dep = " + deposito +" and ppd_act >= "+cantidadMinima;
               // new String [] { "Codigo", "Descripcion", "Venta", "Costo", "Stock Total", "Stock Suc." }
        
        SWDVY.consultar(query);
        SWDVY.consultar.addPropertyChangeListener(this);
        SWDVY.consultar.execute();   
    }
    
    public void procesarDatos(){
        
        Boolean bandera = false;
        Integer registros = 0;
        
        Object[][] datosEnProceso = new Object[SWDVY.consultar.datatypes.length][SWDVY.consultar.datatypes[0].length];
        Object[][] discvTablaContenido;
        
        List<String> anhos =        Arrays.asList(lAnho.getText().split(","));
        List<String> procedencias =  Arrays.asList(lProcedencia.getText().split(","));
        List<String> tipos =         Arrays.asList(lTipo.getText().split(","));
        List<String> nombres =       Arrays.asList(lNombre.getText().split(","));
        List<String> colores =        Arrays.asList(lColor.getText().split(","));
        List<String> sexos =         Arrays.asList(lSexo.getText().split(","));
        List<String> tamanhos =      Arrays.asList(lTamanho.getText().split(","));
        
        //SE DEBERIA DE OPTIMIZAR ESTE CODIGO
        for (Object[] registro : SWDVY.consultar.datatypes) {
            if(registro[0].toString().trim().length() == 13){
                //BANDERA AÑO
                if(anhos.size() > 0){
                    for (String anho : anhos) {
                        if(registro[0].toString().substring(0, 2).contains(anho)){
                            bandera = true;
                            break;
                        }else{
                            bandera = false;
                        }
                    }
                }

                //BANDERA PROCEDENCIA
                if(procedencias.size() > 0 && bandera){
                    for (String procedencia : procedencias) {
                        if(registro[0].toString().substring(2, 4).contains(procedencia)){
                            bandera = true;
                            break;
                        }else{
                            bandera = false;
                        }
                    }
                }
                
                //BANDERA CATEGORIA
                if(tipos.size() > 0 && bandera){
                    for (String tipo : tipos) {
                        if(registro[0].toString().substring(4, 6).contains(tipo)){
                            bandera = true;
                            break;
                        }else{
                            bandera = false;
                        }
                    }
                }
                
                //BANDERA NOMBRE
                if(nombres.size() > 0 && bandera){
                    for (String nombre : nombres) {
                        if(registro[0].toString().substring(6, 8).contains(nombre)){
                            bandera = true;
                            break;
                        }else{
                            bandera = false;
                        }
                    }
                }
                
                //BANDERA COLOR
                if(colores.size() > 0 && bandera){
                    for (String color : colores) {
                        if(registro[0].toString().substring(8, 10).contains(color)){
                            bandera = true;
                            break;
                        }else{
                            bandera = false;
                        }
                    }
                }
                
                //BANDERA SEXO
                if(sexos.size() > 0 && bandera){
                    for (String sexo : sexos) {
                        if(registro[0].toString().substring(10, 11).contains(sexo)){
                            bandera = true;
                            break;
                        }else{
                            bandera = false;
                        }
                    }
                }
                
                //BANDERA TAMANHO
                if(tamanhos.size() > 0 && bandera){
                    for (String tamanho : tamanhos) {
                        if(registro[0].toString().substring(11, 13).contains(tamanho)){
                            bandera = true;
                            break;
                        }else{
                            bandera = false;
                        }
                    }
                }

                if(bandera){
                    datosEnProceso[registros] = registro;
                    registros++;
                }
            }
        }
        
        
        
        discvProductos = new Producto[registros];
        discvTablaContenido = new Object[registros][SWDVY.consultar.datatypes[0].length];
        registros = 0;
        
        //LIMPIEZA DE ARRAY
        for (Object[] registro : datosEnProceso) {
            if(registro[0] != null){
                discvProductos[registros] = new Producto();
                discvProductos[registros].setReferenciaInterna(registro[0].toString());
                discvProductos[registros].setNombre(registro[1].toString());
                discvProductos[registros].setPrecioVenta((Double) registro[2]);
                discvProductos[registros].setPrecioCosto((Double) registro[3]);
                discvProductos[registros].setStockTotal((Double) registro[4]);
                discvProductos[registros].setStockSucursal((Double) registro[5]);
                if(discvProductos[registros].getStockSucursal() >= (Integer.valueOf(tCantidadMinima.getText()))){
                    discvProductos[registros].setPublicado(true);
                }else{
                    discvProductos[registros].setPublicado(false);
                }
                
                for (Website odooWebsite : odooWebsites) {
                    if(registro[0].toString().substring(10, 11).equals(odooWebsite.getReferenciaExterna())){
                        discvProductos[registros].setWebsite(odooWebsite);
                    }
                }
                for (Categoria odooCategoria : odooCategorias) {
                    if(registro[0].toString().substring(4, 6).equals(odooCategoria.getReferenciaExterna())){
                        //System.out.println("COINCIDE: "+registro[0].toString().substring(4, 6) + " CON: "+odooCategoria.getReferenciaExterna());
                        discvProductos[registros].setCategorias(new Categoria[]{odooCategoria});
                    }
                }
                
                discvTablaContenido[registros] = registro;
                registros++;
            }
        }
        
        DefaultTableModel modelo = new DefaultTableModel(discvTablaContenido, SWDVY.consultar.encabezado[0]);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>((TableModel) modelo);
        sorter.toggleSortOrder(1);
        tProductos.setModel(modelo);
        tProductos.setRowSorter(sorter);
        
        eMensaje.setText("De los "+SWDVY.consultar.datatypes.length+" registros, se filtraron "+registros+" productos.");

        
    }
    
    public void limpiar(Boolean full){
        if(full){
            tCantidadMinima.setText("");
            tDeposito.setText("");
            //fechaSelector.setSelectedIndex(0);
        }
        
        eMensaje.setText("");
        eMensaje.setForeground(Color.BLACK);
        
        
    }
    
    public File seleccionarArchivo(){
        JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        /* Se desactiva la opcion de tipo de archivo "Todos los archivos" */
        jfc.setAcceptAllFileFilterUsed(false);
        /* Se establece los tipos de archivos permitidos. */
        FileNameExtensionFilter filtro=new FileNameExtensionFilter("Planilla Excel (*.xlsx,)", "xlsx");
        jfc.setFileFilter(filtro);

        int returnValue = jfc.showOpenDialog(null);
        // int returnValue = jfc.showSaveDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return jfc.getSelectedFile();
            
        }
        
        return null;
    }
    
    public void procesarArchivoCategorias(File archivo){
        FileInputStream inputStream = null;
        int cantCategorias = 0;
        int cantCategoriasValidas = 0;
        boolean categoriaValida;
        boolean archivoValido = false;
            
        try {
            String excelFilePath = archivo.getAbsolutePath();
            inputStream = new FileInputStream(new File(excelFilePath));
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            
            categorias = new Categoria[sheet.getPhysicalNumberOfRows()];
            Iterator<Row> rowIterator = sheet.iterator();
            Iterator<Cell> cellIterator;
            Categoria categoria;
            

            while (rowIterator.hasNext()) {
                Row nextRow = rowIterator.next();
                cellIterator = nextRow.cellIterator();
                categoria = new Categoria();
                categoriaValida = false;
                
                if(nextRow.getPhysicalNumberOfCells() == 3){
                    archivoValido = true;
                }

                if(nextRow.getRowNum() > 1){
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        
                        try{
                            switch (cell.getColumnIndex()){
                                case 0:
                                    categoria.setID((int) cell.getNumericCellValue());
                                    break;
                                case 1:
                                    if(!cell.getStringCellValue().isEmpty() && cell.getStringCellValue().length() == 2){
                                        categoriaValida = true;
                                    }
                                    categoria.setReferenciaExterna(cell.getStringCellValue());
                                    break;
                                case 2:
                                    categoria.setNombre(cell.getStringCellValue());
                                    break;
                                default:
                                    System.out.println("Numero de columna no esperada.");
                                    break;
                            }
                        }catch(IllegalStateException ex){
                            eMensaje.setText("Error al procesar valores de la celda: ["+cell.getRowIndex()+"]["+cell.getColumnIndex()+"]");
                        }

                        
                    }

                    if (categoriaValida){
                        categorias[cantCategoriasValidas] = categoria;
                        cantCategoriasValidas++;
                        //categoria.imprimir();
                    }
                    cantCategorias++;
                }
            } 

            if(cantCategoriasValidas > 0){
                eMensaje.setText("Se cargaron "+cantCategoriasValidas+" válidas de "+cantCategorias+" categorias encontradas.");
                eMensaje.setForeground(Color.BLUE);
            }else{
                if(archivoValido){
                    eMensaje.setText("No se encontraron categorias válidas, verifique las referencias externas.");
                }else{
                    eMensaje.setText("La cantidad de columnas no coincide con el formato requerido (3).");
                }
                
                eMensaje.setForeground(Color.RED);
            }
            
            workbook.close();
            inputStream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    public void procesarArchivoProductos(File archivo){
        
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        bExtraer = new javax.swing.JButton();
        spProductos = new javax.swing.JScrollPane();
        tProductos = new javax.swing.JTable();
        jLabel17 = new javax.swing.JLabel();
        tDeposito = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        tCantidadMinima = new javax.swing.JTextField();
        bExtraer1 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        tMaestroCategoriasEcommerce = new javax.swing.JTextField();
        bSeleccionarMaestroCategorias = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        tMaestroProductos = new javax.swing.JTextField();
        bSeleccionarMaestroProductos = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tOdooProductos = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        bOdooTestServidor = new javax.swing.JButton();
        bOdooTestLogin = new javax.swing.JButton();
        bOdooTestModeloPermisos = new javax.swing.JButton();
        tOdooTestModelo = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        tOdooUID = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        tOdooVersion = new javax.swing.JTextField();
        bOdooTestModeloCampos = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        bOdooTestModeloListar = new javax.swing.JButton();
        jLabel25 = new javax.swing.JLabel();
        tOdooTestModeloInsertRefenciaExterna = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        tOdooTestModeloInsertNombre = new javax.swing.JTextField();
        bOdooTestModeloListar1 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel27 = new javax.swing.JLabel();
        tOdooTestModeloUpdateReferenciaExterna = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        tOdooTestModeloUpdateNombre = new javax.swing.JTextField();
        bOdooTestModeloListar2 = new javax.swing.JButton();
        bOdooTestModeloListar3 = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel24 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        taDebug = new javax.swing.JTextArea();
        bOdooTest5 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jButton3 = new javax.swing.JButton();
        tOdooTest2IDProducto = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        bOdooTestModeloListar4 = new javax.swing.JButton();
        tOdooTestModeloInsertNombre1 = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        tOdooTestModeloInsertRefenciaExterna1 = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lAnho = new javax.swing.JLabel();
        lProcedencia = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        lTipo = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lNombre = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        lColor = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        lSexo = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        lTamanho = new javax.swing.JLabel();
        eMensaje = new javax.swing.JLabel();

        setClosable(true);
        setTitle("Mantenimiento de productos");
        setPreferredSize(new java.awt.Dimension(800, 600));

        bExtraer.setText("Extraer");
        bExtraer.setPreferredSize(new java.awt.Dimension(120, 25));
        bExtraer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bExtraerActionPerformed(evt);
            }
        });

        tProductos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Codigo", "Descripcion", "Venta", "Costo", "StockTotal", "StockSucursal", "WebSite", "Categoria"
            }
        ));
        spProductos.setViewportView(tProductos);

        jLabel17.setText("Depósito");
        jLabel17.setPreferredSize(new java.awt.Dimension(120, 25));

        tDeposito.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel19.setText("Cantidad mínima");
        jLabel19.setPreferredSize(new java.awt.Dimension(120, 25));

        tCantidadMinima.setPreferredSize(new java.awt.Dimension(80, 25));

        bExtraer1.setText("Sincronizar");
        bExtraer1.setPreferredSize(new java.awt.Dimension(120, 25));
        bExtraer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bExtraer1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spProductos, javax.swing.GroupLayout.DEFAULT_SIZE, 739, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(29, 29, 29)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(tDeposito, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tCantidadMinima, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(bExtraer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bExtraer1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(10, 10, 10))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bExtraer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tDeposito, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tCantidadMinima, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bExtraer1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(39, 39, 39)
                .addComponent(spProductos, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Discovery", jPanel1);

        jLabel18.setText("Categorias eCommerce");
        jLabel18.setPreferredSize(new java.awt.Dimension(120, 25));

        tMaestroCategoriasEcommerce.setEditable(false);
        tMaestroCategoriasEcommerce.setPreferredSize(new java.awt.Dimension(150, 25));

        bSeleccionarMaestroCategorias.setText("Seleccionar");
        bSeleccionarMaestroCategorias.setPreferredSize(new java.awt.Dimension(120, 25));
        bSeleccionarMaestroCategorias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSeleccionarMaestroCategoriasActionPerformed(evt);
            }
        });

        jLabel20.setText("Productos");
        jLabel20.setPreferredSize(new java.awt.Dimension(120, 25));

        tMaestroProductos.setEditable(false);
        tMaestroProductos.setPreferredSize(new java.awt.Dimension(150, 25));

        bSeleccionarMaestroProductos.setText("Seleccionar");
        bSeleccionarMaestroProductos.setPreferredSize(new java.awt.Dimension(120, 25));
        bSeleccionarMaestroProductos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSeleccionarMaestroProductosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tMaestroCategoriasEcommerce, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(bSeleccionarMaestroCategorias, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tMaestroProductos, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(bSeleccionarMaestroProductos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(65, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tMaestroCategoriasEcommerce, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bSeleccionarMaestroCategorias, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tMaestroProductos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bSeleccionarMaestroProductos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(440, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Maestros", jPanel3);

        jButton1.setText("Extraer");
        jButton1.setPreferredSize(new java.awt.Dimension(130, 25));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        tOdooProductos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Codigo", "Nombre", "PrecioCosto", "PrecioVenta", "Categorias", "WebSite", "Publicado"
            }
        ));
        jScrollPane2.setViewportView(tOdooProductos);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 734, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50))
        );

        jTabbedPane2.addTab("Productos", jPanel6);

        bOdooTestServidor.setText("Servidor");
        bOdooTestServidor.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestServidor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestServidorActionPerformed(evt);
            }
        });

        bOdooTestLogin.setText("Login");
        bOdooTestLogin.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestLoginActionPerformed(evt);
            }
        });

        bOdooTestModeloPermisos.setText("Permisos de lectura");
        bOdooTestModeloPermisos.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestModeloPermisos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestModeloPermisosActionPerformed(evt);
            }
        });

        tOdooTestModelo.setText("product.public.category");
        tOdooTestModelo.setPreferredSize(new java.awt.Dimension(80, 25));
        tOdooTestModelo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tOdooTestModeloActionPerformed(evt);
            }
        });

        jLabel21.setText("Modelo");
        jLabel21.setPreferredSize(new java.awt.Dimension(100, 25));

        jLabel22.setText("UID");
        jLabel22.setPreferredSize(new java.awt.Dimension(120, 25));

        tOdooUID.setEditable(false);
        tOdooUID.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel23.setText("Version");
        jLabel23.setPreferredSize(new java.awt.Dimension(120, 25));

        tOdooVersion.setEditable(false);
        tOdooVersion.setPreferredSize(new java.awt.Dimension(80, 25));

        bOdooTestModeloCampos.setText("Obtener campos");
        bOdooTestModeloCampos.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestModeloCampos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestModeloCamposActionPerformed(evt);
            }
        });

        bOdooTestModeloListar.setText("Listar contenido");
        bOdooTestModeloListar.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestModeloListar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestModeloListarActionPerformed(evt);
            }
        });

        jLabel25.setText("Referencia Externa");
        jLabel25.setPreferredSize(new java.awt.Dimension(100, 25));

        tOdooTestModeloInsertRefenciaExterna.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel26.setText("Nombre");
        jLabel26.setPreferredSize(new java.awt.Dimension(100, 25));

        tOdooTestModeloInsertNombre.setPreferredSize(new java.awt.Dimension(80, 25));

        bOdooTestModeloListar1.setText("Insertar contenido");
        bOdooTestModeloListar1.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestModeloListar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestModeloListar1ActionPerformed(evt);
            }
        });

        jLabel27.setText("Referencia Externa");
        jLabel27.setPreferredSize(new java.awt.Dimension(100, 25));

        tOdooTestModeloUpdateReferenciaExterna.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel28.setText("Nombre");
        jLabel28.setPreferredSize(new java.awt.Dimension(100, 25));

        tOdooTestModeloUpdateNombre.setPreferredSize(new java.awt.Dimension(80, 25));

        bOdooTestModeloListar2.setText("Actualizar contenido");
        bOdooTestModeloListar2.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestModeloListar2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestModeloListar2ActionPerformed(evt);
            }
        });

        bOdooTestModeloListar3.setText("Obtener contenido");
        bOdooTestModeloListar3.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestModeloListar3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestModeloListar3ActionPerformed(evt);
            }
        });

        jLabel24.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        jLabel24.setText("DEBUG");
        jLabel24.setPreferredSize(new java.awt.Dimension(120, 25));

        taDebug.setEditable(false);
        taDebug.setColumns(20);
        taDebug.setRows(5);
        jScrollPane1.setViewportView(taDebug);

        bOdooTest5.setText("Clear");
        bOdooTest5.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTest5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTest5ActionPerformed(evt);
            }
        });

        jButton2.setText("Despublicar productos");
        jButton2.setPreferredSize(new java.awt.Dimension(130, 25));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jSeparator1)
                .addGap(10, 10, 10))
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator2)
                .addContainerGap())
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(tOdooTestModeloUpdateReferenciaExterna, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(tOdooTestModeloInsertRefenciaExterna, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(tOdooTestModelo, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tOdooUID, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tOdooTestModeloUpdateNombre, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(tOdooVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 42, Short.MAX_VALUE))
                    .addComponent(tOdooTestModeloInsertNombre, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(24, 24, 24)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(bOdooTestModeloListar3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloListar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloListar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloPermisos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestServidor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(bOdooTestLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(bOdooTestModeloCampos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(bOdooTestModeloListar2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator3)
                .addContainerGap())
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(bOdooTest5, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tOdooUID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tOdooVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestServidor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestLogin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tOdooTestModelo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloPermisos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloCampos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bOdooTestModeloListar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tOdooTestModeloInsertRefenciaExterna, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloListar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tOdooTestModeloInsertNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tOdooTestModeloUpdateReferenciaExterna, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloListar2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tOdooTestModeloUpdateNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloListar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTest5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane2.addTab("Test", jPanel5);

        jButton3.setText("Get Producto");
        jButton3.setPreferredSize(new java.awt.Dimension(130, 25));
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        tOdooTest2IDProducto.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel29.setText("ID");
        jLabel29.setPreferredSize(new java.awt.Dimension(120, 25));

        bOdooTestModeloListar4.setText("Insertar Producto");
        bOdooTestModeloListar4.setPreferredSize(new java.awt.Dimension(130, 25));
        bOdooTestModeloListar4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOdooTestModeloListar4ActionPerformed(evt);
            }
        });

        tOdooTestModeloInsertNombre1.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel30.setText("Nombre");
        jLabel30.setPreferredSize(new java.awt.Dimension(100, 25));

        tOdooTestModeloInsertRefenciaExterna1.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel31.setText("Referencia Externa");
        jLabel31.setPreferredSize(new java.awt.Dimension(100, 25));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bOdooTestModeloListar4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(tOdooTestModeloInsertNombre1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tOdooTest2IDProducto, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tOdooTestModeloInsertRefenciaExterna1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(140, 140, 140))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tOdooTest2IDProducto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bOdooTestModeloListar4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tOdooTestModeloInsertRefenciaExterna1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tOdooTestModeloInsertNombre1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(369, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Test Variantes", jPanel7);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 759, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jTabbedPane2)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Odoo", jPanel4);

        jLabel1.setText("Nomenclatura");
        jLabel1.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setText("AAPPTTNNCCGÑÑ");
        jLabel2.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel3.setText("AÑO");
        jLabel3.setPreferredSize(new java.awt.Dimension(80, 20));

        lAnho.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lAnho.setText("2021");
        lAnho.setPreferredSize(new java.awt.Dimension(80, 20));

        lProcedencia.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lProcedencia.setText("ARGENTINA");
        lProcedencia.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel6.setText("PROCEDENCIA");
        jLabel6.setPreferredSize(new java.awt.Dimension(80, 20));

        lTipo.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lTipo.setText("CAMPERA");
        lTipo.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel8.setText("TIPO");
        jLabel8.setPreferredSize(new java.awt.Dimension(80, 20));

        lNombre.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lNombre.setText("EMMA SUPER");
        lNombre.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel10.setText("NOMBRE");
        jLabel10.setPreferredSize(new java.awt.Dimension(80, 20));

        lColor.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lColor.setText("BLUE");
        lColor.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel12.setText("COLOR");
        jLabel12.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel13.setText("SEXO");
        jLabel13.setPreferredSize(new java.awt.Dimension(80, 20));

        lSexo.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lSexo.setText("FEMENINO");
        lSexo.setPreferredSize(new java.awt.Dimension(80, 20));

        jLabel15.setText("TAMAÑO");
        jLabel15.setPreferredSize(new java.awt.Dimension(80, 20));

        lTamanho.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lTamanho.setText("XS");
        lTamanho.setPreferredSize(new java.awt.Dimension(80, 20));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(18, 18, 18)
                                            .addComponent(lNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(18, 18, 18)
                                                    .addComponent(lTipo, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE))
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(18, 18, 18)
                                                    .addComponent(lProcedencia, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(18, 18, 18)
                                                    .addComponent(lAnho, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                            .addGap(61, 61, 61)))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 427, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lTamanho, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lSexo, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lAnho, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lProcedencia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lTipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lSexo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lTamanho, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(282, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Filtros", jPanel2);

        eMensaje.setPreferredSize(new java.awt.Dimension(40, 25));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(eMensaje, javax.swing.GroupLayout.DEFAULT_SIZE, 764, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 764, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(eMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bExtraerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bExtraerActionPerformed
        extraerDatos();
    }//GEN-LAST:event_bExtraerActionPerformed

    private void bSeleccionarMaestroCategoriasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSeleccionarMaestroCategoriasActionPerformed
        File archivo = seleccionarArchivo();
        if(archivo != null){
            maestroCategorias = archivo;
            tMaestroCategoriasEcommerce.setText(maestroCategorias.getAbsolutePath());
            procesarArchivoCategorias(maestroCategorias);
        }else{
            tMaestroCategoriasEcommerce.setText("");
            eMensaje.setText("");
            eMensaje.setForeground(Color.BLACK);
        }
    }//GEN-LAST:event_bSeleccionarMaestroCategoriasActionPerformed

    private void bSeleccionarMaestroProductosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSeleccionarMaestroProductosActionPerformed
        File archivo = seleccionarArchivo();
        if(archivo != null){
            maestroProductos = archivo;
            tMaestroProductos.setText(maestroProductos.getAbsolutePath());
            procesarArchivoProductos(maestroProductos);
        }else{
            tMaestroProductos.setText("");
            eMensaje.setText("");
            eMensaje.setForeground(Color.BLACK);
        }
    }//GEN-LAST:event_bSeleccionarMaestroProductosActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        odooModeloListarT();        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void bExtraer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bExtraer1ActionPerformed
        odooProductosSincronizar();
    }//GEN-LAST:event_bExtraer1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        if(odooUID != null){
            if(!tOdooTest2IDProducto.getText().isEmpty()){
                try {
                    taDebug.append("Obteniendo registro. \n");
                    odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                            asList(odooDB, odooUID, odooPassword, "product.product", 
                            "search_read", asList(asList(asList("id", "=", tOdooTest2IDProducto.getText().trim()))),
                            emptyMap())
                            )
                    );
                    
                    if(odooRegistros.size() == 1){
                        taDebug.append("Se encontró 1 registro. \n");
                        System.out.println("");
                        System.out.println("__________________________________ ID = "+tOdooTest2IDProducto.getText().trim());
                        
                        HashMap registroP = (HashMap) odooRegistros.get(0);
                        /*
                        Object[] registroC = (Object[]) registroP.get("public_categ_ids");
                        
                        Producto producto = new Producto();
                        Categoria[] categorias = new Categoria[registroC.length];
                        
                        for (int i = 0; i < registroC.length; i++) {
                            categorias[i] = new Categoria();
                            categorias[i].setID((Integer) registroC[i]);   
                        }
                        */
                        
                        for (Object key : registroP.keySet()) {
                            System.out.print("KEY: "+key);
                            System.out.println(" VALUE: "+registroP.get(key));
                            
                            
                            
                            if(registroP.get(key) instanceof Object[]){
                                Object[] contenido = (Object[]) registroP.get(key);
                                for (Object object : contenido) {
                                    System.out.println("\t"+object.toString());
                                }
                            }
                            
                        }
                        
                        /*
                        taDebug.append("\tID\tREF\tNOMBRE\n");
                        taDebug.append("\t");
                        taDebug.append(categoria.getID()+"\t");
                        taDebug.append(categoria.getReferenciaExterna()+"\t");
                        taDebug.append(categoria.getNombre()+"\n");
                        
                        tOdooTestModeloUpdateNombre.setText(categoria.getNombre());
                        */
                        
                    }else if(odooRegistros.size() > 1){
                        taDebug.append("ERROR. Se encontró más de 1 registro. \n");
                    }else{
                        taDebug.append("No se encontraron registros. \n");
                    }
                    
                    

                    //taDebug.append("Se registro satisfactoriamente el registro, asignando el nuevo ID: "+odooNuevoID+"\n");
                } catch (XmlRpcException | ClassCastException ex) {
                    taDebug.append(ex.getMessage()+"\n");
                    Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                }
                eMensaje.setText("Listo.");
                eMensaje.setForeground(Color.blue);
            }else{
                eMensaje.setText("Complete correctamente los campos a ingresar.");
                eMensaje.setForeground(Color.red);
            }
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void bOdooTestModeloListar4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestModeloListar4ActionPerformed
        try {
            Integer[] odooIDs = new Integer[3];
            //odooInsertProducto.imprimir();
            odooIDs[0] = (Integer) odooCliente.execute(odooConfigObject, "execute_kw",
                    asList(odooDB, odooUID, odooPassword, "product.product",
                            "create", asList( 
                                    new HashMap(){{
                                        put("default_code", "FALDAP");
                                        put("name", "SHORT");
                                        put("list_price", 100000);
                                        put("standard_price", 50000);
                                        put("is_published", true);
                                        put("website_id", 1);
                                        put("attribute_line_ids", "product_variant_id");
                                        
                                        
                                    }}
                                    )
                    )
            );
            
            odooIDs[1] = (Integer) odooCliente.execute(odooConfigObject, "execute_kw",
                    asList(odooDB, odooUID, odooPassword, "product.product",
                            "create", asList( 
                                    new HashMap(){{
                                        put("default_code", "FALDAM");
                                        put("name", "SHORT");
                                        put("list_price", 100000);
                                        put("standard_price", 50000);
                                        put("is_published", true);
                                        put("website_id", 1);
                                        put("attribute_line_ids",2);
                                        put("product_template_attribute_values_ids",38);
                                        
                                        
                                    }}
                                    )
                    )
            );
            
            odooIDs[2] = (Integer) odooCliente.execute(odooConfigObject, "execute_kw",
                    asList(odooDB, odooUID, odooPassword, "product.product",
                            "create", asList( 
                                    new HashMap(){{
                                        put("default_code", "FALDAXL");
                                        put("name", "SHORT");
                                        put("list_price", 100000);
                                        put("standard_price", 50000);
                                        put("is_published", true);
                                        put("website_id", 1);
                                        put("attribute_line_ids",2);
                                        put("product_template_attribute_values_ids",38);
                                    }}
                                    )
                    )
            );
            
            for (Integer odooID : odooIDs) {
                odooBandera = (Boolean) odooCliente.execute(odooConfigObject, "execute_kw",
                        asList(odooDB, odooUID, odooPassword, "product.product",
                                "write", asList(asList(odooID), 
                                        new HashMap(){{
                                            for (Integer odooIDx : odooIDs) {
                                                if(odooID != odooIDx)
                                                    put("product_variant_id", odooIDx);
                                            }
                                            
                                        }}
                                        )
                        )
                );

                System.out.println("Actualizado el ID: "+odooID+ "   ["+odooBandera+"]");
            }
            
            

            
        } catch (XmlRpcException ex) {
            Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_bOdooTestModeloListar4ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        for (Producto odooUpdateProducto : odooProductos) {
            try {
                //odooUpdateProducto.imprimir();
                odooBandera = (Boolean) odooCliente.execute(odooConfigObject, "execute_kw",
                    asList(odooDB, odooUID, odooPassword, "product.product",
                        "write", asList(asList(odooUpdateProducto.getID()),
                            new HashMap(){{
                                put("is_published", false);
                            }}
                        )
                    )
                );
            } catch (XmlRpcException ex) {
                Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Se actualizaron " + odooProductos.length + " productos.");
    }//GEN-LAST:event_jButton2ActionPerformed

    private void bOdooTest5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTest5ActionPerformed
        taDebug.setText("");
        eMensaje.setText("");
    }//GEN-LAST:event_bOdooTest5ActionPerformed

    private void bOdooTestModeloListar3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestModeloListar3ActionPerformed
        odooModeloObtener();
    }//GEN-LAST:event_bOdooTestModeloListar3ActionPerformed

    private void bOdooTestModeloListar2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestModeloListar2ActionPerformed
        odooModeloActualizar();
    }//GEN-LAST:event_bOdooTestModeloListar2ActionPerformed

    private void bOdooTestModeloListar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestModeloListar1ActionPerformed
        odooModeloInsertar();
    }//GEN-LAST:event_bOdooTestModeloListar1ActionPerformed

    private void bOdooTestModeloListarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestModeloListarActionPerformed
        odooModeloListar();
    }//GEN-LAST:event_bOdooTestModeloListarActionPerformed

    private void bOdooTestModeloCamposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestModeloCamposActionPerformed
        odooModeloAtributos();
    }//GEN-LAST:event_bOdooTestModeloCamposActionPerformed

    private void tOdooTestModeloActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tOdooTestModeloActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tOdooTestModeloActionPerformed

    private void bOdooTestModeloPermisosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestModeloPermisosActionPerformed
        odooModeloPermisos();
    }//GEN-LAST:event_bOdooTestModeloPermisosActionPerformed

    private void bOdooTestLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestLoginActionPerformed
        odooLogin();
    }//GEN-LAST:event_bOdooTestLoginActionPerformed

    private void bOdooTestServidorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOdooTestServidorActionPerformed
        odooConexion();
    }//GEN-LAST:event_bOdooTestServidorActionPerformed

    private void odooImprimirRespuesta(HashMap respuesta){
        for (int i = 0; i < respuesta.size(); i++) {
            taDebug.append(respuesta.get(i).toString());
        }
    }
    private void odooStart(){
        odooURL = "https://www.kosiuko.com.py";
        odooDB = "Zatex";
        odooUser = "soporte@junjuis.com";
        odooPassword = "C0nsult0r14%";
        odooCliente = new XmlRpcClient();
        odooConfigCommon = new XmlRpcClientConfigImpl();
        odooConfigObject = new XmlRpcClientConfigImpl();
        
        try {
            odooConfigCommon.setServerURL(new URL(String.format("%s/xmlrpc/2/common", odooURL)));
            odooConfigObject.setServerURL(new URL(String.format("%s/xmlrpc/2/object", odooURL)));
        } catch (MalformedURLException ex) {
            taDebug.append(ex.getMessage()+"\n");
            Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
        }
        //DOCUMENTACION UTILIZADA
        //https://github.com/odoo/documentation/blob/14.0/content/developer/misc/api/odoo.rst#id23
    }
    
    private void odooConexion(){
        //Se obtiene los datos del servidor, no necesita autenticacion.
        try {
            taDebug.append("Intentando conexión. \n");
            odooRespuesta = (HashMap) odooCliente.execute(odooConfigCommon, "version", emptyList());
            tOdooVersion.setText(odooRespuesta.get("server_version").toString());
            odooImprimirRespuesta(odooRespuesta);
            eMensaje.setText("Conectado.");
            eMensaje.setForeground(Color.blue);
        } catch (XmlRpcException | ClassCastException ex) {
            taDebug.append(ex.getMessage()+"\n");
            Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
            eMensaje.setText("Error de conexion.");
            eMensaje.setForeground(Color.red);
        }
    }
    
    private void odooLogin(){
        try {
            taDebug.append("Iniciando sesion. \n");
            odooUID = (Integer) odooCliente.execute(odooConfigCommon, "authenticate", asList(odooDB, odooUser, odooPassword, emptyMap()));
            tOdooUID.setText(String.valueOf(odooUID));
            eMensaje.setText("Sesion iniciada.");
            eMensaje.setForeground(Color.blue);
        } catch (XmlRpcException | ClassCastException ex) {
            eMensaje.setText("Error de sesion.");
            eMensaje.setForeground(Color.red);
            taDebug.append(ex.getMessage()+"\n");
            Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void odooModeloPermisos(){
        if(odooUID != null){
            try {
                taDebug.append("Obteniendo permisos de modelo. \n");
                odooBandera = (Boolean) odooCliente.execute(odooConfigObject, "execute_kw", 
                        asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                        "check_access_rights", asList("read"), new HashMap() {{ put("raise_exception", false);}}
                        )
                );
                taDebug.append("Permisos para acceder a "+tOdooTestModelo.getText().trim()+": "+odooBandera+".\n");
            } catch (XmlRpcException | ClassCastException ex) {
                taDebug.append(ex.getMessage()+"\n");
                Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
            }
            eMensaje.setText("Listo.");
            eMensaje.setForeground(Color.blue);
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
    
    private void odooModeloAtributos(){
        if(odooUID != null){
            try {
                taDebug.append("Obteniendo atributos de modelo. \n");
                odooRespuesta = (HashMap) odooCliente.execute(odooConfigObject, "execute_kw", 
                        asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                        "fields_get", emptyList(), emptyMap()
                        )
                );
                
                for (Object key : odooRespuesta.keySet()) {
                    taDebug.append("\t");
                    taDebug.append(odooRespuesta.get(key).toString()+"\n");
                }

                taDebug.append("Se encontraron: "+ odooRespuesta.size() +" campos disponibles en "+tOdooTestModelo.getText().trim()+".\n");
            } catch (XmlRpcException | ClassCastException ex) {
                taDebug.append(ex.getMessage()+"\n");
                Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
            }
            eMensaje.setText("Listo.");
            eMensaje.setForeground(Color.blue);
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
    
    private void odooModeloListar(){
        if(odooUID != null){
            try {
                taDebug.append("Obteniendo contenido del modelo. \n");
                odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                        asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                        "search_read", emptyList(), emptyMap()
                        //BUSQUEDA CON FILTRO
                        //"search_read", asList(asList(asList("x_referencia_externa", "<>", ""))),new HashMap() {{put("fields", asList("name", "x_referencia_externa"));}}        
                        )
                ));
                
                taDebug.append("\tID\tREF\tNOMBRE\n");
                for (Object objeto : odooRegistros) {
                    HashMap registro = (HashMap) objeto;
                    taDebug.append("\t");
                    taDebug.append(registro.get("id")+"\t");  
                    taDebug.append(registro.get("name")+"\n");
                }
                
                taDebug.append("Se encontraron: "+ odooRegistros.size() +" campos disponibles en "+tOdooTestModelo.getText().trim()+".\n");
            } catch (XmlRpcException | ClassCastException ex) {
                taDebug.append(ex.getMessage()+"\n");
                Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
            }
            eMensaje.setText("Listo.");
            eMensaje.setForeground(Color.blue);
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
    
    private void odooModeloListarT(){
        if(odooUID != null){
            try {
                // WEBSITES
                odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                        asList(odooDB, odooUID, odooPassword, "website", 
                        "search_read", emptyList(), emptyMap()
                        )));
                odooWebsites = new Website[odooRegistros.size()];
                for (int i = 0; i < odooRegistros.size(); i++) {
                    HashMap registroW = (HashMap) odooRegistros.get(i);
                    odooWebsites[i] = new Website();
                    odooWebsites[i].setID((Integer) registroW.get("id"));
                    odooWebsites[i].setNombre((String) registroW.get("name"));
                    odooWebsites[i].setReferenciaExterna((String) registroW.get("x_referencia_externa"));
                    odooWebsites[i].setUrl((String) registroW.get("domain"));
                }
                
                
                // CATEGORIAS
                odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                        asList(odooDB, odooUID, odooPassword, "product.public.category", 
                        "search_read", asList(asList(asList("x_referencia_externa", "<>", ""))), new HashMap() {{ put("fields", asList("name", "x_referencia_externa"));}}
                        )
                ));
                odooCategorias = new Categoria[odooRegistros.size()];
                for (int i = 0; i < odooRegistros.size(); i++) {
                    HashMap registroC = (HashMap) odooRegistros.get(i);
                    odooCategorias[i] = new Categoria();
                    odooCategorias[i].setID((Integer) registroC.get("id"));
                    odooCategorias[i].setNombre((String) registroC.get("name"));
                    odooCategorias[i].setReferenciaExterna((String) registroC.get("x_referencia_externa"));
                }
                
                //TAMAÑOS 
                odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                        asList(odooDB, odooUID, odooPassword, "product.product",   
                        "search_read", asList(asList(asList("valid_product_template_attribute_line_ids", "<>", ""))), new HashMap() {{ put("fields", asList("name"));}}
                        )
                ));
                odooTamanho = new Tamanho[odooRegistros.size()];
                for (int j = 0; j < odooRegistros.size(); j++) {
                    HashMap registroV = (HashMap) odooRegistros.get(j);
                    odooTamanho[j] = new Tamanho();
                    odooTamanho[j].setID((Integer) registroV.get("id"));
                    odooTamanho[j].setNombre((String) registroV.get("name"));
                  
                }
                
                // PRODUCTOS
                odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                        asList(odooDB, odooUID, odooPassword, "product.product", 
                        "search_read", emptyList(), new HashMap() {{ put("fields", 
                                asList(
                                        "default_code", 
                                        "name", 
                                        "list_price",
                                        "standard_price",
                                        "public_categ_ids",
                                        "valid_product_template_attribute_line_ids",
                                        "website_id",
                                        "is_published"));}}
                        //"search_read", emptyList(), emptyMap()
                        //BUSQUEDA CON FILTRO
                        //"search_read", asList(asList(asList("x_referencia_externa", "<>", ""))),new HashMap() {{put("fields", asList("name", "x_referencia_externa"));}}        
                        )
                ));
                
                odooProductos = new Producto[odooRegistros.size()];
                
                
                for (int j = 0; j < odooRegistros.size(); j++) {
                    //Definicion de variables finales
                    Producto producto = new Producto();
                    Categoria[] categorias;
                    Tamanho[] tamanho;
                    Website website = new Website();
                    
                    
                    //Definicion de variables a parsear.
                    HashMap registroP = (HashMap) odooRegistros.get(j);
                    Object[] registroC = (Object[]) registroP.get("public_categ_ids");
                    Object[] registroV = (Object[]) registroP.get("valid_product_template_attribute_line_ids");
                    Object[] registroW;
                    categorias = new Categoria[registroC.length];
                    tamanho = new Tamanho[registroV.length];
                    //WEBSITE
                    if(registroP.get("website_id") instanceof Object[] ){
                        registroW = (Object[]) registroP.get("website_id");
                        website.setID((Integer) registroW[0]);
                    }
                    
                    //CATEGORIAS
                    for (int i = 0; i < registroC.length; i++) {
                        categorias[i] = new Categoria();
                        categorias[i].setID((Integer) registroC[i]);   
                    }
                    //VARIANTES
                    for (int i = 0; i < registroV.length; i++) {
                        tamanho[i] = new Tamanho();
                        tamanho[i].setID((Integer) registroV[i]);   
                    }
                    
                    //SET DE VALORES EN VARIABLE FINAL
                    producto.setID((Integer) registroP.get("id"));
                    producto.setReferenciaInterna(registroP.get("default_code").toString());
                    producto.setNombre(registroP.get("name").toString());
                    producto.setPrecioVenta((Double) registroP.get("list_price"));
                    producto.setPrecioCosto((Double) registroP.get("standard_price"));
                    producto.setCategorias(categorias);
                    producto.setTamanho(tamanho);
                    producto.setWebsite(website); 
                    producto.setPublicado((Boolean) registroP.get("is_published"));
                    odooProductos[j] = producto;
                }
                
                // CARGA DE TABLA
                Object[][] odooTablaContenido = new Object[odooProductos.length][9];
                //Object[] tablaEncabezado = 
                for (int i = 0; i < odooProductos.length; i++) {
                    odooTablaContenido[i][0] = odooProductos[i].getID();
                    odooTablaContenido[i][1] = odooProductos[i].getReferenciaInterna();
                    odooTablaContenido[i][2] = odooProductos[i].getNombre();
                    odooTablaContenido[i][3] = odooProductos[i].getPrecioVenta();
                    odooTablaContenido[i][4] = odooProductos[i].getPrecioCosto();
                    String tablaConteniadoCategorias = "";
                    for (Categoria x : odooProductos[i].getCategorias()) {
                        tablaConteniadoCategorias += x.getID()+", ";
                    }
                    odooTablaContenido[i][5] = tablaConteniadoCategorias;
                    String tablaContenidoTamanhos = "";
                    for (Tamanho x : odooProductos[i].getTamanho()) {
                        tablaContenidoTamanhos += x.getID()+", ";
                    }
                    odooTablaContenido[i][6] = tablaContenidoTamanhos;
                    odooTablaContenido[i][7] = odooProductos[i].getWebsite().getID();
                    odooTablaContenido[i][8] = odooProductos[i].getPublicado().toString();
                }
                 
                tOdooProductos.setModel(new javax.swing.table.DefaultTableModel(
                    odooTablaContenido,
                    new String [] { "ID","Codigo", "Nombre", "PrecioVenta","PrecioCosto", "Categorias","Tamaño", "WebSite", "Publicado", 
                    }
                ));
                
                
                eMensaje.setText("Se encontraron: "+ odooRegistros.size() +" productos en Odoo.");
                eMensaje.setForeground(Color.blue);
            } catch (XmlRpcException | ClassCastException ex) {
                taDebug.append(ex.getMessage()+"\n");
                Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }        
    }
    
    private void odooProductosSincronizar(){
        if(odooProductos != null){
            if(discvProductos != null){
                odooUpdateProductos = new ArrayList<> (); 
                odooInsertProductos = new ArrayList<> (); 
                odooDeleteProductos = new ArrayList<> (); 
                odooNoneProductos = new ArrayList<> (); 
                
                Boolean productoNuevo;
                
                List<Integer> odooWriteIDs = new ArrayList<> (); 
                                
                
                //RECORRIDO DISCOVERY
                
                for (Producto discvProducto : discvProductos) {
                    productoNuevo = true;
                    for (Producto odooProducto : odooProductos) {
                        if(discvProducto.getReferenciaInterna().equals(odooProducto.getReferenciaInterna())){
                            productoNuevo = false;
                            // URGENTE MEJORAR ESTE DISPARATE
                            if(discvProducto.getStockSucursal() >= Integer.valueOf(tCantidadMinima.getText()) && odooProducto.getPublicado()){
                                odooNoneProductos.add(odooProducto);
                            }else if(discvProducto.getStockSucursal() >= Integer.valueOf(tCantidadMinima.getText()) && !odooProducto.getPublicado()){
                                odooProducto.setPublicado(true);
                                odooProducto.setCategorias(discvProducto.getCategorias());
                                odooProducto.setTamanho(discvProducto.getTamanho());
                                    
                                odooUpdateProductos.add(odooProducto);
                            }else if(discvProducto.getStockSucursal() <= Integer.valueOf(tCantidadMinima.getText()) && odooProducto.getPublicado()){
                                odooProducto.setPublicado(false);
                                odooProducto.setCategorias(discvProducto.getCategorias());
                                odooProducto.setTamanho(discvProducto.getTamanho());
                                
                                
                                odooUpdateProductos.add(odooProducto);
                                
                                    
                                
                            }else{
                                odooNoneProductos.add(odooProducto);
                            }
                        }
                        
                    }
                    if(productoNuevo){
                        odooInsertProductos.add(discvProducto);
                        
                    }
                }
                
                for (Producto odooProducto : odooProductos) {
                    Boolean bandera = false;
                    
                    for (Producto odooUpdateProducto : odooUpdateProductos) {
                        if(odooProducto.getReferenciaInterna().equals(odooUpdateProducto.getReferenciaInterna())){
                            bandera = true;
                        }
                    }
                    
                    for (Producto odooInsertProducto : odooInsertProductos) {
                        if(odooProducto.getReferenciaInterna().equals(odooInsertProducto.getReferenciaInterna())){
                            bandera = true;
                        }
                    }
                    
                    for (Producto odooNoneProducto : odooNoneProductos) {
                        if(odooProducto.getReferenciaInterna().equals(odooNoneProducto.getReferenciaInterna())){
                            bandera = true;
                        }
                    }
                    
                    if(!bandera){
                        odooDeleteProductos.add(odooProducto);
                    }
                    
                }
                
                /************ RECORRIDO DE LISTAS *****************/
                System.out.println("\tSINCORNIZACION");
                // PRODUCTOS A DESPUBLICAR
                for (Producto odooDeleteProducto : odooDeleteProductos) {
                    odooWriteIDs.add(odooDeleteProducto.getID());
                    //System.out.println("DELETE_ID:"+odooDeleteProducto.getID());
                }
                odooWriteAttributes = new HashMap(){{put("is_published", false);}};
                
                try {
                    odooBandera = (Boolean) odooCliente.execute(odooConfigObject, "execute_kw",
                            asList(odooDB, odooUID, odooPassword, "product.product",
                                    "write", asList(odooWriteIDs, odooWriteAttributes)
                            )
                    );
                    System.out.println("Se despublicaron "+odooWriteIDs.size()+" productos.");
                } catch (XmlRpcException ex) {
                    Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                // PRODUCTOS A ACTUALIZAR
                Integer contadorUpdate = 0;
                for (Producto odooUpdateProducto : odooUpdateProductos) {
                    try {
                        //odooUpdateProducto.imprimir();
                        odooBandera = (Boolean) odooCliente.execute(odooConfigObject, "execute_kw",
                                asList(odooDB, odooUID, odooPassword, "product.product",
                                        "write", asList(asList(odooUpdateProducto.getID()), 
                                                new HashMap(){{
                                                    put("default_code", odooUpdateProducto.getReferenciaInterna());
                                                    put("name", odooUpdateProducto.getNombre());
                                                    put("list_price", odooUpdateProducto.getPrecioVenta());
                                                    put("standard_price", odooUpdateProducto.getPrecioCosto());
                                                    put("is_published", odooUpdateProducto.getPublicado());
                                                    put("website_id", odooUpdateProducto.getWebsite().getID());
                                                    if(odooUpdateProducto.getCategorias() != null)
                                                        put("public_categ_ids",odooUpdateProducto.getArrayCategorias());
                                                    if(odooUpdateProducto.getCategorias() != null)
                                                        put("valid_product_template_attribute_line_ids",odooUpdateProducto.getArrayTamanho());
                                                    //agregar un check dinamico de caracteristicas
                                                }}
                                                )
                                )
                        );
                        
                        if(odooBandera){
                            contadorUpdate++;
                        }
                    } catch (XmlRpcException ex) {
                        Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.out.println("Se actualizaron "+contadorUpdate+" de "+odooUpdateProductos.size()+" productos.");
                
                
                // PRODUCTOS A NADIE NONE
                System.out.println("Se obviaron "+odooNoneProductos.size()+" productos.");
                
                // PRODUCTOS A INSERTAR 
                Integer contadorInsert = 0;
                for (Producto odooInsertProducto : odooInsertProductos) {
                    try {
                        //odooInsertProducto.imprimir();
                        odooID = (Integer) odooCliente.execute(odooConfigObject, "execute_kw",
                                asList(odooDB, odooUID, odooPassword, "product.product",
                                        "create", asList( 
                                                new HashMap(){{
                                                    put("default_code", odooInsertProducto.getReferenciaInterna());
                                                    put("name", odooInsertProducto.getNombre());
                                                    put("list_price", odooInsertProducto.getPrecioVenta());
                                                    put("standard_price", odooInsertProducto.getPrecioCosto());
                                                    put("is_published", odooInsertProducto.getPublicado());
                                                    put("website_id", odooInsertProducto.getWebsite().getID());
                                                    if(odooInsertProducto.getCategorias() != null){
                                                        put("public_categ_ids",odooInsertProducto.getArrayCategorias());
                                                    }
                                                    if(odooInsertProducto.getTamanho() != null){
                                                        put("valid_product_template_attribute_line_ids",odooInsertProducto.getArrayTamanho());
                                                        
                                                    }
                                                }}
                                                )
                                )
                        );
                        
                        if(odooID instanceof Integer){
                            contadorInsert++;
                        }
                    } catch (XmlRpcException ex) {
                        Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.out.println("Se insertaron "+contadorInsert+" de "+odooInsertProductos.size()+" productos.");
                
                
            }else{
                eMensaje.setText("No se encontraron registros en Discovery.");
            }
        }else{
            eMensaje.setText("No se encontraron registros en Odoo.");
        }
    }
    
    private void odooModeloInsertar(){
        if(odooUID != null){
            if(!tOdooTestModeloInsertRefenciaExterna.getText().isEmpty() && !tOdooTestModeloInsertNombre.getText().isEmpty()){
                try {
                    taDebug.append("Insertando nuevo registro. \n");
                    odooID = (Integer) odooCliente.execute(odooConfigObject, "execute_kw", 
                            asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                            "create", asList(new HashMap() {{ 
                                put("x_referencia_externa", tOdooTestModeloInsertRefenciaExterna.getText());
                                put("name", tOdooTestModeloInsertNombre.getText());
                            }})
                            )
                    );

                    taDebug.append("Se registro satisfactoriamente el registro, asignando el nuevo ID: "+odooID+"\n");
                } catch (XmlRpcException | ClassCastException ex) {
                    taDebug.append(ex.getMessage()+"\n");
                    Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                }
                eMensaje.setText("Listo.");
                eMensaje.setForeground(Color.blue);
            }else{
                eMensaje.setText("Complete correctamente los campos a ingresar.");
                eMensaje.setForeground(Color.red);
            }
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
    
    private void odooModeloObtener(){
        if(odooUID != null){
            if(!tOdooTestModeloUpdateReferenciaExterna.getText().isEmpty()){
                try {
                    taDebug.append("Obteniendo registro. \n");
                    odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                            asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                            "search_read", asList(asList(asList("x_referencia_externa", "=", tOdooTestModeloUpdateReferenciaExterna.getText().trim()))),new HashMap() {{put("fields", asList("name", "x_referencia_externa"));}}        
                            )
                    ));
                    
                    if(odooRegistros.size() == 1){
                        taDebug.append("Se encontró 1 registro. \n");
                        HashMap registro = (HashMap) odooRegistros.get(0);
                        
                        categoria = new Categoria();
                        categoria.setID((Integer) registro.get("id"));
                        categoria.setReferenciaExterna(registro.get("x_referencia_externa").toString());
                        categoria.setNombre(registro.get("name").toString());
                        taDebug.append("\tID\tREF\tNOMBRE\n");
                        taDebug.append("\t");
                        taDebug.append(categoria.getID()+"\t");
                        taDebug.append(categoria.getReferenciaExterna()+"\t");
                        taDebug.append(categoria.getNombre()+"\n");
                        
                        tOdooTestModeloUpdateNombre.setText(categoria.getNombre());
                        
                    }else if(odooRegistros.size() > 1){
                        taDebug.append("ERROR. Se encontró más de 1 registro. \n");
                    }else{
                        taDebug.append("No se encontraron registros. \n");
                    }
                    
                    

                    //taDebug.append("Se registro satisfactoriamente el registro, asignando el nuevo ID: "+odooNuevoID+"\n");
                } catch (XmlRpcException | ClassCastException ex) {
                    taDebug.append(ex.getMessage()+"\n");
                    Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                }
                eMensaje.setText("Listo.");
                eMensaje.setForeground(Color.blue);
            }else{
                eMensaje.setText("Complete correctamente los campos a ingresar.");
                eMensaje.setForeground(Color.red);
            }
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
    
    private void odooModeloActualizar(){
        if(odooUID != null){
            if(!tOdooTestModeloUpdateReferenciaExterna.getText().isEmpty() && !tOdooTestModeloUpdateNombre.getText().isEmpty()){
                try {
                    taDebug.append("Actualizando registro. \n");
                    odooBandera = (Boolean) odooCliente.execute(odooConfigObject, "execute_kw", 
                            asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                            "write", asList(asList(categoria.getID()), new HashMap() {{ 
                                put("x_referencia_externa", tOdooTestModeloUpdateReferenciaExterna.getText());
                                put("name", tOdooTestModeloUpdateNombre.getText());
                            }})
                            )
                    );
                    
                    taDebug.append((odooBandera?"Si":"No")+ " se pudo actualizar el registro. "+"\n");
                } catch (XmlRpcException | ClassCastException ex) {
                    taDebug.append(ex.getMessage()+"\n");
                    Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                }
                eMensaje.setText("Listo.");
                eMensaje.setForeground(Color.blue);
            }else{
                eMensaje.setText("Complete correctamente los campos a ingresar.");
                eMensaje.setForeground(Color.red);
            }
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
     private void odooModeloInsertarT(){
        if(odooUID != null){
            if(!tOdooTestModeloInsertRefenciaExterna.getText().isEmpty() && !tOdooTestModeloInsertNombre.getText().isEmpty()){
                try {
                    taDebug.append("Insertando nuevo registro. \n");
                    odooID = (Integer) odooCliente.execute(odooConfigObject, "execute_kw", 
                            asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                            "create", asList(new HashMap() {{ 
                                put("x_referencia_externa", tOdooTestModeloInsertRefenciaExterna.getText());
                                put("name", tOdooTestModeloInsertNombre.getText());
                            }})
                            )
                    );

                    taDebug.append("Se registro satisfactoriamente el registro, asignando el nuevo ID: "+odooID+"\n");
                } catch (XmlRpcException | ClassCastException ex) {
                    taDebug.append(ex.getMessage()+"\n");
                    Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                }
                eMensaje.setText("Listo.");
                eMensaje.setForeground(Color.blue);
            }else{
                eMensaje.setText("Complete correctamente los campos a ingresar.");
                eMensaje.setForeground(Color.red);
            }
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
    
    private void odooModeloTabla(){
        if(odooUID != null){
            if(!tOdooTestModeloUpdateReferenciaExterna.getText().isEmpty()){
                try {
                    taDebug.append("Obteniendo registro. \n");
                    odooRegistros = asList((Object[]) odooCliente.execute(odooConfigObject, "execute_kw", 
                            asList(odooDB, odooUID, odooPassword, tOdooTestModelo.getText().trim(), 
                            "search_read", asList(asList(asList("x_referencia_externa", "=", tOdooTestModeloUpdateReferenciaExterna.getText().trim()))),new HashMap() {{put("fields", asList("name", "x_referencia_externa"));}}        
                            )
                    ));
                    
                    if(odooRegistros.size() == 1){
                        taDebug.append("Se encontró 1 registro. \n");
                        HashMap registro = (HashMap) odooRegistros.get(0);
                        
                        categoria = new Categoria();
                        categoria.setID((Integer) registro.get("id"));
                        categoria.setReferenciaExterna(registro.get("x_referencia_externa").toString());
                        categoria.setNombre(registro.get("name").toString());
                        taDebug.append("\tID\tREF\tNOMBRE\n");
                        taDebug.append("\t");
                        taDebug.append(categoria.getID()+"\t");
                        taDebug.append(categoria.getReferenciaExterna()+"\t");
                        taDebug.append(categoria.getNombre()+"\n");
                        
                        tOdooTestModeloUpdateNombre.setText(categoria.getNombre());
                        
                    }else if(odooRegistros.size() > 1){
                        taDebug.append("ERROR. Se encontró más de 1 registro. \n");
                    }else{
                        taDebug.append("No se encontraron registros. \n");
                    }
                    
                    

                    //taDebug.append("Se registro satisfactoriamente el registro, asignando el nuevo ID: "+odooNuevoID+"\n");
                } catch (XmlRpcException | ClassCastException ex) {
                    taDebug.append(ex.getMessage()+"\n");
                    Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
                }
                eMensaje.setText("Listo.");
                eMensaje.setForeground(Color.blue);
            }else{
                eMensaje.setText("Complete correctamente los campos a ingresar.");
                eMensaje.setForeground(Color.red);
            }
        }else{
            eMensaje.setText("Inicie sesión, antes de realizar consultas. ");
            eMensaje.setForeground(Color.red);
        }
    }
    private void odooVarios(){
        HashMap respuesta;
        Integer uid;
        List<Object> resultado;
        
        try {
            taDebug.append("Iniciando sesion... \n");
            //Variables
            final String url = "https://www.kosiuko.com.py",
                    db = "Zatex",
                    username = "soporte@junjuis.com",
                    password = "C0nsult0r14%";
            odooCliente = new XmlRpcClient();
            
            //Se obtiene los datos del servidor, no necesita autenticacion.
            final XmlRpcClientConfigImpl common_config = new XmlRpcClientConfigImpl();
            common_config.setServerURL(new URL(String.format("%s/xmlrpc/2/common", url)));
            respuesta = (HashMap) odooCliente.execute(common_config, "version", emptyList());
            taDebug.append("Versión identificada: " + respuesta.get("server_version") + " \n");
            
            //Se intenta autenticar
            uid = (Integer) odooCliente.execute(common_config, "authenticate", asList(db, username, password, emptyMap()));
            taDebug.append("Conexion realizada. UID: "+ uid +"\n");
            
            //Se verifica si se cuenta con los permisos para acceder al recurso indicado
            String recurso = "product.public.category";
            final XmlRpcClient models = new XmlRpcClient() {{
                setConfig(new XmlRpcClientConfigImpl() {{
                    setServerURL(new URL(String.format("%s/xmlrpc/2/object", url)));
                }});
            }};
            Boolean tienePermisos = (Boolean) models.execute("execute_kw", asList(
                db, uid, password,
                recurso, "check_access_rights",
                asList("read"),
                new HashMap() {{ put("raise_exception", false); }}
            ));
            taDebug.append("Permiso para leer productos: "+ tienePermisos.toString() +"\n");

            //Se lista los campos que cuenta el modelo.
            respuesta = (HashMap) models.execute("execute_kw", asList(
                db, uid, password,
                recurso, "fields_get",
                emptyList(),
                new HashMap() {{
                    put("attributes", asList("string", "help", "type"));
                }}
            ));
            taDebug.append("Se encontraron: "+ respuesta.size() +" campos disponibles en "+recurso+".\n");
            
            //Se busca y obtiene los registros que cumplan el filtro indicado
            resultado = asList((Object[]) models.execute("execute_kw", asList(
                db, uid, password,
                recurso, "search_read",
                asList(asList(
                    asList("x_referencia_externa", "<>", ""))),
                new HashMap() {{
                    put("fields", asList("name", "x_referencia_externa"));
                    put("limit", 5);
                }}
            )));
            
            //Se imprimen los valores del resultado de la consulta anterior
            taDebug.append("ID\tREF\tNOMBRE\n");
            for (Object objeto : resultado) {
                HashMap registro = (HashMap) objeto;
                
                taDebug.append(registro.get("id")+"\t");
                taDebug.append(registro.get("x_referencia_externa")+"\t");
                taDebug.append(registro.get("name")+"\n");
            }
            
            
            /* INSERT
            final Integer id = (Integer)models.execute("execute_kw", asList(
                db, uid, password,
                "res.partner", "create",
                asList(new HashMap() {{ put("name", "New Partner"); }})
            ));
            */
            
            
            
            /* UPDATE
            models.execute("execute_kw", asList(
                db, uid, password,
                "res.partner", "write",
                asList(
                    asList(id),
                    new HashMap() {{ put("name", "Newer Partner"); }}
                )
            ));
            // get record name after having changed it
            asList((Object[])models.execute("execute_kw", asList(
                db, uid, password,
                "res.partner", "name_get",
                asList(asList(id))
            )));
            */
            
            //DOCUMENTACION UTILIZADA
            //https://github.com/odoo/documentation/blob/14.0/content/developer/misc/api/odoo.rst#id23
        } catch (MalformedURLException | XmlRpcException | ClassCastException ex) {
            taDebug.append(ex.getMessage()+"\n");
            Logger.getLogger(aProductos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bExtraer;
    private javax.swing.JButton bExtraer1;
    private javax.swing.JButton bOdooTest5;
    private javax.swing.JButton bOdooTestLogin;
    private javax.swing.JButton bOdooTestModeloCampos;
    private javax.swing.JButton bOdooTestModeloListar;
    private javax.swing.JButton bOdooTestModeloListar1;
    private javax.swing.JButton bOdooTestModeloListar2;
    private javax.swing.JButton bOdooTestModeloListar3;
    private javax.swing.JButton bOdooTestModeloListar4;
    private javax.swing.JButton bOdooTestModeloPermisos;
    private javax.swing.JButton bOdooTestServidor;
    private javax.swing.JButton bSeleccionarMaestroCategorias;
    private javax.swing.JButton bSeleccionarMaestroProductos;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel eMensaje;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JLabel lAnho;
    private javax.swing.JLabel lColor;
    private javax.swing.JLabel lNombre;
    private javax.swing.JLabel lProcedencia;
    private javax.swing.JLabel lSexo;
    private javax.swing.JLabel lTamanho;
    private javax.swing.JLabel lTipo;
    private javax.swing.JScrollPane spProductos;
    private javax.swing.JTextField tCantidadMinima;
    private javax.swing.JTextField tDeposito;
    private javax.swing.JTextField tMaestroCategoriasEcommerce;
    private javax.swing.JTextField tMaestroProductos;
    private javax.swing.JTable tOdooProductos;
    private javax.swing.JTextField tOdooTest2IDProducto;
    private javax.swing.JTextField tOdooTestModelo;
    private javax.swing.JTextField tOdooTestModeloInsertNombre;
    private javax.swing.JTextField tOdooTestModeloInsertNombre1;
    private javax.swing.JTextField tOdooTestModeloInsertRefenciaExterna;
    private javax.swing.JTextField tOdooTestModeloInsertRefenciaExterna1;
    private javax.swing.JTextField tOdooTestModeloUpdateNombre;
    private javax.swing.JTextField tOdooTestModeloUpdateReferenciaExterna;
    private javax.swing.JTextField tOdooUID;
    private javax.swing.JTextField tOdooVersion;
    private javax.swing.JTable tProductos;
    private javax.swing.JTextArea taDebug;
    // End of variables declaration//GEN-END:variables

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        
        System.out.println(evt.getSource().toString());
        
        String source = evt.getSource().toString().substring(evt.getSource().toString().lastIndexOf("$")+1, evt.getSource().toString().indexOf("@"));
        String value = evt.getNewValue().toString();
        String id = (String) evt.getPropagationId();
        
        Consola.out(JColor.MAGENTA,"Evento: "+source+": ["+value+"]");
        
        switch(source){
            case "Consultar":
                if(value.equals("STARTED")){
                    bExtraer.setEnabled(false);
                }else if(value.equals("DONE")){
                    bExtraer.setEnabled(true);
                    
                    if(SWDVY.largo > 0){
                        procesarDatos();
                    }else{
                        JOptionPane.showMessageDialog(this, "No se puede procesar los datos, no se encontraron registros.");
                    }
                    
                    
                }else{
                    Consola.out(JColor.RED,"Evento: "+source+": ["+value+"] - ERROR");
                }
                break;
            default:
                //
                break;
        }
        
        
    }
}
