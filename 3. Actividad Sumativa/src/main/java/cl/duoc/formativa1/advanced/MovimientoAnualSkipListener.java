package cl.duoc.formativa1.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import cl.duoc.formativa1.business.MovimientoAnual;

@Component
public class MovimientoAnualSkipListener
        implements SkipListener<MovimientoAnual, MovimientoAnual> {

    private static final Logger logger =
            LoggerFactory.getLogger(MovimientoAnualSkipListener.class);

    @Override
    public void onSkipInRead(Throwable throwable) {

        if (throwable instanceof FlatFileParseException exception) {

            logger.warn(
                    "[SKIP-LISTENER-ESTADOS] Error de lectura. Línea {} omitida: {}",
                    exception.getLineNumber(),
                    exception.getInput()
            );

        } else {

            logger.warn(
                    "[SKIP-LISTENER-ESTADOS] Error durante la lectura: {}",
                    throwable.getMessage()
            );
        }
    }

    @Override
    public void onSkipInProcess(
            MovimientoAnual item,
            Throwable throwable) {

        logger.warn(
                "[SKIP-LISTENER-ESTADOS] Movimiento omitido durante procesamiento: {} - Error: {}",
                item,
                throwable.getMessage()
        );
    }

    @Override
    public void onSkipInWrite(
            MovimientoAnual item,
            Throwable throwable) {

        logger.warn(
                "[SKIP-LISTENER-ESTADOS] Movimiento omitido durante escritura: {} - Error: {}",
                item,
                throwable.getMessage()
        );
    }
}