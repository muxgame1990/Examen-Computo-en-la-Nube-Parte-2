package mx.NovaCore.ia;

import java.util.List;

public final class Codificador {

    public static final List<String> INTERES = List.of("ambos", "salud", "apariencia");
    public static final List<String> EJERCICIO = List.of("sedentario", "moderado", "activo");
    public static final List<String> MOTIVACION = List.of("moderado", "muy motivado");
    public static final List<String> TECNOLOGIA = List.of("si", "no");

    public static final String[] MODELOS = {"i100", "i500"};

    private Codificador() {
    }

    public static double[] codificar(String interes, String ejercicio, String motivacion, String tecnologia) {
        double[] x = new double[RedNeuronal.ENTRADAS];
        int base = 0;
        x[base + indice(INTERES, interes)] = 1;
        base += INTERES.size();
        x[base + indice(EJERCICIO, ejercicio)] = 1;
        base += EJERCICIO.size();
        x[base + indice(MOTIVACION, motivacion)] = 1;
        base += MOTIVACION.size();
        x[base + indice(TECNOLOGIA, tecnologia)] = 1;
        return x;
    }

    public static int clase(String modelo) {
        if ("i100".equals(modelo)) {
            return 0;
        }
        if ("i500".equals(modelo)) {
            return 1;
        }
        throw new IllegalArgumentException("Modelo desconocido: " + modelo);
    }

    private static int indice(List<String> valores, String valor) {
        int i = valores.indexOf(valor);
        if (i < 0) {
            throw new IllegalArgumentException("Valor no válido: " + valor);
        }
        return i;
    }
}
