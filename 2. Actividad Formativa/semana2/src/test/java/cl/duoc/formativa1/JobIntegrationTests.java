package cl.duoc.formativa1;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import cl.duoc.formativa1.advanced.BatchProperties;

@SpringBootTest
@ActiveProfiles("test")
class JobIntegrationTests {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BatchProperties batchProperties;

    @Autowired
    @Qualifier("transaccionesJob")
    private Job transaccionesJob;

    @Autowired
    @Qualifier("interesesJob")
    private Job interesesJob;

    @Autowired
    @Qualifier("estadosCuentaJob")
    private Job estadosCuentaJob;

    @BeforeEach
    void limpiar() {
        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
        jdbcTemplate.update("DELETE FROM estados_cuenta_anuales");
        jdbcTemplate.update("DELETE FROM movimientos_anuales");
        jdbcTemplate.update("DELETE FROM intereses_calculados");
        jdbcTemplate.update("DELETE FROM resumen_transacciones_diarias");
        jdbcTemplate.update("DELETE FROM transacciones_procesadas");
    }

    @Test
    void transaccionesEsReproducibleEIdempotente() throws Exception {
        JobExecution primera = ejecutar(transaccionesJob, 1L);
        JobExecution segunda = ejecutar(transaccionesJob, 2L);

        assertThat(primera.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(segunda.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertConteos(primera, "transaccionesStep", 8, 8, 2);
        assertThat(conteo("transacciones_procesadas")).isEqualTo(8);
        assertThat(conteo("resumen_transacciones_diarias")).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT total_transacciones FROM resumen_transacciones_diarias
                WHERE fecha = DATE '2024-01-05'
                """, Integer.class)).isEqualTo(2);
        assertThat(instancias("transaccionesJob")).isEqualTo(2);
    }

    @Test
    void interesesEsReproducibleEIdempotente() throws Exception {
        JobExecution primera = ejecutar(interesesJob, 1L);
        JobExecution segunda = ejecutar(interesesJob, 2L);

        assertThat(primera.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(segunda.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertConteos(primera, "interesesStep", 6, 6, 2);
        assertThat(conteo("intereses_calculados")).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT saldo_final FROM intereses_calculados WHERE cuenta_id = 101
                """, java.math.BigDecimal.class)).isEqualByComparingTo("5050.00");
        assertThat(instancias("interesesJob")).isEqualTo(2);
    }

    @Test
    void estadosCuentaEsReproducibleEIdempotente() throws Exception {
        JobExecution primera = ejecutar(estadosCuentaJob, 1L);
        JobExecution segunda = ejecutar(estadosCuentaJob, 2L);

        assertThat(primera.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(segunda.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertConteos(primera, "estadosCuentaStep", 8, 8, 1);
        assertThat(conteo("movimientos_anuales")).isEqualTo(8);
        assertThat(conteo("estados_cuenta_anuales")).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT saldo_neto FROM estados_cuenta_anuales WHERE cuenta_id = 101
                """, java.math.BigDecimal.class)).isEqualByComparingTo("500.00");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT cantidad_movimientos FROM estados_cuenta_anuales WHERE cuenta_id = 101
                """, Integer.class)).isEqualTo(2);
        assertThat(instancias("estadosCuentaJob")).isEqualTo(2);
    }

    @Test
    void configuracionAcademicaUsaChunkCincoYTresThreads() {
        assertThat(batchProperties.getChunkSize()).isEqualTo(5);
        assertThat(batchProperties.getCorePoolSize()).isEqualTo(3);
        assertThat(batchProperties.getMaxPoolSize()).isEqualTo(3);
    }

    private JobExecution ejecutar(Job job, long runId) throws Exception {
        return jobLauncher.run(job, new JobParametersBuilder()
                .addLong("run.id", runId)
                .toJobParameters());
    }

    private void assertConteos(
            JobExecution execution,
            String stepName,
            long read,
            long write,
            long readSkip) {
        StepExecution step = execution.getStepExecutions().stream()
                .filter(candidate -> candidate.getStepName().equals(stepName))
                .findFirst()
                .orElseThrow();
        assertThat(step.getReadCount()).isEqualTo(read);
        assertThat(step.getWriteCount()).isEqualTo(write);
        assertThat(step.getReadSkipCount()).isEqualTo(readSkip);
    }

    private int conteo(String tabla) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private int instancias(String jobName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_job_instance WHERE job_name = ?",
                Integer.class,
                jobName);
    }
}
