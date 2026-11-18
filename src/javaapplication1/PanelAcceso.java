package javaapplication1;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Acceso y registro locales, sin conexión con servicios universitarios. */
public final class PanelAcceso extends JPanel {
    private final ServicioDunab servicio;
    private final Runnable alEntrar;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final JTextField correo = Ui.campo("");
    private final JPasswordField clave = new JPasswordField(24);

    public PanelAcceso(ServicioDunab servicio, Runnable alEntrar) {
        super(new BorderLayout());
        this.servicio = servicio;
        this.alEntrar = alEntrar;
        JPanel marca = new JPanel(new BorderLayout(10, 30));
        marca.setPreferredSize(new Dimension(320, 600));
        marca.setBackground(Ui.morado);
        marca.setBorder(BorderFactory.createEmptyBorder(45, 28, 40, 28));
        JLabel logo = new JLabel("DUNAB");
        logo.setFont(new Font("SansSerif", Font.BOLD, 38));
        logo.setForeground(Color.WHITE);
        marca.add(logo, BorderLayout.NORTH);
        JLabel lema = new JLabel("<html><div style='width:190px'>Tu progreso empieza por estar presente.<br><br>"
                + "Explora actividades, registra tu participación y sigue tus metas de bienestar.</div></html>");
        lema.setVerticalAlignment(SwingConstants.TOP);
        lema.setFont(new Font("SansSerif", Font.PLAIN, 23));
        lema.setForeground(Color.WHITE);
        marca.add(lema, BorderLayout.CENTER);
        JLabel demo = new JLabel("<html>Prototipo académico<br>Datos ficticios · Acceso local</html>");
        demo.setForeground(Color.WHITE);
        marca.add(demo, BorderLayout.SOUTH);
        add(marca, BorderLayout.WEST);
        contenido.add(acceso(), "acceso");
        contenido.add(registro(), "registro");
        add(contenido, BorderLayout.CENTER);
    }

    private JPanel acceso() {
        JPanel pagina = Ui.pagina("Qué bueno verte de nuevo", "Entra a tu espacio y sigue sumando experiencias.");
        JPanel campos = Ui.formulario("Correo", correo, "Contraseña", clave);
        JPanel caja = new JPanel(new BorderLayout(12, 20));
        caja.setOpaque(false);
        caja.add(campos, BorderLayout.NORTH);
        JTextArea ayuda = new JTextArea("Cuentas ficticias de demostración:\n\n"
                + "Estudiante: estudiante@demo.local\nContraseña: Estudiante123!\n\n"
                + "Administrador: admin@demo.local\nContraseña: Admin123!\n\n"
                + "La información se guarda automáticamente en la carpeta data.");
        ayuda.setEditable(false);
        ayuda.setOpaque(false);
        ayuda.setFont(new Font("SansSerif", Font.PLAIN, 14));
        ayuda.setForeground(Ui.texto);
        ayuda.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        caja.add(ayuda, BorderLayout.CENTER);
        pagina.add(caja, BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        acciones.setOpaque(false);
        JButton entrar = Ui.boton("Iniciar sesión", this::entrar);
        acciones.add(entrar);
        acciones.add(Ui.boton("Crear cuenta", () -> tarjetas.show(contenido, "registro")));
        clave.addActionListener(e -> entrar.doClick());
        pagina.add(acciones, BorderLayout.SOUTH);
        return pagina;
    }

    private void entrar() {
        char[] secreto = clave.getPassword();
        try {
            servicio.login(correo.getText(), secreto);
            clave.setText("");
            alEntrar.run();
        } finally {
            Arrays.fill(secreto, '\0');
        }
    }

    private JPanel registro() {
        JPanel pagina = Ui.pagina("Crear tu cuenta", "Registro de estudiante para esta simulación académica.");
        JTextField nombre = Ui.campo("");
        JTextField nuevoCorreo = Ui.campo("");
        JTextField programa = Ui.campo("");
        JPasswordField nuevaClave = new JPasswordField(24);
        JPasswordField confirmacion = new JPasswordField(24);
        JComboBox<String> semestre = new JComboBox<>(Semestres.opciones(false));
        JPanel campos = Ui.formulario("Nombre", nombre, "Correo", nuevoCorreo,
                "Programa académico", programa, "Semestre", semestre,
                "Contraseña (mínimo 8)", nuevaClave, "Confirmar contraseña", confirmacion);
        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(campos, BorderLayout.NORTH);
        pagina.add(centro, BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        acciones.setOpaque(false);
        acciones.add(Ui.boton("Crear cuenta", () -> {
            char[] secreto = nuevaClave.getPassword();
            char[] repetir = confirmacion.getPassword();
            try {
                if (!Arrays.equals(secreto, repetir)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
                servicio.registrar(nombre.getText(), nuevoCorreo.getText(), secreto,
                        programa.getText(), semestre.getSelectedIndex() + 1);
                correo.setText(nuevoCorreo.getText().trim());
                nombre.setText(""); nuevoCorreo.setText(""); programa.setText("");
                nuevaClave.setText(""); confirmacion.setText("");
                Ui.info(this, "Cuenta guardada. Ya puedes iniciar sesión.");
                tarjetas.show(contenido, "acceso");
            } finally {
                Arrays.fill(secreto, '\0'); Arrays.fill(repetir, '\0');
            }
        }));
        acciones.add(Ui.boton("Volver al inicio", () -> {
            nuevaClave.setText(""); confirmacion.setText("");
            tarjetas.show(contenido, "acceso");
        }));
        pagina.add(acciones, BorderLayout.SOUTH);
        return pagina;
    }
}
