package javaapplication1;

import java.io.Serializable;
import java.util.Arrays;

/** Cuenta local. La clave se conserva únicamente como sal y derivación PBKDF2. */
public final class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id, nombre, correo, programa, rol;
    private final int semestre, metaDunab;
    private final byte[] sal, claveDerivada;

    Usuario(String id, String nombre, String correo, String programa, String rol,
            int semestre, int metaDunab, byte[] sal, byte[] claveDerivada) {
        this.id = id; this.nombre = nombre; this.correo = correo;
        this.programa = programa; this.rol = rol; this.semestre = semestre;
        this.metaDunab = metaDunab;
        this.sal = Arrays.copyOf(sal, sal.length);
        this.claveDerivada = Arrays.copyOf(claveDerivada, claveDerivada.length);
    }
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPrograma() { return programa; }
    public String getRol() { return rol; }
    public int getSemestre() { return semestre; }
    public int getMetaDunab() { return metaDunab; }
    boolean comprobarClave(char[] clave) { return SeguridadClave.comprobar(clave, sal, claveDerivada); }
    Usuario conPerfil(String nombre, String correo, String programa, int semestre, int meta) {
        return new Usuario(id, nombre, correo, programa, rol, semestre, meta, sal, claveDerivada);
    }
    Usuario paraRanking() {
        return new Usuario(id, nombre, "", "", "ESTUDIANTE", 1, 0, new byte[0], new byte[0]);
    }
    @Override public String toString() { return nombre; }
}
