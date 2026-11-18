package javaapplication1;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Paneles sin ventanas: visibilidad de inscripciones, motivo del bloqueo y reglas reales de envío. */
public final class PruebasParticipacion {
    private static int comprobaciones;
    private PruebasParticipacion() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path carpeta = Files.createTempDirectory("dunab-pruebas-participacion-");
        ServicioDunab servicio = new ServicioDunab(carpeta);
        servicio.registrar("Estudiante estados", "estados@prueba.local", clave(), "Sistemas", 6);
        servicio.registrar("Estudiante futura", "futura@prueba.local", clave(), "Sistemas", 6);
        servicio.login("admin@demo.local", "Admin123!".toCharArray());
        Map<String, String> actividades = new LinkedHashMap<String, String>();
        for (String estado : new String[]{"hoy", "futura", "cancelada", "pendiente", "aprobada", "rechazada"}) {
            String nombre = "Actividad de prueba " + estado;
            servicio.guardarActividad(null, nombre, "Cultura", "Actividad aislada para comprobar participación.",
                    "futura".equals(estado) ? LocalDate.now().plusDays(5) : LocalDate.now(), "Sala de prueba", 100, 20, true);
            actividades.put(estado, servicio.actividades(nombre, "Todas").get(0).getId());
        }
        servicio.logout();
        Usuario usuario = servicio.login("estados@prueba.local", clave());
        String estudianteId = usuario.getId();
        for (String id : actividades.values()) servicio.inscribir(id);
        for (Inscripcion inscripcion : servicio.misInscripciones()) {
            if (inscripcion.getActividadId().equals(actividades.get("cancelada"))) servicio.cancelarInscripcion(inscripcion.getId());
        }
        for (String estado : new String[]{"pendiente", "aprobada", "rechazada"}) {
            servicio.solicitar(actividades.get(estado), LocalDate.now(), 100, "REFERENCIA-" + estado, 6);
        }
        String solicitudAprobada = solicitudDe(servicio, actividades.get("aprobada"));
        String solicitudRechazada = solicitudDe(servicio, actividades.get("rechazada"));
        servicio.logout();
        servicio.login("admin@demo.local", "Admin123!".toCharArray());
        servicio.resolverSolicitud(solicitudAprobada, true, "Asistencia de prueba verificada.");
        servicio.resolverSolicitud(solicitudRechazada, false, "Código incompleto: adjunta la referencia corregida.");
        servicio.logout();
        servicio.login("estados@prueba.local", clave());

        SwingUtilities.invokeAndWait(() -> {
            PantallasEstudiante pantallas = new PantallasEstudiante(servicio, () -> 6, () -> { });
            JPanel panel = pantallas.crear("Registrar participación");
            JComboBox<?> combo = nombrado(panel, JComboBox.class, "actividadParticipacion");
            JButton enviar = nombrado(panel, JButton.class, "enviarParticipacion");
            JTextField evidencia = nombrado(panel, JTextField.class, "evidenciaParticipacion");
            JTextArea razon = nombrado(panel, JTextArea.class, "estadoParticipacion");
            verificar(combo.getItemCount() == 5, "El selector incluye todas las inscripciones activas, incluso las bloqueadas");
            verificar(!contiene(combo, actividades.get("cancelada")), "Una inscripción cancelada no aparece como activa");
            verificar(enviar.isEnabled(), "Se preselecciona una actividad que permite enviar evidencia");
            int solicitudesAntes = servicio.misSolicitudes().size();
            seleccionar(combo, actividades.get("futura"));
            verificar(!enviar.isEnabled() && !evidencia.isEnabled(), "La actividad futura es visible y bloquea el envío");
            verificar(razon.getText().toLowerCase().contains("futura")
                    && razon.getText().contains(LocalDate.now().plusDays(5).toString()), "La actividad futura explica el bloqueo y su fecha");
            enviar.doClick();
            verificar(servicio.misSolicitudes().size() == solicitudesAntes, "Pulsar el botón deshabilitado no crea una solicitud futura");
            denegar(() -> servicio.solicitar(actividades.get("futura"), LocalDate.now().plusDays(5), 100, "EVIDENCIA", 6),
                    "El servicio también impide reconocer una participación futura");

            seleccionar(combo, actividades.get("pendiente"));
            verificar(!enviar.isEnabled() && !evidencia.isEnabled(), "La actividad pendiente es visible y bloquea un segundo envío");
            verificar(razon.getText().toLowerCase().contains("pendiente"), "Se explica que la solicitud espera revisión");
            denegar(() -> servicio.solicitar(actividades.get("pendiente"), LocalDate.now(), 100, "SEGUNDA-EVIDENCIA", 6),
                    "El servicio rechaza una solicitud pendiente duplicada");

            seleccionar(combo, actividades.get("aprobada"));
            verificar(!enviar.isEnabled() && !evidencia.isEnabled(), "La actividad aprobada es visible y bloquea un segundo envío");
            verificar(razon.getText().toLowerCase().contains("aprobada"), "Se explica que los DUNAB ya fueron reconocidos");
            denegar(() -> servicio.solicitar(actividades.get("aprobada"), LocalDate.now(), 100, "SEGUNDA-EVIDENCIA", 6),
                    "El servicio rechaza otra solicitud para una participación aprobada");
            denegar(() -> servicio.solicitar(actividades.get("cancelada"), LocalDate.now(), 100, "EVIDENCIA", 6),
                    "El servicio rechaza una inscripción cancelada");

            seleccionar(combo, actividades.get("rechazada"));
            verificar(enviar.isEnabled() && evidencia.isEnabled(), "Una solicitud rechazada permite presentar evidencia corregida");
            verificar(razon.getText().toLowerCase().contains("rechazada") && razon.getText().contains("Código incompleto"),
                    "Se muestran el rechazo y la observación que debe corregirse");
            verificar(servicio.saldo(estudianteId, 0) == 100, "Consultar estados no cambia los DUNAB aprobados");
            // El recorrido con clic y JOptionPane se cubre en PruebasNavegacion; aquí se prueba el servicio sin ventanas.
            servicio.solicitar(actividades.get("rechazada"), LocalDate.now(), 100, "REFERENCIA-CORREGIDA", 6);
            verificar(servicio.misSolicitudes().size() == solicitudesAntes + 1, "La evidencia corregida crea una nueva solicitud real");
            verificar(servicio.saldo(estudianteId, 0) == 100, "Reenviar tras rechazo no suma DUNAB sin aprobación");
            panel = pantallas.crear("Registrar participación");
            combo = nombrado(panel, JComboBox.class, "actividadParticipacion");
            seleccionar(combo, actividades.get("rechazada"));
            verificar(!nombrado(panel, JButton.class, "enviarParticipacion").isEnabled()
                    && nombrado(panel, JTextArea.class, "estadoParticipacion").getText().toLowerCase().contains("pendiente"),
                    "Tras reenviar se muestra el estado pendiente y se impide otro envío");
            denegar(() -> servicio.solicitar(actividades.get("rechazada"), LocalDate.now(), 100, "TERCERA-REFERENCIA", 6),
                    "La nueva solicitud pendiente tampoco se duplica");

            seleccionar(combo, actividades.get("hoy"));
            verificar(nombrado(panel, JButton.class, "enviarParticipacion").isEnabled(), "La actividad de hoy sin solicitud permite enviar");
            servicio.solicitar(actividades.get("hoy"), LocalDate.now(), 100, "ASISTENCIA-HOY", 6);
            verificar(servicio.misSolicitudes().size() == solicitudesAntes + 2 && servicio.saldo(estudianteId, 0) == 100,
                    "La solicitud de hoy se guarda pendiente y conserva el saldo");
            panel = pantallas.crear("Registrar participación");
            combo = nombrado(panel, JComboBox.class, "actividadParticipacion");
            verificar(combo.getItemCount() == 5 && combo.getSelectedItem() != null,
                    "Si todas están bloqueadas las inscripciones siguen visibles y seleccionadas");
            verificar(!nombrado(panel, JButton.class, "enviarParticipacion").isEnabled(),
                    "Sin actividades elegibles el envío permanece deshabilitado");
        });

        servicio.logout();
        servicio.login("futura@prueba.local", clave());
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new PantallasEstudiante(servicio, () -> 6, () -> { }).crear("Registrar participación");
            verificar(nombrado(panel, JComboBox.class, "actividadParticipacion").getItemCount() == 0,
                    "Un usuario sin inscripciones tiene el selector vacío");
            verificar(!nombrado(panel, JButton.class, "enviarParticipacion").isEnabled(),
                    "Sin inscripción el envío está deshabilitado");
        });
        servicio.inscribir(actividades.get("futura"));
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new PantallasEstudiante(servicio, () -> 6, () -> { }).crear("Registrar participación");
            JComboBox<?> combo = nombrado(panel, JComboBox.class, "actividadParticipacion");
            verificar(combo.getItemCount() == 1 && ((Actividad) combo.getSelectedItem()).getId().equals(actividades.get("futura")),
                    "Con una sola inscripción futura la actividad aparece en el selector");
            verificar(!nombrado(panel, JButton.class, "enviarParticipacion").isEnabled()
                    && nombrado(panel, JTextArea.class, "estadoParticipacion").getText().contains(LocalDate.now().plusDays(5).toString()),
                    "El caso de una sola actividad futura explica la fecha sin aparentar falta de inscripción");
        });
        ServicioDunab reabierto = new ServicioDunab(carpeta);
        reabierto.login("estados@prueba.local", clave());
        verificar(reabierto.misSolicitudes().size() == 5 && reabierto.saldo(estudianteId, 0) == 100,
                "Los envíos pendientes y el saldo aprobado persisten al reabrir");
        System.out.println("Pruebas de participación: " + comprobaciones + " comprobaciones correctas.");
        System.out.println("Datos de prueba independientes: " + carpeta.toAbsolutePath());
    }

    private static String solicitudDe(ServicioDunab servicio, String actividadId) {
        for (SolicitudParticipacion solicitud : servicio.misSolicitudes())
            if (solicitud.getActividadId().equals(actividadId)) return solicitud.getId();
        throw new AssertionError("No se encontró la solicitud de prueba.");
    }
    private static void seleccionar(JComboBox<?> combo, String actividadId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (((Actividad) combo.getItemAt(i)).getId().equals(actividadId)) { combo.setSelectedIndex(i); return; }
        }
        throw new AssertionError("La inscripción no aparece en el selector: " + actividadId);
    }
    private static boolean contiene(JComboBox<?> combo, String actividadId) {
        for (int i = 0; i < combo.getItemCount(); i++)
            if (((Actividad) combo.getItemAt(i)).getId().equals(actividadId)) return true;
        return false;
    }
    private static <T extends Component> T nombrado(Container padre, Class<T> tipo, String nombre) {
        for (Component componente : padre.getComponents()) {
            if (tipo.isInstance(componente) && nombre.equals(componente.getName())) return tipo.cast(componente);
            if (componente instanceof Container) {
                T encontrado = buscar((Container) componente, tipo, nombre);
                if (encontrado != null) return encontrado;
            }
        }
        throw new AssertionError("No existe el control " + nombre);
    }
    private static <T extends Component> T buscar(Container padre, Class<T> tipo, String nombre) {
        for (Component componente : padre.getComponents()) {
            if (tipo.isInstance(componente) && nombre.equals(componente.getName())) return tipo.cast(componente);
            if (componente instanceof Container) {
                T encontrado = buscar((Container) componente, tipo, nombre);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }
    private static char[] clave() { return "EstadosParticipacion123!".toCharArray(); }
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }
    private static void denegar(Runnable accion, String mensaje) {
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException ex) { comprobaciones++; return; }
        throw new AssertionError(mensaje);
    }
}
