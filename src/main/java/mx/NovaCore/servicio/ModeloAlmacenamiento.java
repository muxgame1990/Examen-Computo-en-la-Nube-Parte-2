package mx.NovaCore.servicio;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;


@Service
public class ModeloAlmacenamiento {

    private static final String CLAVE = "modelos/red-neuronal.json";

    private final boolean usaS3;
    private final String bucket;
    private final S3Client s3;
    private final Path archivoLocal;

    public ModeloAlmacenamiento(@Value("${app.imagenes.almacenamiento}") String modo,
                                @Value("${aws.s3.bucket}") String bucket,
                                @Value("${aws.region}") String region) {
        this.usaS3 = "s3".equalsIgnoreCase(modo);
        this.bucket = bucket;
        if (usaS3) {
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalStateException("Falta la variable de entorno S3_BUCKET.");
            }
            this.s3 = S3Client.builder().region(Region.of(region)).build();
            this.archivoLocal = null;
        } else {
            this.s3 = null;
            this.archivoLocal = Paths.get(System.getProperty("user.home"),
                    "avanti-shop", "modelos", "red-neuronal.json");
        }
    }

    public void guardar(String json) throws IOException {
        if (usaS3) {
            PutObjectRequest solicitud = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(CLAVE)
                    .contentType("application/json")
                    .build();
            try {
                s3.putObject(solicitud, RequestBody.fromString(json, StandardCharsets.UTF_8));
            } catch (SdkException e) {
                throw new IOException("No se pudo guardar el modelo en S3: " + e.getMessage(), e);
            }
        } else {
            Files.createDirectories(archivoLocal.getParent());
            Files.writeString(archivoLocal, json, StandardCharsets.UTF_8);
        }
    }

    public Optional<String> leer() throws IOException {
        if (usaS3) {
            GetObjectRequest solicitud = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(CLAVE)
                    .build();
            try {
                return Optional.of(s3.getObjectAsBytes(solicitud).asUtf8String());
            } catch (NoSuchKeyException e) {
                return Optional.empty();
            } catch (SdkException e) {
                throw new IOException("No se pudo leer el modelo de S3: " + e.getMessage(), e);
            }
        }
        if (Files.exists(archivoLocal)) {
            return Optional.of(Files.readString(archivoLocal, StandardCharsets.UTF_8));
        }
        return Optional.empty();
    }

    public String ubicacion() {
        return usaS3 ? "s3://" + bucket + "/" + CLAVE : archivoLocal.toString();
    }

    @PreDestroy
    public void cerrar() {
        if (s3 != null) {
            s3.close();
        }
    }
}
