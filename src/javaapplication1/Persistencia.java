package javaapplication1;

import java.io.*;
import java.nio.file.*;

/** Escritura temporal y reemplazo: un cierre normal nunca deja medio archivo serializado. */
final class Persistencia {
    private final Path carpeta, archivo;
    Persistencia(Path carpeta) {
        if (carpeta == null) throw new IllegalArgumentException("Indica la carpeta de datos.");
        this.carpeta = carpeta.toAbsolutePath().normalize();
        this.archivo = this.carpeta.resolve("dunab.dat");
        try { Files.createDirectories(this.carpeta); }
        catch (IOException ex) { throw error("No se pudo crear la carpeta data.", ex); }
    }
    boolean existe() { return Files.exists(archivo); }
    DatosSistema cargar() {
        try (ObjectInputStream entrada = new ObjectInputStream(Files.newInputStream(archivo))) {
            Object objeto = entrada.readObject();
            if (!(objeto instanceof DatosSistema)) throw new IOException("Formato desconocido.");
            return (DatosSistema) objeto;
        } catch (IOException | ClassNotFoundException ex) {
            throw error("No se pudieron leer los datos. El archivo se ha conservado; revisa " + archivo + ".", ex);
        }
    }
    /** Conserva el archivo exacto anterior a la actualización de nombres, sin sobrescribir respaldos. */
    Path respaldarAntesDeNombres() {
        Path respaldo = null;
        try {
            respaldo = Files.createTempFile(carpeta, "dunab-antes-nombres-", ".bak");
            Files.copy(archivo, respaldo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return respaldo;
        } catch (IOException ex) {
            if (respaldo != null) {
                try { Files.deleteIfExists(respaldo); }
                catch (IOException limpieza) { ex.addSuppressed(limpieza); }
            }
            throw error("No se pudieron respaldar los datos antes de actualizar sus nombres. El archivo original se ha conservado.", ex);
        }
    }
    void guardar(DatosSistema datos) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile(carpeta, "dunab-", ".tmp");
            try (FileOutputStream archivoSalida = new FileOutputStream(temporal.toFile());
                    ObjectOutputStream salida = new ObjectOutputStream(archivoSalida)) {
                salida.writeObject(datos); salida.flush(); archivoSalida.getFD().sync();
            }
            try { Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw error("No se pudo guardar la operación. Comprueba permisos y espacio en " + carpeta + ".", ex);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException ex) { /* El archivo original no se modifica por este fallo de limpieza. */ }
            }
        }
    }
    private IllegalStateException error(String mensaje, Exception causa) {
        return new IllegalStateException(mensaje, causa);
    }
}
