package javaapplication1;

import java.io.Serializable;

/** El avance se calcula a partir de movimientos; no se almacena un saldo duplicado. */
public final class Meta implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id, estudianteId, nombre, categoria;
    private final int objetivo, semestre;
    public Meta(String id, String estudianteId, String nombre, int objetivo, String categoria, int semestre) {
        this.id = id; this.estudianteId = estudianteId; this.nombre = nombre;
        this.objetivo = objetivo; this.categoria = categoria; this.semestre = semestre;
    }
    public String getId() { return id; }
    public String getEstudianteId() { return estudianteId; }
    public String getNombre() { return nombre; }
    public int getObjetivo() { return objetivo; }
    public String getCategoria() { return categoria; }
    public int getSemestre() { return semestre; }
}
