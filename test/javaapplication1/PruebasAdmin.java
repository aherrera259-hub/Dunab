package javaapplication1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Pruebas de permisos y administración con almacenamiento temporal independiente. */
public final class PruebasAdmin {
    private static int comprobaciones;

    private PruebasAdmin() { }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("dunab-pruebas-admin-");
        ServicioDunab s = new ServicioDunab(carpeta);
        s.registrar("Ana de prueba", "ana@prueba.local", clave("Prueba123!"), "Sistemas", 1);
        s.registrar("Luis de prueba", "luis@prueba.local", clave("Prueba123!"), "Sistemas", 2);
        Usuario ana = s.login("ana@prueba.local", clave("Prueba123!"));
        s.logout();
        Usuario luis = s.login("luis@prueba.local", clave("Prueba123!"));
        s.logout();
        Usuario admin = s.login("admin@demo.local", clave("Admin123!"));

        s.guardarActividad(null, "Prueba de cupos", "Cultura", "Actividad aislada para verificar cupos.",
                LocalDate.now(), "Sala de prueba", 100, 1, true);
        s.guardarActividad(null, "Prueba de rechazo", "Bienestar", "Actividad aislada para verificar solicitudes.",
                LocalDate.now(), "Campus de prueba", 100, 2, true);
        Actividad cupos = buscar(s.actividades("Prueba de cupos", "Todas"), "Prueba de cupos");
        Actividad rechazo = buscar(s.actividades("Prueba de rechazo", "Todas"), "Prueba de rechazo");
        verificar(s.cuposDisponibles(cupos.getId()) == 1, "Cupo inicial disponible");
        int auditoriaInicial = s.auditoria().size();

        s.logout();
        s.login("ana@prueba.local", clave("Prueba123!"));
        denegar(() -> s.estudiantes(), "Estudiante no consulta lista privada de estudiantes");
        denegar(() -> s.auditoria(), "Estudiante no consulta auditoría");
        denegar(() -> s.solicitudesPendientes(), "Estudiante no revisa solicitudes ajenas");
        denegar(() -> s.usuario(luis.getId()), "Estudiante no consulta perfil ajeno");
        denegar(() -> s.saldo(luis.getId(), 1), "Estudiante no consulta saldo privado ajeno");
        denegar(() -> s.ajustar(ana.getId(), 100, 1, "Intento sin permisos"), "Estudiante no realiza ajustes");
        denegar(() -> s.guardarActividad(null, "Actividad no autorizada", "Cultura", "Sin permisos",
                LocalDate.now(), "Campus", 100, 10, true), "Estudiante no crea actividades");
        verificar(s.saldo(ana.getId(), 0) == 0, "Intentos no autorizados no cambian el saldo");

        s.inscribir(cupos.getId());
        verificar(s.cuposDisponibles(cupos.getId()) == 0, "Inscripción consume el único cupo");
        denegar(() -> s.inscribir(cupos.getId()), "No se duplica la inscripción");
        s.logout();
        s.login("luis@prueba.local", clave("Prueba123!"));
        denegar(() -> s.inscribir(cupos.getId()), "Otro estudiante no supera el cupo máximo");
        s.logout();
        s.login("ana@prueba.local", clave("Prueba123!"));
        String inscripcion = s.misInscripciones().get(0).getId();
        s.cancelarInscripcion(inscripcion);
        verificar(s.cuposDisponibles(cupos.getId()) == 1, "Cancelar libera el cupo");
        s.logout();
        s.login("luis@prueba.local", clave("Prueba123!"));
        s.inscribir(cupos.getId());
        verificar(s.misInscripciones().size() == 1, "Cupo liberado permite nueva inscripción");

        s.logout();
        s.login("ana@prueba.local", clave("Prueba123!"));
        s.inscribir(rechazo.getId());
        s.solicitar(rechazo.getId(), LocalDate.now(), 100, "CODIGO-PRUEBA-RECHAZO", 1);
        String solicitud = s.misSolicitudes().get(0).getId();
        verificar(s.saldo(ana.getId(), 0) == 0, "Solicitud pendiente no suma DUNAB");
        denegar(() -> s.resolverSolicitud(solicitud, true, "Intento sin permisos"), "Estudiante no aprueba solicitudes");

        s.logout();
        s.login("admin@demo.local", clave("Admin123!"));
        s.resolverSolicitud(solicitud, false, "La evidencia de prueba no acredita participación.");
        verificar(s.saldo(ana.getId(), 0) == 0, "Rechazar conserva el saldo sin DUNAB nuevos");
        denegar(() -> s.resolverSolicitud(solicitud, true, "Segundo intento"), "Solicitud rechazada no puede aprobarse después");
        s.guardarActividad(cupos.getId(), cupos.getNombre(), cupos.getCategoria(), cupos.getDescripcion(),
                cupos.getFecha(), cupos.getLugar(), cupos.getDunab(), cupos.getCupos(), false);
        verificar(!s.actividad(cupos.getId()).isActiva(), "Administrador desactiva la actividad");
        verificar(s.actividad(cupos.getId()).getNombre().equals("Prueba de cupos"), "Desactivar conserva datos de actividad");
        s.logout();
        s.login("luis@prueba.local", clave("Prueba123!"));
        verificar(s.misInscripciones().get(0).getActividadId().equals(cupos.getId()), "Desactivar conserva inscripciones históricas");
        verificar(s.actividades("Prueba de cupos", "Todas").isEmpty(), "Actividad inactiva no aparece en catálogo estudiantil");
        s.logout();
        s.login("ana@prueba.local", clave("Prueba123!"));
        verificar("RECHAZADA".equals(s.misSolicitudes().get(0).getEstado()), "Estudiante puede consultar el rechazo");
        verificar(s.misSolicitudes().get(0).getObservacion().contains("no acredita"), "Observación administrativa se conserva");

        s.logout();
        s.login("admin@demo.local", clave("Admin123!"));
        int antesAjustes = s.auditoria().size();
        s.ajustar(ana.getId(), 300, 1, "Reconocimiento de prueba autorizado.");
        verificar(s.saldo(ana.getId(), 1) == 300, "Ajuste positivo suma al semestre");
        s.ajustar(ana.getId(), -100, 1, "Corrección justificada de prueba.");
        verificar(s.saldo(ana.getId(), 1) == 200, "Ajuste negativo resta del semestre");
        denegar(() -> s.ajustar(ana.getId(), -201, 1, "Retiro excesivo"), "No se permite saldo histórico negativo");
        denegar(() -> s.ajustar(ana.getId(), -1, 2, "Retiro en semestre sin saldo"), "No se permite saldo semestral negativo");
        denegar(() -> s.ajustar(ana.getId(), 10, 1, " "), "Ajuste exige motivo");
        denegar(() -> s.ajustar(ana.getId(), 0, 1, "Cantidad cero"), "Ajuste exige cantidad distinta de cero");
        verificar(s.saldo(ana.getId(), 0) == 200, "Ajustes rechazados conservan el saldo");
        s.ajustar(ana.getId(), 50, 2, "Reconocimiento de prueba segundo semestre.");
        verificar(s.saldo(ana.getId(), 0) == 250, "Histórico suma ambos semestres");
        verificar(s.saldo(ana.getId(), 1) == 200 && s.saldo(ana.getId(), 2) == 50, "Semestres permanecen separados");
        List<RegistroAuditoria> registros = s.auditoria();
        verificar(registros.size() == antesAjustes + 3, "Cada ajuste exitoso crea un registro de auditoría");
        RegistroAuditoria ultimo = registros.get(registros.size() - 1);
        verificar(ultimo.getAdministradorId().equals(admin.getId()), "Auditoría identifica administrador responsable");
        verificar(ultimo.getEstudianteId().equals(ana.getId()), "Auditoría identifica estudiante afectado");
        verificar(ultimo.getCantidad() == 50 && ultimo.getValorAnterior() == 200 && ultimo.getValorPosterior() == 250,
                "Auditoría conserva cantidad y valores anterior y posterior");
        verificar(ultimo.getFecha() != null && !ultimo.getMotivo().trim().isEmpty(), "Auditoría conserva fecha y motivo");
        verificar(registros.size() > auditoriaInicial, "Auditoría acumula operaciones reales");

        s.logout();
        ServicioDunab reabierto = new ServicioDunab(carpeta);
        reabierto.login("admin@demo.local", clave("Admin123!"));
        verificar(reabierto.saldo(ana.getId(), 0) == 250, "Ajustes persisten al reabrir el almacenamiento");
        verificar(reabierto.auditoria().size() == registros.size(), "Auditoría persiste al reabrir");
        verificar(!reabierto.actividad(cupos.getId()).isActiva(), "Desactivación persiste al reabrir");
        reabierto.logout();
        verificar(reabierto.getActual() == null, "Cerrar sesión elimina el usuario actual");
        denegar(() -> reabierto.estudiantes(), "Sesión cerrada no consulta administración");
        System.out.println("Pruebas administrativas: " + comprobaciones + " comprobaciones correctas.");
        System.out.println("Datos de prueba independientes: " + carpeta.toAbsolutePath());
    }

    private static Actividad buscar(List<Actividad> actividades, String nombre) {
        for (Actividad a : actividades) if (nombre.equals(a.getNombre())) return a;
        throw new AssertionError("No se encontró la actividad creada: " + nombre);
    }

    private static char[] clave(String texto) { return texto.toCharArray(); }

    private static void verificar(boolean condicion, String descripcion) {
        if (!condicion) throw new AssertionError(descripcion);
        comprobaciones++;
    }

    private static void denegar(Runnable operacion, String descripcion) {
        try {
            operacion.run();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            comprobarMensaje(ex, descripcion);
            comprobaciones++;
            return;
        }
        throw new AssertionError("Se permitió una operación inválida: " + descripcion);
    }

    private static void comprobarMensaje(RuntimeException ex, String descripcion) {
        if (ex.getMessage() == null || ex.getMessage().trim().isEmpty()) {
            throw new AssertionError("Falta mensaje de validación: " + descripcion);
        }
    }
}
