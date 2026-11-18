package javaapplication1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/** Comprueba períodos avanzados y la conservación de los datos de los semestres iniciales. */
public final class PruebasSemestres {
    private static int comprobaciones;
    private PruebasSemestres() { }

    public static void main(String[] args) throws Exception {
        verificar(Semestres.MAXIMO == 12, "El rango del prototipo incluye doce semestres");
        String[] opciones = Semestres.opciones(false);
        verificar(opciones.length == 12 && "Semestre 1".equals(opciones[0])
                && "Semestre 12".equals(opciones[11]), "Opciones completas de semestre");
        verificar(Semestres.opciones(true).length == 13
                && "Semestre 12".equals(Semestres.opciones(true)[12]), "Opciones con histórico y doce períodos");
        Semestres.validar(0, true);
        denegar(() -> Semestres.validar(0, false), "Cero no es un semestre académico");
        denegar(() -> Semestres.validar(13, false), "Se rechaza semestre fuera del rango");
        denegar(() -> Semestres.validar(13, true), "Histórico tampoco permite semestre trece");

        Path carpeta = Files.createTempDirectory("dunab-pruebas-semestres-");
        ServicioDunab servicio = new ServicioDunab(carpeta);
        servicio.registrar("Estudiante períodos avanzados", "avanzados@prueba.local", clave(), "Sistemas", 3);
        Usuario estudiante = servicio.login("avanzados@prueba.local", clave());
        String estudianteId = estudiante.getId();
        verificar(estudiante.getSemestre() == 3, "Registro acepta semestre tres");
        servicio.editarPerfil(estudiante.getNombre(), estudiante.getCorreo(), estudiante.getPrograma(), 10, 12000);
        verificar(servicio.getActual().getSemestre() == 10, "Perfil acepta semestre diez");
        servicio.editarPerfil(estudiante.getNombre(), estudiante.getCorreo(), estudiante.getPrograma(), 12, 12000);
        verificar(servicio.getActual().getSemestre() == 12, "Perfil acepta semestre doce");
        denegar(() -> servicio.editarPerfil("Prueba", "avanzados@prueba.local", "Sistemas", 0, 10000), "Perfil rechaza cero");
        denegar(() -> servicio.editarPerfil("Prueba", "avanzados@prueba.local", "Sistemas", 13, 10000), "Perfil rechaza trece");
        denegar(() -> servicio.registrar("Prueba", "invalido0@prueba.local", clave(), "Sistemas", 0), "Registro rechaza cero");
        denegar(() -> servicio.registrar("Prueba", "invalido13@prueba.local", clave(), "Sistemas", 13), "Registro rechaza trece");

        for (int periodo : new int[]{3, 10, 12}) {
            servicio.logout();
            servicio.login("admin@demo.local", "Admin123!".toCharArray());
            String nombreActividad = "Actividad semestre " + periodo;
            servicio.guardarActividad(null, nombreActividad, "Cultura", "Actividad aislada de prueba.",
                    LocalDate.now(), "Sala de prueba", 300, 5, true);
            Actividad actividad = servicio.actividades(nombreActividad, "Todas").get(0);
            servicio.logout();
            servicio.login("avanzados@prueba.local", clave());
            servicio.inscribir(actividad.getId());
            denegar(() -> servicio.solicitar(actividad.getId(), LocalDate.now(), 100, "EVIDENCIA", 0), "Solicitud rechaza cero");
            denegar(() -> servicio.solicitar(actividad.getId(), LocalDate.now(), 100, "EVIDENCIA", 13), "Solicitud rechaza trece");
            servicio.solicitar(actividad.getId(), LocalDate.now(), 100, "EVIDENCIA-SEMESTRE-" + periodo, periodo);
            SolicitudParticipacion solicitud = servicio.misSolicitudes().get(servicio.misSolicitudes().size() - 1);
            verificar(solicitud.getSemestre() == periodo, "Solicitud conserva semestre " + periodo);
            verificar(servicio.saldo(estudianteId, periodo) == 0, "Pendiente no suma en semestre " + periodo);
            servicio.guardarMeta(null, "Meta semestre " + periodo, 1000, "Todas", periodo);
            Meta meta = servicio.misMetas().get(servicio.misMetas().size() - 1);
            verificar(meta.getSemestre() == periodo, "Meta acepta semestre " + periodo);
            servicio.logout();
            servicio.login("admin@demo.local", "Admin123!".toCharArray());
            servicio.resolverSolicitud(solicitud.getId(), true, "Evidencia de prueba revisada.");
            verificar(servicio.saldo(estudianteId, periodo) == 100, "Aprobación suma en semestre " + periodo);
            servicio.ajustar(estudianteId, 20, periodo, "Ajuste de prueba del período.");
            servicio.ajustar(estudianteId, -5, periodo, "Corrección de prueba del período.");
            verificar(servicio.saldo(estudianteId, periodo) == 115, "Ajustes conservan semestre " + periodo);
            verificar(servicio.ranking(periodo).get(0).getId().equals(estudianteId)
                    && servicio.saldoRanking(estudianteId, periodo) == 115, "Ranking calcula semestre " + periodo);
            servicio.logout();
            servicio.login("avanzados@prueba.local", clave());
            verificar(servicio.progresoMeta(meta) == 115, "Meta calcula avance en semestre " + periodo);
        }

        denegar(() -> servicio.ranking(0), "Ranking exige un semestre concreto");
        denegar(() -> servicio.ranking(13), "Ranking rechaza trece");
        denegar(() -> servicio.saldo(estudianteId, 13), "Saldo rechaza trece");
        denegar(() -> servicio.guardarMeta(null, "Meta inválida", 1000, "Todas", 13), "Meta rechaza trece");
        servicio.logout();
        servicio.login("admin@demo.local", "Admin123!".toCharArray());
        denegar(() -> servicio.ajustar(estudianteId, 10, 0, "Inválido"), "Ajuste rechaza cero");
        denegar(() -> servicio.ajustar(estudianteId, 10, 13, "Inválido"), "Ajuste rechaza trece");

        ServicioDunab reabierto = new ServicioDunab(carpeta);
        reabierto.login("avanzados@prueba.local", clave());
        verificar(reabierto.getActual().getSemestre() == 12, "Perfil avanzado persiste al reabrir");
        verificar(reabierto.misSolicitudes().size() == 3 && reabierto.misMetas().size() == 3,
                "Solicitudes y metas avanzadas persisten");
        for (int periodo : new int[]{3, 10, 12}) {
            verificar(reabierto.saldo(estudianteId, periodo) == 115, "Saldo persistente del semestre " + periodo);
            verificar(reabierto.ranking(periodo).get(0).getId().equals(estudianteId), "Ranking persistente del semestre " + periodo);
        }
        verificar(reabierto.saldo(estudianteId, 0) == 345, "Histórico reúne los períodos avanzados sin mezclarlos");
        verificar(reabierto.saldo(estudianteId, 1) == 0 && reabierto.saldo(estudianteId, 2) == 0,
                "Nuevos períodos no modifican semestres uno y dos");
        reabierto.logout();
        Usuario demo = reabierto.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        verificar(reabierto.saldo(demo.getId(), 1) == 800 && reabierto.saldo(demo.getId(), 2) == 600,
                "Datos serializados de demostración de semestres uno y dos conservan compatibilidad");
        verificar(reabierto.getActual().getSemestre() == 2 && reabierto.misSolicitudes().size() == 3,
                "Perfil y solicitudes originales siguen disponibles");
        System.out.println("Pruebas de semestres: " + comprobaciones + " comprobaciones correctas.");
        System.out.println("Datos de prueba independientes: " + carpeta.toAbsolutePath());
    }

    private static char[] clave() { return "PruebaAvanzada123!".toCharArray(); }
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
