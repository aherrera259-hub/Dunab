package javaapplication1;

/** Cálculos académicos independientes de las unidades DUNAB. */
public final class CalculadoraNotas {
    private double primerCorte;
    private double segundoCorte;
    private double tercerCorte;
    private double notaMinima;

    public CalculadoraNotas() {
        this(0, 0, 0, 3);
    }

    public CalculadoraNotas(double primerCorte, double segundoCorte, double tercerCorte, double notaMinima) {
        setPrimerCorte(primerCorte);
        setSegundoCorte(segundoCorte);
        setTercerCorte(tercerCorte);
        setNotaMinima(notaMinima);
    }

    public double getPrimerCorte() { return primerCorte; }
    public double getSegundoCorte() { return segundoCorte; }
    public double getTercerCorte() { return tercerCorte; }
    public double getNotaMinima() { return notaMinima; }
    public void setPrimerCorte(double valor) { validar(valor); primerCorte = valor; }
    public void setSegundoCorte(double valor) { validar(valor); segundoCorte = valor; }
    public void setTercerCorte(double valor) { validar(valor); tercerCorte = valor; }
    public void setNotaMinima(double valor) { validar(valor); notaMinima = valor; }

    public double calcularFinal() { return finalEstimada(primerCorte, segundoCorte, tercerCorte); }
    public double calcularNecesaria() { return necesaria(primerCorte, segundoCorte, notaMinima); }

    private static void validar(double nota) {
        if (Double.isNaN(nota) || Double.isInfinite(nota) || nota < 0 || nota > 5)
            throw new IllegalArgumentException("Las notas deben estar entre 0 y 5.");
    }

    public static double finalEstimada(double c1, double c2, double c3) {
        validar(c1); validar(c2); validar(c3);
        return 0.30 * c1 + 0.30 * c2 + 0.40 * c3;
    }

    // Un resultado mayor a 5 es imposible; menor o igual a cero ya alcanza la mínima.
    public static double necesaria(double c1, double c2, double minima) {
        validar(c1); validar(c2); validar(minima);
        return (minima - 0.30 * c1 - 0.30 * c2) / 0.40;
    }

    /** Calcula tantas evaluaciones como contengan los arreglos, sin pesos fijos. */
    public static double ponderada(double[] notas, double[] porcentajes) {
        validarArreglos(notas, porcentajes);
        double resultado = 0;
        for (int i = 0; i < notas.length; i++) {
            validar(notas[i]);
            resultado += notas[i] * porcentajes[i] / 100.0;
        }
        return resultado;
    }

    /** La evaluación pendiente se identifica desde cero y su nota se ignora. */
    public static double necesaria(double[] notas, double[] porcentajes, int indicePendiente, double minima) {
        validarArreglos(notas, porcentajes);
        validar(minima);
        if (indicePendiente < 0 || indicePendiente >= notas.length) {
            throw new IllegalArgumentException("Selecciona una evaluación pendiente válida.");
        }
        double conocido = 0;
        for (int i = 0; i < notas.length; i++) {
            if (i == indicePendiente) continue;
            validar(notas[i]);
            conocido += notas[i] * porcentajes[i] / 100.0;
        }
        return (minima - conocido) / (porcentajes[indicePendiente] / 100.0);
    }

    private static void validarArreglos(double[] notas, double[] porcentajes) {
        if (notas == null || porcentajes == null || notas.length != porcentajes.length
                || notas.length < 1 || notas.length > 30) {
            throw new IllegalArgumentException("Ingresa entre 1 y 30 evaluaciones con una nota y un porcentaje por fila.");
        }
        double total = 0;
        for (double porcentaje : porcentajes) {
            if (Double.isNaN(porcentaje) || Double.isInfinite(porcentaje) || porcentaje <= 0 || porcentaje > 100) {
                throw new IllegalArgumentException("Cada porcentaje debe ser mayor que 0 y menor o igual a 100.");
            }
            total += porcentaje;
        }
        if (Math.abs(total - 100.0) > 0.000001) {
            throw new IllegalArgumentException("Los porcentajes deben sumar 100 %. Revisa el total o usa Repartir 100 %.");
        }
    }

    /** Usa centésimas para que cualquier cantidad de filas sume exactamente 100 %. */
    public static double[] repartirPorcentajes(int cantidad) {
        if (cantidad < 1 || cantidad > 30) throw new IllegalArgumentException("La cantidad de notas debe estar entre 1 y 30.");
        double[] porcentajes = new double[cantidad];
        int base = 10000 / cantidad;
        int sobrantes = 10000 % cantidad;
        for (int i = 0; i < cantidad; i++) porcentajes[i] = (base + (i < sobrantes ? 1 : 0)) / 100.0;
        return porcentajes;
    }
}
