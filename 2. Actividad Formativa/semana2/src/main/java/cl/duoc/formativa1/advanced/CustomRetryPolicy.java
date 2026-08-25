package cl.duoc.formativa1.advanced;

import java.time.Duration;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Component
public class CustomRetryPolicy {

    public RetryPolicy crearRetryPolicy() {

        return RetryPolicy.builder()

                // Solo errores temporales de acceso a datos
                .includes(TransientDataAccessException.class)

                // Máximo 3 reintentos
                .maxRetries(3)

                // Espera de medio segundo entre intentos
                .delay(Duration.ofMillis(500))

                .build();
    }
}