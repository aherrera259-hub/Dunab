package javaapplication1;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Prueba la cantidad variable de notas, pesos, incógnita y edición real de la tabla. */
public final class PruebasCalculadora {
    private static int comprobaciones;

    public static void main(String[] args) throws Exception {
        cerca(CalculadoraNotas.ponderada(new double[]{4, 3, 5}, new double[]{30, 30, 40}), 4.1, "Tres evaluaciones personalizadas");
        cerca(CalculadoraNotas.ponderada(new double[]{2, 5}, new double[]{20, 80}), 4.4, "Dos notas con porcentajes propios");
        cerca(CalculadoraNotas.ponderada(new double[]{3.5}, new double[]{100}), 3.5, "Una sola evaluación");
        for (int cantidad = 1; cantidad <= 30; cantidad++) {
            double[] pesos = CalculadoraNotas.repartirPorcentajes(cantidad);
            double[] notas = new double[cantidad];
            double total = 0;
            for (int i = 0; i < cantidad; i++) {
                notas[i] = 4;
                total += pesos[i];
                comprobar(pesos[i] > 0, "Cada peso predeterminado es positivo");
            }
            cerca(total, 100, "Pesos repartidos suman 100 para " + cantidad + " notas");
            cerca(CalculadoraNotas.ponderada(notas, pesos), 4, "Promedio constante para " + cantidad + " notas");
        }
        cerca(CalculadoraNotas.necesaria(new double[]{4, Double.NaN, 3}, new double[]{25, 50, 25}, 1, 3), 2.5, "Incógnita intermedia se excluye");
        cerca(CalculadoraNotas.necesaria(new double[]{0}, new double[]{100}, 0, 3), 3, "Incógnita única");
        comprobar(CalculadoraNotas.necesaria(new double[]{0, 0}, new double[]{10, 90}, 0, 3) > 5, "Identifica nota imposible");
        comprobar(CalculadoraNotas.necesaria(new double[]{5, 0}, new double[]{90, 10}, 1, 3) < 0, "Mínima ya alcanzada");
        cerca(CalculadoraNotas.finalEstimada(4, 3, 5), 4.1, "Compatibilidad con API original");
        CalculadoraNotas objeto = new CalculadoraNotas(4, 3, 5, 3);
        objeto.setTercerCorte(4);
        cerca(objeto.calcularFinal(), 3.7, "Setters originales mantienen validación y cálculo");
        denegar(() -> CalculadoraNotas.ponderada(null, new double[]{100}), "Arreglo nulo");
        denegar(() -> CalculadoraNotas.ponderada(new double[0], new double[0]), "Sin evaluaciones");
        denegar(() -> CalculadoraNotas.ponderada(new double[31], new double[31]), "Más de treinta evaluaciones");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{3}, new double[]{50, 50}), "Cantidades distintas");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{3, 4}, new double[]{30, 30}), "Pesos inferiores a 100");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{3, 4}, new double[]{60, 60}), "Pesos superiores a 100");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{3, 4}, new double[]{0, 100}), "Peso cero");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{3, 4}, new double[]{-10, 110}), "Peso negativo");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{3}, new double[]{Double.NaN}), "Peso NaN");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{3}, new double[]{Double.POSITIVE_INFINITY}), "Peso infinito");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{-1}, new double[]{100}), "Nota negativa");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{5.1}, new double[]{100}), "Nota mayor que cinco");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{Double.NaN}, new double[]{100}), "Nota NaN");
        denegar(() -> CalculadoraNotas.ponderada(new double[]{Double.NEGATIVE_INFINITY}, new double[]{100}), "Nota infinita");
        denegar(() -> CalculadoraNotas.necesaria(new double[]{3}, new double[]{100}, 1, 3), "Índice fuera de rango");
        denegar(() -> CalculadoraNotas.necesaria(new double[]{3}, new double[]{100}, -1, 3), "Índice negativo");
        denegar(() -> CalculadoraNotas.necesaria(new double[]{3}, new double[]{100}, 0, 6), "Mínima inválida");
        denegar(() -> CalculadoraNotas.necesaria(new double[]{Double.NaN, 0}, new double[]{50, 50}, 1, 3), "Nota conocida vacía o NaN");
        denegar(() -> CalculadoraNotas.repartirPorcentajes(0), "No se reparten cero filas");
        denegar(() -> CalculadoraNotas.repartirPorcentajes(31), "No se reparten más de treinta filas");
        probarInterfaz();
        System.out.println("PruebasCalculadora: " + comprobaciones + " comprobaciones correctas.");
    }

    private static void probarInterfaz() throws Exception {
        ServicioDunab servicio = new ServicioDunab(Files.createTempDirectory("dunab-calculadora-"));
        servicio.login("estudiante@demo.local", "Estudiante123!".toCharArray());
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new PantallasEstudiante(servicio, () -> 1, () -> { }).crear("Calculadora de notas");
            JTable tabla = buscarNombre(panel, "tablaCalculadora", JTable.class);
            JSpinner cantidad = buscarNombre(panel, "cantidadNotas", JSpinner.class);
            JSpinner pendiente = buscarNombre(panel, "evaluacionPendiente", JSpinner.class);
            JTextField minima = buscarNombre(panel, "notaMinima", JTextField.class);
            comprobar(tabla.getRowCount() == 3 && tabla.isCellEditable(0, 1), "Tabla inicial editable");
            tabla.setValueAt("Mi parcial", 0, 0);
            tabla.setValueAt("4", 0, 1);
            cantidad.setValue(5);
            boton(panel, "Aplicar cantidad").doClick();
            comprobar(tabla.getRowCount() == 5, "Aplica cantidad elegida");
            comprobar("Mi parcial".equals(tabla.getValueAt(0, 0)) && "4".equals(tabla.getValueAt(0, 1)), "Conserva nombres y notas existentes");
            for (int i = 0; i < 5; i++) {
                cerca(Double.parseDouble(tabla.getValueAt(i, 2).toString()), 20, "Reparte cinco evaluaciones");
                tabla.setValueAt("4", i, 1);
            }
            boton(panel, "Calcular nota final").doClick();
            comprobar(textoContiene(panel, "Nota final estimada: 4,00"), "Calcula nota final desde la tabla");
            pendiente.setValue(2);
            tabla.setValueAt("", 1, 1);
            minima.setText("4");
            boton(panel, "Calcular nota necesaria").doClick();
            comprobar(textoContiene(panel, "Necesitas 4,00"), "Nota necesaria acepta celda incógnita vacía");
            tabla.setValueAt("4", 1, 1);
            comprobar(textoContiene(panel, "Los datos cambiaron"), "Editar invalida resultado anterior");
            comprobar(tabla.editCellAt(0, 1), "Se inicia edición de una nota");
            ((JTextField) tabla.getEditorComponent()).setText("5");
            boton(panel, "Calcular nota final").doClick();
            comprobar(!tabla.isEditing(), "Calcular confirma la edición de la celda");
            comprobar(textoContiene(panel, "Nota final estimada: 4,20"), "El cálculo usa la edición recién escrita");
        });
    }

    private static boolean textoContiene(Container padre, String fragmento) {
        for (Component componente : padre.getComponents()) {
            if (componente instanceof JTextArea && ((JTextArea) componente).getText().contains(fragmento)) return true;
            if (componente instanceof Container && textoContiene((Container) componente, fragmento)) return true;
        }
        return false;
    }

    private static JButton boton(Container padre, String texto) {
        for (Component componente : padre.getComponents()) {
            if (componente instanceof JButton && texto.equals(((JButton) componente).getText())) return (JButton) componente;
            if (componente instanceof Container) {
                JButton encontrado = buscarBoton((Container) componente, texto);
                if (encontrado != null) return encontrado;
            }
        }
        throw new AssertionError("No existe el botón " + texto);
    }

    private static JButton buscarBoton(Container padre, String texto) {
        for (Component componente : padre.getComponents()) {
            if (componente instanceof JButton && texto.equals(((JButton) componente).getText())) return (JButton) componente;
            if (componente instanceof Container) {
                JButton encontrado = buscarBoton((Container) componente, texto);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private static <T extends Component> T buscarNombre(Container padre, String nombre, Class<T> tipo) {
        for (Component componente : padre.getComponents()) {
            if (tipo.isInstance(componente) && nombre.equals(componente.getName())) return tipo.cast(componente);
            if (componente instanceof Container) {
                T encontrado = buscarNombre((Container) componente, nombre, tipo);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private static void cerca(double actual, double esperado, String mensaje) {
        comprobar(Math.abs(actual - esperado) < 0.000001, mensaje);
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }

    private static void denegar(Runnable accion, String mensaje) {
        try { accion.run(); }
        catch (IllegalArgumentException esperado) { comprobaciones++; return; }
        throw new AssertionError("Se permitió: " + mensaje);
    }
}
