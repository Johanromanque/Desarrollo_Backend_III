package cl.duoc.formativa1.jobs;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.formativa1.items.ResumenService;

@Configuration
public class ResumenStepConfig {

    @Bean
    public Step transaccionesResumenStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ResumenService resumenService) {
        return new StepBuilder("transaccionesResumenStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    resumenService.reconstruirResumenTransacciones();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step estadosCuentaResumenStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ResumenService resumenService) {
        return new StepBuilder("estadosCuentaResumenStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    resumenService.reconstruirEstadosCuenta();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
