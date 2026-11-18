package javaapplication1;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

/** Colores de la aplicación y preferencia local; cambiar tema no reconstruye los formularios. */
public final class Tema {
    private static final Color NEGRO = new Color(15, 15, 19);
    private static final Color TARJETA = new Color(27, 27, 35);
    private static final Color ENTRADA = new Color(36, 36, 46);
    private static final Color CLARO = new Color(240, 237, 246);
    private static final Color ACENTO = new Color(200, 160, 255);
    private static final Color BOTON = new Color(112, 55, 177);
    private static final Color BORDE = new Color(79, 75, 91);
    private static boolean oscuro;
    private static Path archivo;

    private Tema() { }

    public static boolean esOscuro() { return oscuro; }
    public static Color fondo() { return oscuro ? NEGRO : Ui.fondo; }
    public static Color superficie() { return oscuro ? TARJETA : Color.WHITE; }
    public static Color texto() { return oscuro ? CLARO : Ui.texto; }

    public static void iniciar(Path preferencia) {
        archivo = preferencia;
        oscuro = false;
        if (archivo != null && Files.isRegularFile(archivo)) {
            Properties propiedades = new Properties();
            try (InputStream entrada = Files.newInputStream(archivo)) {
                propiedades.load(entrada);
                oscuro = Boolean.parseBoolean(propiedades.getProperty("modoOscuro", "false"));
            } catch (IOException | IllegalArgumentException ex) {
                Logger.getLogger(Tema.class.getName()).log(Level.WARNING, "No se pudo leer la apariencia; se usará modo claro.", ex);
            }
        }
        instalarColores();
    }

    public static void cambiar() {
        boolean siguiente = !oscuro;
        guardar(siguiente);
        oscuro = siguiente;
        instalarColores();
        // Se mantienen las mismas tablas, valores escritos y selecciones.
        for (Window ventana : Window.getWindows()) {
            if (ventana.isDisplayable()) {
                SwingUtilities.updateComponentTreeUI(ventana);
                aplicar(ventana);
                ventana.validate();
                ventana.repaint();
            }
        }
    }

    private static void guardar(boolean valor) {
        if (archivo == null) return; // Las pruebas inyectan servicios sin cambiar preferencias del usuario.
        Path destino = archivo.toAbsolutePath().normalize();
        Path temporal = null;
        try {
            Files.createDirectories(destino.getParent());
            temporal = Files.createTempFile(destino.getParent(), "apariencia-", ".tmp");
            Properties propiedades = new Properties();
            propiedades.setProperty("modoOscuro", Boolean.toString(valor));
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                propiedades.store(salida, "Preferencia visual DUNAB");
            }
            try { Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el modo de apariencia. Revisa los permisos de la carpeta data.", ex);
        } finally {
            if (temporal != null) try { Files.deleteIfExists(temporal); } catch (IOException ex) { /* No altera los datos DUNAB. */ }
        }
    }

    private static void instalarColores() {
        // Metal respeta directamente los colores de controles y editores en ambos modos.
        javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new javax.swing.plaf.metal.DefaultMetalTheme() {
            private javax.swing.plaf.ColorUIResource recurso(Color color) { return new javax.swing.plaf.ColorUIResource(color); }
            @Override protected javax.swing.plaf.ColorUIResource getPrimary1() { return recurso(oscuro ? ACENTO : Ui.morado); }
            @Override protected javax.swing.plaf.ColorUIResource getPrimary2() { return recurso(oscuro ? BORDE : new Color(190, 165, 215)); }
            @Override protected javax.swing.plaf.ColorUIResource getPrimary3() { return recurso(oscuro ? ENTRADA : new Color(230, 214, 245)); }
            @Override protected javax.swing.plaf.ColorUIResource getSecondary1() { return recurso(oscuro ? new Color(160, 151, 177) : new Color(100, 90, 110)); }
            @Override protected javax.swing.plaf.ColorUIResource getSecondary2() { return recurso(oscuro ? BORDE : new Color(190, 180, 202)); }
            @Override protected javax.swing.plaf.ColorUIResource getSecondary3() { return recurso(oscuro ? TARJETA : Ui.fondo); }
            @Override protected javax.swing.plaf.ColorUIResource getBlack() { return recurso(oscuro ? CLARO : Ui.texto); }
            @Override protected javax.swing.plaf.ColorUIResource getWhite() { return recurso(oscuro ? ENTRADA : Color.WHITE); }
        });
        try { UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel()); }
        catch (UnsupportedLookAndFeelException ex) { throw new IllegalStateException("No se pudo configurar la apariencia.", ex); }
        Color base = fondo(), campo = oscuro ? ENTRADA : Color.WHITE, letra = texto();
        Color acento = oscuro ? BOTON : Ui.morado;
        UIManager.put("control", base);
        UIManager.put("text", letra);
        String[] fondos = {"Panel", "OptionPane", "Viewport", "ScrollPane", "TabbedPane", "ToolBar", "PopupMenu"};
        for (String control : fondos) UIManager.put(control + ".background", base);
        String[] controles = {"Label", "Button", "ToggleButton", "CheckBox", "RadioButton", "ComboBox", "List", "Table", "TableHeader",
                "TextField", "FormattedTextField", "PasswordField", "TextArea", "TextPane", "EditorPane", "Spinner", "TabbedPane", "OptionPane", "MenuItem", "Menu"};
        for (String control : controles) UIManager.put(control + ".foreground", letra);
        String[] campos = {"TextField", "FormattedTextField", "PasswordField", "TextArea", "TextPane", "EditorPane", "ComboBox", "Spinner", "List", "Table"};
        for (String control : campos) {
            UIManager.put(control + ".background", campo);
            UIManager.put(control + ".selectionBackground", acento);
            UIManager.put(control + ".selectionForeground", Color.WHITE);
            UIManager.put(control + ".caretForeground", letra);
            UIManager.put(control + ".inactiveForeground", oscuro ? new Color(177, 170, 190) : Color.GRAY);
        }
        UIManager.put("OptionPane.messageForeground", letra);
        UIManager.put("Button.background", acento);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.disabledText", oscuro ? new Color(191, 182, 204) : Color.GRAY);
        UIManager.put("ToggleButton.background", acento);
        UIManager.put("ToggleButton.foreground", Color.WHITE);
        UIManager.put("CheckBox.background", base);
        UIManager.put("RadioButton.background", base);
        UIManager.put("TableHeader.background", oscuro ? ENTRADA : new Color(237, 226, 246));
        UIManager.put("TabbedPane.selected", superficie());
        UIManager.put("TabbedPane.contentAreaColor", superficie());
        UIManager.put("ScrollBar.background", base);
        UIManager.put("ScrollBar.thumb", oscuro ? BORDE : new Color(190, 180, 202));
        UIManager.put("ProgressBar.foreground", acento);
        UIManager.put("ProgressBar.background", oscuro ? ENTRADA : new Color(231, 225, 237));
        UIManager.put("ProgressBar.selectionBackground", letra);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
        UIManager.put("ToolTip.background", superficie());
        UIManager.put("ToolTip.foreground", letra);
        UIManager.put("MenuItem.background", superficie());
        UIManager.put("Menu.background", superficie());
    }

    public static JButton botonCambio() {
        JButton boton = Ui.boton(oscuro ? "Modo claro" : "Modo oscuro", Tema::cambiar);
        boton.putClientProperty("tema.selector", Boolean.TRUE);
        boton.setToolTipText("Cambiar entre modo claro y modo oscuro; conserva los campos escritos.");
        boton.setName("selectorTema");
        return boton;
    }

    public static void aplicar(Component componente) {
        if (componente instanceof JComponent) {
            JComponent c = (JComponent) componente;
            // Se recuerdan los colores definidos por el diseño para regresar al modo claro.
            if (c.getClientProperty("tema.originalRegistrado") == null) {
                c.putClientProperty("tema.originalRegistrado", Boolean.TRUE);
                c.putClientProperty("tema.fondoOriginal", c.getBackground());
                c.putClientProperty("tema.textoOriginal", c.getForeground());
                c.putClientProperty("tema.bordeOriginal", c.getBorder());
            }
            Color anterior = (Color) c.getClientProperty("tema.fondoOriginal");
            Color tinta = (Color) c.getClientProperty("tema.textoOriginal");
            boolean marca = Ui.morado.equals(anterior) && !(c instanceof AbstractButton);
            c.setForeground(colorTexto(tinta));
            if (c instanceof AbstractButton) {
                if (c instanceof JCheckBox || c instanceof JRadioButton) {
                    c.setBackground(fondo());
                    c.setForeground(texto());
                } else {
                    c.setBackground(oscuro ? BOTON : (anterior == null || anterior instanceof UIResource ? Ui.morado : anterior));
                    c.setForeground(Color.WHITE);
                }
                if (Boolean.TRUE.equals(c.getClientProperty("tema.selector")))
                    ((AbstractButton) c).setText(oscuro ? "Modo claro" : "Modo oscuro");
            } else if (c instanceof JTable) {
                JTable tabla = (JTable) c;
                tabla.setBackground(superficie());
                tabla.setForeground(texto());
                tabla.setSelectionBackground(oscuro ? BOTON : new Color(226, 208, 243));
                tabla.setSelectionForeground(oscuro ? Color.WHITE : Ui.texto);
                tabla.setGridColor(oscuro ? BORDE : new Color(220, 215, 228));
                aplicar(tabla.getTableHeader());
            } else if (c instanceof javax.swing.table.JTableHeader) {
                c.setBackground(oscuro ? ENTRADA : new Color(237, 226, 246));
                c.setForeground(texto());
            } else if (c instanceof JTextComponent) {
                JTextComponent campo = (JTextComponent) c;
                campo.setBackground(oscuro ? (campo.isEditable() ? ENTRADA : TARJETA)
                        : (anterior instanceof UIResource || anterior == null ? Color.WHITE : anterior));
                campo.setForeground(texto());
                campo.setCaretColor(texto());
                campo.setSelectionColor(oscuro ? BOTON : Ui.morado);
                campo.setSelectedTextColor(Color.WHITE);
            } else if (c instanceof JComboBox || c instanceof JSpinner || c instanceof JList) {
                c.setBackground(oscuro ? ENTRADA : Color.WHITE);
                c.setForeground(texto());
            } else if (c instanceof JProgressBar) {
                c.setBackground(oscuro ? ENTRADA : new Color(231, 225, 237));
                c.setForeground(oscuro ? BOTON : Ui.morado);
            } else if (c instanceof JPanel || c instanceof JScrollPane || c instanceof JViewport || c instanceof JTabbedPane) {
                c.setBackground(marca ? (oscuro ? new Color(39, 23, 58) : Ui.morado)
                        : (Color.WHITE.equals(anterior) && !(anterior instanceof UIResource) ? superficie() : fondo()));
            }
            Border original = (Border) c.getClientProperty("tema.bordeOriginal");
            if (original != null) c.setBorder(oscuro ? bordeOscuro(original) : original);
        } else if (componente instanceof Window) componente.setBackground(fondo());
        if (componente instanceof Container)
            for (Component hijo : ((Container) componente).getComponents()) aplicar(hijo);
    }

    private static Color colorTexto(Color original) {
        if (Color.WHITE.equals(original) && !(original instanceof UIResource)) return Color.WHITE;
        if (Ui.morado.equals(original)) return oscuro ? ACENTO : Ui.morado;
        if (Ui.marron.equals(original)) return oscuro ? new Color(229, 181, 143) : Ui.marron;
        return oscuro ? CLARO : (original == null || original instanceof UIResource ? Ui.texto : original);
    }

    private static Border bordeOscuro(Border borde) {
        if (borde instanceof LineBorder) {
            LineBorder linea = (LineBorder) borde;
            return BorderFactory.createLineBorder(BORDE, linea.getThickness(), linea.getRoundedCorners());
        }
        if (borde instanceof CompoundBorder) {
            CompoundBorder compuesto = (CompoundBorder) borde;
            return BorderFactory.createCompoundBorder(bordeOscuro(compuesto.getOutsideBorder()), bordeOscuro(compuesto.getInsideBorder()));
        }
        return borde;
    }
}
