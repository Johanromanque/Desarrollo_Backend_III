package cl.duoc.formativa1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import cl.duoc.formativa1.business.Transaccion;

@SpringBootTest(properties = "app.input-file=classpath:transacciones_15.csv")
@ActiveProfiles("test")
@Import(ProductionResilienceIntegrationTests.FailureInjectionConfig.class)
class ProductionResilienceIntegrationTests {

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FailureInjectingTransaccionWriter failureWriter;
    @Autowired
    @Qualifier("transaccionesJob")
    private Job transaccionesJob;

    @BeforeEach
    void limpiar() {
        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
        jdbcTemplate.update("DELETE FROM resumen_transacciones_diarias");
        jdbcTemplate.update("DELETE FROM transacciones_procesadas");
        failureWriter.disableFailures();
    }

    @Test
    void transaccionesJobReintentaFallosTransitoriosDelWriter() throws Exception {
        failureWriter.failTransiently(2);

        var execution = jobLauncher.run(transaccionesJob, parameters(201L));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(failureWriter.transientFailures()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_procesadas", Integer.class)).isEqualTo(15);
    }

    @Test
    void transaccionesJobReiniciaLaMismaInstanciaTrasFalloRealDelWriter() throws Exception {
        failureWriter.failPermanentlyOnce();
        JobParameters sameParameters = parameters(202L);

        var failed = jobLauncher.run(transaccionesJob, sameParameters);
        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_procesadas", Integer.class)).isEqualTo(5);

        failureWriter.disableFailures();
        var restarted = jobLauncher.run(transaccionesJob, sameParameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        var restartedStep = restarted.getStepExecutions().stream()
                .filter(step -> step.getStepName().equals("transaccionesStep"))
                .findFirst()
                .orElseThrow();
        assertThat(restartedStep.getReadCount()).isEqualTo(10);
        assertThat(restartedStep.getWriteCount()).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_procesadas", Integer.class)).isEqualTo(15);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_job_instance WHERE job_name = 'transaccionesJob'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_job_execution e JOIN batch_job_instance i "
                        + "ON e.job_instance_id = i.job_instance_id "
                        + "WHERE i.job_name = 'transaccionesJob'",
                Integer.class)).isEqualTo(2);
    }

    private JobParameters parameters(long runId) {
        return new JobParametersBuilder()
                .addLong("run.id", runId)
                .toJobParameters();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureInjectionConfig {

        @Bean
        @Primary
        FailureInjectingTransaccionWriter failureInjectingTransaccionWriter(
                @Qualifier("transaccionWriter") ItemWriter<Transaccion> delegate) {
            return new FailureInjectingTransaccionWriter(delegate);
        }
    }

    static class FailureInjectingTransaccionWriter implements ItemWriter<Transaccion> {

        private final ItemWriter<Transaccion> delegate;
        private final AtomicInteger transientFailuresRemaining = new AtomicInteger();
        private final AtomicInteger transientFailures = new AtomicInteger();
        private final AtomicInteger successfulWritesBeforePermanentFailure = new AtomicInteger(-1);

        FailureInjectingTransaccionWriter(ItemWriter<Transaccion> delegate) {
            this.delegate = delegate;
        }

        void failTransiently(int times) {
            transientFailuresRemaining.set(times);
            transientFailures.set(0);
            successfulWritesBeforePermanentFailure.set(-1);
        }

        void failPermanentlyOnce() {
            transientFailuresRemaining.set(0);
            transientFailures.set(0);
            successfulWritesBeforePermanentFailure.set(1);
        }

        void disableFailures() {
            transientFailuresRemaining.set(0);
            successfulWritesBeforePermanentFailure.set(-1);
        }

        int transientFailures() {
            return transientFailures.get();
        }

        @Override
        public void write(Chunk<? extends Transaccion> chunk) throws Exception {
            if (consumeTransientFailure()) {
                transientFailures.incrementAndGet();
                throw new TransientDataAccessResourceException("Fallo transitorio controlado");
            }
            if (consumePermanentFailure()) {
                throw new IllegalStateException("Fallo permanente controlado");
            }
            delegate.write(chunk);
        }

        private boolean consumeTransientFailure() {
            while (true) {
                int remaining = transientFailuresRemaining.get();
                if (remaining <= 0) {
                    return false;
                }
                if (transientFailuresRemaining.compareAndSet(remaining, remaining - 1)) {
                    return true;
                }
            }
        }

        private boolean consumePermanentFailure() {
            while (true) {
                int successfulWritesRemaining = successfulWritesBeforePermanentFailure.get();
                if (successfulWritesRemaining < 0) {
                    return false;
                }
                int nextValue = successfulWritesRemaining == 0
                        ? -1
                        : successfulWritesRemaining - 1;
                if (successfulWritesBeforePermanentFailure.compareAndSet(
                        successfulWritesRemaining, nextValue)) {
                    return successfulWritesRemaining == 0;
                }
            }
        }
    }
}
