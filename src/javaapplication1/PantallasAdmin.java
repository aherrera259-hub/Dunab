package javaapplication1;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/** Controles administrativos. El servicio vuelve a verificar el rol en cada operación. */
public class PantallasAdmin {
    private final ServicioDunab servicio;
    private final IntSupplier semestre;
    private final Runnable refrescarTodo;

    public PantallasAdmin(ServicioDunab servicio, IntSupplier semestre, Runnable refrescarTodo) {
        this.servicio = servicio;
        this.semestre = semestre;
        this.refrescarTodo = refrescarTodo;
    }

    public JPanel crear(String seccion) {
        if (servicio.getActual() == null
                || !"ADMINISTRADOR".equals(servicio.getActual().getRol())) {
            throw new IllegalStateException("Esta sección requiere una sesión de administrador.");
        }
        switch (seccion) {
            case "Administrar actividades": return actividades();
            case "Revisar solicitudes": return solicitudes();
            case "Estudiantes y ajustes": return estudiantes();
            case "Auditoría": return auditoria();
            default: throw new IllegalArgumentException("Sección administrativa desconocida: " + seccion);
        }
    }

    private JPanel actividades() {
        JPanel pagina = Ui.pagina("Administrar actividades",
                "Crea, edita y desactiva actividades ficticias. Las participaciones históricas se conservan.");
        List<Actividad> lista = servicio.actividades("", "Todas");
        List<String> ids = new ArrayList<String>();
        JTable tabla = Ui.tabla("Actividad", "Categoría", "Fecha", "Lugar", "DUNAB", "Cupos", "Disponibles", "Estado");
        Object[][] filas = new Object[lista.size()][8];
        for (int i = 0; i < lista.size(); i++) {
            Actividad a = lista.get(i);
            ids.add(a.getId());
            filas[i] = new Object[]{a.getNombre(), a.getCategoria(), a.getFecha(), a.getLugar(),
                a.getDunab(), a.getCupos(), servicio.cuposDisponibles(a.getId()), a.isActiva() ? "Activa" : "Inactiva"};
        }
        Ui.filas(tabla, filas);
        JTextArea descripcion = textoLectura("Selecciona una actividad para leer su descripción.", 3);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                int indice = tabla.convertRowIndexToModel(tabla.getSelectedRow());
                descripcion.setText(lista.get(indice).getDescripcion());
                descripcion.setCaretPosition(0);
            }
        });
        pagina.add(cuerpoTabla(tabla, lista.size() + " actividades registradas", descripcion), BorderLayout.CENTER);
        JPanel acciones = acciones();
        acciones.add(Ui.boton("Crear actividad", () -> editarActividad(pagina, null)));
        acciones.add(Ui.boton("Editar seleccionada", () -> {
            Actividad a = servicio.actividad(Ui.idSeleccionado(tabla, ids));
            editarActividad(pagina, a);
        }));
        acciones.add(Ui.boton("Activar / desactivar", () -> {
            Actividad a = servicio.actividad(Ui.idSeleccionado(tabla, ids));
            String operacion = a.isActiva() ? "Desactivar" : "Activar";
            if (JOptionPane.showConfirmDialog(pagina, operacion + " la actividad «" + a.getNombre() + "»?\n"
                    + "Sus inscripciones y solicitudes existentes se conservarán.", operacion,
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            servicio.guardarActividad(a.getId(), a.getNombre(), a.getCategoria(), a.getDescripcion(),
                    a.getFecha(), a.getLugar(), a.getDunab(), a.getCupos(), !a.isActiva());
            refrescarTodo.run();
        }));
        pagina.add(acciones, BorderLayout.SOUTH);
        return pagina;
    }

    private void editarActividad(Component padre, Actividad original) {
        JTextField nombre = Ui.campo(original == null ? "" : original.getNombre());
        JComboBox<String> categoria = new JComboBox<String>();
        for (String c : servicio.categorias()) if (!"Todas".equals(c)) categoria.addItem(c);
        categoria.setEditable(true);
        if (original != null) categoria.setSelectedItem(original.getCategoria());
        JTextArea descripcion = new JTextArea(original == null ? "" : original.getDescripcion(), 4, 34);
        descripcion.setLineWrap(true);
        descripcion.setWrapStyleWord(true);
        JTextField fecha = Ui.campo((original == null ? LocalDate.now().plusDays(7) : original.getFecha()).toString());
        JTextField lugar = Ui.campo(original == null ? "" : original.getLugar());
        JSpinner dunab = Ui.entero(original == null ? 500 : original.getDunab(), 1, 1000000);
        JSpinner cupos = Ui.entero(original == null ? 20 : original.getCupos(), 1, 1000000);
        JCheckBox activa = new JCheckBox("Disponible para nuevas inscripciones", original == null || original.isActiva());
        activa.setOpaque(false);
        JPanel formulario = Ui.formulario("Nombre", nombre, "Categoría", categoria,
                "Descripción", new JScrollPane(descripcion), "Fecha (AAAA-MM-DD)", fecha, "Lugar", lugar,
                "DUNAB que reconoce", dunab, "Cupos totales", cupos, "Estado", activa);
        JScrollPane desplazamiento = new JScrollPane(formulario);
        desplazamiento.setBorder(BorderFactory.createEmptyBorder());
        desplazamiento.setPreferredSize(new Dimension(600, 430));
        while (JOptionPane.showConfirmDialog(padre, desplazamiento,
                original == null ? "Nueva actividad" : "Editar actividad",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                dunab.commitEdit();
                cupos.commitEdit();
                Object categoriaElegida = categoria.getEditor().getItem();
                servicio.guardarActividad(original == null ? null : original.getId(), nombre.getText(),
                        categoriaElegida == null ? "" : categoriaElegida.toString(), descripcion.getText(),
                        LocalDate.parse(fecha.getText().trim()), lugar.getText(), Ui.valor(dunab), Ui.valor(cupos),
                        activa.isSelected());
                refrescarTodo.run();
                return;
            } catch (DateTimeParseException ex) {
                error(padre, "Escribe una fecha válida con formato AAAA-MM-DD, por ejemplo 2026-11-20.");
            } catch (java.text.ParseException ex) {
                error(padre, "Revisa los valores numéricos de DUNAB y cupos.");
            } catch (RuntimeException ex) {
                error(padre, ex.getMessage());
            }
        }
    }

    private JPanel solicitudes() {
        JPanel pagina = Ui.pagina("Revisar solicitudes",
                "Aprobar reconoce los DUNAB solicitados una sola vez. Rechazar conserva el registro sin sumarlos.");
        List<SolicitudParticipacion> lista = servicio.solicitudesPendientes();
        List<String> ids = new ArrayList<String>();
        JTable tabla = Ui.tabla("Estudiante", "Actividad", "Fecha participación", "DUNAB", "Semestre", "Estado");
        Object[][] filas = new Object[lista.size()][6];
        for (int i = 0; i < lista.size(); i++) {
            SolicitudParticipacion s = lista.get(i);
            ids.add(s.getId());
            filas[i] = new Object[]{servicio.nombreEstudiante(s.getEstudianteId()),
                Ui.nombreActividad(servicio, s.getActividadId()), s.getFecha(), s.getCantidad(), s.getSemestre(), s.getEstado()};
        }
        Ui.filas(tabla, filas);
        JTextArea evidencia = textoLectura("Selecciona una solicitud para consultar su evidencia o código de referencia.", 5);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                SolicitudParticipacion s = lista.get(tabla.convertRowIndexToModel(tabla.getSelectedRow()));
                evidencia.setText("Evidencia o código de referencia:\n" + s.getEvidencia()
                        + "\n\nObservación actual: " + (s.getObservacion().isEmpty() ? "Sin observaciones" : s.getObservacion()));
                evidencia.setCaretPosition(0);
            }
        });
        pagina.add(cuerpoTabla(tabla, lista.size() + " solicitudes pendientes de revisión", evidencia), BorderLayout.CENTER);
        JPanel acciones = acciones();
        acciones.add(Ui.boton("Aprobar seleccionada", () -> resolver(pagina, tabla, ids, lista, true)));
        acciones.add(Ui.boton("Rechazar seleccionada", () -> resolver(pagina, tabla, ids, lista, false)));
        acciones.add(Ui.boton("Actualizar", refrescarTodo));
        pagina.add(acciones, BorderLayout.SOUTH);
        return pagina;
    }

    private void resolver(Component padre, JTable tabla, List<String> ids,
            List<SolicitudParticipacion> lista, boolean aprobar) {
        String id = Ui.idSeleccionado(tabla, ids);
        SolicitudParticipacion s = lista.get(tabla.convertRowIndexToModel(tabla.getSelectedRow()));
        JTextArea observacion = new JTextArea(4, 36);
        observacion.setLineWrap(true);
        observacion.setWrapStyleWord(true);
        JPanel formulario = new JPanel(new BorderLayout(8, 12));
        formulario.add(new JLabel("<html>" + (aprobar ? "Reconocer " : "Rechazar ") + s.getCantidad()
                + " DUNAB en el semestre " + s.getSemestre() + ".<br>Escribe la justificación de la decisión:</html>"), BorderLayout.NORTH);
        formulario.add(new JScrollPane(observacion), BorderLayout.CENTER);
        while (JOptionPane.showConfirmDialog(padre, formulario, aprobar ? "Aprobar participación" : "Rechazar participación",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                if (observacion.getText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Escribe una observación para dejar constancia de la revisión.");
                }
                servicio.resolverSolicitud(id, aprobar, observacion.getText());
                Ui.info(padre, aprobar ? "Participación aprobada. Los DUNAB ya están en el acumulado del estudiante."
                        : "Participación rechazada. La solicitud se conserva y no suma DUNAB.");
                refrescarTodo.run();
                return;
            } catch (RuntimeException ex) {
                error(padre, ex.getMessage());
            }
        }
    }

    private JPanel estudiantes() {
        JPanel pagina = Ui.pagina("Estudiantes y ajustes",
                "Los ajustes requieren un motivo y registran administrador, estudiante y saldos anterior y posterior.");
        List<Usuario> lista = servicio.estudiantes();
        List<String> ids = new ArrayList<String>();
        JTable tabla = Ui.tabla("Estudiante", "Correo", "Programa", "Semestre académico", "DUNAB históricos", "DUNAB semestre " + semestre.getAsInt());
        Object[][] filas = new Object[lista.size()][6];
        for (int i = 0; i < lista.size(); i++) {
            Usuario u = lista.get(i);
            ids.add(u.getId());
            filas[i] = new Object[]{u.getNombre(), u.getCorreo(), u.getPrograma(), u.getSemestre(),
                servicio.saldo(u.getId(), 0), servicio.saldo(u.getId(), semestre.getAsInt())};
        }
        Ui.filas(tabla, filas);
        JTextArea aviso = textoLectura("Para otorgar DUNAB escribe una cantidad positiva; para retirarlos, una cantidad negativa.\n"
                + "No se permiten retiros que dejen un saldo negativo en el semestre o en el acumulado histórico.", 3);
        pagina.add(cuerpoTabla(tabla, lista.size() + " estudiantes registrados · Período consultado: semestre " + semestre.getAsInt(), aviso), BorderLayout.CENTER);
        JPanel acciones = acciones();
        acciones.add(Ui.boton("Realizar ajuste justificado", () -> {
            String id = Ui.idSeleccionado(tabla, ids);
            ajustar(pagina, servicio.usuario(id));
        }));
        acciones.add(Ui.boton("Actualizar", refrescarTodo));
        pagina.add(acciones, BorderLayout.SOUTH);
        return pagina;
    }

    private void ajustar(Component padre, Usuario estudiante) {
        JSpinner cantidad = Ui.entero(100, -1000000, 1000000);
        JComboBox<String> periodo = new JComboBox<String>(Semestres.opciones(false));
        periodo.setSelectedIndex(semestre.getAsInt() - 1);
        JTextArea motivo = new JTextArea(4, 34);
        motivo.setLineWrap(true);
        motivo.setWrapStyleWord(true);
        JLabel saldo = new JLabel("Saldo histórico actual: " + Ui.numero(servicio.saldo(estudiante.getId(), 0)) + " DUNAB");
        JPanel formulario = Ui.formulario("Estudiante", new JLabel(estudiante.getNombre()), "Referencia", saldo,
                "Semestre del ajuste", periodo, "Cantidad (+ otorga / - retira)", cantidad, "Motivo obligatorio", new JScrollPane(motivo));
        while (JOptionPane.showConfirmDialog(padre, formulario, "Ajuste administrativo de DUNAB",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                cantidad.commitEdit();
                int valor = Ui.valor(cantidad);
                if (valor == 0) throw new IllegalArgumentException("El ajuste debe ser distinto de cero.");
                if (motivo.getText().trim().isEmpty()) throw new IllegalArgumentException("Escribe el motivo del ajuste.");
                int sem = periodo.getSelectedIndex() + 1;
                int anterior = servicio.saldo(estudiante.getId(), 0);
                servicio.ajustar(estudiante.getId(), valor, sem, motivo.getText());
                Ui.info(padre, "Ajuste guardado en el semestre " + sem + ".\nSaldo histórico anterior: "
                        + Ui.numero(anterior) + " DUNAB\nSaldo histórico posterior: "
                        + Ui.numero(servicio.saldo(estudiante.getId(), 0)) + " DUNAB");
                refrescarTodo.run();
                return;
            } catch (java.text.ParseException ex) {
                error(padre, "Escribe una cantidad entera válida de DUNAB.");
            } catch (RuntimeException ex) {
                error(padre, ex.getMessage());
            }
        }
    }

    private JPanel auditoria() {
        JPanel pagina = Ui.pagina("Historial de auditoría",
                "Registro histórico de decisiones y ajustes. Los valores anterior y posterior corresponden al saldo histórico.");
        List<RegistroAuditoria> lista = servicio.auditoria();
        JTable tabla = Ui.tabla("Fecha", "Administrador", "Estudiante", "Operación", "DUNAB", "Valor anterior", "Valor posterior", "Justificación");
        Object[][] filas = new Object[lista.size()][8];
        for (int i = 0; i < lista.size(); i++) {
            RegistroAuditoria r = lista.get(i);
            filas[i] = new Object[]{r.getFecha(), nombreUsuario(r.getAdministradorId()), nombreUsuario(r.getEstudianteId()),
                r.getOperacion(), r.getCantidad(), r.getValorAnterior(), r.getValorPosterior(), r.getMotivo()};
        }
        Ui.filas(tabla, filas);
        JTextArea detalle = textoLectura("Selecciona un registro para consultar su justificación completa.", 4);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                RegistroAuditoria r = lista.get(tabla.convertRowIndexToModel(tabla.getSelectedRow()));
                detalle.setText("Operación: " + r.getOperacion() + "\nJustificación: " + r.getMotivo()
                        + "\nIdentificador del registro: " + r.getId());
                detalle.setCaretPosition(0);
            }
        });
        pagina.add(cuerpoTabla(tabla, lista.size() + " registros · Todos los semestres", detalle), BorderLayout.CENTER);
        JPanel acciones = acciones();
        acciones.add(Ui.boton("Actualizar", refrescarTodo));
        pagina.add(acciones, BorderLayout.SOUTH);
        return pagina;
    }

    private String nombreUsuario(String id) {
        if (id == null || id.isEmpty()) return "—";
        return servicio.usuario(id).getNombre();
    }

    private JPanel cuerpoTabla(JTable tabla, String resumen, JTextArea detalle) {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel cuerpo = new JPanel(new BorderLayout(10, 12));
        cuerpo.setOpaque(false);
        JLabel etiqueta = new JLabel(resumen);
        etiqueta.setForeground(Ui.texto);
        cuerpo.add(etiqueta, BorderLayout.NORTH);
        JScrollPane lista = new JScrollPane(tabla);
        lista.setPreferredSize(new Dimension(820, 320));
        cuerpo.add(lista, BorderLayout.CENTER);
        JScrollPane informacion = new JScrollPane(detalle);
        informacion.setPreferredSize(new Dimension(820, detalle.getRows() * 22 + 12));
        cuerpo.add(informacion, BorderLayout.SOUTH);
        return cuerpo;
    }

    private JTextArea textoLectura(String texto, int filas) {
        JTextArea area = new JTextArea(texto, filas, 30);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(java.awt.Color.WHITE);
        area.setForeground(Ui.texto);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return area;
    }

    private JPanel acciones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setOpaque(false);
        return panel;
    }

    private void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje == null ? "No se pudo completar la operación." : mensaje,
                "Revisa la información", JOptionPane.WARNING_MESSAGE);
    }
}
