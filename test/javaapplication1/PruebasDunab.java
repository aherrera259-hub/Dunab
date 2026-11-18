package javaapplication1;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

/** Pruebas de integración ejecutables sin JUnit ni datos personales. */
public final class PruebasDunab {
    private static int comprobaciones;

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("dunab-flujo-");
        ServicioDunab s = new ServicioDunab(carpeta);
        comprobar(Files.isRegularFile(carpeta.resolve("dunab.dat")), "Primer inicio crea el archivo");
        denegar(() -> s.misMovimientos(), "Sin sesión no se leen movimientos");
        s.registrar("Ana Prueba", "ANA@prueba.local", "ClaveSegura123!".toCharArray(), "Sistemas", 1);
        denegar(() -> s.registrar("Ana", "ana@prueba.local", "ClaveSegura123!".toCharArray(), "Sistemas", 1), "Correo duplicado");
        denegar(() -> s.registrar("", "otra@prueba.local", "ClaveSegura123!".toCharArray(), "Sistemas", 1), "Nombre vacío");
        denegar(() -> s.registrar("Otra", "invalido", "ClaveSegura123!".toCharArray(), "Sistemas", 1), "Correo inválido");
        denegar(() -> s.registrar("Otra", "otra@prueba.local", "corta".toCharArray(), "Sistemas", 1), "Clave corta");
        ServicioDunab recargado = new ServicioDunab(carpeta);
        Usuario ana = recargado.login("ana@prueba.local", "ClaveSegura123!".toCharArray());
        comprobar(ana.getNombre().equals("Ana Prueba"), "Registro persiste después de reiniciar");
        comprobar(recargado.saldo(ana.getId(), 0) == 0, "Cuenta nueva sin DUNAB");
        denegar(() -> recargado.login("ana@prueba.local", "incorrecta".toCharArray()), "Credenciales erróneas rechazadas");
        comprobar(recargado.getActual() == null, "Fallo de acceso limpia sesión");
        recargado.login("ana@prueba.local", "ClaveSegura123!".toCharArray());
        recargado.logout();
        comprobar(recargado.getActual() == null, "Cerrar sesión");
        recargado.login("ana@prueba.local", "ClaveSegura123!".toCharArray());
        recargado.editarPerfil("Ana Actualizada", "ana@prueba.local", "Ingeniería de Sistemas", 2, 12000);
        comprobar(recargado.getActual().getMetaDunab() == 12000, "Meta global configurable");
        comprobar(recargado.getActual().getSemestre() == 2, "Semestre del perfil editable");
        denegar(() -> recargado.estudiantes(), "Estudiante no consulta administración");
        denegar(() -> recargado.ajustar(ana.getId(), 100, 1, "Sin permiso"), "Estudiante no puede otorgar DUNAB");
        recargado.login("admin@demo.local", "Admin123!".toCharArray());
        recargado.guardarActividad(null, "Prueba completa", "Cultura", "Descripción ficticia", LocalDate.now(), "Aula demo", 400, 2, true);
        String actividadId = recargado.actividades("Prueba completa", "Cultura").get(0).getId();
        comprobar(recargado.actividades("No existe", "Todas").isEmpty(), "Buscar actividad sin coincidencia");
        recargado.login("ana@prueba.local", "ClaveSegura123!".toCharArray());
        recargado.inscribir(actividadId);
        comprobar(recargado.cuposDisponibles(actividadId) == 1, "Inscribir consume cupo");
        comprobar(recargado.misInscripciones().size() == 1, "Consultar inscripción propia");
        denegar(() -> recargado.inscribir(actividadId), "Inscripción duplicada");
        comprobar(recargado.saldo(ana.getId(), 0) == 0, "Inscribirse no suma DUNAB");
        denegar(() -> recargado.solicitar(actividadId, LocalDate.now(), 401, "EVIDENCIA", 1), "Solicitud excede actividad");
        denegar(() -> recargado.solicitar(actividadId, LocalDate.now().plusDays(1), 350, "EVIDENCIA", 1), "Fecha futura");
        denegar(() -> recargado.solicitar(actividadId, LocalDate.now(), -5, "EVIDENCIA", 1), "Cantidad negativa");
        denegar(() -> recargado.solicitar(actividadId, LocalDate.now(), 350, "", 1), "Evidencia vacía");
        recargado.solicitar(actividadId, LocalDate.now(), 350, "PRUEBA-350", 1);
        comprobar(recargado.misSolicitudes().get(0).getEstado().equals("PENDIENTE"), "Estado pendiente");
        comprobar(recargado.saldo(ana.getId(), 0) == 0, "Solicitud pendiente no suma DUNAB");
        String solicitudId = recargado.misSolicitudes().get(0).getId();
        denegar(() -> recargado.resolverSolicitud(solicitudId, true, "No autorizado"), "Estudiante no aprueba");
        denegar(() -> recargado.solicitar(actividadId, LocalDate.now(), 350, "OTRA", 2), "Solicitud duplicada aun en otro semestre");
        recargado.login("admin@demo.local", "Admin123!".toCharArray());
        int auditoriaAntes = recargado.auditoria().size();
        recargado.resolverSolicitud(solicitudId, true, "Evidencia verificada de prueba");
        comprobar(recargado.auditoria().size() == auditoriaAntes + 1, "Aprobación auditada");
        comprobar(recargado.saldo(ana.getId(), 1) == 350, "Aprobación suma cantidad solicitada");
        denegar(() -> recargado.resolverSolicitud(solicitudId, true, "Otra vez"), "Aprobación duplicada rechazada");
        comprobar(recargado.saldo(ana.getId(), 1) == 350, "Doble aprobación no duplica saldo");
        recargado.ajustar(ana.getId(), 100, 2, "Prueba de segundo semestre");
        comprobar(recargado.saldo(ana.getId(), 0) == 450, "Saldo histórico suma semestres");
        comprobar(recargado.saldo(ana.getId(), 1) == 350, "Semestre 1 conserva sus registros");
        comprobar(recargado.saldo(ana.getId(), 2) == 100, "Semestre 2 separado");
        ServicioDunab segundaCarga = new ServicioDunab(carpeta);
        segundaCarga.login("ana@prueba.local", "ClaveSegura123!".toCharArray());
        comprobar(segundaCarga.saldo(ana.getId(), 0) == 450, "Saldo y aprobaciones persisten tras reinicio");
        comprobar(segundaCarga.misSolicitudes().get(0).getEstado().equals("APROBADA"), "Decisión persiste");
        comprobar(segundaCarga.misMovimientos().size() == 2, "Historial real: aprobación y ajuste");
        comprobar(segundaCarga.getActual().getNombre().equals("Ana Actualizada"), "Perfil persiste");
        segundaCarga.guardarMeta(null, "Meta cultural", 1000, "Cultura", 1);
        Meta meta = segundaCarga.misMetas().get(0);
        comprobar(segundaCarga.progresoMeta(meta) == 350, "Meta calcula progreso por categoría y semestre");
        segundaCarga.guardarMeta(meta.getId(), "Meta cultural actualizada", 500, "Cultura", 1);
        comprobar(segundaCarga.misMetas().get(0).getObjetivo() == 500, "Meta editable");
        segundaCarga.eliminarMeta(meta.getId());
        comprobar(segundaCarga.misMetas().isEmpty(), "Meta eliminada");
        List<Usuario> ranking = segundaCarga.ranking(1);
        comprobar(ranking.size() == 2, "Top muestra solamente estudiantes registrados");
        comprobar(segundaCarga.saldoRanking(ranking.get(0).getId(), 1) >= segundaCarga.saldoRanking(ranking.get(1).getId(), 1), "Ranking orden descendente");
        segundaCarga.login("admin@demo.local", "Admin123!".toCharArray());
        segundaCarga.ajustar(ana.getId(), 700, 1, "Actualizar ranking de prueba");
        comprobar(segundaCarga.ranking(1).get(0).getId().equals(ana.getId()), "Top9 se actualiza con datos nuevos");
        for (int i = 0; i < 9; i++) segundaCarga.registrar("Demo ranking " + i, "ranking" + i + "@prueba.local", "ClaveRanking123!".toCharArray(), "Sistemas", 1);
        comprobar(segundaCarga.ranking(1).size() == 9, "Ranking limitado a nueve");
        ServicioDunab terceraCarga = new ServicioDunab(carpeta);
        terceraCarga.login("admin@demo.local", "Admin123!".toCharArray());
        comprobar(terceraCarga.estudiantes().size() == 11, "No repite usuarios demo al abrir");
        comprobar(terceraCarga.actividades("", "Todas").size() == 8, "No repite actividades demo al abrir");
        comprobar(terceraCarga.saldo(ana.getId(), 0) == 1150, "Persistencia después de actualizar ranking");
        String serializado = new String(Files.readAllBytes(carpeta.resolve("dunab.dat")), StandardCharsets.ISO_8859_1);
        comprobar(!serializado.contains("ClaveSegura123!") && !serializado.contains("Estudiante123!") && !serializado.contains("Admin123!"), "Archivo no contiene claves en texto plano");
        comprobar(Math.abs(CalculadoraNotas.finalEstimada(3.5, 3.0, 3.2) - 3.23) < 0.000001, "Fórmula ponderada");
        comprobar(Math.abs(CalculadoraNotas.necesaria(3.5, 3, 3) - 2.625) < 0.000001, "Tercer corte necesario");
        comprobar(CalculadoraNotas.necesaria(0, 0, 3) > 5, "Detecta imposibilidad matemática");
        comprobar(CalculadoraNotas.necesaria(5, 5, 3) <= 0, "Detecta mínima alcanzada");
        denegar(() -> CalculadoraNotas.finalEstimada(-1, 3, 4), "Nota negativa");
        denegar(() -> CalculadoraNotas.finalEstimada(3, 6, 4), "Nota mayor que cinco");
        denegar(() -> CalculadoraNotas.necesaria(Double.NaN, 3, 3), "Nota no finita");
        System.out.println("PruebasDunab: " + comprobaciones + " comprobaciones correctas.");
        System.out.println("Almacén de prueba aislado: " + carpeta);
    }

    private static void comprobar(boolean resultado, String mensaje) {
        if (!resultado) throw new AssertionError(mensaje);
        comprobaciones++;
    }

    private static void denegar(Runnable operacion, String mensaje) {
        try { operacion.run(); }
        catch (IllegalArgumentException | IllegalStateException esperado) { comprobaciones++; return; }
        throw new AssertionError("Debió rechazarse: " + mensaje);
    }
}
