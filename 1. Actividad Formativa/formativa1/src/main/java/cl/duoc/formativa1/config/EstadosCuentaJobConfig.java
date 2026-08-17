package cl.duoc.formativa1.config;

import cl.duoc.formativa1.model.MovimientoAnual;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class EstadosCuentaJobConfig {

    @Bean
    public Step estadosCuentaStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<MovimientoAnual> movimientoAnualReader,
            ItemProcessor<MovimientoAnual, MovimientoAnual> movimientoAnualProcessor,
            ItemWriter<MovimientoAnual> movimientoAnualWriter) {

        return new ChunkOrientedStepBuilder
                <MovimientoAnual, MovimientoAnual>(
                        "estadosCuentaStep",
                        jobRepository,
                        5)
                .reader(movimientoAnualReader)
                .processor(movimientoAnualProcessor)
                .writer(movimientoAnualWriter)
                .transactionManager(transactionManager)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .build();
    }


    @Bean
    public Job estadosCuentaJob(
            JobRepository jobRepository,
            Step estadosCuentaStep) {

        return new JobBuilder(
                "estadosCuentaJob",
                jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(estadosCuentaStep)
                .build();
    }
}