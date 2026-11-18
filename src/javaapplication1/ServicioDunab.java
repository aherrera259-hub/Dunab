package javaapplication1;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/** Reglas del prototipo. Cada operación comprueba la sesión y persiste sus cambios. */
public final class ServicioDunab {
    private static final String ESTUDIANTE = "ESTUDIANTE", ADMINISTRADOR = "ADMINISTRADOR";
    private final Persistencia persistencia;
    private DatosSistema datos;
    private DatosSistema ultimoEstadoGuardado;
    private final HashMap<String, Usuario> usuariosPorId = new HashMap<String, Usuario>();
    private String actualId;

    public ServicioDunab(Path carpetaData) {
        persistencia = new Persistencia(carpetaData);
        if (persistencia.existe()) {
            datos = persistencia.cargar();
            DatosSistema nombresActualizados = nombresSinMarcadores(datos);
            if (nombresActualizados != null) {
                // Solo reemplazamos el archivo tras conservar una copia íntegra de la versión anterior.
                persistencia.respaldarAntesDeNombres();
                persistencia.guardar(nombresActualizados);
                datos = nombresActualizados;
            }
        }
        else {
            datos = new DatosSistema();
            crearDemostracion();
            persistencia.guardar(datos);
        }
        ultimoEstadoGuardado = datos.copia();
        indexarUsuarios();
    }

    public Usuario login(String correo, char[] clave) {
        // Un intento fallido nunca conserva una sesión anterior.
        actualId = null;
        String normalizado = normalizarCorreo(correo);
        if (clave == null || clave.length == 0) throw new IllegalArgumentException("Ingresa la contraseña.");
        for (Usuario usuario : datos.usuarios) {
            if (usuario.getCorreo().equals(normalizado) && usuario.comprobarClave(clave)) {
                actualId = usuario.getId();
                return usuario;
            }
        }
        throw new IllegalArgumentException("El correo o la contraseña son incorrectos.");
    }

    public void logout() { actualId = null; }
    public Usuario getActual() { return actualId == null ? null : usuariosPorId.get(actualId); }

    public void registrar(String nombre, String correo, char[] clave, String programa, int semestre) {
        nombre = obligatorio(nombre, "nombre", 100);
        correo = normalizarCorreo(correo);
        programa = obligatorio(programa, "programa académico", 120);
        validarSemestre(semestre, false);
        if (clave == null || clave.length < 8 || clave.length > 128) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 128 caracteres.");
        }
        correoDisponible(correo, null);
        byte[] sal = SeguridadClave.nuevaSal();
        datos.usuarios.add(new Usuario(id(), nombre, correo, programa, ESTUDIANTE,
                semestre, 10000, sal, SeguridadClave.derivar(clave, sal)));
        guardarCambios();
    }

    public void editarPerfil(String nombre, String correo, String programa, int semestre, int meta) {
        Usuario actual = sesion();
        nombre = obligatorio(nombre, "nombre", 100);
        correo = normalizarCorreo(correo);
        programa = obligatorio(programa, "programa académico", 120);
        validarSemestre(semestre, false);
        positivo(meta, "La meta", 1000000000);
        correoDisponible(correo, actual.getId());
        int posicion = datos.usuarios.indexOf(actual);
        datos.usuarios.set(posicion, actual.conPerfil(nombre, correo, programa, semestre, meta));
        guardarCambios();
    }

    public List<Actividad> actividades(String busqueda, String categoria) {
        Usuario actual = sesion();
        String buscar = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        ArrayList<Actividad> resultado = new ArrayList<Actividad>();
        for (Actividad actividad : datos.actividades) {
            if ((ADMINISTRADOR.equals(actual.getRol()) || actividad.isActiva())
                    && actividad.getNombre().toLowerCase(Locale.ROOT).contains(buscar)
                    && (esTodas(categoria) || actividad.getCategoria().equals(categoria))) {
                resultado.add(actividad);
            }
        }
        Collections.sort(resultado, Comparator.comparing(Actividad::getFecha).thenComparing(Actividad::getNombre));
        return Collections.unmodifiableList(resultado);
    }

    public Actividad actividad(String id) {
        sesion();
        return buscarActividad(id);
    }

    public List<String> categorias() {
        sesion();
        TreeSet<String> categorias = new TreeSet<String>();
        for (Actividad actividad : datos.actividades) categorias.add(actividad.getCategoria());
        for (MovimientoDunab movimiento : datos.movimientos) categorias.add(movimiento.getCategoria());
        ArrayList<String> resultado = new ArrayList<String>();
        resultado.add("Todas"); resultado.addAll(categorias);
        return Collections.unmodifiableList(resultado);
    }

    public int cuposDisponibles(String actividadId) {
        sesion();
        return buscarActividad(actividadId).getCupos() - inscripcionesActivas(actividadId);
    }

    public void inscribir(String actividadId) {
        Usuario estudiante = exigirEstudiante();
        Actividad actividad = buscarActividad(actividadId);
        if (!actividad.isActiva()) throw new IllegalArgumentException("La actividad está desactivada.");
        if (actividad.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No es posible inscribirse en una actividad que ya terminó.");
        }
        if (inscripcionActiva(estudiante.getId(), actividadId) != null) {
            throw new IllegalArgumentException("Ya tienes una inscripción activa en esta actividad.");
        }
        if (inscripcionesActivas(actividadId) >= actividad.getCupos()) {
            throw new IllegalArgumentException("La actividad no tiene cupos disponibles.");
        }
        datos.inscripciones.add(new Inscripcion(id(), estudiante.getId(), actividadId, LocalDate.now(), "ACTIVA"));
        guardarCambios();
    }

    public List<Inscripcion> misInscripciones() {
        Usuario actual = exigirEstudiante();
        ArrayList<Inscripcion> resultado = new ArrayList<Inscripcion>();
        for (Inscripcion inscripcion : datos.inscripciones) {
            if (inscripcion.getEstudianteId().equals(actual.getId())) resultado.add(inscripcion);
        }
        return Collections.unmodifiableList(resultado);
    }

    public void cancelarInscripcion(String inscripcionId) {
        Usuario actual = exigirEstudiante();
        for (int i = 0; i < datos.inscripciones.size(); i++) {
            Inscripcion inscripcion = datos.inscripciones.get(i);
            if (inscripcion.getId().equals(inscripcionId)) {
                propietario(inscripcion.getEstudianteId());
                if (!"ACTIVA".equals(inscripcion.getEstado())) {
                    throw new IllegalArgumentException("La inscripción ya está cancelada.");
                }
                if (buscarActividad(inscripcion.getActividadId()).getFecha().isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("No se puede cancelar una inscripción de una actividad pasada.");
                }
                for (SolicitudParticipacion solicitud : datos.solicitudes) {
                    if (solicitud.getEstudianteId().equals(actual.getId())
                            && solicitud.getActividadId().equals(inscripcion.getActividadId())
                            && !"RECHAZADA".equals(solicitud.getEstado())) {
                        throw new IllegalArgumentException("No puedes cancelar una actividad con participación pendiente o aprobada.");
                    }
                }
                datos.inscripciones.set(i, inscripcion.cancelada());
                guardarCambios();
                return;
            }
        }
        throw new IllegalArgumentException("No se encontró la inscripción.");
    }

    public void solicitar(String actividadId, LocalDate fecha, int cantidad, String evidencia, int semestre) {
        Usuario estudiante = exigirEstudiante();
        Actividad actividad = buscarActividad(actividadId);
        validarSemestre(semestre, false);
        positivo(cantidad, "La cantidad solicitada", 1000000);
        evidencia = obligatorio(evidencia, "evidencia o código de referencia", 500);
        if (fecha == null || fecha.isAfter(LocalDate.now()) || !fecha.equals(actividad.getFecha())) {
            throw new IllegalArgumentException("La fecha debe coincidir con la actividad y no puede ser futura.");
        }
        if (cantidad > actividad.getDunab()) {
            throw new IllegalArgumentException("La cantidad solicitada supera los DUNAB de esta actividad.");
        }
        if (inscripcionActiva(estudiante.getId(), actividadId) == null) {
            throw new IllegalArgumentException("Primero debes tener una inscripción activa en esta actividad.");
        }
        for (SolicitudParticipacion solicitud : datos.solicitudes) {
            if (solicitud.getEstudianteId().equals(estudiante.getId())
                    && solicitud.getActividadId().equals(actividadId)
                    && !"RECHAZADA".equals(solicitud.getEstado())) {
                throw new IllegalArgumentException("Ya existe una solicitud pendiente o aprobada para esta actividad.");
            }
        }
        datos.solicitudes.add(new SolicitudParticipacion(id(), estudiante.getId(), actividadId,
                fecha, cantidad, evidencia, "PENDIENTE", "", semestre));
        guardarCambios();
    }

    public List<SolicitudParticipacion> misSolicitudes() {
        Usuario actual = exigirEstudiante();
        ArrayList<SolicitudParticipacion> resultado = new ArrayList<SolicitudParticipacion>();
        for (SolicitudParticipacion solicitud : datos.solicitudes) {
            if (solicitud.getEstudianteId().equals(actual.getId())) resultado.add(solicitud);
        }
        return Collections.unmodifiableList(resultado);
    }

    public List<MovimientoDunab> misMovimientos() {
        Usuario actual = exigirEstudiante();
        ArrayList<MovimientoDunab> resultado = new ArrayList<MovimientoDunab>();
        for (MovimientoDunab movimiento : datos.movimientos) {
            if (movimiento.getEstudianteId().equals(actual.getId())) resultado.add(movimiento);
        }
        Collections.sort(resultado, Comparator.comparing(MovimientoDunab::getFecha));
        return Collections.unmodifiableList(resultado);
    }

    public int saldo(String estudianteId, int semestre) {
        propietarioOAdmin(estudianteId);
        validarSemestre(semestre, true);
        return saldoInterno(estudianteId, semestre);
    }

    public List<Meta> misMetas() {
        Usuario actual = exigirEstudiante();
        ArrayList<Meta> resultado = new ArrayList<Meta>();
        for (Meta meta : datos.metas) {
            if (meta.getEstudianteId().equals(actual.getId())) resultado.add(meta);
        }
        return Collections.unmodifiableList(resultado);
    }

    public void guardarMeta(String metaId, String nombre, int objetivo, String categoria, int semestre) {
        Usuario actual = exigirEstudiante();
        nombre = obligatorio(nombre, "nombre de la meta", 120);
        positivo(objetivo, "El objetivo", 1000000000);
        validarSemestre(semestre, true);
        categoria = esTodas(categoria) ? "Todas" : obligatorio(categoria, "categoría", 80);
        if (!"Todas".equals(categoria) && !categorias().contains(categoria)) {
            throw new IllegalArgumentException("Selecciona una categoría disponible.");
        }
        if (metaId == null) datos.metas.add(new Meta(id(), actual.getId(), nombre, objetivo, categoria, semestre));
        else {
            int indice = indiceMeta(metaId);
            propietario(datos.metas.get(indice).getEstudianteId());
            datos.metas.set(indice, new Meta(metaId, actual.getId(), nombre, objetivo, categoria, semestre));
        }
        guardarCambios();
    }

    public void eliminarMeta(String metaId) {
        exigirEstudiante();
        int indice = indiceMeta(metaId);
        propietario(datos.metas.get(indice).getEstudianteId());
        datos.metas.remove(indice);
        guardarCambios();
    }

    public int progresoMeta(Meta meta) {
        exigirEstudiante();
        if (meta == null) throw new IllegalArgumentException("Selecciona una meta.");
        propietario(meta.getEstudianteId());
        long cantidad = 0;
        for (MovimientoDunab movimiento : datos.movimientos) {
            if (movimiento.getEstudianteId().equals(meta.getEstudianteId())
                    && (meta.getSemestre() == 0 || meta.getSemestre() == movimiento.getSemestre())
                    && (esTodas(meta.getCategoria()) || meta.getCategoria().equals(movimiento.getCategoria()))) {
                cantidad += movimiento.getCantidad();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, cantidad));
    }

    public List<Usuario> ranking(final int semestre) {
        sesion(); validarSemestre(semestre, false);
        ArrayList<Usuario> resultado = new ArrayList<Usuario>();
        for (Usuario usuario : datos.usuarios) {
            if (ESTUDIANTE.equals(usuario.getRol())) resultado.add(usuario.paraRanking());
        }
        Collections.sort(resultado, new Comparator<Usuario>() {
            @Override public int compare(Usuario primero, Usuario segundo) {
                int comparar = Integer.compare(saldoInterno(segundo.getId(), semestre), saldoInterno(primero.getId(), semestre));
                if (comparar == 0) comparar = primero.getNombre().compareToIgnoreCase(segundo.getNombre());
                return comparar == 0 ? primero.getId().compareTo(segundo.getId()) : comparar;
            }
        });
        return Collections.unmodifiableList(new ArrayList<Usuario>(resultado.subList(0, Math.min(9, resultado.size()))));
    }

    public int saldoRanking(String estudianteId, int semestre) {
        sesion(); validarSemestre(semestre, false); buscarEstudiante(estudianteId);
        return saldoInterno(estudianteId, semestre);
    }

    public List<Usuario> estudiantes() {
        exigirAdmin();
        ArrayList<Usuario> resultado = new ArrayList<Usuario>();
        for (Usuario usuario : datos.usuarios) {
            if (ESTUDIANTE.equals(usuario.getRol())) resultado.add(usuario);
        }
        Collections.sort(resultado, Comparator.comparing(Usuario::getNombre));
        return Collections.unmodifiableList(resultado);
    }

    public Usuario usuario(String usuarioId) {
        propietarioOAdmin(usuarioId);
        Usuario usuario = usuariosPorId.get(usuarioId);
        if (usuario == null) throw new IllegalArgumentException("No se encontró el usuario.");
        return usuario;
    }

    public String nombreEstudiante(String estudianteId) {
        sesion();
        return buscarEstudiante(estudianteId).getNombre();
    }

    public List<SolicitudParticipacion> solicitudesPendientes() {
        exigirAdmin();
        ArrayList<SolicitudParticipacion> resultado = new ArrayList<SolicitudParticipacion>();
        for (SolicitudParticipacion solicitud : datos.solicitudes) {
            if ("PENDIENTE".equals(solicitud.getEstado())) resultado.add(solicitud);
        }
        return Collections.unmodifiableList(resultado);
    }

    public void resolverSolicitud(String solicitudId, boolean aprobar, String observacion) {
        Usuario administrador = exigirAdmin();
        observacion = obligatorio(observacion, "observación de la revisión", 500);
        for (int i = 0; i < datos.solicitudes.size(); i++) {
            SolicitudParticipacion solicitud = datos.solicitudes.get(i);
            if (solicitud.getId().equals(solicitudId)) {
                if (!"PENDIENTE".equals(solicitud.getEstado())) {
                    throw new IllegalArgumentException("Esta solicitud ya fue revisada; no puede reconocerse de nuevo.");
                }
                int anterior = saldoInterno(solicitud.getEstudianteId(), 0);
                if (aprobar) {
                    sumarSeguro(anterior, solicitud.getCantidad());
                    for (MovimientoDunab movimiento : datos.movimientos) {
                        if (solicitudId.equals(movimiento.getSolicitudId())) {
                            throw new IllegalStateException("La solicitud ya tiene un movimiento registrado.");
                        }
                    }
                    Actividad actividad = buscarActividad(solicitud.getActividadId());
                    datos.movimientos.add(new MovimientoDunab(id(), solicitud.getEstudianteId(),
                            solicitud.getFecha(), solicitud.getCantidad(), actividad.getNombre(),
                            actividad.getCategoria(), solicitud.getSemestre(), "APROBACION", solicitudId));
                }
                datos.solicitudes.set(i, solicitud.resuelta(aprobar, observacion));
                datos.auditoria.add(new RegistroAuditoria(id(), administrador.getId(), solicitud.getEstudianteId(),
                        LocalDate.now(), aprobar ? "APROBAR_SOLICITUD" : "RECHAZAR_SOLICITUD",
                        aprobar ? solicitud.getCantidad() : 0, observacion, anterior,
                        aprobar ? anterior + solicitud.getCantidad() : anterior));
                guardarCambios();
                return;
            }
        }
        throw new IllegalArgumentException("No se encontró la solicitud.");
    }

    public void guardarActividad(String actividadId, String nombre, String categoria, String descripcion,
            LocalDate fecha, String lugar, int dunab, int cupos, boolean activa) {
        Usuario administrador = exigirAdmin();
        nombre = obligatorio(nombre, "nombre de la actividad", 120);
        categoria = obligatorio(categoria, "categoría", 80);
        if ("Todas".equalsIgnoreCase(categoria)) throw new IllegalArgumentException("Usa una categoría específica.");
        descripcion = obligatorio(descripcion, "descripción", 1000);
        lugar = obligatorio(lugar, "lugar", 150);
        if (fecha == null) throw new IllegalArgumentException("Ingresa la fecha de la actividad.");
        positivo(dunab, "Los DUNAB de la actividad", 1000000);
        positivo(cupos, "Los cupos", 1000000);
        if (actividadId == null) {
            datos.actividades.add(new Actividad(id(), nombre, categoria, descripcion, fecha, lugar, dunab, cupos, activa));
        } else {
            Actividad original = buscarActividad(actividadId);
            if (cupos < inscripcionesActivas(actividadId)) {
                throw new IllegalArgumentException("Los cupos no pueden ser menores que las inscripciones activas.");
            }
            for (Inscripcion inscripcion : datos.inscripciones) {
                if (inscripcion.getActividadId().equals(actividadId) && !original.getFecha().equals(fecha)) {
                    throw new IllegalArgumentException("Una actividad con inscripciones históricas debe conservar su fecha.");
                }
            }
            datos.actividades.set(datos.actividades.indexOf(original),
                    new Actividad(actividadId, nombre, categoria, descripcion, fecha, lugar, dunab, cupos, activa));
        }
        datos.auditoria.add(new RegistroAuditoria(id(), administrador.getId(), "", LocalDate.now(),
                actividadId == null ? "CREAR_ACTIVIDAD" : "EDITAR_ACTIVIDAD", 0,
                nombre + (activa ? " · activa" : " · desactivada"), 0, 0));
        guardarCambios();
    }

    public void ajustar(String estudianteId, int cantidad, int semestre, String motivo) {
        Usuario administrador = exigirAdmin();
        buscarEstudiante(estudianteId); validarSemestre(semestre, false);
        motivo = obligatorio(motivo, "motivo del ajuste", 500);
        if (cantidad == 0 || cantidad < -1000000 || cantidad > 1000000) {
            throw new IllegalArgumentException("El ajuste debe estar entre -1.000.000 y 1.000.000 DUNAB y ser diferente de cero.");
        }
        int anterior = saldoInterno(estudianteId, 0);
        int posterior = sumarSeguro(anterior, cantidad);
        sumarSeguro(saldoInterno(estudianteId, semestre), cantidad);
        datos.movimientos.add(new MovimientoDunab(id(), estudianteId, LocalDate.now(), cantidad,
                "Ajuste: " + motivo, "Ajuste administrativo", semestre, "AJUSTE", ""));
        datos.auditoria.add(new RegistroAuditoria(id(), administrador.getId(), estudianteId,
                LocalDate.now(), "AJUSTE_DUNAB", cantidad, motivo, anterior, posterior));
        guardarCambios();
    }

    public List<RegistroAuditoria> auditoria() {
        exigirAdmin();
        return Collections.unmodifiableList(new ArrayList<RegistroAuditoria>(datos.auditoria));
    }

    private void guardarCambios() {
        try {
            persistencia.guardar(datos);
            ultimoEstadoGuardado = datos.copia();
            indexarUsuarios();
        }
        catch (RuntimeException error) {
            // Incluso si data deja de estar accesible, no dejamos cambios sin guardar en memoria.
            datos = ultimoEstadoGuardado.copia();
            indexarUsuarios();
            throw error;
        }
    }

    private void indexarUsuarios() {
        usuariosPorId.clear();
        for (Usuario usuario : datos.usuarios) usuariosPorId.put(usuario.getId(), usuario);
    }
    private Usuario sesion() {
        Usuario actual = getActual();
        if (actual == null) throw new IllegalStateException("Debes iniciar sesión.");
        return actual;
    }
    private Usuario exigirEstudiante() {
        Usuario actual = sesion();
        if (!ESTUDIANTE.equals(actual.getRol())) throw new IllegalStateException("Esta función corresponde al estudiante.");
        return actual;
    }
    private Usuario exigirAdmin() {
        Usuario actual = sesion();
        if (!ADMINISTRADOR.equals(actual.getRol())) throw new IllegalStateException("Acceso exclusivo del administrador.");
        return actual;
    }
    private void propietario(String estudianteId) {
        if (!sesion().getId().equals(estudianteId)) throw new IllegalStateException("Solo puedes modificar tu propia información.");
    }
    private void propietarioOAdmin(String estudianteId) {
        Usuario actual = sesion();
        if (!ADMINISTRADOR.equals(actual.getRol()) && !actual.getId().equals(estudianteId)) {
            throw new IllegalStateException("Solo puedes consultar tu propia información.");
        }
    }
    private Usuario buscarEstudiante(String estudianteId) {
        Usuario estudiante = usuariosPorId.get(estudianteId);
        if (estudiante == null || !ESTUDIANTE.equals(estudiante.getRol())) {
            throw new IllegalArgumentException("No se encontró el estudiante.");
        }
        return estudiante;
    }
    private Actividad buscarActividad(String actividadId) {
        for (Actividad actividad : datos.actividades) {
            if (actividad.getId().equals(actividadId)) return actividad;
        }
        throw new IllegalArgumentException("No se encontró la actividad.");
    }
    private int inscripcionesActivas(String actividadId) {
        int cantidad = 0;
        for (Inscripcion inscripcion : datos.inscripciones) {
            if (inscripcion.getActividadId().equals(actividadId) && "ACTIVA".equals(inscripcion.getEstado())) cantidad++;
        }
        return cantidad;
    }
    private Inscripcion inscripcionActiva(String estudianteId, String actividadId) {
        for (Inscripcion inscripcion : datos.inscripciones) {
            if (inscripcion.getEstudianteId().equals(estudianteId) && inscripcion.getActividadId().equals(actividadId)
                    && "ACTIVA".equals(inscripcion.getEstado())) return inscripcion;
        }
        return null;
    }
    private int saldoInterno(String estudianteId, int semestre) {
        long cantidad = 0;
        for (MovimientoDunab movimiento : datos.movimientos) {
            if (movimiento.getEstudianteId().equals(estudianteId)
                    && (semestre == 0 || movimiento.getSemestre() == semestre)) cantidad += movimiento.getCantidad();
        }
        if (cantidad < 0 || cantidad > Integer.MAX_VALUE) throw new IllegalStateException("El saldo almacenado está fuera del rango permitido.");
        return (int) cantidad;
    }
    private int sumarSeguro(int saldo, int cantidad) {
        long nuevo = (long) saldo + cantidad;
        if (nuevo < 0) throw new IllegalArgumentException("El retiro produciría un saldo negativo en el semestre o en el acumulado.");
        if (nuevo > Integer.MAX_VALUE) throw new IllegalArgumentException("El saldo supera el máximo admitido.");
        return (int) nuevo;
    }
    private int indiceMeta(String metaId) {
        for (int i = 0; i < datos.metas.size(); i++) if (datos.metas.get(i).getId().equals(metaId)) return i;
        throw new IllegalArgumentException("No se encontró la meta.");
    }
    private void correoDisponible(String correo, String excluirId) {
        for (Usuario usuario : datos.usuarios) {
            if (usuario.getCorreo().equals(correo) && !usuario.getId().equals(excluirId)) {
                throw new IllegalArgumentException("Ya existe una cuenta con este correo.");
            }
        }
    }
    private String normalizarCorreo(String correo) {
        correo = obligatorio(correo, "correo", 150).toLowerCase(Locale.ROOT);
        if (!correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Ingresa un correo válido.");
        return correo;
    }
    private String obligatorio(String valor, String campo, int maximo) {
        if (valor == null || valor.trim().isEmpty()) throw new IllegalArgumentException("Completa el campo " + campo + ".");
        if (valor.trim().length() > maximo) throw new IllegalArgumentException("El campo " + campo + " admite máximo " + maximo + " caracteres.");
        return valor.trim();
    }
    private void positivo(int valor, String nombre, int maximo) {
        if (valor <= 0 || valor > maximo) throw new IllegalArgumentException(nombre + " debe ser mayor que cero y no superar " + maximo + ".");
    }
    private void validarSemestre(int semestre, boolean historico) {
        Semestres.validar(semestre, historico);
    }
    private boolean esTodas(String categoria) { return categoria == null || categoria.trim().isEmpty() || "Todas".equals(categoria); }
    private String id() { return UUID.randomUUID().toString(); }

    /** Actualización compatible con archivos anteriores, sin tocar identificadores ni saldos. */
    private DatosSistema nombresSinMarcadores(DatosSistema originales) {
        DatosSistema actualizados = originales.copia();
        boolean cambio = false;
        for (int i = 0; i < originales.usuarios.size(); i++) {
            Usuario usuario = originales.usuarios.get(i);
            String nombre = usuario.getNombre();
            if ("Estudiante Demo".equals(nombre)) nombre = "Estudiante";
            else if ("Administrador Demo".equals(nombre)) nombre = "Administrador";
            nombre = quitarSufijoMarcado(nombre);
            String programa = "Bienestar (demostración)".equals(usuario.getPrograma())
                    ? "Bienestar" : quitarSufijoMarcado(usuario.getPrograma());
            if (!nombre.equals(usuario.getNombre()) || !programa.equals(usuario.getPrograma())) {
                actualizados.usuarios.set(i, usuario.conPerfil(nombre, usuario.getCorreo(), programa,
                        usuario.getSemestre(), usuario.getMetaDunab()));
                cambio = true;
            }
        }
        for (int i = 0; i < originales.actividades.size(); i++) {
            Actividad actividad = originales.actividades.get(i);
            String nombre = quitarSufijoMarcado(actividad.getNombre());
            String lugar = quitarSufijoMarcado(actividad.getLugar());
            if (!nombre.equals(actividad.getNombre()) || !lugar.equals(actividad.getLugar())) {
                actualizados.actividades.set(i, new Actividad(actividad.getId(), nombre, actividad.getCategoria(),
                        actividad.getDescripcion(), actividad.getFecha(), lugar, actividad.getDunab(),
                        actividad.getCupos(), actividad.isActiva()));
                cambio = true;
            }
        }
        for (int i = 0; i < originales.movimientos.size(); i++) {
            MovimientoDunab movimiento = originales.movimientos.get(i);
            // Los motivos libres de ajustes administrativos y la auditoría se conservan literalmente.
            if (!"APROBACION".equals(movimiento.getTipo())) continue;
            String concepto = quitarSufijoMarcado(movimiento.getConcepto());
            if (!concepto.equals(movimiento.getConcepto())) {
                actualizados.movimientos.set(i, new MovimientoDunab(movimiento.getId(), movimiento.getEstudianteId(),
                        movimiento.getFecha(), movimiento.getCantidad(), concepto, movimiento.getCategoria(),
                        movimiento.getSemestre(), movimiento.getTipo(), movimiento.getSolicitudId()));
                cambio = true;
            }
        }
        for (int i = 0; i < originales.metas.size(); i++) {
            Meta meta = originales.metas.get(i);
            String nombre = "Mi meta cultural de demostración".equals(meta.getNombre())
                    ? "Mi meta cultural" : quitarSufijoMarcado(meta.getNombre());
            if (!nombre.equals(meta.getNombre())) {
                actualizados.metas.set(i, new Meta(meta.getId(), meta.getEstudianteId(), nombre,
                        meta.getObjetivo(), meta.getCategoria(), meta.getSemestre()));
                cambio = true;
            }
        }
        return cambio ? actualizados : null;
    }

    private String quitarSufijoMarcado(String texto) {
        String limpio = texto.replaceFirst("(?i)\\s*(?:·\\s*demo|\\(demo\\))\\s*$", "");
        if (limpio.equals(texto) || limpio.trim().isEmpty()) return texto;
        return quitarSufijoMarcado(limpio);
    }

    private void crearDemostracion() {
        LocalDate hoy = LocalDate.now();
        byte[] salEstudiante = SeguridadClave.nuevaSal(), salAdmin = SeguridadClave.nuevaSal();
        Usuario estudiante = new Usuario(id(), "Estudiante", "estudiante@demo.local", "Ingeniería de Sistemas",
                ESTUDIANTE, 2, 10000, salEstudiante, SeguridadClave.derivar("Estudiante123!".toCharArray(), salEstudiante));
        Usuario administrador = new Usuario(id(), "Administrador", "admin@demo.local", "Bienestar",
                ADMINISTRADOR, 2, 10000, salAdmin, SeguridadClave.derivar("Admin123!".toCharArray(), salAdmin));
        datos.usuarios.add(estudiante); datos.usuarios.add(administrador);
        Actividad cultura = new Actividad(id(), "Encuentro de teatro", "Cultura", "Participación ficticia de ejemplo.",
                hoy.minusDays(35), "Auditorio", 800, 30, true);
        Actividad deporte = new Actividad(id(), "Jornada deportiva", "Deporte", "Actividad ficticia para mostrar el historial.",
                hoy.minusDays(7), "Cancha", 600, 20, true);
        Actividad bienestar = new Actividad(id(), "Pausa de bienestar", "Bienestar", "Actividad de hoy; permite probar solicitudes con datos ficticios.",
                hoy, "Sala de bienestar", 300, 15, true);
        datos.actividades.add(cultura); datos.actividades.add(deporte); datos.actividades.add(bienestar);
        datos.actividades.add(new Actividad(id(), "Taller de pintura", "Cultura", "Exploración artística con materiales de práctica.",
                hoy.plusDays(3), "Aula creativa", 500, 12, true));
        datos.actividades.add(new Actividad(id(), "Caminata universitaria", "Deporte", "Recorrido de bienestar del prototipo.",
                hoy.plusDays(5), "Punto de encuentro", 700, 25, true));
        datos.actividades.add(new Actividad(id(), "Voluntariado ambiental", "Voluntariado", "Jornada ficticia de cuidado ambiental.",
                hoy.plusDays(8), "Jardín universitario", 900, 18, true));
        datos.actividades.add(new Actividad(id(), "Conversatorio de lectura", "Cultura", "Espacio de lectura y conversación.",
                hoy.plusDays(10), "Biblioteca", 400, 16, true));
        sembrarParticipacion(estudiante, administrador, cultura, 800, 1, true);
        sembrarParticipacion(estudiante, administrador, deporte, 600, 2, true);
        sembrarParticipacion(estudiante, administrador, bienestar, 300, 2, false);
        datos.metas.add(new Meta(id(), estudiante.getId(), "Mi meta cultural", 2000, "Cultura", 0));
    }

    private void sembrarParticipacion(Usuario estudiante, Usuario admin, Actividad actividad, int cantidad, int semestre, boolean aprobada) {
        datos.inscripciones.add(new Inscripcion(id(), estudiante.getId(), actividad.getId(), actividad.getFecha(), "ACTIVA"));
        String solicitudId = id();
        datos.solicitudes.add(new SolicitudParticipacion(solicitudId, estudiante.getId(), actividad.getId(), actividad.getFecha(), cantidad,
                "DEMO-" + semestre + "-" + cantidad, aprobada ? "APROBADA" : "PENDIENTE",
                aprobada ? "Participación ficticia inicial, aprobada para demostración." : "", semestre));
        if (aprobada) {
            int anterior = saldoInterno(estudiante.getId(), 0);
            datos.movimientos.add(new MovimientoDunab(id(), estudiante.getId(), actividad.getFecha(), cantidad,
                    actividad.getNombre(), actividad.getCategoria(), semestre, "APROBACION", solicitudId));
            datos.auditoria.add(new RegistroAuditoria(id(), admin.getId(), estudiante.getId(), actividad.getFecha(),
                    "APROBAR_SOLICITUD", cantidad, "Dato ficticio inicial de demostración.", anterior, anterior + cantidad));
        }
    }
}
