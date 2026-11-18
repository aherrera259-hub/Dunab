package javaapplication1;

import java.io.Serializable;
import java.util.ArrayList;

/** Contenedor del archivo local; cada lista representa una colección del sistema. */
final class DatosSistema implements Serializable {
    private static final long serialVersionUID = 1L;
    final ArrayList<Usuario> usuarios = new ArrayList<Usuario>();
    final ArrayList<Actividad> actividades = new ArrayList<Actividad>();
    final ArrayList<Inscripcion> inscripciones = new ArrayList<Inscripcion>();
    final ArrayList<SolicitudParticipacion> solicitudes = new ArrayList<SolicitudParticipacion>();
    final ArrayList<MovimientoDunab> movimientos = new ArrayList<MovimientoDunab>();
    final ArrayList<Meta> metas = new ArrayList<Meta>();
    final ArrayList<RegistroAuditoria> auditoria = new ArrayList<RegistroAuditoria>();

    /** Los modelos son inmutables: basta copiar las listas para conservar un punto de retorno. */
    DatosSistema copia() {
        DatosSistema copia = new DatosSistema();
        copia.usuarios.addAll(usuarios);
        copia.actividades.addAll(actividades);
        copia.inscripciones.addAll(inscripciones);
        copia.solicitudes.addAll(solicitudes);
        copia.movimientos.addAll(movimientos);
        copia.metas.addAll(metas);
        copia.auditoria.addAll(auditoria);
        return copia;
    }
}
