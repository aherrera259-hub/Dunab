package javaapplication1;

import java.io.Serializable;
import java.time.LocalDate;

public final class RegistroAuditoria implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id, administradorId, estudianteId, operacion, motivo;
    private final LocalDate fecha;
    private final int cantidad, valorAnterior, valorPosterior;
    public RegistroAuditoria(String id, String administradorId, String estudianteId,
            LocalDate fecha, String operacion, int cantidad, String motivo, int valorAnterior, int valorPosterior) {
        this.id = id; this.administradorId = administradorId; this.estudianteId = estudianteId;
        this.fecha = fecha; this.operacion = operacion; this.cantidad = cantidad;
        this.motivo = motivo; this.valorAnterior = valorAnterior; this.valorPosterior = valorPosterior;
    }
    public String getId() { return id; }
    public String getAdministradorId() { return administradorId; }
    public String getEstudianteId() { return estudianteId; }
    public LocalDate getFecha() { return fecha; }
    public String getOperacion() { return operacion; }
    public int getCantidad() { return cantidad; }
    public String getMotivo() { return motivo; }
    public int getValorAnterior() { return valorAnterior; }
    public int getValorPosterior() { return valorPosterior; }
}
