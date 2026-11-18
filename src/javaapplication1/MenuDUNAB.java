/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package javaapplication1;

/**
 *
 * @author saral
 */
public class MenuDUNAB extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MenuDUNAB.class.getName());


    private final ServicioDunab servicio;
    private String seccionActual = "Inicio";
    private JPanelSesion sesion;

    public MenuDUNAB() {
        this(new ServicioDunab(java.nio.file.Paths.get("data")), java.nio.file.Paths.get("data", "apariencia.properties"));
    }

    public MenuDUNAB(ServicioDunab servicio) {
        this(servicio, null);
    }

    public MenuDUNAB(ServicioDunab servicio, java.nio.file.Path preferenciaApariencia) {
        this.servicio = servicio;
        Tema.iniciar(preferenciaApariencia);
        initComponents();
        configurarMenu();
    }

    private void configurarMenu() {
        setTitle("DUNAB | Bienestar universitario · Simulación académica");
        java.awt.Rectangle area = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new java.awt.Dimension(Math.min(1000, area.width), Math.min(620, area.height)));
        setSize(Math.min(1240, area.width), Math.min(840, area.height));
        setLocation(area.x + (area.width - getWidth()) / 2, area.y + (area.height - getHeight()) / 2);
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(Semestres.opciones(false)));
        jComboBox1.addActionListener(e -> {
            if (servicio.getActual() != null && sesion != null) mostrarModulo(seccionActual);
        });
        mostrarAcceso();
    }

    private void mostrarAcceso() {
        sesion = null;
        seccionActual = "Inicio";
        javax.swing.JPanel acceso = new javax.swing.JPanel(new java.awt.BorderLayout());
        javax.swing.JPanel apariencia = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 18, 10));
        apariencia.add(Tema.botonCambio());
        acceso.add(apariencia, java.awt.BorderLayout.NORTH);
        acceso.add(new PanelAcceso(servicio, () -> {
            jComboBox1.setSelectedIndex(servicio.getActual().getSemestre() - 1);
            mostrarSesion();
        }), java.awt.BorderLayout.CENTER);
        setContentPane(acceso);
        Tema.aplicar(this);
        revalidate();
        repaint();
    }

    private void mostrarSesion() {
        sesion = new JPanelSesion();
        setContentPane(sesion);
        mostrarModulo(seccionActual);
    }

    public String obtenerSemestreSeleccionado() {
        return (String) jComboBox1.getSelectedItem();
    }

    private int semestre() {
        return jComboBox1.getSelectedIndex() + 1;
    }

    private void mostrarModulo(String nombre) {
        if (servicio.getActual() == null) { mostrarAcceso(); return; }
        seccionActual = nombre;
        boolean admin = "ADMINISTRADOR".equals(servicio.getActual().getRol());
        javax.swing.JPanel pantalla;
        if (nombre.equals("Administrar actividades") || nombre.equals("Revisar solicitudes")
                || nombre.equals("Estudiantes y ajustes") || nombre.equals("Auditoría")) {
            if (!admin) throw new IllegalStateException("Esta sección requiere un administrador.");
            pantalla = new PantallasAdmin(servicio, this::semestre, this::refrescar).crear(nombre);
        } else {
            pantalla = new PantallasEstudiante(servicio, this::semestre, this::refrescar, this::mostrarModulo).crear(nombre);
        }
        sesion.centro.removeAll();
        sesion.centro.add(pantalla, java.awt.BorderLayout.CENTER);
        sesion.identidad.setText(servicio.getActual().getNombre() + " · " + servicio.getActual().getRol());
        Tema.aplicar(this);
        sesion.revalidate();
        sesion.repaint();
    }

    private void refrescar() {
        mostrarModulo(seccionActual);
    }

    /** Navegación independiente del código generado por NetBeans. */
    private final class JPanelSesion extends javax.swing.JPanel {
        private final javax.swing.JPanel centro = new javax.swing.JPanel(new java.awt.BorderLayout());
        private final javax.swing.JLabel identidad = new javax.swing.JLabel();

        JPanelSesion() {
            super(new java.awt.BorderLayout());
            javax.swing.JPanel lateral = new javax.swing.JPanel(new java.awt.BorderLayout(0, 16));
            lateral.setName("navegacionLateral");
            lateral.setBackground(Ui.morado);
            lateral.setPreferredSize(new java.awt.Dimension(240, 680));
            lateral.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 14, 18, 14));
            javax.swing.JLabel logo = new javax.swing.JLabel("DUNAB");
            logo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 30));
            logo.setForeground(java.awt.Color.WHITE);
            lateral.add(logo, java.awt.BorderLayout.NORTH);
            javax.swing.JPanel botones = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 7));
            botones.setOpaque(false);
            boolean admin = "ADMINISTRADOR".equals(servicio.getActual().getRol());
            String[] opciones = admin
                ? new String[]{"Inicio", "Mi perfil", "Administrar actividades", "Revisar solicitudes",
                    "Estudiantes y ajustes", "Auditoría", "Top 9", "Calculadora de notas"}
                : new String[]{"Inicio", "Mi perfil", "Consultar DUNAB", "Actividades y espacios", "Inscripciones",
                    "Historial de DUNAB", "Metas personales", "Top 9",
                    "Calculadora de notas", "Estadísticas y reportes"};
            for (String opcion : opciones) {
                javax.swing.JButton boton = Ui.boton(opcion, () -> mostrarModulo(opcion));
                boton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                boton.setPreferredSize(new java.awt.Dimension(208, 42));
                botones.add(boton);
            }
            javax.swing.JPanel caja = new javax.swing.JPanel(new java.awt.BorderLayout());
            caja.setOpaque(false);
            caja.add(botones, java.awt.BorderLayout.NORTH);
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(caja);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            lateral.add(scroll, java.awt.BorderLayout.CENTER);
            javax.swing.JPanel pie = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 7));
            pie.setOpaque(false);
            javax.swing.JButton cerrar = Ui.boton("Cerrar sesión", () -> {
                servicio.logout();
                mostrarAcceso();
            });
            cerrar.setBackground(Ui.marron);
            pie.add(cerrar);
            javax.swing.JButton salir = Ui.boton("Salir", () -> dispose());
            salir.setBackground(Ui.marron);
            pie.add(salir);
            lateral.add(pie, java.awt.BorderLayout.SOUTH);
            add(lateral, java.awt.BorderLayout.WEST);
            javax.swing.JPanel principal = new javax.swing.JPanel(new java.awt.BorderLayout());
            javax.swing.JPanel barra = new javax.swing.JPanel(new java.awt.BorderLayout(12, 8));
            barra.setBackground(java.awt.Color.WHITE);
            barra.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 25, 14, 25));
            barra.add(identidad, java.awt.BorderLayout.WEST);
            javax.swing.JPanel periodo = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            periodo.setOpaque(false);
            periodo.add(Tema.botonCambio());
            periodo.add(new javax.swing.JLabel("Período de consulta"));
            periodo.add(jComboBox1);
            barra.add(periodo, java.awt.BorderLayout.EAST);
            principal.add(barra, java.awt.BorderLayout.NORTH);
            principal.add(centro, java.awt.BorderLayout.CENTER);
            javax.swing.JLabel demo = new javax.swing.JLabel("  Simulación académica · Datos ficticios · Meta inicial configurable de 10.000 DUNAB");
            demo.setForeground(Ui.marron);
            demo.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
            principal.add(demo, java.awt.BorderLayout.SOUTH);
            add(principal, java.awt.BorderLayout.CENTER);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanelLateral = new javax.swing.JPanel();
        jPanelContenido = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanelLateral.setBackground(new java.awt.Color(66, 39, 91));
        jPanelContenido.setBackground(new java.awt.Color(247, 245, 242));

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 28));
        jLabel1.setForeground(java.awt.Color.WHITE);
        jLabel1.setText("DUNAB");
        jLabel2.setFont(new java.awt.Font("SansSerif", 0, 16));
        jLabel2.setForeground(new java.awt.Color(105, 96, 109));
        jLabel2.setText("Sistema de Gestión de Horas Libres UNAB");
        jLabel3.setFont(new java.awt.Font("SansSerif", 1, 15));
        jLabel3.setForeground(new java.awt.Color(121, 83, 58));
        jLabel3.setText("Seleccionar semestre");
        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 27));
        jLabel4.setForeground(new java.awt.Color(66, 39, 91));
        jLabel4.setText("¡Bienvenido a DUNAB!");
        jLabel5.setFont(new java.awt.Font("SansSerif", 0, 14));
        jLabel5.setForeground(new java.awt.Color(105, 96, 109));
        jLabel5.setText("Selecciona una sección del menú para comenzar.");
        jLabel6.setText("<html><b>Consultar horas DUNAB</b><br><br>Revisa la información disponible de tus horas libres.</html>");
        jLabel7.setText("<html><b>Actividades y espacios</b><br><br>Encuentra actividades y espacios de bienestar.</html>");
        jLabel8.setText("<html><b>Inscripciones</b><br><br>Consulta las opciones de inscripción disponibles.</html>");
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Semestre 1", "Semestre 2" }));

        jButton1.setText("Mi perfil");
        jButton2.setText("Consultar horas DUNAB");
        jButton3.setText("Actividades y espacios");
        jButton4.setText("Inscripciones");
        jButton5.setText("Historial de horas");
        jButton6.setText("Salir");

        javax.swing.GroupLayout lateralLayout = new javax.swing.GroupLayout(jPanelLateral);
        jPanelLateral.setLayout(lateralLayout);
        lateralLayout.setHorizontalGroup(lateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lateralLayout.createSequentialGroup().addContainerGap(18, 18)
                .addGroup(lateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                    .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                    .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE))
                .addContainerGap(18, 18)));
        lateralLayout.setVerticalGroup(lateralLayout.createSequentialGroup()
            .addContainerGap(24, 24).addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(28, 28, 28).addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(12, 12, 12).addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(12, 12, 12).addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(12, 12, 12).addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(12, 12, 12).addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(24, 24, 24).addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(24, Short.MAX_VALUE));

        javax.swing.GroupLayout contenidoLayout = new javax.swing.GroupLayout(jPanelContenido);
        jPanelContenido.setLayout(contenidoLayout);
        contenidoLayout.setHorizontalGroup(contenidoLayout.createSequentialGroup().addContainerGap(38, 38)
            .addGroup(contenidoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
                .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
                .addGroup(contenidoLayout.createSequentialGroup().addComponent(jLabel3)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(contenidoLayout.createSequentialGroup()
                    .addComponent(jLabel6, 205, 205, 205).addGap(18, 18, 18)
                    .addComponent(jLabel7, 205, 205, 205).addGap(18, 18, 18)
                    .addComponent(jLabel8, 205, 205, 205)))
            .addContainerGap(38, Short.MAX_VALUE));
        contenidoLayout.setVerticalGroup(contenidoLayout.createSequentialGroup()
            .addContainerGap(42, 42).addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(6, 6, 6).addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(32, 32, 32).addGroup(contenidoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(42, 42, 42).addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(22, 22, 22).addGroup(contenidoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(42, Short.MAX_VALUE));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addComponent(jPanelLateral, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(jPanelContenido, javax.swing.GroupLayout.DEFAULT_SIZE, 756, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelLateral, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
            .addComponent(jPanelContenido, javax.swing.GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE));
        pack();
    }// </editor-fold>//GEN-END:initComponents

  
    public static void main(String args[]) {
 
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        javax.swing.UIManager.put("nimbusOrange", Ui.morado);
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        java.awt.EventQueue.invokeLater(() -> {
            try {
                new MenuDUNAB().setVisible(true);
            } catch (RuntimeException ex) {
                logger.log(java.util.logging.Level.SEVERE, "No fue posible abrir DUNAB", ex);
                javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage(),
                        "No se pudo abrir DUNAB", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanelContenido;
    private javax.swing.JPanel jPanelLateral;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
}





