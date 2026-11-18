package javaapplication1;

/** Rango de períodos disponible en el prototipo; no representa una norma universitaria. */
public final class Semestres {
    public static final int MAXIMO = 12;

    private Semestres() { }

    public static String[] opciones(boolean incluirHistorico) {
        int desplazamiento = incluirHistorico ? 1 : 0;
        String[] opciones = new String[MAXIMO + desplazamiento];
        if (incluirHistorico) opciones[0] = "Histórico (todos)";
        for (int semestre = 1; semestre <= MAXIMO; semestre++) {
            opciones[semestre - 1 + desplazamiento] = "Semestre " + semestre;
        }
        return opciones;
    }

    public static void validar(int semestre, boolean permitirHistorico) {
        if (permitirHistorico && semestre == 0) return;
        if (semestre < 1 || semestre > MAXIMO) {
            throw new IllegalArgumentException("Selecciona un semestre entre 1 y " + MAXIMO + ".");
        }
    }
}
