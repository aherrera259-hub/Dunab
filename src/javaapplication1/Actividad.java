package javaapplication1;

import java.io.Serializable;
import java.time.LocalDate;

public final class Actividad implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id, nombre, categoria, descripcion, lugar;
    private final LocalDate fecha;
    private final int dunab, cupos;
    private final boolean activa;
    public Actividad(String id, String nombre, String categoria, String descripcion,
            LocalDate fecha, String lugar, int dunab, int cupos, boolean activa) {
        this.id = id; this.nombre = nombre; this.categoria = categoria;
        this.descripcion = descripcion; this.fecha = fecha; this.lugar = lugar;
        this.dunab = dunab; this.cupos = cupos; this.activa = activa;
    }
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public LocalDate getFecha() { return fecha; }
    public String getLugar() { return lugar; }
    public int getDunab() { return dunab; }
    public int getCupos() { return cupos; }
    public boolean isActiva() { return activa; }
    @Override public String toString() { return nombre + " (" + fecha + ")"; }
}
