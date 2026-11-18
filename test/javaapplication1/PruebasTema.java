package javaapplication1;

import java.awt.Color;
import java.awt.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Apariencia sin JFrame y sin preferencias ni registros del usuario. */
public final class PruebasTema {
    private static int comprobaciones;

    private PruebasTema() { }

    public static void main(String[] args) throws Exception {
        Path raiz = Files.createTempDirectory("dunab-pruebas-tema-");
        Path data = raiz.resolve("data");
        ServicioDunab servicio = new ServicioDunab(data);
        Path archivoDatos = data.resolve("dunab.dat");
        byte[] datosAntes = Files.readAllBytes(archivoDatos);
        Path preferencia = data.resolve("apariencia.properties");

        SwingUtilities.invokeAndWait(() -> probarComponentes(preferencia));
        verificar(Arrays.equals(datosAntes, Files.readAllBytes(archivoDatos)),
                "Cambiar apariencia no modifica los datos DUNAB serializados");
        ServicioDunab reabierto = new ServicioDunab(data);
        Usuario demo = reabierto.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        verificar(reabierto.saldo(demo.getId(), 0) == 1400,
                "El almacenamiento de DUNAB sigue siendo legible después de cambiar tema");
        reabierto.logout();

        Properties guardadas = new Properties();
        try (java.io.InputStream entrada = Files.newInputStream(preferencia)) { guardadas.load(entrada); }
        verificar("true".equals(guardadas.getProperty("modoOscuro")), "La preferencia persistida registra modo oscuro");
        byte[] preferenciaAntes = Files.readAllBytes(preferencia);
        SwingUtilities.invokeAndWait(() -> {
            Tema.iniciar(null);
            verificar(!Tema.esOscuro(), "Preferencia nula comienza en modo claro");
            Tema.cambiar();
            verificar(Tema.esOscuro(), "Preferencia nula permite cambiar sin guardar");
            Tema.cambiar();
            verificar(!Tema.esOscuro(), "Preferencia nula permite regresar a claro");
        });
        verificar(Arrays.equals(preferenciaAntes, Files.readAllBytes(preferencia)),
                "Cambios con preferencia nula no alteran el archivo anterior");
        verificar(Arrays.equals(datosAntes, Files.readAllBytes(archivoDatos)),
                "Cambios sin persistencia tampoco modifican los registros DUNAB");

        Path corrupta = raiz.resolve("apariencia-corrupta.properties");
        String propiedadInvalida = "modoOscuro=" + (char) 92 + "uZZZZ\n";
        byte[] contenidoInvalido = propiedadInvalida.getBytes(StandardCharsets.ISO_8859_1);
        Files.write(corrupta, contenidoInvalido);
        SwingUtilities.invokeAndWait(() -> {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Tema.class.getName());
            java.util.logging.Level nivelAnterior = logger.getLevel();
            try {
                // El aviso de este archivo inválido es esperado y no necesita imprimir su traza.
                logger.setLevel(java.util.logging.Level.OFF);
                Tema.iniciar(corrupta);
                verificar(!Tema.esOscuro(), "Una propiedad con escape inválido utiliza modo claro sin impedir el inicio");
            } finally {
                logger.setLevel(nivelAnterior);
            }
        });
        verificar(Arrays.equals(contenidoInvalido, Files.readAllBytes(corrupta)),
                "La carga fallida conserva el archivo de apariencia inválido");

        Path obstruccion = raiz.resolve("padre-que-es-un-archivo");
        Files.write(obstruccion, new byte[]{7});
        SwingUtilities.invokeAndWait(() -> {
            Tema.iniciar(obstruccion.resolve("apariencia.properties"));
            verificar(!Tema.esOscuro(), "Preferencia inexistente utiliza modo claro");
            denegar(Tema::cambiar, "Error al guardar apariencia se comunica con mensaje");
            verificar(!Tema.esOscuro(), "Un fallo al guardar no cambia el modo actual");
            Tema.iniciar(null);
        });
        verificar(Arrays.equals(new byte[]{7}, Files.readAllBytes(obstruccion)),
                "El fallo de escritura conserva el archivo que obstruye la ruta");
        System.out.println("PruebasTema: " + comprobaciones + " comprobaciones correctas.");
        System.out.println("Datos de prueba independientes: " + raiz.toAbsolutePath());
    }

    private static void probarComponentes(Path preferencia) {
        Tema.iniciar(preferencia);
        verificar(!Tema.esOscuro(), "Sin archivo se inicia en modo claro");
        verificar(!Files.exists(preferencia), "Iniciar sin archivo no guarda una preferencia implícita");
        JPanel panel = new JPanel();
        panel.setBackground(Ui.fondo);
        JTextField campo = Ui.campo("Texto escrito por el estudiante");
        JPasswordField clave = new JPasswordField("ClaveTemporal123!");
        JComboBox<String> semestre = new JComboBox<String>(Semestres.opciones(false));
        semestre.setSelectedIndex(5);
        JTable tabla = new JTable(new DefaultTableModel(new Object[][]{{"Evaluación 1", "3,5", "100"}},
                new String[]{"Evaluación", "Nota", "Peso %"}));
        tabla.setRowSelectionInterval(0, 0);
        JButton boton = Tema.botonCambio();
        panel.add(campo);
        panel.add(clave);
        panel.add(semestre);
        panel.add(new JScrollPane(tabla));
        panel.add(boton);
        Tema.aplicar(panel);
        verificar(Color.WHITE.equals(campo.getBackground()), "Campo claro tiene fondo blanco");
        verificar(Ui.texto.equals(campo.getForeground()), "Campo claro tiene texto oscuro");
        verificar(Color.WHITE.equals(tabla.getBackground()), "Tabla clara tiene fondo blanco");
        verificar(Ui.texto.equals(tabla.getForeground()), "Tabla clara tiene texto oscuro");
        verificar("Modo oscuro".equals(boton.getText()), "Botón ofrece activar modo oscuro");
        verificar("selectorTema".equals(boton.getName()), "Selector de tema tiene identificador accesible");

        Tema.cambiar();
        SwingUtilities.updateComponentTreeUI(panel);
        Tema.aplicar(panel);
        verificar(Tema.esOscuro(), "Se activa modo oscuro");
        verificar(Files.isRegularFile(preferencia), "Cambiar crea el archivo de preferencia");
        verificar(brillo(campo.getBackground()) < 80 && brillo(campo.getForeground()) > 200,
                "Campo oscuro tiene fondo oscuro y letras claras");
        verificar(brillo(tabla.getBackground()) < 80 && brillo(tabla.getForeground()) > 200,
                "Tabla oscura tiene fondo oscuro y letras claras");
        verificar(brillo(tabla.getTableHeader().getBackground()) < 80
                && brillo(tabla.getTableHeader().getForeground()) > 200, "Encabezado de tabla conserva contraste oscuro");
        verificar("Modo claro".equals(boton.getText()), "Botón ofrece regresar a modo claro");
        verificar(campo.getText().equals("Texto escrito por el estudiante"), "Cambiar conserva el campo escrito");
        verificar(Arrays.equals(clave.getPassword(), "ClaveTemporal123!".toCharArray()), "Cambiar conserva el campo de contraseña");
        verificar(semestre.getSelectedIndex() == 5, "Cambiar conserva la selección de semestre");
        verificar(tabla.getSelectedRow() == 0 && tabla.getValueAt(0, 1).equals("3,5"), "Cambiar conserva selección y datos de tabla");

        verificar(tabla.editCellAt(0, 1), "Se puede editar una nota con modo oscuro");
        Component editor = tabla.getEditorComponent();
        verificar(editor instanceof JTextField, "La celda usa un editor de texto");
        ((JTextField) editor).setText("4,75");
        Tema.cambiar();
        SwingUtilities.updateComponentTreeUI(panel);
        Tema.aplicar(panel);
        verificar(!Tema.esOscuro(), "Se regresa a modo claro");
        verificar(tabla.isEditing() && tabla.getEditorComponent() instanceof JTextField
                && "4,75".equals(((JTextField) tabla.getEditorComponent()).getText()),
                "Cambiar tema durante edición conserva el texto pendiente de confirmar");
        verificar(tabla.getCellEditor().stopCellEditing(), "La edición sigue siendo confirmable tras cambiar tema");
        verificar("4,75".equals(tabla.getValueAt(0, 1)), "La nota editada queda en el modelo");
        verificar(Color.WHITE.equals(campo.getBackground()) && Ui.texto.equals(campo.getForeground()),
                "Volver a claro restaura colores de campo");
        verificar(Color.WHITE.equals(tabla.getBackground()) && Ui.texto.equals(tabla.getForeground()),
                "Volver a claro restaura colores de tabla");
        verificar("Modo oscuro".equals(boton.getText()), "Texto del botón se actualiza al regresar a claro");
        verificar("Texto escrito por el estudiante".equals(campo.getText()) && semestre.getSelectedIndex() == 5,
                "Regresar a claro conserva formulario y semestre");
        Tema.iniciar(preferencia);
        verificar(!Tema.esOscuro(), "Reiniciar recupera la preferencia clara guardada");
        Tema.cambiar();
        Tema.iniciar(preferencia);
        verificar(Tema.esOscuro(), "Reiniciar recupera la preferencia oscura guardada");
        JTextField nuevo = Ui.campo("Creado en oscuro");
        Tema.aplicar(nuevo);
        verificar(brillo(nuevo.getBackground()) < 80 && brillo(nuevo.getForeground()) > 200,
                "Un campo nuevo respeta el modo oscuro cargado");
        Tema.aplicar(panel);
        verificar("4,75".equals(tabla.getValueAt(0, 1)), "Reaplicar apariencia conserva el dato confirmado");
    }

    private static int brillo(Color color) {
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }

    private static void denegar(Runnable operacion, String mensaje) {
        try { operacion.run(); }
        catch (IllegalStateException ex) {
            verificar(ex.getMessage() != null && !ex.getMessage().trim().isEmpty(), mensaje);
            return;
        }
        throw new AssertionError(mensaje);
    }
}
