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

@SpringBootTest(properties = "app.input-file=classpath:transacciones_11_invalid.csv")
@ActiveProfiles("test")
class SkipLimitIntegrationTests {

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("transaccionesJob")
    private Job transaccionesJob;

    @BeforeEach
    void limpiar() {
        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
        jdbcTemplate.update("DELETE FROM resumen_transacciones_diarias");
        jdbcTemplate.update("DELETE FROM transacciones_procesadas");
    }

    @Test
    void laOmisionNumeroOnceHaceFallarElStep() throws Exception {
        var execution = jobLauncher.run(transaccionesJob, new JobParametersBuilder()
                .addLong("run.id", 11L)
                .toJobParameters());

        var step = execution.getStepExecutions().stream()
                .filter(candidate -> candidate.getStepName().equals("transaccionesStep"))
                .findFirst()
                .orElseThrow();
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(step.getReadSkipCount()).isEqualTo(10);
        assertThat(step.getFailureExceptions()).isNotEmpty();
    }
}
