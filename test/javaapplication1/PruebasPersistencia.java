package javaapplication1;

import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Verificación de primera ejecución, corrupción y recuperación de un fallo de escritura. */
public final class PruebasPersistencia {
    private static int comprobaciones;
    private PruebasPersistencia() { }

    public static void main(String[] args) throws Exception {
        Path raiz = Files.createTempDirectory("dunab-pruebas-persistencia-");
        Path carpeta = raiz.resolve("data");
        ServicioDunab servicio = new ServicioDunab(carpeta);
        verificar(Files.isRegularFile(carpeta.resolve("dunab.dat")), "Primera ejecución crea carpeta y archivo");
        Usuario estudiante = servicio.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        verificar(servicio.saldo(estudiante.getId(), 0) == 1400, "Datos iniciales contienen participaciones aprobadas");
        verificar(servicio.misSolicitudes().size() == 3, "Demostración contiene solicitudes aprobadas y pendiente");
        String archivoComoTexto = new String(Files.readAllBytes(carpeta.resolve("dunab.dat")), StandardCharsets.ISO_8859_1);
        verificar(!archivoComoTexto.contains("Estudiante123!") && !archivoComoTexto.contains("Admin123!"),
                "El archivo no contiene las contraseñas de demostración en texto plano");
        servicio.logout();
        ServicioDunab reabierto = new ServicioDunab(carpeta);
        reabierto.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        verificar(reabierto.misSolicitudes().size() == 3 && reabierto.misMovimientos().size() == 2,
                "Abrir nuevamente no duplica la información de demostración");
        reabierto.logout();
        reabierto.login("admin@demo.local", "Admin123!".toCharArray());
        verificar(reabierto.estudiantes().size() == 1, "Abrir nuevamente no duplica usuarios");
        int saldoInicial = reabierto.saldo(estudiante.getId(), 0);
        int registrosIniciales = reabierto.auditoria().size();

        // Se obstruye temporalmente la ruta con un archivo para provocar un error real de E/S.
        Path respaldo = raiz.resolve("data-respaldo-prueba");
        Files.move(carpeta, respaldo);
        Files.write(carpeta, new byte[]{1});
        denegar(() -> reabierto.ajustar(estudiante.getId(), 50, 1, "Prueba de escritura fallida"),
                "Un fallo real de escritura se comunica al llamador");
        verificar(reabierto.saldo(estudiante.getId(), 0) == saldoInicial,
                "Una escritura fallida revierte el saldo en memoria");
        verificar(reabierto.auditoria().size() == registrosIniciales,
                "Una escritura fallida revierte la auditoría en memoria");
        Files.delete(carpeta); // Archivo de prueba conocido, sin eliminación recursiva.
        Files.move(respaldo, carpeta);
        reabierto.ajustar(estudiante.getId(), 50, 1, "Prueba de escritura recuperada");
        verificar(reabierto.saldo(estudiante.getId(), 0) == saldoInicial + 50,
                "Tras restaurar la carpeta una operación nueva puede guardarse");
        ServicioDunab comprobacion = new ServicioDunab(carpeta);
        comprobacion.login("admin@demo.local", "Admin123!".toCharArray());
        verificar(comprobacion.saldo(estudiante.getId(), 0) == saldoInicial + 50,
                "La recuperación persiste exactamente un ajuste");
        verificar(comprobacion.auditoria().size() == registrosIniciales + 1,
                "La recuperación persiste exactamente un registro de auditoría");

        Path corruptos = raiz.resolve("data-corruptos");
        Files.createDirectories(corruptos);
        byte[] contenidoCorrupto = new byte[]{1, 2, 3, 4, 5};
        Files.write(corruptos.resolve("dunab.dat"), contenidoCorrupto);
        denegar(() -> new ServicioDunab(corruptos), "Un archivo corrupto no inicia una demostración nueva");
        verificar(Arrays.equals(contenidoCorrupto, Files.readAllBytes(corruptos.resolve("dunab.dat"))),
                "El archivo corrupto se conserva para recuperación manual");

        Path ajenos = raiz.resolve("data-formato-ajeno");
        Files.createDirectories(ajenos);
        try (ObjectOutputStream salida = new ObjectOutputStream(Files.newOutputStream(ajenos.resolve("dunab.dat")))) {
            salida.writeObject("Este objeto no corresponde a DatosSistema");
        }
        byte[] contenidoAjeno = Files.readAllBytes(ajenos.resolve("dunab.dat"));
        denegar(() -> new ServicioDunab(ajenos), "Un objeto serializado de otro tipo es rechazado");
        verificar(Arrays.equals(contenidoAjeno, Files.readAllBytes(ajenos.resolve("dunab.dat"))),
                "El archivo con formato ajeno también se conserva");
        System.out.println("Pruebas de persistencia: " + comprobaciones + " comprobaciones correctas.");
        System.out.println("Datos de prueba independientes: " + raiz.toAbsolutePath());
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }
    private static void denegar(Runnable accion, String mensaje) {
        try { accion.run(); }
        catch (IllegalStateException ex) {
            if (ex.getMessage() == null || ex.getMessage().trim().isEmpty()) throw new AssertionError("Error sin explicación: " + mensaje);
            comprobaciones++;
            return;
        }
        throw new AssertionError(mensaje);
    }
}
