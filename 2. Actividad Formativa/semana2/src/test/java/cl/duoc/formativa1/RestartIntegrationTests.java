package cl.duoc.formativa1;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
@ActiveProfiles("test")
class RestartIntegrationTests {

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpiar() {
        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS restart_items (id INT PRIMARY KEY)");
        jdbcTemplate.update("DELETE FROM restart_items");
    }

    @Test
    void reiniciaLaMismaInstanciaDesdeElCheckpoint() throws Exception {
        AtomicBoolean provocarFallo = new AtomicBoolean(true);
        AtomicInteger llamadasWriter = new AtomicInteger();

        FlatFileItemReader<Integer> reader = new FlatFileItemReaderBuilder<Integer>()
                .name("checkpointRestartReader")
                .resource(new ClassPathResource("restart_items.csv"))
                .delimited(spec -> spec.delimiter(",").names("id"))
                .fieldSetMapper(fieldSet -> fieldSet.readInt("id"))
                .build();

        ItemWriter<Integer> writer = chunk -> {
            int llamada = llamadasWriter.incrementAndGet();
            if (llamada == 2 && provocarFallo.get()) {
                throw new IllegalStateException("Fallo controlado despues del primer commit");
            }
            for (Integer id : chunk) {
                try {
                    jdbcTemplate.update("INSERT INTO restart_items (id) VALUES (?)", id);
                } catch (DuplicateKeyException duplicate) {
                    jdbcTemplate.update("UPDATE restart_items SET id = ? WHERE id = ?", id, id);
                }
            }
        };

        Step step = new ChunkOrientedStepBuilder<Integer, Integer>(
                "checkpointRestartStep", jobRepository, 5)
                .reader(reader)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
        Job job = new JobBuilder("checkpointRestartJob", jobRepository)
                .start(step)
                .build();
        JobParameters sameParameters = new JobParameters();

        var failed = jobLauncher.run(job, sameParameters);
        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM restart_items", Integer.class)).isEqualTo(5);

        provocarFallo.set(false);
        var restarted = jobLauncher.run(job, sameParameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM restart_items", Integer.class)).isEqualTo(12);
        assertThat(restarted.getStepExecutions()).singleElement()
                .satisfies(execution -> {
                    assertThat(execution.getReadCount()).isEqualTo(7);
                    assertThat(execution.getWriteCount()).isEqualTo(7);
                });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_job_instance WHERE job_name = 'checkpointRestartJob'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batch_job_execution e JOIN batch_job_instance i "
                        + "ON e.job_instance_id = i.job_instance_id "
                        + "WHERE i.job_name = 'checkpointRestartJob'",
                Integer.class)).isEqualTo(2);
    }
}
