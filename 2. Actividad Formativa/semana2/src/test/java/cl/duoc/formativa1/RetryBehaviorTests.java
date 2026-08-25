package cl.duoc.formativa1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.dao.TransientDataAccessResourceException;

import cl.duoc.formativa1.advanced.CustomRetryListener;
import cl.duoc.formativa1.advanced.CustomRetryPolicy;

class RetryBehaviorTests {

    @Test
    void recuperaEnElTercerIntentoYEscribeUnaSolaVez() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        AtomicInteger escrituras = new AtomicInteger();
        RecordingRetryListener listener = new RecordingRetryListener();
        RetryTemplate template = template(listener);

        String result = template.execute(() -> {
            if (intentos.incrementAndGet() < 3) {
                throw new TransientDataAccessResourceException("temporal");
            }
            escrituras.incrementAndGet();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(intentos).hasValue(3);
        assertThat(escrituras).hasValue(1);
        assertThat(listener.failures.get()).isGreaterThanOrEqualTo(1);
        assertThat(listener.successes).hasValue(1);
    }

    @Test
    void fallaCuandoSeAgotaElMaximoDeReintentos() {
        AtomicInteger intentos = new AtomicInteger();
        RecordingRetryListener listener = new RecordingRetryListener();
        RetryTemplate template = template(listener);

        assertThatThrownBy(() -> template.execute(() -> {
            intentos.incrementAndGet();
            throw new TransientDataAccessResourceException("sigue fallando");
        })).isInstanceOf(RetryException.class);

        assertThat(intentos).hasValue(4);
        assertThat(listener.exhaustions).hasValue(1);
    }

    @Test
    void noReintentaErroresPermanentes() {
        RetryPolicy policy = new CustomRetryPolicy().crearRetryPolicy();
        assertThat(policy.shouldRetry(new TransientDataAccessResourceException("temporal"))).isTrue();
        assertThat(policy.shouldRetry(new IllegalArgumentException("permanente"))).isFalse();
    }

    private RetryTemplate template(CustomRetryListener listener) {
        RetryTemplate template = new RetryTemplate(new CustomRetryPolicy().crearRetryPolicy());
        template.setRetryListener(listener);
        return template;
    }

    private static class RecordingRetryListener extends CustomRetryListener {
        private final AtomicInteger failures = new AtomicInteger();
        private final AtomicInteger successes = new AtomicInteger();
        private final AtomicInteger exhaustions = new AtomicInteger();

        @Override
        public void onRetryFailure(RetryPolicy policy, Retryable<?> retryable, Throwable throwable) {
            failures.incrementAndGet();
            super.onRetryFailure(policy, retryable, throwable);
        }

        @Override
        public void onRetrySuccess(RetryPolicy policy, Retryable<?> retryable, Object result) {
            successes.incrementAndGet();
            super.onRetrySuccess(policy, retryable, result);
        }

        @Override
        public void onRetryPolicyExhaustion(
                RetryPolicy policy, Retryable<?> retryable, RetryException exception) {
            exhaustions.incrementAndGet();
            super.onRetryPolicyExhaustion(policy, retryable, exception);
        }

        @Override
        public void beforeRetry(RetryPolicy policy, Retryable<?> retryable, RetryState state) {
            super.beforeRetry(policy, retryable, state);
        }
    }
}
