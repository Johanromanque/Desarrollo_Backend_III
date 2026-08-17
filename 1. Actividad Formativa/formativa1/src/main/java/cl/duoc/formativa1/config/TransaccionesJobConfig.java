package cl.duoc.formativa1.config;

import cl.duoc.formativa1.model.Transaccion;

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
public class TransaccionesJobConfig {

    @Bean
    public Step transaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Transaccion> transaccionReader,
            ItemProcessor<Transaccion, Transaccion> transaccionProcessor,
            ItemWriter<Transaccion> transaccionWriter) {

        return new ChunkOrientedStepBuilder<Transaccion, Transaccion>(
                "transaccionesStep",
                jobRepository,
                5)
                .reader(transaccionReader)
                .processor(transaccionProcessor)
                .writer(transaccionWriter)
                .transactionManager(transactionManager)

                // Si una línea del CSV está realmente mal formada,
                // Spring Batch puede omitirla.
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)

                .build();
    }


    @Bean
    public Job transaccionesJob(
            JobRepository jobRepository,
            Step transaccionesStep) {

        return new JobBuilder(
                "transaccionesJob",
                jobRepository)

                .incrementer(new RunIdIncrementer())
                .start(transaccionesStep)
                .build();
    }
}