package javaapplication1;

import java.io.Serializable;
import java.time.LocalDate;

/** Única fuente del saldo: solo se crean movimientos al aprobar o ajustar. */
public final class MovimientoDunab implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id, estudianteId, concepto, categoria, tipo, solicitudId;
    private final LocalDate fecha;
    private final int cantidad, semestre;
    public MovimientoDunab(String id, String estudianteId, LocalDate fecha, int cantidad,
            String concepto, String categoria, int semestre, String tipo, String solicitudId) {
        this.id = id; this.estudianteId = estudianteId; this.fecha = fecha;
        this.cantidad = cantidad; this.concepto = concepto; this.categoria = categoria;
        this.semestre = semestre; this.tipo = tipo; this.solicitudId = solicitudId;
    }
    public String getId() { return id; }
    public String getEstudianteId() { return estudianteId; }
    public LocalDate getFecha() { return fecha; }
    public int getCantidad() { return cantidad; }
    public String getConcepto() { return concepto; }
    public String getCategoria() { return categoria; }
    public int getSemestre() { return semestre; }
    public String getTipo() { return tipo; }
    public String getSolicitudId() { return solicitudId; }
}
