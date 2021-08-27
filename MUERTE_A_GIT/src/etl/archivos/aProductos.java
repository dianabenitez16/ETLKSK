/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package etl.archivos;

import clases.Categoria;
import etl.Configuracion;
import etl.etl;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.TableModel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    
    SWDiscovery SWDVY;
    String query;
    
    String talonariosfactura;
    String talonariosncr;
    
    Integer cantidadMinima;
    Integer deposito;
    
    File maestroProductos;
    File maestroCategorias;
    
    private Categoria categorias[];
    
    public aProductos(etl etl) {
        initComponents();
        SWDVY = new SWDiscovery(eMensaje);
        loadConfig();
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
    
    public void extraerDatos(){
        limpiar(false);
        
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
        
        System.out.println("");
        System.out.println("QUERY: "+query);
        System.out.println("");
    
    }
    
    public void procesarDatos(){
        Boolean bandera = false;
        Integer registros = 0;
        
        Object[][] datosEnProceso = new Object[SWDVY.consultar.datatypes.length][SWDVY.consultar.datatypes[0].length];
        Object[][] datosProcesados;
        
        List<String> anhos =        Arrays.asList(lAnho.getText().split(","));
        List<String> procedencias =  Arrays.asList(lProcedencia.getText().split(","));
        List<String> tipos =         Arrays.asList(lTipo.getText().split(","));
        List<String> nombres =       Arrays.asList(lNombre.getText().split(","));
        List<String> colores =        Arrays.asList(lColor.getText().split(","));
        List<String> sexos =         Arrays.asList(lSexo.getText().split(","));
        List<String> tamanhos =      Arrays.asList(lTamanho.getText().split(","));
        
        //SE DEBERIA DE OPTIMIZAR ESTE CODIGO
        
        for (Object[] datatype : SWDVY.consultar.datatypes) {
            if(datatype[0].toString().trim().length() == 13){
                //BANDERA AÑO
                if(anhos.size() > 0){
                    for (String registro : anhos) {
                        if(datatype[0].toString().substring(0, 2).contains(registro)){
                            bandera = true;
                        }
                    }
                }

                //BANDERA PROCEDENCIA
                if(procedencias.size() > 0 && bandera){
                    for (String registro : procedencias) {
                        if(datatype[0].toString().substring(2, 4).contains(registro)){
                            bandera = true;
                        }
                    }
                }
                
                //BANDERA TIPO
                if(tipos.size() > 0 && bandera){
                    for (String registro : tipos) {
                        if(datatype[0].toString().substring(4, 6).contains(registro)){
                            bandera = true;
                        }
                    }
                }
                
                //BANDERA NOMBRE
                if(nombres.size() > 0 && bandera){
                    for (String registro : nombres) {
                        if(datatype[0].toString().substring(6, 8).contains(registro)){
                            bandera = true;
                        }
                    }
                }
                
                //BANDERA COLOR
                if(colores.size() > 0 && bandera){
                    for (String registro : colores) {
                        if(datatype[0].toString().substring(8, 10).contains(registro)){
                            bandera = true;
                        }
                    }
                }
                
                //BANDERA SEXO
                if(sexos.size() > 0 && bandera){
                    for (String registro : sexos) {
                        if(datatype[0].toString().substring(10, 11).contains(registro)){
                            bandera = true;
                        }
                    }
                }
                
                //BANDERA TAMANHO
                if(tamanhos.size() > 0 && bandera){
                    for (String registro : tamanhos) {
                        if(datatype[0].toString().substring(11, 13).contains(registro)){
                            bandera = true;
                        }
                    }
                }

                if(bandera){
                    datosEnProceso[registros] = datatype;
                    registros++;
                }
            }
        }
        
        
        
        datosProcesados = new Object[registros][SWDVY.consultar.datatypes[0].length];
        registros = 0;
        
        //LIMPIEZA DE ARRAY
        for (Object[] registro : datosEnProceso) {
            if(registro[0] != null){
                datosProcesados[registros] = registro;
                registros++;
            }
        }
        
        
        
        
        
        tProductos.setModel(new javax.swing.table.DefaultTableModel(
            //SWDVY.consultar.datatypes,
            datosProcesados,
            SWDVY.consultar.encabezado[0]
            // new String [] { "Codigo", "Descripcion", "Venta", "Costo", "Stock Total", "Stock Suc." }
        ));

        
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

                if(nextRow.getRowNum() > 1){
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();

                        switch (cell.getColumnIndex()){
                            case 0:
                                categoria.setID(cell.getStringCellValue());
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
                eMensaje.setText("No se encontraron categorias válidas, verifique las referencias externas.");
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
        jPanel3 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        tMaestroCategoriasEcommerce = new javax.swing.JTextField();
        bSeleccionarMaestroCategorias = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        tMaestroProductos = new javax.swing.JTextField();
        bSeleccionarMaestroProductos = new javax.swing.JButton();
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
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        spProductos.setViewportView(tProductos);

        jLabel17.setText("Depósito");
        jLabel17.setPreferredSize(new java.awt.Dimension(120, 25));

        tDeposito.setPreferredSize(new java.awt.Dimension(80, 25));

        jLabel19.setText("Cantidad mínima");
        jLabel19.setPreferredSize(new java.awt.Dimension(120, 25));

        tCantidadMinima.setPreferredSize(new java.awt.Dimension(80, 25));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spProductos, javax.swing.GroupLayout.DEFAULT_SIZE, 759, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(29, 29, 29)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(tDeposito, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tCantidadMinima, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(bExtraer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(10, 10, 10))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bExtraer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tDeposito, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tCantidadMinima, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(39, 39, 39)
                .addComponent(spProductos, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Importar", jPanel1);

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
                        .addContainerGap()
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
                                .addGap(0, 447, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lTamanho, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lSexo, javax.swing.GroupLayout.DEFAULT_SIZE, 660, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
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
                .addContainerGap(281, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Filtros", jPanel2);

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
                .addContainerGap()
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
                .addContainerGap(75, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tMaestroCategoriasEcommerce, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bSeleccionarMaestroCategorias, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tMaestroProductos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bSeleccionarMaestroProductos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(439, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Maestros", jPanel3);

        eMensaje.setPreferredSize(new java.awt.Dimension(40, 25));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
            .addComponent(eMensaje, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bExtraer;
    private javax.swing.JButton bSeleccionarMaestroCategorias;
    private javax.swing.JButton bSeleccionarMaestroProductos;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel eMensaje;
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
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane1;
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
    private javax.swing.JTable tProductos;
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
