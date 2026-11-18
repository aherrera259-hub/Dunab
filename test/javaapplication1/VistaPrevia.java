package javaapplication1;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Renderiza los paneles sin abrir ventanas ni modificar el almacén del usuario. */
public final class VistaPrevia {
    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("dunab-vista-");
        ServicioDunab servicio = new ServicioDunab(carpeta);
        Tema.iniciar(null);
        Files.createDirectories(Paths.get("tmp", "vistas"));
        SwingUtilities.invokeAndWait(() -> {
            guardar(new PanelAcceso(servicio, () -> { }), "00-acceso", 1240, 800);
            servicio.login("estudiante@demo.local", "Estudiante123!".toCharArray());
            PantallasEstudiante estudiante = new PantallasEstudiante(servicio, () -> 2, () -> { });
            String[] pantallas = {"Inicio", "Mi perfil", "Consultar DUNAB", "Actividades y espacios", "Inscripciones",
                "Registrar participación", "Historial de DUNAB", "Metas personales", "Top 9", "Calculadora de notas", "Estadísticas y reportes"};
            int i = 1;
            for (String pantalla : pantallas) guardar(estudiante.crear(pantalla), String.format("%02d-estudiante", i++), 980, 720);
            servicio.login("admin@demo.local", "Admin123!".toCharArray());
            PantallasAdmin administrador = new PantallasAdmin(servicio, () -> 2, () -> { });
            String[] administrativas = {"Administrar actividades", "Revisar solicitudes", "Estudiantes y ajustes", "Auditoría"};
            for (String pantalla : administrativas) guardar(administrador.crear(pantalla), String.format("%02d-admin", i++), 980, 720);
            guardar(new PantallasEstudiante(servicio, () -> 2, () -> { }).crear("Inicio"), "16-inicio-admin", 980, 720);
            Tema.cambiar();
            guardar(new PanelAcceso(servicio, () -> { }), "18-oscuro-acceso", 1240, 800);
            for (String pantalla : administrativas) guardar(administrador.crear(pantalla), "oscuro-admin-" + pantalla.replace(' ', '-'), 1050, 760);
            servicio.login("estudiante@demo.local", "Estudiante123!".toCharArray());
            for (String pantalla : pantallas) guardar(estudiante.crear(pantalla), "oscuro-" + pantalla.replace(' ', '-'), 1050, 760);
        });
        System.out.println("VistaPrevia: 17 paneles claros y 16 oscuros renderizados en tmp/vistas.");
    }

    private static void distribuir(Container contenedor) {
        contenedor.doLayout();
        for (Component hijo : contenedor.getComponents()) if (hijo instanceof Container) distribuir((Container) hijo);
    }

    private static void guardar(JPanel panel, String nombre, int ancho, int alto) {
        Tema.aplicar(panel);
        panel.setSize(ancho, alto);
        panel.addNotify();
        distribuir(panel);
        // JScrollPane instala las cabeceras de las tablas al conectar sus componentes.
        distribuir(panel);
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        panel.printAll(g);
        g.dispose();
        try { ImageIO.write(imagen, "png", Paths.get("tmp", "vistas", nombre + ".png").toFile()); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        panel.removeNotify();
    }
}
