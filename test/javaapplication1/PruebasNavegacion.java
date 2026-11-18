package javaapplication1;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Prueba opcional de la ventana real; necesita un escritorio gráfico, sin ventanas visibles. */
public final class PruebasNavegacion {
    private static int comprobaciones;

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("dunab-navegacion-");
        ServicioDunab servicio = new ServicioDunab(carpeta);
        servicio.registrar("María Participante", "participante@prueba.local", "Participante123!".toCharArray(), "Sistemas", 6);
        String estudianteId = servicio.login("participante@prueba.local", "Participante123!".toCharArray()).getId();
        servicio.login("admin@demo.local", "Admin123!".toCharArray());
        servicio.guardarActividad(null, "Actividad para inscripción", "Bienestar", "Actividad para comprobar el recorrido completo.",
                java.time.LocalDate.now(), "Sala de bienestar", 125, 2, true);
        String actividadId = servicio.actividades("Actividad para inscripción", "Todas").get(0).getId();
        servicio.guardarActividad(null, "Actividad futura para inscripción", "Cultura", "Actividad programada para comprobar el selector.",
                java.time.LocalDate.now().plusDays(2), "Aula creativa", 200, 3, true);
        String futuraId = servicio.actividades("Actividad futura para inscripción", "Todas").get(0).getId();
        servicio.logout();
        SwingUtilities.invokeAndWait(() -> {
            MenuDUNAB menu = new MenuDUNAB(servicio, carpeta.resolve("apariencia.properties"));
            try {
                verificar(servicio.getActual() == null, "Inicio sin sesión");
                verificar(existeBoton(menu, "Modo oscuro"), "Control de tema en el acceso");
                JTextField borrador = componentes(menu, JTextField.class).get(0);
                borrador.setText("borrador@demo.local");
                boton(menu, "Modo oscuro").doClick();
                verificar(Tema.esOscuro(), "Activar modo oscuro desde acceso");
                verificar(borrador.getText().equals("borrador@demo.local"), "Cambiar tema conserva campos del acceso");
                entrar(menu, "estudiante@demo.local", "Estudiante123!");
                verificar(servicio.getActual().getRol().equals("ESTUDIANTE"), "Acceso de estudiante desde el formulario");
                verificar(!existeBoton(menu, "Revisar solicitudes"), "Navegación oculta administración");
                JPanel lateral = nombrado(menu, JPanel.class, "navegacionLateral");
                verificar(!existeBoton(lateral, "Registrar participación"), "Participación no aparece en la barra lateral");
                verificar(existeBoton(menu, "Ver actividades e inscribirme"), "El inicio ofrece un acceso visible para inscribirse");
                Rectangle area = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                verificar(area.contains(menu.getBounds()), "La ventana cabe en el espacio útil de la pantalla");
                String[] secciones = {"Inicio", "Mi perfil", "Consultar DUNAB", "Actividades y espacios", "Inscripciones",
                    "Historial de DUNAB", "Metas personales", "Top 9", "Calculadora de notas", "Estadísticas y reportes"};
                for (String seccion : secciones) {
                    boton(menu, seccion).doClick();
                    verificar(servicio.getActual() != null, "Se abre " + seccion);
                }
                boton(menu, "Actividades y espacios").doClick();
                JTable actividadesFuturas = nombrado(menu, JTable.class, "tablaActividades");
                seleccionar(actividadesFuturas, 0, "Actividad futura para inscripción");
                pulsarConDialogos(nombrado(menu, JButton.class, "inscribirmeActividad"));
                verificar(servicio.misInscripciones().size() == 4, "La inscripción futura se guarda desde el catálogo");
                boton(menu, "Inscripciones").doClick();
                boton(menu, "Registrar participación").doClick();
                verificar(existeBoton(menu, "Volver a mis inscripciones"), "La participación se abre desde Inscripciones");
                JComboBox<?> todasLasInscritas = nombrado(menu, JComboBox.class, "actividadParticipacion");
                verificar(todasLasInscritas.getItemCount() == 4, "El selector conserva futuras, pendientes y aprobadas visibles");
                seleccionarActividad(todasLasInscritas, futuraId);
                verificar(todasLasInscritas.isEnabled(), "La actividad futura se puede seleccionar");
                verificar(nombrado(menu, JTextArea.class, "estadoParticipacion").getText().contains(java.time.LocalDate.now().plusDays(2).toString()),
                        "La fecha para registrar la actividad futura se explica en pantalla");
                verificar(!nombrado(menu, JButton.class, "enviarParticipacion").isEnabled(),
                        "Una actividad futura no permite enviar una participación anticipada");
                menu.validate();
                capturar(menu, "38-participacion-futura-visible.png");
                for (SolicitudParticipacion solicitud : servicio.misSolicitudes()) {
                    seleccionarActividad(todasLasInscritas, solicitud.getActividadId());
                    verificar(!nombrado(menu, JButton.class, "enviarParticipacion").isEnabled(),
                            "Una solicitud pendiente o aprobada se muestra sin permitir duplicarla");
                }
                boton(menu, "Volver a mis inscripciones").doClick();
                verificar(existeBoton(menu, "Buscar actividades"), "Se puede regresar a Inscripciones y buscar otras actividades");
                boton(menu, "Inicio").doClick();
                JComboBox<?> selector = componentes(menu, JComboBox.class).get(0);
                verificar(selector.getItemCount() == Semestres.MAXIMO, "Selector ofrece todos los semestres");
                selector.setSelectedIndex(0);
                verificar(textoContiene(menu, "Semestre 1"), "Selector refresca semestre 1");
                selector.setSelectedIndex(1);
                verificar(textoContiene(menu, "Semestre 2"), "Selector refresca semestre 2");
                selector.setSelectedIndex(5);
                verificar(textoContiene(menu, "Semestre 6"), "Selector refresca semestre 6");
                selector.setSelectedIndex(11);
                verificar(textoContiene(menu, "Semestre 12"), "Selector refresca semestre 12");
                selector.setSelectedIndex(1);
                boton(menu, "Calculadora de notas").doClick();
                JSpinner cantidad = nombrado(menu, JSpinner.class, "cantidadNotas");
                cantidad.setValue(4);
                boton(menu, "Aplicar cantidad").doClick();
                JTable tabla = nombrado(menu, JTable.class, "tablaCalculadora");
                verificar(tabla.getRowCount() == 4, "Calculadora admite cuatro evaluaciones");
                double[] notas = {3, 4, 5, 4}, pesos = {20, 25, 15, 40};
                for (int i = 0; i < 4; i++) {
                    tabla.setValueAt(notas[i], i, 1);
                    tabla.setValueAt(pesos[i], i, 2);
                }
                boton(menu, "Calcular nota final").doClick();
                verificar(textoContiene(menu, "3,95") || textoContiene(menu, "3.95"), "Nota con pesos personalizados");
                tabla.editCellAt(0, 1);
                ((JTextField) tabla.getEditorComponent()).setText("4");
                boton(menu, "Calcular nota final").doClick();
                verificar(textoContiene(menu, "4,15") || textoContiene(menu, "4.15"), "Se guarda edición de celda antes de calcular");
                boton(menu, "Modo claro").doClick();
                verificar(!Tema.esOscuro(), "Volver al modo claro dentro de sesión");
                verificar(tabla.getValueAt(0, 1).toString().equals("4"), "Cambio de tema conserva notas personalizadas");
                verificar(textoContiene(menu, "4,15") || textoContiene(menu, "4.15"), "Cambio de tema conserva el resultado");
                tabla.setValueAt("", 3, 1);
                nombrado(menu, JSpinner.class, "evaluacionPendiente").setValue(4);
                boton(menu, "Calcular nota necesaria").doClick();
                verificar(textoContiene(menu, "1,13") || textoContiene(menu, "1.13"), "Nota necesaria de evaluación elegida");
                boton(menu, "Modo oscuro").doClick();
                menu.validate();
                capturar(menu, "34-calculadora-oscura.png");
                boton(menu, "Inicio").doClick();
                menu.validate();
                capturar(menu, "35-menu-oscuro.png");
                boton(menu, "Cerrar sesión").doClick();
                verificar(servicio.getActual() == null, "Botón cierra sesión");
                verificar(!existeBoton(menu, "Consultar DUNAB"), "Salir quita datos de sesión anterior");
                verificar(existeBoton(menu, "Modo claro"), "El tema se conserva al cerrar sesión");
                entrar(menu, "admin@demo.local", "Admin123!");
                verificar(servicio.getActual().getRol().equals("ADMINISTRADOR"), "Cambio a administrador");
                String[] administrativas = {"Administrar actividades", "Revisar solicitudes", "Estudiantes y ajustes", "Auditoría", "Top 9", "Calculadora de notas", "Mi perfil", "Inicio"};
                for (String seccion : administrativas) {
                    boton(menu, seccion).doClick();
                    verificar(servicio.getActual().getRol().equals("ADMINISTRADOR"), "Se abre " + seccion);
                }
                boton(menu, "Cerrar sesión").doClick();
                entrar(menu, "estudiante@demo.local", "Estudiante123!");
                verificar(!existeBoton(menu, "Auditoría"), "Cambio de usuario no conserva controles de administrador");
                boton(menu, "Cerrar sesión").doClick();
                entrar(menu, "participante@prueba.local", "Participante123!");
                verificar(servicio.saldo(estudianteId, 0) == 0, "Participante nuevo comienza sin DUNAB");
                menu.setSize(Math.min(1100, area.width), Math.min(700, area.height));
                boton(menu, "Ver actividades e inscribirme").doClick();
                JTable catalogo = nombrado(menu, JTable.class, "tablaActividades");
                seleccionar(catalogo, 0, "Actividad para inscripción");
                JButton inscribirme = nombrado(menu, JButton.class, "inscribirmeActividad");
                verificar(inscribirme.isEnabled(), "El estudiante puede inscribirse en una actividad disponible");
                menu.validate();
                Rectangle posicion = SwingUtilities.convertRectangle(inscribirme.getParent(), inscribirme.getBounds(), menu.getContentPane());
                verificar(posicion.y >= 0 && posicion.y + posicion.height <= menu.getContentPane().getHeight(),
                        "Inscribirme permanece visible con una ventana de 700 píxeles de alto");
                capturar(menu, "36-inscripcion-visible.png");
                pulsarConDialogos(inscribirme);
                verificar(servicio.misInscripciones().size() == 1, "El botón guarda una inscripción real");
                verificar(nombrado(menu, JTable.class, "tablaInscripciones").getRowCount() == 1,
                        "Después de inscribirse se abre el listado de inscripciones");
                verificar(servicio.saldo(estudianteId, 0) == 0, "La inscripción no suma DUNAB");
                boton(menu, "Registrar participación").doClick();
                JComboBox<?> actividad = nombrado(menu, JComboBox.class, "actividadParticipacion");
                verificar(((Actividad) actividad.getSelectedItem()).getId().equals(actividadId),
                        "La actividad inscrita de hoy está disponible para registrar participación");
                nombrado(menu, JTextField.class, "evidenciaParticipacion").setText("ASISTENCIA-PRUEBA-001");
                pulsarConDialogos(nombrado(menu, JButton.class, "enviarParticipacion"));
                verificar(servicio.misSolicitudes().size() == 1 && "PENDIENTE".equals(servicio.misSolicitudes().get(0).getEstado()),
                        "El formulario envía una solicitud pendiente real");
                verificar(servicio.saldo(estudianteId, 0) == 0, "La solicitud pendiente no suma DUNAB");
                boton(menu, "Volver a mis inscripciones").doClick();
                JTable misInscripciones = nombrado(menu, JTable.class, "tablaInscripciones");
                verificar(String.valueOf(misInscripciones.getValueAt(0, 2)).contains("Pendiente"),
                        "Inscripciones muestra que el reconocimiento espera revisión");
                menu.validate();
                capturar(menu, "37-inscripciones-participacion.png");
                boton(menu, "Cerrar sesión").doClick();
                entrar(menu, "admin@demo.local", "Admin123!");
                boton(menu, "Revisar solicitudes").doClick();
                JTable solicitudes = componentes(menu, JTable.class).get(0);
                seleccionar(solicitudes, 0, "María Participante");
                pulsarConDialogos(boton(menu, "Aprobar seleccionada"));
                verificar(servicio.saldo(estudianteId, 6) == 125, "La aprobación desde el formulario reconoce DUNAB una sola vez");
                boton(menu, "Cerrar sesión").doClick();
                entrar(menu, "participante@prueba.local", "Participante123!");
                verificar(servicio.saldo(estudianteId, 0) == 125 && servicio.misMovimientos().size() == 1,
                        "El estudiante consulta su acumulado y movimiento aprobado");
            } finally {
                menu.dispose();
            }
        });
        Tema.iniciar(carpeta.resolve("apariencia.properties"));
        verificar(Tema.esOscuro(), "La preferencia se conserva al reiniciar");
        ServicioDunab reabierto = new ServicioDunab(carpeta);
        reabierto.login("participante@prueba.local", "Participante123!".toCharArray());
        verificar(reabierto.saldo(estudianteId, 6) == 125 && reabierto.misInscripciones().size() == 1,
                "El recorrido de inscripción y reconocimiento persiste al reiniciar");
        System.out.println("PruebasNavegacion: " + comprobaciones + " comprobaciones correctas.");
    }

    private static void entrar(Container ventana, String correo, String clave) {
        List<JTextField> campos = componentes(ventana, JTextField.class);
        for (JTextField campo : campos) {
            if (!(campo instanceof JPasswordField)) { campo.setText(correo); break; }
        }
        componentes(ventana, JPasswordField.class).get(0).setText(clave);
        boton(ventana, "Iniciar sesión").doClick();
    }

    private static JButton boton(Container padre, String texto) {
        for (JButton boton : componentes(padre, JButton.class)) if (texto.equals(boton.getText())) return boton;
        throw new AssertionError("No existe el botón " + texto);
    }

    private static void seleccionar(JTable tabla, int columna, String valor) {
        for (int fila = 0; fila < tabla.getModel().getRowCount(); fila++) {
            if (valor.equals(String.valueOf(tabla.getModel().getValueAt(fila, columna)))) {
                int visible = tabla.convertRowIndexToView(fila);
                tabla.setRowSelectionInterval(visible, visible);
                return;
            }
        }
        throw new AssertionError("No se encontró la fila " + valor);
    }

    private static void seleccionarActividad(JComboBox<?> combo, String actividadId) {
        for (int indice = 0; indice < combo.getItemCount(); indice++) {
            if (((Actividad) combo.getItemAt(indice)).getId().equals(actividadId)) {
                combo.setSelectedIndex(indice);
                return;
            }
        }
        throw new AssertionError("La actividad inscrita desapareció del selector: " + actividadId);
    }

    /** Responde únicamente a los diálogos del proceso de prueba, nunca a ventanas de otras apps. */
    private static void pulsarConDialogos(JButton boton) {
        List<String> errores = new ArrayList<>();
        javax.swing.Timer temporizador = new javax.swing.Timer(60, evento -> {
            for (Window ventana : Window.getWindows()) {
                if (!(ventana instanceof JDialog) || !ventana.isVisible()) continue;
                for (JOptionPane dialogo : componentes((Container) ventana, JOptionPane.class)) {
                    if (dialogo.getMessageType() == JOptionPane.ERROR_MESSAGE || dialogo.getMessageType() == JOptionPane.WARNING_MESSAGE) {
                        errores.add(String.valueOf(dialogo.getMessage()));
                        dialogo.setValue(JOptionPane.CANCEL_OPTION);
                    } else {
                        for (JTextArea campo : componentes(dialogo, JTextArea.class))
                            if (campo.isEditable()) campo.setText("Participación verificada en la prueba automática.");
                        dialogo.setValue(JOptionPane.OK_OPTION);
                    }
                }
            }
        });
        temporizador.start();
        try { boton.doClick(); }
        finally { temporizador.stop(); }
        if (!errores.isEmpty()) throw new AssertionError("El recorrido mostró un error: " + errores);
    }

    private static <T extends Component> T nombrado(Container padre, Class<T> clase, String nombre) {
        for (T componente : componentes(padre, clase)) if (nombre.equals(componente.getName())) return componente;
        throw new AssertionError("No existe el control " + nombre);
    }

    private static boolean existeBoton(Container padre, String texto) {
        for (JButton boton : componentes(padre, JButton.class)) if (texto.equals(boton.getText())) return true;
        return false;
    }

    private static boolean textoContiene(Container padre, String texto) {
        for (JTextArea area : componentes(padre, JTextArea.class)) if (area.getText().contains(texto)) return true;
        for (JLabel label : componentes(padre, JLabel.class)) if (label.getText() != null && label.getText().contains(texto)) return true;
        return false;
    }

    private static <T extends Component> List<T> componentes(Container padre, Class<T> clase) {
        List<T> resultado = new ArrayList<>();
        for (Component componente : padre.getComponents()) {
            if (clase.isInstance(componente)) resultado.add(clase.cast(componente));
            if (componente instanceof Container) resultado.addAll(componentes((Container) componente, clase));
        }
        return resultado;
    }

    private static void capturar(MenuDUNAB menu, String nombre) {
        distribuir(menu.getContentPane());
        distribuir(menu.getContentPane());
        BufferedImage imagen = new BufferedImage(menu.getContentPane().getWidth(), menu.getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        menu.getContentPane().printAll(g);
        g.dispose();
        try {
            Files.createDirectories(Paths.get("tmp", "vistas"));
            ImageIO.write(imagen, "png", Paths.get("tmp", "vistas", nombre).toFile());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private static void distribuir(Container padre) {
        padre.doLayout();
        for (Component hijo : padre.getComponents()) if (hijo instanceof Container) distribuir((Container) hijo);
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }
}
