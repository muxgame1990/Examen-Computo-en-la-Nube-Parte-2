package mx.NovaCore.vista.Frijol;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import mx.NovaCore.ia.RedNeuronal;
import mx.NovaCore.servicio.RecomendadorServicio;
import mx.NovaCore.servicio.RecomendadorServicio.Consulta;
import mx.NovaCore.servicio.RecomendadorServicio.Dato;
import mx.NovaCore.servicio.RecomendadorServicio.Resultado;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Named("recomendadorBean")
@ViewScoped
public class RecomendadorBean implements Serializable {

    @Inject
    private RecomendadorServicio servicio;

    @Inject
    private SesionBean sesionBean;

    private String interes = "ambos";
    private String ejercicio = "sedentario";
    private String motivacion = "moderado";
    private String tecnologia = "si";

    private Resultado resultado;
    private List<Dato> datos = new ArrayList<>();
    private List<Consulta> consultas = new ArrayList<>();

    @PostConstruct
    public void init() {
        refrescar();
    }

    private void refrescar() {
        try {
            datos = servicio.listarDatos();
            consultas = servicio.ultimasConsultas();
        } catch (RuntimeException e) {
            datos = new ArrayList<>();
            consultas = new ArrayList<>();
            mensaje(FacesMessage.SEVERITY_WARN, "Sin datos de IA",
                    "No se encontraron las tablas de IA en RDS. Corre db/ia_red_neuronal.sql.");
        }
    }

    public void recomendar() {
        try {
            String usuario = sesionBean.isLoggedIn() ? sesionBean.getUsuario() : null;
            resultado = servicio.predecir(usuario, interes, ejercicio, motivacion, tecnologia);
            consultas = servicio.ultimasConsultas();
        } catch (Exception e) {
            resultado = null;
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo recomendar", e.getMessage());
        }
    }

    public void reentrenar() {
        if (!sesionBean.isAdmin()) {
            mensaje(FacesMessage.SEVERITY_ERROR, "Acceso denegado", "Solo el administrador puede reentrenar.");
            return;
        }
        try {
            servicio.entrenar();
            refrescar();
            mensaje(FacesMessage.SEVERITY_INFO, "Red neuronal entrenada",
                    "El modelo se guardó en " + servicio.getOrigen());
        } catch (Exception e) {
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo entrenar", e.getMessage());
        }
    }

    private void mensaje(FacesMessage.Severity severidad, String titulo, String detalle) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, titulo, detalle));
    }


    public boolean isModeloListo() {
        return servicio.getRed() != null;
    }

    public String getOrigen() {
        return servicio.getOrigen();
    }

    public String getArquitectura() {
        return RedNeuronal.ENTRADAS + " entradas → " + RedNeuronal.OCULTAS
                + " neuronas ocultas (tanh) → " + RedNeuronal.SALIDAS + " salidas (softmax)";
    }

    public int getEpocas() {
        return isModeloListo() ? servicio.getRed().getEpocas() : 0;
    }

    public String getPerdida() {
        return isModeloListo() ? String.format(Locale.US, "%.4f", servicio.getRed().getPerdidaFinal()) : "-";
    }

    public String getPrecisionEntrenamiento() {
        return isModeloListo()
                ? String.format(Locale.US, "%.1f %%", servicio.getRed().getPrecisionEntrenamiento() * 100) : "-";
    }

    public String getPrecisionValidacion() {
        return isModeloListo() && servicio.getRed().tieneValidacionCruzada()
                ? String.format(Locale.US, "%.1f %%", servicio.getRed().getPrecisionValidacionCruzada() * 100) : "-";
    }


    public String getInteres() { return interes; }
    public void setInteres(String interes) { this.interes = interes; }
    public String getEjercicio() { return ejercicio; }
    public void setEjercicio(String ejercicio) { this.ejercicio = ejercicio; }
    public String getMotivacion() { return motivacion; }
    public void setMotivacion(String motivacion) { this.motivacion = motivacion; }
    public String getTecnologia() { return tecnologia; }
    public void setTecnologia(String tecnologia) { this.tecnologia = tecnologia; }
    public Resultado getResultado() { return resultado; }
    public List<Dato> getDatos() { return datos; }
    public List<Consulta> getConsultas() { return consultas; }
}
