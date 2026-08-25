package cl.duoc.formativa1;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import cl.duoc.formativa1.advanced.ProcessingThreadTracker;

@SpringBootTest(properties = "app.input-file=classpath:transacciones_15.csv")
@ActiveProfiles("test")
class ParallelProcessingTests {

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ProcessingThreadTracker tracker;
    @Autowired
    @Qualifier("transaccionesJob")
    private Job transaccionesJob;

    @BeforeEach
    void limpiar() {
        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
        jdbcTemplate.update("DELETE FROM resumen_transacciones_diarias");
        jdbcTemplate.update("DELETE FROM transacciones_procesadas");
        tracker.reset();
    }

    @Test
    void datasetDeQuinceRegistrosUsaTresThreads() throws Exception {
        var execution = jobLauncher.run(transaccionesJob, new JobParametersBuilder()
                .addLong("run.id", 15L)
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_procesadas", Integer.class)).isEqualTo(15);
        assertThat(tracker.snapshot())
                .containsExactlyInAnyOrder("Batch-Thread-1", "Batch-Thread-2", "Batch-Thread-3");
    }
}
