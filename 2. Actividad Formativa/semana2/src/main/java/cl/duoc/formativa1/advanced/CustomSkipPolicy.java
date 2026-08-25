package cl.duoc.formativa1.advanced;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomSkipPolicy implements SkipPolicy {

    private static final long SKIP_LIMIT = 10;
    private static final Logger logger = LoggerFactory.getLogger(CustomSkipPolicy.class);

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {

        // Spring Batch puede consultar previamente si
        // una excepción es omisible.
        if (skipCount < 0) {
            return throwable instanceof FlatFileParseException;
        }

        // Si llegamos al límite, ya no se permiten más omisiones.
        if (skipCount >= SKIP_LIMIT) {
            logger.error("[SKIP-LIMIT] Se alcanzo el maximo de {} registros omitidos", SKIP_LIMIT);

            return false;
        }

        // Solo omitimos errores producidos al leer datos
        // mal formados desde el CSV.
        if (throwable instanceof FlatFileParseException exception) {
            logger.warn(
                    "[CUSTOM-SKIP] Linea {} omitida por formato invalido",
                    exception.getLineNumber());

            return true;
        }

        // Cualquier otro error se considera importante.
        return false;
    }
}
