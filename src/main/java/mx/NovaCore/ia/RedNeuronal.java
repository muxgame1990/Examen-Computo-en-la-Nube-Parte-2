package mx.NovaCore.ia;

import java.util.Locale;
import java.util.Random;


public class RedNeuronal {

    public static final int ENTRADAS = 10;
    public static final int OCULTAS = 6;
    public static final int SALIDAS = 2;

    private final double[][] w1 = new double[OCULTAS][ENTRADAS];
    private final double[] b1 = new double[OCULTAS];
    private final double[][] w2 = new double[SALIDAS][OCULTAS];
    private final double[] b2 = new double[SALIDAS];

    private int epocas;
    private double tasaAprendizaje;
    private double regularizacion;
    private double perdidaFinal;
    private double precisionEntrenamiento;
    private double precisionValidacionCruzada = Double.NaN;

    public RedNeuronal(long semilla) {
        Random azar = new Random(semilla);
        double l1 = Math.sqrt(6.0 / (ENTRADAS + OCULTAS));
        double l2 = Math.sqrt(6.0 / (OCULTAS + SALIDAS));
        for (int j = 0; j < OCULTAS; j++) {
            for (int i = 0; i < ENTRADAS; i++) {
                w1[j][i] = (azar.nextDouble() * 2 - 1) * l1;
            }
        }
        for (int k = 0; k < SALIDAS; k++) {
            for (int j = 0; j < OCULTAS; j++) {
                w2[k][j] = (azar.nextDouble() * 2 - 1) * l2;
            }
        }
    }

    private RedNeuronal() {
    }


    private double[] ocultas(double[] x) {
        double[] h = new double[OCULTAS];
        for (int j = 0; j < OCULTAS; j++) {
            double s = b1[j];
            for (int i = 0; i < ENTRADAS; i++) {
                s += w1[j][i] * x[i];
            }
            h[j] = Math.tanh(s);
        }
        return h;
    }

    private double[] softmax(double[] h) {
        double[] z = new double[SALIDAS];
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < SALIDAS; k++) {
            double s = b2[k];
            for (int j = 0; j < OCULTAS; j++) {
                s += w2[k][j] * h[j];
            }
            z[k] = s;
            max = Math.max(max, s);
        }
        double suma = 0;
        for (int k = 0; k < SALIDAS; k++) {
            z[k] = Math.exp(z[k] - max);
            suma += z[k];
        }
        for (int k = 0; k < SALIDAS; k++) {
            z[k] /= suma;
        }
        return z;
    }

    public double[] predecir(double[] x) {
        return softmax(ocultas(x));
    }



    public void entrenar(double[][] x, int[] y, int epocas, double tasa, double lambda) {
        int n = x.length;
        double perdida = 0;

        for (int e = 0; e < epocas; e++) {
            double[][] gw1 = new double[OCULTAS][ENTRADAS];
            double[] gb1 = new double[OCULTAS];
            double[][] gw2 = new double[SALIDAS][OCULTAS];
            double[] gb2 = new double[SALIDAS];
            perdida = 0;

            for (int m = 0; m < n; m++) {
                double[] h = ocultas(x[m]);
                double[] p = softmax(h);
                perdida -= Math.log(Math.max(p[y[m]], 1e-12));

                // Error en la capa de salida: p - objetivo
                double[] dz2 = new double[SALIDAS];
                for (int k = 0; k < SALIDAS; k++) {
                    dz2[k] = p[k] - (k == y[m] ? 1.0 : 0.0);
                }
                // Error propagado a la capa oculta
                double[] dz1 = new double[OCULTAS];
                for (int j = 0; j < OCULTAS; j++) {
                    double s = 0;
                    for (int k = 0; k < SALIDAS; k++) {
                        s += w2[k][j] * dz2[k];
                    }
                    dz1[j] = s * (1 - h[j] * h[j]); // derivada de tanh
                }
                for (int k = 0; k < SALIDAS; k++) {
                    gb2[k] += dz2[k];
                    for (int j = 0; j < OCULTAS; j++) {
                        gw2[k][j] += dz2[k] * h[j];
                    }
                }
                for (int j = 0; j < OCULTAS; j++) {
                    gb1[j] += dz1[j];
                    for (int i = 0; i < ENTRADAS; i++) {
                        gw1[j][i] += dz1[j] * x[m][i];
                    }
                }
            }

            // Actualización de pesos
            for (int k = 0; k < SALIDAS; k++) {
                b2[k] -= tasa * gb2[k] / n;
                for (int j = 0; j < OCULTAS; j++) {
                    w2[k][j] -= tasa * (gw2[k][j] / n + lambda * w2[k][j]);
                }
            }
            for (int j = 0; j < OCULTAS; j++) {
                b1[j] -= tasa * gb1[j] / n;
                for (int i = 0; i < ENTRADAS; i++) {
                    w1[j][i] -= tasa * (gw1[j][i] / n + lambda * w1[j][i]);
                }
            }
        }

        this.epocas = epocas;
        this.tasaAprendizaje = tasa;
        this.regularizacion = lambda;
        this.perdidaFinal = perdida / n;
        this.precisionEntrenamiento = precision(x, y);
    }

    public double precision(double[][] x, int[] y) {
        int aciertos = 0;
        for (int m = 0; m < x.length; m++) {
            double[] p = predecir(x[m]);
            int clase = p[1] >= p[0] ? 1 : 0;
            if (clase == y[m]) {
                aciertos++;
            }
        }
        return (double) aciertos / x.length;
    }


    public static double validacionCruzada(double[][] x, int[] y, long semilla, int epocas, double tasa, double lambda) {
        int n = x.length;
        int aciertos = 0;
        for (int fuera = 0; fuera < n; fuera++) {
            double[][] xt = new double[n - 1][];
            int[] yt = new int[n - 1];
            int idx = 0;
            for (int m = 0; m < n; m++) {
                if (m != fuera) {
                    xt[idx] = x[m];
                    yt[idx] = y[m];
                    idx++;
                }
            }
            RedNeuronal red = new RedNeuronal(semilla);
            red.entrenar(xt, yt, epocas, tasa, lambda);
            double[] p = red.predecir(x[fuera]);
            if ((p[1] >= p[0] ? 1 : 0) == y[fuera]) {
                aciertos++;
            }
        }
        return (double) aciertos / n;
    }


    public String aJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"arquitectura\": \"").append(ENTRADAS).append("-").append(OCULTAS).append("-").append(SALIDAS).append("\",\n");
        sb.append("  \"epocas\": ").append(epocas).append(",\n");
        sb.append("  \"tasaAprendizaje\": ").append(num(tasaAprendizaje)).append(",\n");
        sb.append("  \"perdidaFinal\": ").append(num(perdidaFinal)).append(",\n");
        sb.append("  \"precisionEntrenamiento\": ").append(num(precisionEntrenamiento)).append(",\n");
        sb.append("  \"precisionValidacionCruzada\": ").append(num(precisionValidacionCruzada)).append(",\n");
        sb.append("  \"w1\": ").append(arreglo(aplanar(w1))).append(",\n");
        sb.append("  \"b1\": ").append(arreglo(b1)).append(",\n");
        sb.append("  \"w2\": ").append(arreglo(aplanar(w2))).append(",\n");
        sb.append("  \"b2\": ").append(arreglo(b2)).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static RedNeuronal deJson(String json) {
        RedNeuronal red = new RedNeuronal();
        double[] a = leerArreglo(json, "w1");
        double[] b = leerArreglo(json, "b1");
        double[] c = leerArreglo(json, "w2");
        double[] d = leerArreglo(json, "b2");
        if (a.length != OCULTAS * ENTRADAS || b.length != OCULTAS
                || c.length != SALIDAS * OCULTAS || d.length != SALIDAS) {
            throw new IllegalArgumentException("El modelo guardado no tiene la arquitectura esperada.");
        }
        for (int j = 0; j < OCULTAS; j++) {
            System.arraycopy(a, j * ENTRADAS, red.w1[j], 0, ENTRADAS);
        }
        System.arraycopy(b, 0, red.b1, 0, OCULTAS);
        for (int k = 0; k < SALIDAS; k++) {
            System.arraycopy(c, k * OCULTAS, red.w2[k], 0, OCULTAS);
        }
        System.arraycopy(d, 0, red.b2, 0, SALIDAS);
        red.epocas = (int) leerNumero(json, "epocas");
        red.tasaAprendizaje = leerNumero(json, "tasaAprendizaje");
        red.regularizacion = leerNumero(json, "regularizacion");
        red.perdidaFinal = leerNumero(json, "perdidaFinal");
        red.precisionEntrenamiento = leerNumero(json, "precisionEntrenamiento");
        red.precisionValidacionCruzada = leerNumero(json, "precisionValidacionCruzada");
        return red;
    }

    private static String num(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "null";
        }
        return String.format(Locale.ROOT, "%.10f", v);
    }

    private static String arreglo(double[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(num(v[i]));
        }
        return sb.append("]").toString();
    }

    private static double[] aplanar(double[][] m) {
        double[] r = new double[m.length * m[0].length];
        int idx = 0;
        for (double[] fila : m) {
            for (double v : fila) {
                r[idx++] = v;
            }
        }
        return r;
    }

    private static double[] leerArreglo(String json, String clave) {
        int pos = json.indexOf("\"" + clave + "\"");
        if (pos < 0) {
            throw new IllegalArgumentException("Falta '" + clave + "' en el modelo.");
        }
        int ini = json.indexOf('[', pos);
        int fin = json.indexOf(']', ini);
        String[] partes = json.substring(ini + 1, fin).split(",");
        double[] r = new double[partes.length];
        for (int i = 0; i < partes.length; i++) {
            r[i] = Double.parseDouble(partes[i].trim());
        }
        return r;
    }

    private static double leerNumero(String json, String clave) {
        int pos = json.indexOf("\"" + clave + "\"");
        if (pos < 0) {
            return Double.NaN;
        }
        int ini = json.indexOf(':', pos) + 1;
        int fin = json.indexOf(',', ini);
        if (fin < 0 || json.indexOf('\n', ini) < fin) {
            fin = json.indexOf('\n', ini);
        }
        String t = json.substring(ini, fin).trim();
        return t.equals("null") ? Double.NaN : Double.parseDouble(t);
    }


    public void setPrecisionValidacionCruzada(double v) {
        this.precisionValidacionCruzada = v;
    }

    public int getEpocas() {
        return epocas;
    }

    public double getTasaAprendizaje() {
        return tasaAprendizaje;
    }

    public double getRegularizacion() {
        return regularizacion;
    }

    public double getPerdidaFinal() {
        return perdidaFinal;
    }

    public double getPrecisionEntrenamiento() {
        return precisionEntrenamiento;
    }

    public double getPrecisionValidacionCruzada() {
        return precisionValidacionCruzada;
    }

    public boolean tieneValidacionCruzada() {
        return !Double.isNaN(precisionValidacionCruzada);
    }
}
