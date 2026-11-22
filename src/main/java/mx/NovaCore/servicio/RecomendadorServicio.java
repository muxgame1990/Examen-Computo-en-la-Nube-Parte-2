package mx.NovaCore.servicio;

import mx.NovaCore.ia.Codificador;
import mx.NovaCore.ia.RedNeuronal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;


@Service
public class RecomendadorServicio {

    private static final Logger LOG = LoggerFactory.getLogger(RecomendadorServicio.class);

    private static final long SEMILLA = 42;
    private static final int EPOCAS = 2000;
    private static final double TASA = 0.3;
    private static final double REGULARIZACION = 0.03;

    private final JdbcTemplate jdbc;
    private final ModeloAlmacenamiento almacenamiento;

    private volatile RedNeuronal red;
    private volatile String origen = "Sin modelo todavía";

    public RecomendadorServicio(JdbcTemplate jdbc, ModeloAlmacenamiento almacenamiento) {
        this.jdbc = jdbc;
        this.almacenamiento = almacenamiento;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        try {
            cargarOEntrenar();
        } catch (Exception e) {
            LOG.warn("No se pudo preparar la red neuronal al arrancar (¿se corrió db/ia_red_neuronal.sql?): {}",
                    e.getMessage());
        }
    }

    private synchronized void cargarOEntrenar() throws IOException {
        Optional<String> guardado = almacenamiento.leer();
        if (guardado.isPresent()) {
            try {
                red = RedNeuronal.deJson(guardado.get());
                origen = "Cargado desde " + almacenamiento.ubicacion();
                LOG.info("Modelo de red neuronal cargado desde {}", almacenamiento.ubicacion());
                return;
            } catch (RuntimeException e) {
                LOG.warn("El modelo guardado no sirve ({}), se entrenará uno nuevo.", e.getMessage());
            }
        }
        entrenar();
    }

    public synchronized void entrenar() throws IOException {
        List<Dato> datos = listarDatos();
        if (datos.size() < 4) {
            throw new IllegalStateException(
                    "Faltan datos de entrenamiento en RDS. Corre db/ia_red_neuronal.sql.");
        }

        double[][] x = new double[datos.size()][];
        int[] y = new int[datos.size()];
        for (int i = 0; i < datos.size(); i++) {
            Dato d = datos.get(i);
            x[i] = Codificador.codificar(d.getInteres(), d.getEjercicio(), d.getMotivacion(), d.getTecnologia());
            y[i] = Codificador.clase(d.getModelo());
        }

        RedNeuronal nueva = new RedNeuronal(SEMILLA);
        nueva.entrenar(x, y, EPOCAS, TASA, REGULARIZACION);
        nueva.setPrecisionValidacionCruzada(
                RedNeuronal.validacionCruzada(x, y, SEMILLA, EPOCAS, TASA, REGULARIZACION));

        almacenamiento.guardar(nueva.aJson());
        red = nueva;
        origen = "Entrenado ahora con " + datos.size() + " datos de RDS y guardado en " + almacenamiento.ubicacion();
        LOG.info("Red neuronal entrenada: pérdida={}, precisión={}, validación cruzada={}",
                nueva.getPerdidaFinal(), nueva.getPrecisionEntrenamiento(), nueva.getPrecisionValidacionCruzada());
    }

    public Resultado predecir(String idPublico, String interes, String ejercicio,
                              String motivacion, String tecnologia) throws IOException {
        if (red == null) {
            cargarOEntrenar();
        }
        double[] p = red.predecir(Codificador.codificar(interes, ejercicio, motivacion, tecnologia));
        Resultado r = new Resultado(p[0], p[1]);

        try {
            jdbc.update("INSERT INTO ia_consulta (id_publico, interes, ejercicio, motivacion, tecnologia, "
                            + "prob_i100, prob_i500, recomendado, fecha_hora) VALUES (?,?,?,?,?,?,?,?,NOW())",
                    idPublico, interes, ejercicio, motivacion, tecnologia, p[0], p[1], r.getRecomendado());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo registrar la consulta: {}", e.getMessage());
        }
        return r;
    }

    public List<Dato> listarDatos() {
        return jdbc.query(
                "SELECT dato_id, interes, ejercicio, motivacion, tecnologia, modelo "
                        + "FROM ia_dato_entrenamiento ORDER BY dato_id",
                (rs, n) -> new Dato(rs.getInt("dato_id"), rs.getString("interes"), rs.getString("ejercicio"),
                        rs.getString("motivacion"), rs.getString("tecnologia"), rs.getString("modelo")));
    }

    public List<Consulta> ultimasConsultas() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return jdbc.query(
                "SELECT id_publico, interes, ejercicio, motivacion, tecnologia, prob_i500, recomendado, fecha_hora "
                        + "FROM ia_consulta ORDER BY consulta_id DESC LIMIT 5",
                (rs, n) -> new Consulta(rs.getString("id_publico"),
                        rs.getString("interes") + " / " + rs.getString("ejercicio") + " / "
                                + rs.getString("motivacion") + " / " + rs.getString("tecnologia"),
                        rs.getDouble("prob_i500"), rs.getString("recomendado"),
                        rs.getTimestamp("fecha_hora").toLocalDateTime().format(formato)));
    }

    public RedNeuronal getRed() {
        return red;
    }

    public String getOrigen() {
        return origen;
    }


    public static class Dato implements Serializable {
        private final int id;
        private final String interes;
        private final String ejercicio;
        private final String motivacion;
        private final String tecnologia;
        private final String modelo;

        public Dato(int id, String interes, String ejercicio, String motivacion, String tecnologia, String modelo) {
            this.id = id;
            this.interes = interes;
            this.ejercicio = ejercicio;
            this.motivacion = motivacion;
            this.tecnologia = tecnologia;
            this.modelo = modelo;
        }

        public int getId() { return id; }
        public String getInteres() { return interes; }
        public String getEjercicio() { return ejercicio; }
        public String getMotivacion() { return motivacion; }
        public String getTecnologia() { return tecnologia; }
        public String getModelo() { return modelo; }
    }

    public static class Resultado implements Serializable {
        private final double probI100;
        private final double probI500;

        public Resultado(double probI100, double probI500) {
            this.probI100 = probI100;
            this.probI500 = probI500;
        }

        public double getProbI100() { return probI100; }
        public double getProbI500() { return probI500; }
        public String getRecomendado() { return probI500 >= probI100 ? "i500" : "i100"; }
        public String getPorcentajeI100() { return String.format(java.util.Locale.US, "%.1f", probI100 * 100); }
        public String getPorcentajeI500() { return String.format(java.util.Locale.US, "%.1f", probI500 * 100); }
    }

    public static class Consulta implements Serializable {
        private final String usuario;
        private final String perfil;
        private final double probI500;
        private final String recomendado;
        private final String fecha;

        public Consulta(String usuario, String perfil, double probI500, String recomendado, String fecha) {
            this.usuario = usuario;
            this.perfil = perfil;
            this.probI500 = probI500;
            this.recomendado = recomendado;
            this.fecha = fecha;
        }

        public String getUsuario() { return usuario == null ? "(sin sesión)" : usuario; }
        public String getPerfil() { return perfil; }
        public String getProbI500() { return String.format(java.util.Locale.US, "%.1f", probI500 * 100); }
        public String getRecomendado() { return recomendado; }
        public String getFecha() { return fecha; }
    }
}
