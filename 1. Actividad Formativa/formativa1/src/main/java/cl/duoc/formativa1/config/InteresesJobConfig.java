package cl.duoc.formativa1.config;

import cl.duoc.formativa1.model.InteresCuenta;

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
public class InteresesJobConfig {

    @Bean
    public Step interesesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<InteresCuenta> interesReader,
            ItemProcessor<InteresCuenta, InteresCuenta> interesProcessor,
            ItemWriter<InteresCuenta> interesWriter) {

        return new ChunkOrientedStepBuilder
                <InteresCuenta, InteresCuenta>(
                        "interesesStep",
                        jobRepository,
                        5)
                .reader(interesReader)
                .processor(interesProcessor)
                .writer(interesWriter)
                .transactionManager(transactionManager)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .build();
    }


    @Bean
    public Job interesesJob(
            JobRepository jobRepository,
            Step interesesStep) {

        return new JobBuilder(
                "interesesJob",
                jobRepository)
                .incrementer(
                        new RunIdIncrementer())
                .start(interesesStep)
                .build();
    }
}