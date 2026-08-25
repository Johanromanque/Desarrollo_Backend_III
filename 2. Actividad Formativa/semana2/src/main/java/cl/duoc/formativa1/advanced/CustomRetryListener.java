package cl.duoc.formativa1.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.Retryable;
import org.springframework.stereotype.Component;

@Component
public class CustomRetryListener implements RetryListener {

    private static final Logger logger =
            LoggerFactory.getLogger(CustomRetryListener.class);

    @Override
    public void beforeRetry(
            RetryPolicy retryPolicy,
            Retryable<?> retryable,
            RetryState retryState) {

        logger.warn(
                "[RETRY-LISTENER] Ejecutando reintento número {}",
                retryState.getRetryCount()
        );
    }

    @Override
    public void onRetryFailure(
            RetryPolicy retryPolicy,
            Retryable<?> retryable,
            Throwable throwable) {

        logger.warn(
                "[RETRY-LISTENER] Falló un intento por {}",
                throwable.getClass().getSimpleName()
        );
    }

    @Override
    public void onRetrySuccess(
            RetryPolicy retryPolicy,
            Retryable<?> retryable,
            Object result) {

        logger.info(
                "[RETRY-LISTENER] Operación recuperada correctamente después del reintento."
        );
    }

    @Override
    public void onRetryPolicyExhaustion(
            RetryPolicy retryPolicy,
            Retryable<?> retryable,
            RetryException exception) {

        if (exception.getRetryCount() > 0) {

            logger.error(
                    "[RETRY-LISTENER] Se agotaron los reintentos. Total: {}",
                    exception.getRetryCount()
            );
        }
    }
}
