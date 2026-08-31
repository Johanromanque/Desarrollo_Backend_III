package cl.duoc.formativa1.advanced;

import cl.duoc.formativa1.business.Transaccion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

@Component
public class TransaccionSkipListener
        implements SkipListener<Transaccion, Transaccion> {

    private static final Logger logger =
            LoggerFactory.getLogger(TransaccionSkipListener.class);

    @Override
    public void onSkipInRead(Throwable throwable) {

        if (throwable instanceof FlatFileParseException exception) {

            logger.warn(
                    "[SKIP-LISTENER] Error de lectura. Línea {} omitida: {}",
                    exception.getLineNumber(),
                    exception.getInput()
            );

        } else {

            logger.warn(
                    "[SKIP-LISTENER] Error durante la lectura: {}",
                    throwable.getMessage()
            );
        }
    }

    @Override
    public void onSkipInProcess(
            Transaccion item,
            Throwable throwable) {

        logger.warn(
                "[SKIP-LISTENER] Transacción {} omitida durante procesamiento: {}",
                item.getId(),
                throwable.getMessage()
        );
    }

    @Override
    public void onSkipInWrite(
            Transaccion item,
            Throwable throwable) {

        logger.warn(
                "[SKIP-LISTENER] Transacción {} omitida durante escritura: {}",
                item.getId(),
                throwable.getMessage()
        );
    }
}