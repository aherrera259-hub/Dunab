package javaapplication1;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Migración de nombres de versiones anteriores, siempre sobre archivos temporales aislados. */
public final class PruebasNombres {
    private static int comprobaciones;
    private PruebasNombres() { }

    public static void main(String[] args) throws Exception {
        Path raiz = Files.createTempDirectory("dunab-pruebas-nombres-");
        Path carpeta = raiz.resolve("version-anterior");
        DatosSistema anteriores = versionAnterior();
        Persistencia persistencia = new Persistencia(carpeta);
        persistencia.guardar(anteriores);
        byte[] archivoAnterior = Files.readAllBytes(carpeta.resolve("dunab.dat"));

        ServicioDunab servicio = new ServicioDunab(carpeta);
        Usuario estudiante = servicio.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        verificar("Estudiante".equals(estudiante.getNombre()), "Se actualiza únicamente el nombre inicial conocido");
        verificar("u-estudiante".equals(estudiante.getId()) && estudiante.getSemestre() == 10
                && estudiante.getMetaDunab() == 15000, "La cuenta conserva identificador, semestre y objetivo");
        verificar(servicio.saldo(estudiante.getId(), 0) == 850 && servicio.saldo(estudiante.getId(), 1) == 800
                && servicio.saldo(estudiante.getId(), 10) == 50, "Saldos históricos y semestrales permanecen intactos");
        verificar(servicio.misSolicitudes().size() == 2 && servicio.misInscripciones().size() == 2,
                "Se conservan solicitudes e inscripciones del estudiante");
        verificar(servicio.cuposDisponibles("a-teatro") == 7, "Se conservan los cupos y su ocupación");
        verificar("Teatro".equals(servicio.actividad("a-teatro").getNombre())
                && "Auditorio".equals(servicio.actividad("a-teatro").getLugar()), "Se limpian nombres y lugares marcados");
        verificar("Lectura".equals(servicio.actividad("a-lectura").getNombre())
                && "Biblioteca".equals(servicio.actividad("a-lectura").getLugar()), "Se reconocen ambos tipos de sufijo");
        verificar(!servicio.actividad("a-lectura").isActiva(), "Una actividad desactivada conserva su estado");
        verificar("Demostración de robótica".equals(servicio.actividad("a-personal").getNombre())
                && "Salón Demo Central".equals(servicio.actividad("a-personal").getLugar()), "No se borra texto legítimo sin marcador final");
        verificar("Teatro".equals(servicio.misMovimientos().get(0).getConcepto()), "Se actualiza el concepto histórico de la actividad");
        verificar("Ajuste: motivo escrito (demo)".equals(servicio.misMovimientos().get(1).getConcepto()),
                "Los motivos libres de ajustes se conservan literalmente");
        verificar("Mi meta cultural".equals(servicio.misMetas().get(0).getNombre()), "Se actualiza el nombre inicial conocido de la meta");
        verificar("Meta semestral".equals(servicio.misMetas().get(1).getNombre()), "Se limpia un sufijo explícito de meta");
        verificar("Conocer demostraciones artísticas".equals(servicio.misMetas().get(2).getNombre()),
                "Las metas con texto legítimo se conservan");
        servicio.logout();
        Usuario administrador = servicio.login("admin@demo.local", "Admin123!".toCharArray());
        verificar("Administrador".equals(administrador.getNombre()) && "Bienestar".equals(administrador.getPrograma()),
                "Se actualizan nombre y programa inicial del administrador");
        verificar("ADMINISTRADOR".equals(administrador.getRol()) && "u-admin".equals(administrador.getId()),
                "Las credenciales conservan rol e identificador");
        verificar(servicio.estudiantes().size() == 3, "No se insertan cuentas duplicadas durante la migración");
        verificar("Demócrito Rivera".equals(servicio.usuario("u-personal").getNombre())
                && "Estudiante Demo Pérez".equals(servicio.usuario("u-personal-2").getNombre()),
                "Los nombres de personas que no corresponden al nombre inicial exacto permanecen intactos");
        verificar("Ingeniería de demostración".equals(servicio.usuario("u-personal").getPrograma()),
                "Los programas personales no se modifican por contener una palabra parecida");

        DatosSistema actuales = persistencia.cargar();
        verificar(Arrays.equals(serializar(anteriores.inscripciones), serializar(actuales.inscripciones)),
                "Todos los campos de las inscripciones permanecen idénticos");
        verificar(Arrays.equals(serializar(anteriores.solicitudes), serializar(actuales.solicitudes)),
                "Estados, evidencia, observaciones y fechas de solicitudes permanecen idénticos");
        verificar(Arrays.equals(serializar(anteriores.auditoria), serializar(actuales.auditoria)),
                "La auditoría histórica completa permanece idéntica");
        verificar(anteriores.movimientos.size() == actuales.movimientos.size()
                && anteriores.actividades.size() == actuales.actividades.size()
                && anteriores.metas.size() == actuales.metas.size(), "No se eliminan ni duplican registros");
        for (int i = 0; i < anteriores.usuarios.size(); i++) {
            Usuario antes = anteriores.usuarios.get(i), despues = actuales.usuarios.get(i);
            verificar(Arrays.equals(secreto(antes, "sal"), secreto(despues, "sal"))
                    && Arrays.equals(secreto(antes, "claveDerivada"), secreto(despues, "claveDerivada")),
                    "Sal y contraseña protegida se conservan para " + antes.getId());
            verificar(antes.getId().equals(despues.getId()) && antes.getCorreo().equals(despues.getCorreo())
                    && antes.getRol().equals(despues.getRol()) && antes.getSemestre() == despues.getSemestre()
                    && antes.getMetaDunab() == despues.getMetaDunab(), "Los campos de cuenta ajenos al nombre se conservan");
        }
        for (int i = 0; i < anteriores.movimientos.size(); i++) {
            MovimientoDunab antes = anteriores.movimientos.get(i), despues = actuales.movimientos.get(i);
            verificar(antes.getId().equals(despues.getId()) && antes.getEstudianteId().equals(despues.getEstudianteId())
                    && antes.getFecha().equals(despues.getFecha()) && antes.getCantidad() == despues.getCantidad()
                    && antes.getSemestre() == despues.getSemestre() && antes.getCategoria().equals(despues.getCategoria())
                    && antes.getTipo().equals(despues.getTipo()) && antes.getSolicitudId().equals(despues.getSolicitudId()),
                    "Los campos del movimiento ajenos al concepto se conservan");
        }
        for (int i = 0; i < anteriores.actividades.size(); i++) {
            Actividad antes = anteriores.actividades.get(i), despues = actuales.actividades.get(i);
            verificar(antes.getId().equals(despues.getId()) && antes.getFecha().equals(despues.getFecha())
                    && antes.getCupos() == despues.getCupos() && antes.getDunab() == despues.getDunab()
                    && antes.getCategoria().equals(despues.getCategoria()) && antes.getDescripcion().equals(despues.getDescripcion())
                    && antes.isActiva() == despues.isActiva(), "Los campos de actividad ajenos al nombre y lugar se conservan");
        }

        List<Path> respaldos = respaldos(carpeta);
        verificar(respaldos.size() == 1, "La migración crea una copia previa independiente");
        verificar(Arrays.equals(archivoAnterior, Files.readAllBytes(respaldos.get(0))), "El respaldo conserva exactamente el archivo anterior");
        byte[] migrado = Files.readAllBytes(carpeta.resolve("dunab.dat"));
        FileTime fechaControl = FileTime.fromMillis(978307200000L);
        Files.setLastModifiedTime(carpeta.resolve("dunab.dat"), fechaControl);
        ServicioDunab reabierto = new ServicioDunab(carpeta);
        verificar(Files.getLastModifiedTime(carpeta.resolve("dunab.dat")).equals(fechaControl)
                && Arrays.equals(migrado, Files.readAllBytes(carpeta.resolve("dunab.dat"))),
                "Reabrir es idempotente: no reescribe un archivo que ya fue actualizado");
        verificar(respaldos(carpeta).size() == 1, "Reabrir no produce respaldos repetidos");
        reabierto.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        verificar(reabierto.saldo("u-estudiante", 0) == 850 && "Estudiante".equals(reabierto.getActual().getNombre()),
                "Los nombres limpios y saldos persisten al reabrir");

        Path nuevaCarpeta = raiz.resolve("primera-ejecucion");
        ServicioDunab nuevo = new ServicioDunab(nuevaCarpeta);
        nuevo.login("admin@demo.local", "Admin123!".toCharArray());
        for (Actividad actividad : nuevo.actividades("", "Todas")) {
            verificar(!actividad.getNombre().toLowerCase().contains("demo")
                    && !actividad.getLugar().toLowerCase().contains("demo"), "Los datos nuevos usan nombres y lugares sin marcadores");
        }
        verificar("Administrador".equals(nuevo.getActual().getNombre()), "Nombre inicial nuevo del administrador");
        nuevo.logout();
        nuevo.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        verificar("Estudiante".equals(nuevo.getActual().getNombre())
                && "Mi meta cultural".equals(nuevo.misMetas().get(0).getNombre()), "Nombre y meta iniciales nuevos sin marcadores");
        nuevo.logout();
        new ServicioDunab(nuevaCarpeta);
        verificar(respaldos(nuevaCarpeta).isEmpty(), "Los datos nuevos no requieren migración ni respaldo de nombres");
        System.out.println("Pruebas de nombres: " + comprobaciones + " comprobaciones correctas.");
        System.out.println("Datos de prueba independientes: " + raiz.toAbsolutePath());
    }

    private static DatosSistema versionAnterior() {
        DatosSistema datos = new DatosSistema();
        byte[] salEstudiante = SeguridadClave.nuevaSal(), salAdmin = SeguridadClave.nuevaSal(), salPersonal = SeguridadClave.nuevaSal();
        datos.usuarios.add(new Usuario("u-estudiante", "Estudiante Demo", "estudiante@demo.local", "Sistemas", "ESTUDIANTE", 10, 15000,
                salEstudiante, SeguridadClave.derivar("Estudiante123!".toCharArray(), salEstudiante)));
        datos.usuarios.add(new Usuario("u-admin", "Administrador Demo", "admin@demo.local", "Bienestar (demostración)", "ADMINISTRADOR", 12, 10000,
                salAdmin, SeguridadClave.derivar("Admin123!".toCharArray(), salAdmin)));
        datos.usuarios.add(new Usuario("u-personal", "Demócrito Rivera", "democrito@prueba.local", "Ingeniería de demostración", "ESTUDIANTE", 3, 10000,
                salPersonal, SeguridadClave.derivar("Personal123!".toCharArray(), salPersonal)));
        datos.usuarios.add(new Usuario("u-personal-2", "Estudiante Demo Pérez", "perez@prueba.local", "Sistemas", "ESTUDIANTE", 4, 12000,
                salPersonal, SeguridadClave.derivar("Personal123!".toCharArray(), salPersonal)));
        LocalDate fecha = LocalDate.now().minusDays(20);
        datos.actividades.add(new Actividad("a-teatro", "Teatro · demo", "Cultura", "Descripción original.", fecha, "Auditorio (demo)", 800, 8, true));
        datos.actividades.add(new Actividad("a-lectura", "Lectura (demo)", "Cultura", "Actividad desactivada.", fecha.plusDays(1), "Biblioteca · demo", 100, 10, false));
        datos.actividades.add(new Actividad("a-personal", "Demostración de robótica", "Ciencia", "Nombre legítimo.", fecha.plusDays(2), "Salón Demo Central", 50, 4, true));
        datos.inscripciones.add(new Inscripcion("i-teatro", "u-estudiante", "a-teatro", fecha, "ACTIVA"));
        datos.inscripciones.add(new Inscripcion("i-lectura", "u-estudiante", "a-lectura", fecha, "ACTIVA"));
        datos.inscripciones.add(new Inscripcion("i-cancelada", "u-personal", "a-teatro", fecha, "CANCELADA"));
        datos.solicitudes.add(new SolicitudParticipacion("s-aprobada", "u-estudiante", "a-teatro", fecha, 800,
                "DEMO-EVIDENCIA-ORIGINAL", "APROBADA", "Observación histórica original.", 1));
        datos.solicitudes.add(new SolicitudParticipacion("s-pendiente", "u-estudiante", "a-lectura", fecha.plusDays(1), 100,
                "EVIDENCIA-PENDIENTE", "PENDIENTE", "", 10));
        datos.movimientos.add(new MovimientoDunab("m-aprobado", "u-estudiante", fecha, 800, "Teatro · demo", "Cultura", 1, "APROBACION", "s-aprobada"));
        datos.movimientos.add(new MovimientoDunab("m-ajuste", "u-estudiante", fecha.plusDays(1), 50, "Ajuste: motivo escrito (demo)", "Ajuste administrativo", 10, "AJUSTE", ""));
        datos.metas.add(new Meta("meta-inicial", "u-estudiante", "Mi meta cultural de demostración", 2000, "Cultura", 0));
        datos.metas.add(new Meta("meta-marcada", "u-estudiante", "Meta semestral (demo)", 1000, "Todas", 10));
        datos.metas.add(new Meta("meta-personal", "u-estudiante", "Conocer demostraciones artísticas", 1500, "Cultura", 12));
        datos.auditoria.add(new RegistroAuditoria("r-aprobacion", "u-admin", "u-estudiante", fecha,
                "APROBAR_SOLICITUD", 800, "Dato ficticio inicial de demostración.", 0, 800));
        datos.auditoria.add(new RegistroAuditoria("r-ajuste", "u-admin", "u-estudiante", fecha.plusDays(1),
                "AJUSTE_DUNAB", 50, "Motivo histórico (demo)", 800, 850));
        return datos;
    }

    private static List<Path> respaldos(Path carpeta) throws Exception {
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(p -> p.getFileName().toString().startsWith("dunab-antes-nombres-")).collect(Collectors.toList());
        }
    }
    private static byte[] secreto(Usuario usuario, String nombreCampo) throws Exception {
        Field campo = Usuario.class.getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        return (byte[]) campo.get(usuario);
    }
    private static byte[] serializar(Object objeto) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) { salida.writeObject(objeto); }
        return bytes.toByteArray();
    }
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }
}
