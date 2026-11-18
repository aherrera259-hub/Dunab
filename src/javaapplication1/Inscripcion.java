package javaapplication1;

import java.io.Serializable;
import java.time.LocalDate;

public final class Inscripcion implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id, estudianteId, actividadId, estado;
    private final LocalDate fecha;
    public Inscripcion(String id, String estudianteId, String actividadId, LocalDate fecha, String estado) {
        this.id = id; this.estudianteId = estudianteId; this.actividadId = actividadId;
        this.fecha = fecha; this.estado = estado;
    }
    public String getId() { return id; }
    public String getEstudianteId() { return estudianteId; }
    public String getActividadId() { return actividadId; }
    public LocalDate getFecha() { return fecha; }
    public String getEstado() { return estado; }
    Inscripcion cancelada() { return new Inscripcion(id, estudianteId, actividadId, fecha, "CANCELADA"); }
}
