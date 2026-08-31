package cl.duoc.formativa1.advanced;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

@Component
public class CustomSkipPolicy implements SkipPolicy {

    private static final long SKIP_LIMIT = 10;

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {

        // Spring Batch puede consultar previamente si
        // una excepción es omisible.
        if (skipCount < 0) {
            return throwable instanceof FlatFileParseException;
        }

        // Si llegamos al límite, ya no se permiten más omisiones.
        if (skipCount >= SKIP_LIMIT) {
            System.out.println(
                    "[SKIP-LIMIT] Se alcanzó el máximo de "
                    + SKIP_LIMIT + " registros omitidos."
            );

            return false;
        }

        // Solo omitimos errores producidos al leer datos
        // mal formados desde el CSV.
        if (throwable instanceof FlatFileParseException exception) {

            System.out.println(
                    "[CUSTOM-SKIP] Línea "
                    + exception.getLineNumber()
                    + " omitida: "
                    + exception.getInput()
            );

            return true;
        }

        // Cualquier otro error se considera importante.
        return false;
    }
}