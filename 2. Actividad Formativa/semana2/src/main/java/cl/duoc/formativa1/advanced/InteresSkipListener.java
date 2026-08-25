package cl.duoc.formativa1.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import cl.duoc.formativa1.business.InteresCuenta;

@Component
public class InteresSkipListener
        implements SkipListener<InteresCuenta, InteresCuenta> {

    private static final Logger logger =
            LoggerFactory.getLogger(InteresSkipListener.class);

    @Override
    public void onSkipInRead(Throwable throwable) {

        if (throwable instanceof FlatFileParseException exception) {

            logger.warn(
                    "[SKIP-LISTENER-INTERESES] Error de lectura. Línea {} omitida",
                    exception.getLineNumber()
            );

        } else {

            logger.warn(
                    "[SKIP-LISTENER-INTERESES] Error durante la lectura: {}",
                    throwable.getClass().getSimpleName()
            );
        }
    }

    @Override
    public void onSkipInProcess(
            InteresCuenta item,
            Throwable throwable) {

        logger.warn(
                "[SKIP-LISTENER-INTERESES] Registro omitido durante procesamiento: {}",
                throwable.getClass().getSimpleName()
        );
    }

    @Override
    public void onSkipInWrite(
            InteresCuenta item,
            Throwable throwable) {

        logger.warn(
                "[SKIP-LISTENER-INTERESES] Registro omitido durante escritura: {}",
                throwable.getClass().getSimpleName()
        );
    }
}
