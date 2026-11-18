package javaapplication1;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Componentes comunes para mantener formularios simples y consistentes. */
public final class Ui {
    public static final Color fondo = new Color(247, 245, 242);
    public static final Color morado = new Color(105, 38, 166);
    public static final Color marron = new Color(121, 83, 58);
    public static final Color texto = new Color(52, 45, 56);

    private Ui() { }

    public static JPanel pagina(String titulo, String subtitulo) {
        JPanel pagina = new JPanel(new BorderLayout(16, 18));
        pagina.setBackground(fondo);
        pagina.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));
        JPanel cabecera = new JPanel(new BorderLayout(8, 10));
        cabecera.setOpaque(false);
        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(new Font("SansSerif", Font.BOLD, 27));
        tituloLabel.setForeground(morado);
        JTextArea ayuda = new JTextArea(subtitulo);
        ayuda.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ayuda.setForeground(new Color(105, 96, 109));
        ayuda.setLineWrap(true);
        ayuda.setWrapStyleWord(true);
        ayuda.setRows(2);
        ayuda.setEditable(false);
        ayuda.setBackground(fondo);
        ayuda.setBorder(BorderFactory.createEmptyBorder());
        ayuda.setOpaque(false);
        cabecera.add(tituloLabel, BorderLayout.NORTH);
        cabecera.add(ayuda, BorderLayout.CENTER);
        pagina.add(cabecera, BorderLayout.NORTH);
        Tema.aplicar(pagina);
        return pagina;
    }

    public static JButton boton(String textoBoton, Runnable accion) {
        JButton boton = new JButton(textoBoton);
        boton.setBackground(morado);
        boton.setForeground(Color.WHITE);
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setMargin(new Insets(10, 16, 10, 16));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addActionListener(e -> {
            try {
                accion.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(boton, ex.getMessage(), "Revisa la información", JOptionPane.ERROR_MESSAGE);
            }
        });
        Tema.aplicar(boton);
        return boton;
    }

    public static JTable tabla(String... columnas) {
        JTable tabla = new JTable(new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        });
        tabla.setRowHeight(32);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(new Color(237, 226, 246));
        tabla.setSelectionBackground(new Color(226, 208, 243));
        tabla.setSelectionForeground(texto);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        javax.swing.table.TableRowSorter<?> orden = (javax.swing.table.TableRowSorter<?>) tabla.getRowSorter();
        java.util.Comparator<Object> comparar = (a, b) -> {
            if (a instanceof Number && b instanceof Number)
                return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            return java.text.Collator.getInstance(new Locale("es", "CO")).compare(String.valueOf(a), String.valueOf(b));
        };
        for (int i = 0; i < columnas.length; i++) orden.setComparator(i, comparar);
        tabla.setFillsViewportHeight(true);
        Tema.aplicar(tabla);
        return tabla;
    }

    public static void filas(JTable tabla, Object[][] filas) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0);
        for (Object[] fila : filas) modelo.addRow(fila);
    }

    public static JPanel formulario(Object... pares) {
        if (pares.length % 2 != 0) throw new IllegalArgumentException("Cada campo necesita una etiqueta.");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < pares.length; i += 2) {
            c.gridy = i / 2;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            JLabel etiqueta = new JLabel(String.valueOf(pares[i]));
            etiqueta.setFont(new Font("SansSerif", Font.BOLD, 13));
            etiqueta.setForeground(texto);
            panel.add(etiqueta, c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add((Component) pares[i + 1], c);
        }
        Tema.aplicar(panel);
        return panel;
    }

    public static JTextField campo(String valor) {
        JTextField campo = new JTextField(valor, 24);
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setMargin(new Insets(7, 8, 7, 8));
        Tema.aplicar(campo);
        return campo;
    }

    public static JSpinner entero(int valor, int minimo, int maximo) {
        return new JSpinner(new SpinnerNumberModel(valor, minimo, maximo, 1));
    }

    public static int valor(JSpinner campo) {
        try { campo.commitEdit(); }
        catch (java.text.ParseException ex) { throw new IllegalArgumentException("Escribe un número entero válido."); }
        return ((Number) campo.getValue()).intValue();
    }

    public static String idSeleccionado(JTable tabla, List<String> ids) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) throw new IllegalArgumentException("Selecciona una fila de la tabla.");
        return ids.get(tabla.convertRowIndexToModel(fila));
    }

    public static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "DUNAB", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String numero(int cantidad) {
        return NumberFormat.getIntegerInstance(new Locale("es", "CO")).format(cantidad);
    }

    public static String nombreActividad(ServicioDunab servicio, String id) {
        return servicio.actividad(id).getNombre();
    }
}
