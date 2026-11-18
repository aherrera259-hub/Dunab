package javaapplication1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** Pantallas Swing normales: la información y las validaciones viven en el servicio. */
public class PantallasEstudiante {
    private final ServicioDunab servicio;
    private final IntSupplier semestre;
    private final Runnable refrescarTodo;
    private final Consumer<String> navegar;
    private final boolean navegacionLocal;
    private JPanel panelLocal;
    private String seccionLocal;

    public PantallasEstudiante(ServicioDunab servicio, IntSupplier semestre, Runnable refrescarTodo) {
        this(servicio, semestre, refrescarTodo, null);
    }

    public PantallasEstudiante(ServicioDunab servicio, IntSupplier semestre, Runnable refrescarTodo, Consumer<String> navegar) {
        this.servicio = servicio;
        this.semestre = semestre;
        this.navegacionLocal = navegar == null;
        this.navegar = navegacionLocal ? this::navegarLocal : navegar;
        this.refrescarTodo = () -> {
            if (refrescarTodo != null) refrescarTodo.run();
            if (navegacionLocal && seccionLocal != null) crear(seccionLocal);
        };
    }

    public JPanel crear(String seccion) {
        JPanel pagina = crearPagina(seccion);
        if (!navegacionLocal) return pagina;
        if (panelLocal == null) {
            panelLocal = new JPanel(new BorderLayout());
            panelLocal.setOpaque(false);
        }
        seccionLocal = seccion;
        panelLocal.removeAll();
        panelLocal.add(pagina, BorderLayout.CENTER);
        Tema.aplicar(panelLocal);
        panelLocal.revalidate();
        panelLocal.repaint();
        return panelLocal;
    }

    private void navegarLocal(String seccion) {
        crear(seccion);
    }

    private JPanel crearPagina(String seccion) {
        switch (seccion) {
            case "Inicio": return inicio();
            case "Mi perfil": return perfil();
            case "Consultar DUNAB": return consultar();
            case "Actividades y espacios": return actividades();
            case "Inscripciones": return inscripciones();
            case "Registrar participación": return participacion();
            case "Historial de DUNAB": return historial();
            case "Metas personales": return metas();
            case "Top 9": return ranking();
            case "Calculadora de notas": return calculadora();
            case "Estadísticas y reportes": return estadisticas();
            default: throw new IllegalArgumentException("Sección desconocida: " + seccion);
        }
    }

    private JPanel inicio() {
        Usuario usuario = servicio.getActual();
        JPanel pagina = Ui.pagina("¡Hola, " + usuario.getNombre() + "!", "Prototipo académico · Datos locales · Semestre " + semestre.getAsInt());
        JPanel cuerpo = vertical();
        if ("ADMINISTRADOR".equals(usuario.getRol())) {
            cuerpo.add(tarjetas("Estudiantes", servicio.estudiantes().size(), "Solicitudes pendientes", servicio.solicitudesPendientes().size(),
                    "Actividades registradas", servicio.actividades("", "Todas").size()));
            cuerpo.add(aviso("Gestiona las actividades, revisa las solicitudes y registra ajustes justificados desde el menú lateral."));
            cuerpo.add(aviso("Los cambios administrativos se guardan inmediatamente y quedan registrados en la auditoría."));
        } else {
            javax.swing.JButton verActividades = Ui.boton("Ver actividades e inscribirme", () -> navegar.accept("Actividades y espacios"));
            verActividades.setFont(new Font("SansSerif", Font.BOLD, 16));
            verActividades.setPreferredSize(new java.awt.Dimension(310, 48));
            cuerpo.add(acciones(verActividades));
            int historico = servicio.saldo(usuario.getId(), 0);
            int periodo = servicio.saldo(usuario.getId(), semestre.getAsInt());
            int pendientes = 0;
            for (SolicitudParticipacion solicitud : servicio.misSolicitudes()) {
                if (solicitud.getSemestre() == semestre.getAsInt() && "PENDIENTE".equals(solicitud.getEstado())) pendientes++;
            }
            cuerpo.add(tarjetas("DUNAB históricos", historico, "DUNAB del semestre", periodo, "Solicitudes pendientes del semestre", pendientes));
            cuerpo.add(progreso("Meta global de simulación: " + Ui.numero(usuario.getMetaDunab()) + " DUNAB · Faltan "
                    + Ui.numero(Math.max(0, usuario.getMetaDunab() - historico)), historico, usuario.getMetaDunab()));
            cuerpo.add(aviso("La meta es personal y configurable. Corresponde a esta simulación académica; no es una norma oficial de graduación."));
            JTable proximas = Ui.tabla("Actividad próxima", "Categoría", "Fecha", "Lugar", "Cupos libres", "DUNAB posibles");
            List<Actividad> lista = new ArrayList<>(servicio.actividades("", "Todas"));
            Collections.sort(lista, Comparator.comparing(Actividad::getFecha));
            for (Actividad actividad : lista) {
                if (actividad.isActiva() && !actividad.getFecha().isBefore(LocalDate.now())) {
                    fila(proximas, actividad.getNombre(), actividad.getCategoria(), actividad.getFecha(), actividad.getLugar(),
                            servicio.cuposDisponibles(actividad.getId()), actividad.getDunab());
                    if (proximas.getRowCount() == 5) break;
                }
            }
            cuerpo.add(bloqueTabla("Próximas actividades", proximas, 145));
            JTable movimientos = Ui.tabla("Fecha", "Últimos movimientos · semestre " + semestre.getAsInt(), "DUNAB", "Tipo");
            List<MovimientoDunab> ultimos = movimientosOrdenados();
            Collections.reverse(ultimos);
            for (MovimientoDunab movimiento : ultimos) {
                if (movimiento.getSemestre() == semestre.getAsInt()) {
                    fila(movimientos, movimiento.getFecha(), movimiento.getConcepto(), movimiento.getCantidad(), movimiento.getTipo());
                    if (movimientos.getRowCount() == 5) break;
                }
            }
            cuerpo.add(bloqueTabla("Movimientos recientes", movimientos, 145));
        }
        pagina.add(desplazable(cuerpo), BorderLayout.CENTER);
        return pagina;
    }

    private JPanel perfil() {
        Usuario usuario = servicio.getActual();
        JPanel pagina = Ui.pagina("Mi perfil", "Tu información se conserva al cerrar la aplicación. Rol: " + usuario.getRol());
        JTextField nombre = Ui.campo(usuario.getNombre());
        JTextField correo = Ui.campo(usuario.getCorreo());
        JTextField programa = Ui.campo(usuario.getPrograma());
        JComboBox<String> periodo = new JComboBox<>(Semestres.opciones(false));
        periodo.setSelectedIndex(usuario.getSemestre() - 1);
        JSpinner meta = Ui.entero(usuario.getMetaDunab(), 1, Integer.MAX_VALUE);
        JPanel cuerpo = vertical();
        cuerpo.add(formulario("Nombre completo", nombre, "Correo", correo, "Programa académico", programa,
                "Semestre de perfil", periodo, "Meta global de DUNAB", meta));
        cuerpo.add(aviso("La meta utiliza el acumulado histórico. El selector superior del menú permite consultar cada semestre por separado."));
        pagina.add(desplazable(cuerpo), BorderLayout.CENTER);
        pagina.add(acciones(Ui.boton("Guardar perfil", () -> {
            servicio.editarPerfil(nombre.getText(), correo.getText(), programa.getText(), periodo.getSelectedIndex() + 1, Ui.valor(meta));
            Ui.info(pagina, "Perfil actualizado y guardado.");
            refrescarTodo.run();
        })), BorderLayout.SOUTH);
        return pagina;
    }

    private JPanel consultar() {
        Usuario usuario = servicio.getActual();
        int saldo = servicio.saldo(usuario.getId(), 0);
        int objetivo = usuario.getMetaDunab();
        JPanel pagina = Ui.pagina("Mis DUNAB", "Solo se incluyen participaciones aprobadas y ajustes administrativos registrados.");
        JPanel cuerpo = vertical();
        cuerpo.add(tarjetas("Acumulado histórico", saldo, "Meta personal", objetivo, "DUNAB faltantes", Math.max(0, objetivo - saldo)));
        cuerpo.add(progreso("Cumplimiento de la meta global", saldo, objetivo));
        JTable tabla = Ui.tabla("Período", "DUNAB aprobados y ajustes", "Consulta actual");
        for (int i = 1; i <= Semestres.MAXIMO; i++) fila(tabla, "Semestre " + i, servicio.saldo(usuario.getId(), i), i == semestre.getAsInt() ? "Seleccionado" : "");
        cuerpo.add(bloqueTabla("Resumen por semestre", tabla, 260));
        cuerpo.add(aviso("Una inscripción o una solicitud pendiente no suma DUNAB. No existe equivalencia con dinero ni conversión a horas."));
        pagina.add(desplazable(cuerpo), BorderLayout.CENTER);
        return pagina;
    }

    private JPanel actividades() {
        JPanel pagina = Ui.pagina("Actividades y espacios", "1. Selecciona una actividad y pulsa Inscribirme. 2. Participa en su fecha. 3. En Inscripciones, abre Registrar participación para enviar la evidencia.");
        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        JTextField busqueda = Ui.campo("");
        busqueda.setColumns(16);
        JComboBox<String> categoria = categorias();
        categoria.setPreferredSize(new java.awt.Dimension(135, 34));
        JTable tabla = Ui.tabla("Nombre", "Estado", "Categoría", "Fecha", "Lugar", "Cupos libres", "DUNAB posibles");
        tabla.setName("tablaActividades");
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] anchos = {240, 130, 120, 110, 200, 100, 125};
        for (int i = 0; i < anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        List<String> ids = new ArrayList<>();
        List<String> inscritas = new ArrayList<>();
        for (Inscripcion inscripcion : servicio.misInscripciones()) {
            if ("ACTIVA".equals(inscripcion.getEstado())) inscritas.add(inscripcion.getActividadId());
        }
        javax.swing.JButton inscribir = Ui.boton("Inscribirme", () -> {
            servicio.inscribir(Ui.idSeleccionado(tabla, ids));
            Ui.info(pagina, "Inscripción guardada. Ahora se abrirán tus Inscripciones.\n"
                    + "Después de participar en la fecha de la actividad, pulsa Registrar participación para enviar tu evidencia.\n"
                    + "Inscribirte no suma DUNAB. El administrador debe aprobar la solicitud.");
            navegar.accept("Inscripciones");
        });
        inscribir.setName("inscribirmeActividad");
        inscribir.setPreferredSize(new java.awt.Dimension(135, 48));
        inscribir.setEnabled(false);
        JTextArea resumen = texto("Selecciona una actividad para consultar su estado e inscribirte.");
        resumen.setRows(2);
        JTextArea detalle = texto("Selecciona una fila para leer la descripción.");
        detalle.setRows(3);
        Runnable cargar = () -> {
            limpiar(tabla);
            ids.clear();
            inscribir.setEnabled(false);
            resumen.setText("No hay actividades disponibles para inscribirte con estos filtros.");
            tabla.getRowSorter().setSortKeys(null);
            List<Actividad> encontradas = new ArrayList<>(servicio.actividades(busqueda.getText(), seleccionado(categoria)));
            Collections.sort(encontradas, Comparator.comparingInt((Actividad actividad) ->
                    "Disponible".equals(estadoActividad(actividad, inscritas)) ? 0 : 1)
                    .thenComparing(Actividad::getFecha).thenComparing(Actividad::getNombre));
            int primeraDisponible = -1;
            for (Actividad actividad : encontradas) {
                if (!actividad.isActiva()) continue;
                if (primeraDisponible < 0 && "Disponible".equals(estadoActividad(actividad, inscritas))) primeraDisponible = ids.size();
                ids.add(actividad.getId());
                fila(tabla, actividad.getNombre(), estadoActividad(actividad, inscritas), actividad.getCategoria(), actividad.getFecha(), actividad.getLugar(),
                        servicio.cuposDisponibles(actividad.getId()), actividad.getDunab());
            }
            detalle.setText(ids.isEmpty() ? "No hay actividades disponibles para esta búsqueda." : "Selecciona una fila para leer la descripción.");
            if (primeraDisponible >= 0) {
                int filaVista = tabla.convertRowIndexToView(primeraDisponible);
                tabla.setRowSelectionInterval(filaVista, filaVista);
                tabla.scrollRectToVisible(tabla.getCellRect(filaVista, 0, true));
            } else if (tabla.getRowCount() > 0) tabla.setRowSelectionInterval(0, 0);
        };
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (evento.getValueIsAdjusting()) return;
            if (tabla.getSelectedRow() >= 0) {
                Actividad actividad = servicio.actividad(Ui.idSeleccionado(tabla, ids));
                String estado = estadoActividad(actividad, inscritas);
                resumen.setText(actividad.getNombre() + " · " + actividad.getFecha() + " · " + estado + "\n"
                        + ("Disponible".equals(estado) ? "Pulsa Inscribirme para reservar tu cupo. Reconocimiento posible: " + actividad.getDunab() + " DUNAB."
                        : "Ya inscrito".equals(estado) ? "Consulta la inscripción y continúa desde Inscripciones → Registrar participación."
                        : "Finalizada".equals(estado) ? "Esta actividad ya finalizó y no acepta nuevas inscripciones."
                        : "Los cupos están ocupados. Selecciona otra actividad disponible."));
                detalle.setText(actividad.getNombre() + "\n" + actividad.getDescripcion() + "\nDUNAB posibles: " + actividad.getDunab()
                        + ". Cupos disponibles: " + servicio.cuposDisponibles(actividad.getId()) + ". Estado: " + estadoActividad(actividad, inscritas) + ".\n"
                        + (inscritas.contains(actividad.getId()) ? "Ya estás inscrito. Después de participar, entra en Inscripciones → Registrar participación y envía tu evidencia."
                        : "Su reconocimiento requiere inscripción, participación y aprobación administrativa. Las actividades finalizadas no aceptan nuevas inscripciones."));
                inscribir.setEnabled("Disponible".equals(estado));
            } else inscribir.setEnabled(false);
        });
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Nombre"));
        filtros.add(busqueda);
        filtros.add(new JLabel("Categoría"));
        filtros.add(categoria);
        filtros.add(Ui.boton("Buscar", cargar));
        busqueda.addActionListener(evento -> cargar.run());
        categoria.addActionListener(evento -> cargar.run());
        JPanel llamado = new JPanel(new BorderLayout(10, 0));
        llamado.setOpaque(false);
        llamado.add(inscribir, BorderLayout.WEST);
        llamado.add(resumen, BorderLayout.CENTER);
        JPanel controles = new JPanel(new BorderLayout(0, 10));
        controles.setOpaque(false);
        controles.add(llamado, BorderLayout.NORTH);
        controles.add(filtros, BorderLayout.CENTER);
        cuerpo.add(controles, BorderLayout.NORTH);
        cuerpo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        cuerpo.add(new JScrollPane(detalle), BorderLayout.SOUTH);
        pagina.add(cuerpo, BorderLayout.CENTER);
        cargar.run();
        return pagina;
    }

    private JPanel inscripciones() {
        JPanel pagina = Ui.pagina("Mis inscripciones", "Después de asistir, pulsa Registrar participación y envía tu evidencia. Los DUNAB se suman únicamente cuando el administrador aprueba la solicitud.");
        JTable tabla = Ui.tabla("Actividad", "Fecha de actividad", "Participación", "Inscripción", "Fecha de inscripción", "Lugar");
        tabla.setName("tablaInscripciones");
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] anchos = {240, 125, 240, 120, 145, 180};
        for (int i = 0; i < anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        List<String> ids = new ArrayList<>();
        List<SolicitudParticipacion> solicitudes = servicio.misSolicitudes();
        for (Inscripcion inscripcion : servicio.misInscripciones()) {
            Actividad actividad = servicio.actividad(inscripcion.getActividadId());
            ids.add(inscripcion.getId());
            fila(tabla, actividad.getNombre(), actividad.getFecha(), estadoParticipacion(inscripcion, actividad, solicitudes),
                    inscripcion.getEstado(), inscripcion.getFecha(), actividad.getLugar());
        }
        JPanel cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.setOpaque(false);
        cuerpo.add(acciones(Ui.boton("Registrar participación", () -> navegar.accept("Registrar participación")),
                Ui.boton("Buscar actividades", () -> navegar.accept("Actividades y espacios"))), BorderLayout.NORTH);
        cuerpo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        cuerpo.add(aviso("Sin solicitud: ya puedes enviar evidencia si la fecha llegó. Futura: espera a participar. Pendiente: el administrador debe revisar. "
                + "Aprobada: ya se reconocieron los DUNAB. Rechazada: revisa la observación y puedes volver a solicitar."), BorderLayout.SOUTH);
        pagina.add(cuerpo, BorderLayout.CENTER);
        pagina.add(acciones(Ui.boton("Cancelar inscripción seleccionada", () -> {
            String id = Ui.idSeleccionado(tabla, ids);
            if (JOptionPane.showConfirmDialog(pagina, "¿Cancelar esta inscripción y liberar el cupo?", "Cancelar inscripción",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                servicio.cancelarInscripcion(id);
                refrescarTodo.run();
            }
        })), BorderLayout.SOUTH);
        return pagina;
    }

    private JPanel participacion() {
        JPanel pagina = Ui.pagina("Registrar participación", "Selecciona cualquiera de tus inscripciones activas para consultar su estado. Puedes enviar evidencia después de participar, desde la fecha programada y sin una solicitud pendiente o aprobada.");
        JPanel cuerpo = vertical();
        JComboBox<Actividad> actividad = new JComboBox<>();
        actividad.setName("actividadParticipacion");
        actividad.setPreferredSize(new java.awt.Dimension(420, 34));
        actividad.setMinimumSize(new java.awt.Dimension(100, 34));
        List<String> incluidos = new ArrayList<>();
        List<Inscripcion> inscripciones = servicio.misInscripciones();
        List<SolicitudParticipacion> solicitudesGuardadas = servicio.misSolicitudes();
        int primeraElegible = -1;
        int primeraSinSolicitud = -1;
        for (Inscripcion inscripcion : inscripciones) {
            if ("ACTIVA".equals(inscripcion.getEstado()) && !incluidos.contains(inscripcion.getActividadId())) {
                Actividad inscrita = servicio.actividad(inscripcion.getActividadId());
                if (primeraElegible < 0 && puedeSolicitar(inscrita, solicitudesGuardadas)) primeraElegible = actividad.getItemCount();
                SolicitudParticipacion anterior = ultimaSolicitud(inscrita.getId(), solicitudesGuardadas);
                if (primeraSinSolicitud < 0 && (anterior == null || "RECHAZADA".equals(anterior.getEstado()))) {
                    primeraSinSolicitud = actividad.getItemCount();
                }
                actividad.addItem(inscrita);
                incluidos.add(inscripcion.getActividadId());
            }
        }
        actividad.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice, boolean seleccionada, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionada, foco);
                if (valor instanceof Actividad) {
                    Actividad inscrita = (Actividad) valor;
                    setText(inscrita.getNombre() + " · " + inscrita.getFecha() + " · " + estadoSolicitud(inscrita, solicitudesGuardadas));
                } else setText("Sin inscripciones activas");
                return this;
            }
        });
        JTextField fecha = Ui.campo(LocalDate.now().toString());
        JSpinner cantidad = Ui.entero(1, 1, Integer.MAX_VALUE);
        JTextField evidencia = Ui.campo("");
        evidencia.setName("evidenciaParticipacion");
        JComboBox<String> periodo = new JComboBox<>(Semestres.opciones(false));
        periodo.setSelectedIndex(semestre.getAsInt() - 1);
        JTextArea estado = texto("");
        estado.setName("estadoParticipacion");
        estado.setRows(4);
        estado.setAlignmentX(Component.LEFT_ALIGNMENT);
        estado.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 130));
        javax.swing.JButton enviar = Ui.boton("Enviar solicitud de reconocimiento", () -> {
            Actividad seleccionada = (Actividad) actividad.getSelectedItem();
            if (seleccionada == null) throw new IllegalArgumentException("Primero inscríbete en una actividad.");
            servicio.solicitar(seleccionada.getId(), fecha(fecha), Ui.valor(cantidad), evidencia.getText(), periodo.getSelectedIndex() + 1);
            Ui.info(pagina, "Solicitud guardada como PENDIENTE. No se sumarán DUNAB hasta su aprobación.");
            refrescarTodo.run();
        });
        enviar.setName("enviarParticipacion");
        Runnable sugerir = () -> {
            Actividad seleccionada = (Actividad) actividad.getSelectedItem();
            boolean elegible = seleccionada != null && puedeSolicitar(seleccionada, solicitudesGuardadas);
            enviar.setEnabled(elegible);
            fecha.setEditable(elegible);
            fecha.setEnabled(elegible);
            cantidad.setEnabled(elegible);
            evidencia.setEditable(elegible);
            evidencia.setEnabled(elegible);
            periodo.setEnabled(elegible);
            evidencia.setText("");
            if (seleccionada != null) {
                cantidad.setValue(seleccionada.getDunab());
                fecha.setText(seleccionada.getFecha().toString());
                SolicitudParticipacion ultima = ultimaSolicitud(seleccionada.getId(), solicitudesGuardadas);
                if (ultima != null && "PENDIENTE".equals(ultima.getEstado())) {
                    estado.setText("Solicitud PENDIENTE de revisión. Tu inscripción está registrada y ya enviaste una solicitud para esta actividad.\n"
                            + "El administrador debe aprobarla o rechazarla. No puedes enviarla de nuevo mientras esté pendiente y todavía no suma DUNAB.");
                } else if (ultima != null && "APROBADA".equals(ultima.getEstado())) {
                    estado.setText("Participación APROBADA. Ya se reconocieron " + Ui.numero(ultima.getCantidad()) + " DUNAB por esta actividad.\n"
                            + "Puedes consultarlos en el historial. No puedes volver a solicitar el mismo reconocimiento.");
                } else if (seleccionada.getFecha().isAfter(LocalDate.now())) {
                    estado.setText("Inscripción confirmada para una fecha FUTURA: " + seleccionada.getFecha() + ".\n"
                            + "Puedes registrar la participación desde " + seleccionada.getFecha() + ", después de asistir. Hasta entonces el envío está deshabilitado.");
                } else if (ultima != null && "RECHAZADA".equals(ultima.getEstado())) {
                    estado.setText("Solicitud anterior RECHAZADA. Puedes corregir la evidencia y volver a enviar una solicitud.\n"
                            + "Observación del administrador: " + ultima.getObservacion());
                } else {
                    estado.setText("Inscripción activa SIN SOLICITUD. La fecha programada ya llegó: " + seleccionada.getFecha() + ".\n"
                            + "Si ya participaste, ingresa la evidencia y envía tu solicitud. Los DUNAB se sumarán únicamente después de la aprobación del administrador.");
                }
            } else estado.setText(inscripciones.isEmpty()
                    ? "Todavía no tienes inscripciones. Abre Inscripciones → Buscar actividades para inscribirte en una actividad."
                    : "No tienes inscripciones ACTIVAS. Las inscripciones canceladas no permiten registrar participación. Abre Inscripciones → Buscar actividades para comenzar otra actividad.");
        };
        actividad.addActionListener(evento -> sugerir.run());
        if (primeraElegible >= 0) actividad.setSelectedIndex(primeraElegible);
        else if (primeraSinSolicitud >= 0) actividad.setSelectedIndex(primeraSinSolicitud);
        actividad.setEnabled(actividad.getItemCount() > 0);
        sugerir.run();
        cuerpo.add(estado);
        cuerpo.add(formulario("Actividad inscrita · fecha · estado", actividad, "Fecha programada (AAAA-MM-DD)", fecha,
                "DUNAB solicitados", cantidad, "Evidencia o código de referencia", evidencia, "Semestre de reconocimiento", periodo));
        JTable solicitudes = Ui.tabla("Actividad", "Fecha", "Solicitados", "Semestre", "Estado", "Observación");
        for (SolicitudParticipacion solicitud : servicio.misSolicitudes()) {
            if (solicitud.getSemestre() == semestre.getAsInt()) {
                fila(solicitudes, Ui.nombreActividad(servicio, solicitud.getActividadId()), solicitud.getFecha(), solicitud.getCantidad(),
                        solicitud.getSemestre(), solicitud.getEstado(), solicitud.getObservacion());
            }
        }
        cuerpo.add(bloqueTabla("Mis solicitudes del semestre " + semestre.getAsInt(), solicitudes, 200));
        pagina.add(desplazable(cuerpo), BorderLayout.CENTER);
        pagina.add(acciones(enviar, Ui.boton("Volver a mis inscripciones", () -> navegar.accept("Inscripciones"))), BorderLayout.SOUTH);
        return pagina;
    }

    private JPanel historial() {
        JPanel pagina = Ui.pagina("Historial de DUNAB", "Los saldos de las filas son históricos; los filtros no alteran el acumulado real.");
        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        JComboBox<String> periodo = periodos();
        periodo.setSelectedIndex(semestre.getAsInt());
        JComboBox<String> categoria = categorias();
        JTable tabla = Ui.tabla("Fecha", "Actividad o motivo", "DUNAB reconocidos", "DUNAB solicitados", "Estado", "Semestre", "Saldo histórico", "Categoría", "Observación");
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] anchos = {110, 260, 145, 140, 190, 90, 140, 170, 300};
        for (int i = 0; i < anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        List<FilaHistorial> registros = new ArrayList<>();
        int saldo = 0;
        for (MovimientoDunab movimiento : movimientosOrdenados()) {
            saldo += movimiento.getCantidad();
            registros.add(new FilaHistorial(movimiento.getFecha(), movimiento.getConcepto(), movimiento.getCantidad(), 0,
                    "RECONOCIDO · " + movimiento.getTipo(), movimiento.getSemestre(), saldo, movimiento.getCategoria(), ""));
        }
        for (SolicitudParticipacion solicitud : servicio.misSolicitudes()) {
            if (!"APROBADA".equals(solicitud.getEstado())) {
                Actividad actividad = servicio.actividad(solicitud.getActividadId());
                registros.add(new FilaHistorial(solicitud.getFecha(), actividad.getNombre(), 0, solicitud.getCantidad(),
                        solicitud.getEstado(), solicitud.getSemestre(), null, actividad.getCategoria(), solicitud.getObservacion()));
            }
        }
        Collections.sort(registros, Comparator.comparing(registro -> registro.fecha));
        Runnable cargar = () -> {
            limpiar(tabla);
            for (FilaHistorial registro : registros) {
                if (periodo.getSelectedIndex() != 0 && registro.semestre != periodo.getSelectedIndex()) continue;
                if (!"Todas".equals(seleccionado(categoria)) && !registro.categoria.equals(seleccionado(categoria))) continue;
                fila(tabla, registro.fecha, registro.concepto, registro.cantidad, registro.solicitada == 0 ? "—" : registro.solicitada,
                        registro.estado, registro.semestre, registro.saldo == null ? "Sin abono" : registro.saldo, registro.categoria, registro.observacion);
            }
        };
        periodo.addActionListener(evento -> cargar.run());
        categoria.addActionListener(evento -> cargar.run());
        cuerpo.add(formulario("Semestre", periodo, "Categoría", categoria), BorderLayout.NORTH);
        cuerpo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        cuerpo.add(aviso("PENDIENTE y RECHAZADA reconocen 0 DUNAB. Las aprobaciones aparecen una sola vez como movimientos."), BorderLayout.SOUTH);
        pagina.add(cuerpo, BorderLayout.CENTER);
        cargar.run();
        return pagina;
    }

    private JPanel metas() {
        JPanel pagina = Ui.pagina("Metas personales", "Define un objetivo de DUNAB global o por categoría. El progreso se calcula con tus movimientos reales.");
        JTable tabla = Ui.tabla("Nombre", "Categoría", "Período", "Objetivo DUNAB", "DUNAB alcanzados", "Cumplimiento");
        List<Meta> metas = servicio.misMetas();
        List<String> ids = new ArrayList<>();
        for (Meta meta : metas) {
            int avance = servicio.progresoMeta(meta);
            ids.add(meta.getId());
            fila(tabla, meta.getNombre(), meta.getCategoria(), meta.getSemestre() == 0 ? "Histórico" : "Semestre " + meta.getSemestre(),
                    meta.getObjetivo(), avance, porcentaje(avance, meta.getObjetivo()));
        }
        JProgressBar barra = barra(0, 1);
        barra.setString("Selecciona una meta para consultar su progreso");
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                Meta meta = metas.get(tabla.convertRowIndexToModel(tabla.getSelectedRow()));
                int avance = servicio.progresoMeta(meta);
                barra.setMaximum(meta.getObjetivo());
                barra.setValue(Math.max(0, Math.min(avance, meta.getObjetivo())));
                barra.setString(Ui.numero(avance) + " / " + Ui.numero(meta.getObjetivo()) + " DUNAB · " + porcentaje(avance, meta.getObjetivo()));
            }
        });
        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setOpaque(false);
        cuerpo.add(new JScrollPane(tabla), BorderLayout.CENTER);
        cuerpo.add(barra, BorderLayout.SOUTH);
        pagina.add(cuerpo, BorderLayout.CENTER);
        pagina.add(acciones(Ui.boton("Crear meta", () -> editarMeta(pagina, null)), Ui.boton("Editar seleccionada", () -> {
            Ui.idSeleccionado(tabla, ids);
            editarMeta(pagina, metas.get(tabla.convertRowIndexToModel(tabla.getSelectedRow())));
        }), Ui.boton("Eliminar seleccionada", () -> {
            String id = Ui.idSeleccionado(tabla, ids);
            if (JOptionPane.showConfirmDialog(pagina, "¿Eliminar esta meta personal?", "Eliminar meta", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                servicio.eliminarMeta(id);
                refrescarTodo.run();
            }
        })), BorderLayout.SOUTH);
        return pagina;
    }

    private void editarMeta(Component padre, Meta meta) {
        JTextField nombre = Ui.campo(meta == null ? "" : meta.getNombre());
        JSpinner objetivo = Ui.entero(meta == null ? 1000 : meta.getObjetivo(), 1, Integer.MAX_VALUE);
        JComboBox<String> categoria = categorias();
        JComboBox<String> periodo = periodos();
        if (meta != null) {
            categoria.setSelectedItem(meta.getCategoria());
            periodo.setSelectedIndex(meta.getSemestre());
        } else periodo.setSelectedIndex(semestre.getAsInt());
        JPanel formulario = formulario("Nombre de la meta", nombre, "Objetivo DUNAB", objetivo, "Categoría", categoria, "Período", periodo);
        while (JOptionPane.showConfirmDialog(padre, formulario, meta == null ? "Crear meta" : "Editar meta",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                servicio.guardarMeta(meta == null ? null : meta.getId(), nombre.getText(), Ui.valor(objetivo), seleccionado(categoria), periodo.getSelectedIndex());
                refrescarTodo.run();
                return;
            } catch (RuntimeException error) {
                JOptionPane.showMessageDialog(padre, error.getMessage(), "Revisa los datos", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel ranking() {
        JPanel pagina = Ui.pagina("Top 9 de estudiantes", "Ranking calculado a partir de DUNAB aprobados y ajustes del semestre " + semestre.getAsInt() + ".");
        JTable tabla = Ui.tabla("Puesto", "Estudiante", "DUNAB del semestre");
        int puesto = 1;
        for (Usuario estudiante : servicio.ranking(semestre.getAsInt())) {
            fila(tabla, puesto++, estudiante.getNombre(), servicio.saldoRanking(estudiante.getId(), semestre.getAsInt()));
        }
        pagina.add(new JScrollPane(tabla), BorderLayout.CENTER);
        pagina.add(aviso("Se muestran hasta nueve estudiantes registrados. Solo se publican nombre y acumulado; no se muestran correos ni evidencias."), BorderLayout.SOUTH);
        return pagina;
    }

    private JPanel calculadora() {
        JPanel pagina = Ui.pagina("Calculadora de notas", "Elige cuántas evaluaciones tienes y el porcentaje de cada una. Esta herramienta es independiente de los DUNAB.");
        JSpinner cantidad = Ui.entero(3, 1, 30);
        JSpinner pendiente = Ui.entero(3, 1, 3);
        JTextField minima = Ui.campo("3.0");
        minima.setColumns(5);
        cantidad.setPreferredSize(new java.awt.Dimension(75, 30));
        pendiente.setPreferredSize(new java.awt.Dimension(75, 30));
        JTextArea resultado = texto("Edita las notas y sus porcentajes. Para buscar una nota necesaria, selecciona su número de fila y deja esa nota vacía.");
        resultado.setRows(4);
        JLabel total = new JLabel("Total de porcentajes: 100,00 %");
        total.setFont(new Font("SansSerif", Font.BOLD, 14));
        DefaultTableModel modelo = new DefaultTableModel(new String[]{"Evaluación", "Nota (0 a 5)", "Peso %"}, 0);
        JTable tabla = Ui.tabla("Evaluación", "Nota (0 a 5)", "Peso %");
        tabla.setAutoCreateRowSorter(false);
        tabla.setRowSorter(null);
        tabla.setModel(modelo);
        tabla.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tabla.setName("tablaCalculadora");
        cantidad.setName("cantidadNotas");
        pendiente.setName("evaluacionPendiente");
        minima.setName("notaMinima");
        for (int i = 0; i < 3; i++) modelo.addRow(new Object[]{"Evaluación " + (i + 1), "", i == 2 ? "40" : "30"});
        Runnable invalidar = () -> {
            resultado.setText("Los datos cambiaron. Vuelve a calcular para obtener un resultado actualizado.");
            actualizarTotal(modelo, total);
        };
        modelo.addTableModelListener(evento -> invalidar.run());
        pendiente.addChangeListener(evento -> invalidar.run());
        minima.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent evento) { invalidar.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent evento) { invalidar.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent evento) { invalidar.run(); }
        });
        javax.swing.JCheckBox repartir = new javax.swing.JCheckBox("Repartir el 100 % al aplicar", true);
        repartir.setOpaque(false);
        javax.swing.JButton aplicar = Ui.boton("Aplicar cantidad", () -> {
            terminarEdicion(tabla);
            int filas = Ui.valor(cantidad);
            int anteriores = modelo.getRowCount();
            if (filas < anteriores && JOptionPane.showConfirmDialog(pagina,
                    "Se quitarán las últimas " + (anteriores - filas) + " evaluaciones. ¿Continuar?", "Reducir cantidad",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                cantidad.setValue(anteriores);
                return;
            }
            if (filas < anteriores) modelo.setRowCount(filas);
            for (int i = anteriores; i < filas; i++) modelo.addRow(new Object[]{"Evaluación " + (i + 1), "", ""});
            int elegida = Ui.valor(pendiente);
            ((javax.swing.SpinnerNumberModel) pendiente.getModel()).setMaximum(filas);
            pendiente.setValue(Math.min(elegida, filas));
            if (repartir.isSelected()) repartirPesos(modelo);
            invalidar.run();
        });
        javax.swing.JButton repartirBoton = Ui.boton("Repartir 100 %", () -> {
            terminarEdicion(tabla);
            repartirPesos(modelo);
        });
        cantidad.addChangeListener(evento -> {
            terminarEdicion(tabla);
            resultado.setText("Cantidad elegida: " + Ui.valor(cantidad) + ". Pulsa Aplicar cantidad para cambiar la tabla.");
        });
        JPanel configuracion = vertical();
        configuracion.add(acciones(new JLabel("Cantidad de notas (1 a 30)"), cantidad, aplicar, repartir));
        configuracion.add(acciones(new JLabel("Nota mínima deseada"), minima, new JLabel("Fila de evaluación pendiente"), pendiente, repartirBoton));
        configuracion.add(aviso("La casilla reparte los pesos al aplicar y conserva los nombres y notas existentes. Desmárcala para conservar también los pesos. "
                + "Cada porcentaje debe ser mayor que 0 y todos deben sumar 100 %. Puedes usar coma o punto decimal."));
        JPanel cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.setOpaque(false);
        cuerpo.add(configuracion, BorderLayout.NORTH);
        JScrollPane notasScroll = new JScrollPane(tabla);
        notasScroll.setColumnHeaderView(tabla.getTableHeader());
        cuerpo.add(notasScroll, BorderLayout.CENTER);
        JPanel resultados = new JPanel(new BorderLayout(0, 8));
        resultados.setOpaque(false);
        resultados.add(total, BorderLayout.NORTH);
        resultados.add(new JScrollPane(resultado), BorderLayout.CENTER);
        cuerpo.add(resultados, BorderLayout.SOUTH);
        pagina.add(cuerpo, BorderLayout.CENTER);
        pagina.add(acciones(Ui.boton("Calcular nota final", () -> {
            terminarEdicion(tabla);
            comprobarCantidadAplicada(cantidad, modelo);
            double[] notas = leerNotas(modelo, -1);
            double[] pesos = leerPesos(modelo);
            double nota = CalculadoraNotas.ponderada(notas, pesos);
            double objetivo = nota(minima);
            resultado.setText(String.format(Locale.forLanguageTag("es-CO"),
                    "Nota final estimada: %.2f\nMeta: %.2f · %s\nSe sumó cada nota multiplicada por su porcentaje y dividida entre 100.",
                    nota, objetivo, nota + 0.0000001 >= objetivo ? "Alcanzas la nota mínima deseada." : "No alcanzas la nota mínima deseada."));
        }), Ui.boton("Calcular nota necesaria", () -> {
            terminarEdicion(tabla);
            comprobarCantidadAplicada(cantidad, modelo);
            int indice = Ui.valor(pendiente) - 1;
            double necesaria = CalculadoraNotas.necesaria(leerNotas(modelo, indice), leerPesos(modelo), indice, nota(minima));
            String evaluacion = String.valueOf(modelo.getValueAt(indice, 0));
            if (necesaria <= 0) resultado.setText("Ya alcanzas la mínima con las demás evaluaciones. Necesitas 0,00 en " + evaluacion + ".");
            else if (necesaria > 5.0000001) resultado.setText(String.format(Locale.forLanguageTag("es-CO"),
                    "Necesitarías %.2f en %s.\nNo es matemáticamente posible alcanzar esa meta con una nota máxima de 5.", necesaria, evaluacion));
            else resultado.setText(String.format(Locale.forLanguageTag("es-CO"),
                    "Necesitas %.2f en %s.\nSí es matemáticamente posible. Se redondea hacia arriba a dos decimales para alcanzar la mínima.",
                    Math.min(5, Math.ceil(necesaria * 100 - 0.000000001) / 100), evaluacion));
        })), BorderLayout.SOUTH);
        actualizarTotal(modelo, total);
        return pagina;
    }

    private static void terminarEdicion(JTable tabla) {
        if (tabla.isEditing() && !tabla.getCellEditor().stopCellEditing()) {
            throw new IllegalArgumentException("Termina de editar la celda antes de continuar.");
        }
    }

    private static void comprobarCantidadAplicada(JSpinner cantidad, DefaultTableModel modelo) {
        if (Ui.valor(cantidad) != modelo.getRowCount()) throw new IllegalArgumentException("Pulsa Aplicar cantidad antes de calcular.");
    }

    private static void repartirPesos(DefaultTableModel modelo) {
        double[] pesos = CalculadoraNotas.repartirPorcentajes(modelo.getRowCount());
        for (int i = 0; i < pesos.length; i++) modelo.setValueAt(String.valueOf(pesos[i]), i, 2);
    }

    private static double[] leerPesos(DefaultTableModel modelo) {
        double[] pesos = new double[modelo.getRowCount()];
        for (int i = 0; i < pesos.length; i++) pesos[i] = decimal(modelo.getValueAt(i, 2), "porcentaje de la fila " + (i + 1));
        return pesos;
    }

    private static double[] leerNotas(DefaultTableModel modelo, int ignorar) {
        double[] notas = new double[modelo.getRowCount()];
        for (int i = 0; i < notas.length; i++) {
            if (i != ignorar) notas[i] = decimal(modelo.getValueAt(i, 1), "nota de la fila " + (i + 1));
        }
        return notas;
    }

    private static double decimal(Object texto, String nombre) {
        try {
            double valor = Double.parseDouble(String.valueOf(texto).trim().replace(',', '.'));
            if (!Double.isFinite(valor)) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Escribe un número válido para " + nombre + ".");
        }
    }

    private static void actualizarTotal(DefaultTableModel modelo, JLabel etiqueta) {
        try {
            double suma = 0;
            boolean validos = true;
            for (double peso : leerPesos(modelo)) {
                suma += peso;
                if (peso <= 0 || peso > 100) validos = false;
            }
            boolean correcto = validos && Math.abs(suma - 100) <= 0.000001;
            etiqueta.setText(String.format(Locale.forLanguageTag("es-CO"), "Total de porcentajes: %.2f %% · %s", suma,
                    correcto ? "Listo para calcular" : "Debe sumar 100 % y cada peso ser mayor que 0"));
            etiqueta.setForeground(correcto ? Ui.morado : Ui.marron);
        } catch (IllegalArgumentException error) {
            etiqueta.setText("Total de porcentajes: incompleto. Ingresa un porcentaje válido en cada fila.");
            etiqueta.setForeground(Ui.marron);
        }
    }

    private JPanel estadisticas() {
        JPanel pagina = Ui.pagina("Estadísticas y reportes", "Resultados del semestre " + semestre.getAsInt() + " obtenidos de tus registros guardados.");
        int total = servicio.saldo(servicio.getActual().getId(), semestre.getAsInt());
        int pendientes = 0;
        int aprobadas = 0;
        int rechazadas = 0;
        for (SolicitudParticipacion solicitud : servicio.misSolicitudes()) {
            if (solicitud.getSemestre() != semestre.getAsInt()) continue;
            if ("PENDIENTE".equals(solicitud.getEstado())) pendientes++;
            if ("APROBADA".equals(solicitud.getEstado())) aprobadas++;
            if ("RECHAZADA".equals(solicitud.getEstado())) rechazadas++;
        }
        JPanel cuerpo = vertical();
        cuerpo.add(tarjetas("DUNAB del semestre", total, "Solicitudes pendientes", pendientes, "Solicitudes aprobadas", aprobadas));
        cuerpo.add(aviso("Solicitudes rechazadas del semestre: " + rechazadas + ". Los ajustes administrativos se incluyen en los totales de DUNAB."));
        JTable periodos = Ui.tabla("Semestre", "DUNAB aprobados y ajustes");
        for (int i = 1; i <= Semestres.MAXIMO; i++) fila(periodos, "Semestre " + i, servicio.saldo(servicio.getActual().getId(), i));
        JTable categorias = Ui.tabla("Categoría", "DUNAB del semestre");
        JTable evolucion = Ui.tabla("Fecha", "Concepto", "Variación DUNAB", "Acumulado del semestre");
        Map<String, Integer> porCategoria = new LinkedHashMap<>();
        int acumulado = 0;
        LocalDate primera = null;
        for (MovimientoDunab movimiento : movimientosOrdenados()) {
            if (movimiento.getSemestre() != semestre.getAsInt()) continue;
            if (primera == null) primera = movimiento.getFecha();
            acumulado += movimiento.getCantidad();
            porCategoria.put(movimiento.getCategoria(), porCategoria.getOrDefault(movimiento.getCategoria(), 0) + movimiento.getCantidad());
            fila(evolucion, movimiento.getFecha(), movimiento.getConcepto(), movimiento.getCantidad(), acumulado);
        }
        for (Map.Entry<String, Integer> entrada : porCategoria.entrySet()) fila(categorias, entrada.getKey(), entrada.getValue());
        if (primera == null) cuerpo.add(aviso("Aún no hay movimientos en este semestre. Promedio semanal: 0. Promedio mensual: 0."));
        else {
            long dias = Math.max(1, ChronoUnit.DAYS.between(primera, LocalDate.now()) + 1);
            long semanas = Math.max(1, (dias + 6) / 7);
            long meses = Math.max(1, ChronoUnit.MONTHS.between(YearMonth.from(primera), YearMonth.now()) + 1);
            cuerpo.add(aviso(String.format(Locale.forLanguageTag("es-CO"),
                    "Promedio semanal: %.2f DUNAB (%d semanas iniciadas). Promedio mensual: %.2f DUNAB (%d meses calendario).\n"
                            + "Se divide el total neto del semestre entre el tiempo transcurrido desde su primer movimiento (%s) hasta hoy (%s).",
                    total / (double) semanas, semanas, total / (double) meses, meses, primera, LocalDate.now())));
        }
        JTabbedPane tablas = new JTabbedPane();
        tablas.setAlignmentX(Component.LEFT_ALIGNMENT);
        tablas.addTab("Por semestre", new JScrollPane(periodos));
        tablas.addTab("Por categoría", new JScrollPane(categorias));
        tablas.addTab("Evolución", new JScrollPane(evolucion));
        tablas.setPreferredSize(new java.awt.Dimension(650, 300));
        cuerpo.add(tablas);
        pagina.add(desplazable(cuerpo), BorderLayout.CENTER);
        return pagina;
    }

    private List<MovimientoDunab> movimientosOrdenados() {
        List<MovimientoDunab> movimientos = new ArrayList<>(servicio.misMovimientos());
        // El orden estable conserva el orden de registro para movimientos del mismo día.
        Collections.sort(movimientos, Comparator.comparing(MovimientoDunab::getFecha));
        return movimientos;
    }

    private String estadoActividad(Actividad actividad, List<String> inscritas) {
        if (actividad.getFecha().isBefore(LocalDate.now())) return "Finalizada";
        if (inscritas.contains(actividad.getId())) return "Ya inscrito";
        if (servicio.cuposDisponibles(actividad.getId()) <= 0) return "Sin cupos";
        return "Disponible";
    }

    private static String estadoParticipacion(Inscripcion inscripcion, Actividad actividad, List<SolicitudParticipacion> solicitudes) {
        if (!"ACTIVA".equals(inscripcion.getEstado())) return "Inscripción cancelada";
        SolicitudParticipacion ultima = ultimaSolicitud(actividad.getId(), solicitudes);
        if (ultima != null) {
            if ("APROBADA".equals(ultima.getEstado())) return "Aprobada · " + Ui.numero(ultima.getCantidad()) + " DUNAB";
            if ("PENDIENTE".equals(ultima.getEstado())) return "Pendiente de revisión";
            if ("RECHAZADA".equals(ultima.getEstado())) return "Rechazada · puedes reenviar";
        }
        if (actividad.getFecha().isAfter(LocalDate.now())) return "Futura · espera su fecha";
        return "Sin solicitud · registra evidencia";
    }

    private static SolicitudParticipacion ultimaSolicitud(String actividadId, List<SolicitudParticipacion> solicitudes) {
        SolicitudParticipacion ultima = null;
        for (SolicitudParticipacion solicitud : solicitudes) {
            if (solicitud.getActividadId().equals(actividadId)) ultima = solicitud;
        }
        return ultima;
    }

    private static boolean puedeSolicitar(Actividad actividad, List<SolicitudParticipacion> solicitudes) {
        if (actividad.getFecha().isAfter(LocalDate.now())) return false;
        SolicitudParticipacion ultima = ultimaSolicitud(actividad.getId(), solicitudes);
        return ultima == null || "RECHAZADA".equals(ultima.getEstado());
    }

    private static String estadoSolicitud(Actividad actividad, List<SolicitudParticipacion> solicitudes) {
        SolicitudParticipacion ultima = ultimaSolicitud(actividad.getId(), solicitudes);
        if (ultima != null && "PENDIENTE".equals(ultima.getEstado())) return "Solicitud pendiente";
        if (ultima != null && "APROBADA".equals(ultima.getEstado())) return "Participación aprobada";
        if (actividad.getFecha().isAfter(LocalDate.now())) return "Fecha futura";
        if (ultima != null && "RECHAZADA".equals(ultima.getEstado())) return "Solicitud rechazada · puedes reenviar";
        return "Sin solicitud · puedes registrar";
    }

    private JComboBox<String> categorias() {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("Todas");
        for (String categoria : servicio.categorias()) if (!"Todas".equals(categoria)) combo.addItem(categoria);
        return combo;
    }

    private JComboBox<String> periodos() {
        return new JComboBox<>(Semestres.opciones(true));
    }

    private static String seleccionado(JComboBox<String> combo) {
        Object valor = combo.getSelectedItem();
        return valor == null ? "Todas" : valor.toString();
    }

    private static LocalDate fecha(JTextField campo) {
        try { return LocalDate.parse(campo.getText().trim()); }
        catch (java.time.format.DateTimeParseException error) {
            throw new IllegalArgumentException("Escribe una fecha válida con formato AAAA-MM-DD.");
        }
    }

    private static double nota(JTextField campo) {
        try {
            double valor = Double.parseDouble(campo.getText().trim().replace(',', '.'));
            if (!Double.isFinite(valor) || valor < 0 || valor > 5) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Todas las notas necesarias deben ser números entre 0 y 5.");
        }
    }

    private static JPanel vertical() {
        JPanel panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel formulario(Object... pares) {
        JPanel panel = Ui.formulario(pares);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private static JScrollPane desplazable(JComponent componente) {
        JScrollPane scroll = new JScrollPane(componente);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.getViewport().setBackground(Ui.fondo);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static JPanel acciones(JComponent... componentes) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(false);
        for (JComponent componente : componentes) panel.add(componente);
        return panel;
    }

    private static JTextArea texto(String contenido) {
        JTextArea area = new JTextArea(contenido);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        area.setForeground(Ui.texto);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        area.setBackground(Color.WHITE);
        return area;
    }

    private static JComponent aviso(String contenido) {
        JTextArea area = texto(contenido);
        int lineas = 0;
        for (String linea : contenido.split("\n")) lineas += Math.max(1, (linea.length() + 79) / 80);
        area.setRows(Math.min(5, Math.max(2, lineas)));
        area.setBackground(Ui.fondo);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 100));
        return area;
    }

    private static JPanel tarjetas(String titulo1, int valor1, String titulo2, int valor2, String titulo3, int valor3) {
        JPanel panel = new JPanel(new GridLayout(1, 3, 12, 0));
        panel.setOpaque(false);
        String[] titulos = {titulo1, titulo2, titulo3};
        int[] valores = {valor1, valor2, valor3};
        for (int i = 0; i < titulos.length; i++) {
            JPanel tarjeta = new JPanel(new BorderLayout(0, 10));
            tarjeta.setBackground(Color.WHITE);
            tarjeta.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225, 219, 232)),
                    BorderFactory.createEmptyBorder(16, 16, 16, 16)));
            JTextArea titulo = texto(titulos[i]);
            titulo.setRows(2);
            titulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            titulo.setBorder(BorderFactory.createEmptyBorder());
            JLabel valor = new JLabel(Ui.numero(valores[i]));
            valor.setFont(new Font("SansSerif", Font.BOLD, 29));
            valor.setForeground(Ui.morado);
            tarjeta.add(titulo, BorderLayout.NORTH);
            tarjeta.add(valor, BorderLayout.CENTER);
            panel.add(tarjeta);
        }
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 155));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JProgressBar barra(int valor, int objetivo) {
        JProgressBar barra = new JProgressBar(0, Math.max(1, objetivo));
        barra.setValue(Math.max(0, Math.min(valor, objetivo)));
        barra.setForeground(Ui.morado);
        barra.setBackground(new Color(231, 225, 237));
        barra.setStringPainted(true);
        barra.setPreferredSize(new java.awt.Dimension(500, 30));
        return barra;
    }

    private static JPanel progreso(String titulo, int valor, int objetivo) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setForeground(Ui.morado);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 14));
        JProgressBar barra = barra(valor, objetivo);
        barra.setString(Ui.numero(valor) + " / " + Ui.numero(objetivo) + " DUNAB · " + porcentaje(valor, objetivo));
        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(barra, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 90));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static String porcentaje(int valor, int objetivo) {
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f %%", 100.0 * valor / Math.max(1, objetivo));
    }

    private static JPanel bloqueTabla(String titulo, JTable tabla, int altura) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel etiqueta = new JLabel(titulo, SwingConstants.LEFT);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 16));
        etiqueta.setForeground(Ui.morado);
        panel.add(etiqueta, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new java.awt.Dimension(650, altura));
        panel.add(scroll, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, altura + 55));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static void fila(JTable tabla, Object... valores) {
        ((DefaultTableModel) tabla.getModel()).addRow(valores);
    }

    private static void limpiar(JTable tabla) {
        ((DefaultTableModel) tabla.getModel()).setRowCount(0);
    }

    private static class FilaHistorial {
        private final LocalDate fecha;
        private final String concepto;
        private final int cantidad;
        private final int solicitada;
        private final String estado;
        private final int semestre;
        private final Integer saldo;
        private final String categoria;
        private final String observacion;

        FilaHistorial(LocalDate fecha, String concepto, int cantidad, int solicitada, String estado, int semestre, Integer saldo, String categoria, String observacion) {
            this.fecha = fecha;
            this.concepto = concepto;
            this.cantidad = cantidad;
            this.solicitada = solicitada;
            this.estado = estado;
            this.semestre = semestre;
            this.saldo = saldo;
            this.categoria = categoria;
            this.observacion = observacion;
        }
    }
}
