# Contrato interno de implementación

Java 8 o posterior, paquete único `javaapplication1`, sin dependencias. Fechas `java.time.LocalDate`; identificadores `String`. Importes DUNAB `int`. Cada archivo público tiene su clase homónima. Servicios validan y guardan cada mutación; lanzan `IllegalArgumentException`/`IllegalStateException` con mensajes en español. Persistencia propaga errores sin reinicializar archivos corruptos.

## Modelos (getters públicos)
- Usuario: getId(), getNombre(), getCorreo(), getPrograma(), getRol() (ESTUDIANTE/ADMINISTRADOR), getSemestre() (1..Semestres.MAXIMO), getMetaDunab(). No exponer contraseñas.
- Actividad: getId(), getNombre(), getCategoria(), getDescripcion(), getFecha(), getLugar(), getDunab(), getCupos(), isActiva(). Semestre de participación lo selecciona el estudiante.
- Inscripcion: getId(), getEstudianteId(), getActividadId(), getFecha(), getEstado() (ACTIVA/CANCELADA).
- SolicitudParticipacion: getId(), getEstudianteId(), getActividadId(), getFecha(), getCantidad(), getEvidencia(), getEstado() (PENDIENTE/APROBADA/RECHAZADA), getObservacion(), getSemestre().
- MovimientoDunab: getId(), getEstudianteId(), getFecha(), getCantidad() (con signo), getConcepto(), getCategoria(), getSemestre(), getTipo(), getSolicitudId().
- Meta: getId(), getEstudianteId(), getNombre(), getObjetivo(), getCategoria() (Todas o categoría), getSemestre() (0 histórico/1..Semestres.MAXIMO). El progreso lo calcula el servicio.
- RegistroAuditoria: getId(), getAdministradorId(), getEstudianteId(), getFecha(), getOperacion(), getCantidad(), getMotivo(), getValorAnterior(), getValorPosterior().

## ServicioDunab
- constructor ServicioDunab(Path carpetaData), inicializa demo SOLO si no existe datos; persiste. Demo estudiante `estudiante@demo.local` / `Estudiante123!`; admin `admin@demo.local` / `Admin123!`.
- Usuario login(String correo, char[] clave); void logout(); Usuario getActual();
- void registrar(String nombre,String correo,char[] clave,String programa,int semestre)
- void editarPerfil(String nombre,String correo,String programa,int semestre,int meta)
- List<Actividad> actividades(String busqueda,String categoria); Actividad actividad(String id); List<String> categorias(); int cuposDisponibles(String actividadId)
- void inscribir(String actividadId); List<Inscripcion> misInscripciones(); void cancelarInscripcion(String inscripcionId)
- void solicitar(String actividadId,LocalDate fecha,int cantidad,String evidencia,int semestre); List<SolicitudParticipacion> misSolicitudes()
- List<MovimientoDunab> misMovimientos(); int saldo(String estudianteId,int semestre) (0 histórico; autoriza propietario o admin)
- List<Meta> misMetas(); void guardarMeta(String idONull,String nombre,int objetivo,String categoria,int semestre); void eliminarMeta(String id); int progresoMeta(Meta meta)
- List<Usuario> ranking(int semestre) (orden desc saldo; máximo 9, accesible a todos autenticados); int saldoRanking(String estudianteId,int semestre) (solo lectura pública de saldo agregado)
- List<Usuario> estudiantes() (admin); Usuario usuario(String id) (propio/admin); String nombreEstudiante(String id) (nombre público para ranking); List<SolicitudParticipacion> solicitudesPendientes() (admin)
- void resolverSolicitud(String id,boolean aprobar,String observacion)
- void guardarActividad(String idONull,String nombre,String categoria,String descripcion,LocalDate fecha,String lugar,int dunab,int cupos,boolean activa)
- void ajustar(String estudianteId,int cantidad,int semestre,String motivo); List<RegistroAuditoria> auditoria()

## UI compartida (root implementa Ui.java)
- Ui.fondo, Ui.morado, Ui.marron, Ui.texto (Color públicos estáticos)
- JPanel Ui.pagina(String titulo,String subtitulo) -> BorderLayout, cabecera NORTH, agregar cuerpo CENTER y acciones SOUTH.
- JButton Ui.boton(String texto,Runnable accion) captura RuntimeException y muestra JOptionPane.
- JTable Ui.tabla(String... columnas) con DefaultTableModel no editable; Ui.filas(JTable,Object[][]).
- JPanel Ui.formulario(Object... paresEtiquetaComponente) -> GridBagLayout.
- JTextField Ui.campo(String texto); JSpinner Ui.entero(int valor,int minimo,int maximo); int Ui.valor(JSpinner).
- String Ui.idSeleccionado(JTable tabla,List<String> ids) valida selección y convierte índice de vista a modelo.
- void Ui.info(Component padre,String mensaje); String Ui.numero(int n); String Ui.nombreActividad(ServicioDunab s,String id).

## Pantallas
- PantallasEstudiante(ServicioDunab servicio, java.util.function.IntSupplier semestre, Runnable refrescarTodo)
- PantallasEstudiante(ServicioDunab servicio, java.util.function.IntSupplier semestre, Runnable refrescarTodo, java.util.function.Consumer<String> navegar) conecta los botones internos con el menú principal; el constructor anterior conserva navegación local para pruebas y vistas.
- `Registrar participación` es una ruta interna accesible desde `Inscripciones`; no se presenta en la barra lateral. `Inicio` ofrece `Ver actividades e inscribirme` y el catálogo utiliza `Inscribirme` en la zona superior.
- El selector de participación incluye todas las inscripciones activas. `estadoParticipacion` explica si la fecha es futura o si hay una solicitud previa; `enviarParticipacion` solo se habilita para participaciones cuya fecha llegó y sin solicitudes pendientes o aprobadas. Se prioriza una elegible o una todavía no solicitada, antes de las ya procesadas.
- JPanel crear(String seccion): `Inicio`, `Mi perfil`, `Consultar DUNAB`, `Actividades y espacios`, `Inscripciones`, `Registrar participación`, `Historial de DUNAB`, `Metas personales`, `Top 9`, `Calculadora de notas`, `Estadísticas y reportes`. Perfil y todas pantallas pueden recrearse después de cambios.
- PantallasAdmin(ServicioDunab servicio, java.util.function.IntSupplier semestre, Runnable refrescarTodo); JPanel crear(String seccion): `Administrar actividades`, `Revisar solicitudes`, `Estudiantes y ajustes`, `Auditoría`.
- CalculadoraNotas: static double ponderada(double[] notas,double[] porcentajes); static double necesaria(double[] notas,double[] porcentajes,int indicePendiente,double minima); static double[] repartirPorcentajes(int cantidad). Admite 1..30 evaluaciones; porcentajes positivos que suman 100. Conserva las APIs de tres cortes para compatibilidad.

## Semestres y apariencia

- Semestres.MAXIMO=12; String[] Semestres.opciones(boolean incluirHistorico); void Semestres.validar(int semestre,boolean permitirHistorico). El histórico usa 0; el resto usa 1..12.
- Tema.iniciar(Path archivoPreferencia), Tema.esOscuro(), Tema.cambiar(), Tema.aplicar(Component), Tema.botonCambio(). La preferencia de producción se guarda en data/apariencia.properties; una ruta nula no guarda preferencias y se utiliza en pruebas aisladas.
- MenuDUNAB(ServicioDunab) no escribe preferencias de producción. MenuDUNAB(ServicioDunab,Path) permite inyectar una preferencia temporal. El constructor vacío usa data/dunab.dat y data/apariencia.properties.
- Cambiar apariencia actualiza colores sin reconstruir formularios ni perder campos, notas o selecciones.

Menú, login, Ui, CalculadoraNotas, configuración NetBeans, pruebas y documentación general: root. Backend y modelos: agente backend. Pantallas estudiante: agente estudiante. Pantallas administración: agente admin.
