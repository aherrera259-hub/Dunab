package javaapplication1;

import java.io.Serializable;
import java.time.LocalDate;

public final class SolicitudParticipacion implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id, estudianteId, actividadId, evidencia, estado, observacion;
    private final LocalDate fecha;
    private final int cantidad, semestre;
    public SolicitudParticipacion(String id, String estudianteId, String actividadId,
            LocalDate fecha, int cantidad, String evidencia, String estado, String observacion, int semestre) {
        this.id = id; this.estudianteId = estudianteId; this.actividadId = actividadId;
        this.fecha = fecha; this.cantidad = cantidad; this.evidencia = evidencia;
        this.estado = estado; this.observacion = observacion; this.semestre = semestre;
    }
    public String getId() { return id; }
    public String getEstudianteId() { return estudianteId; }
    public String getActividadId() { return actividadId; }
    public LocalDate getFecha() { return fecha; }
    public int getCantidad() { return cantidad; }
    public String getEvidencia() { return evidencia; }
    public String getEstado() { return estado; }
    public String getObservacion() { return observacion; }
    public int getSemestre() { return semestre; }
    SolicitudParticipacion resuelta(boolean aprobar, String observacion) {
        return new SolicitudParticipacion(id, estudianteId, actividadId, fecha, cantidad,
                evidencia, aprobar ? "APROBADA" : "RECHAZADA", observacion, semestre);
    }
}
