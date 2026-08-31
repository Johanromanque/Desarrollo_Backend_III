package cl.duoc.formativa1.jobs;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;

import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;

import org.springframework.batch.core.repository.JobRepository;

import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.io.Resource;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.formativa1.advanced.CustomDecider;
import cl.duoc.formativa1.advanced.CustomRetryListener;
import cl.duoc.formativa1.advanced.CustomRetryPolicy;
import cl.duoc.formativa1.advanced.CustomSkipPolicy;
import cl.duoc.formativa1.advanced.InteresSkipListener;
import cl.duoc.formativa1.advanced.InteresStepExecutionListener;
import cl.duoc.formativa1.advanced.JobCompletionListener;

import cl.duoc.formativa1.business.InteresCuenta;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;


@Configuration
public class InteresesJobConfig {


    // ==================================================
    // PARTICIONADOR
    // ==================================================

    @Bean
    public MultiResourcePartitioner interesesPartitioner(

            @Value("classpath:intereses-*.csv")
            Resource[] resources) {


        MultiResourcePartitioner partitioner =
                new MultiResourcePartitioner();

        partitioner.setResources(resources);

        partitioner.setKeyName("file");

        return partitioner;
    }



    // ==================================================
    // WORKER STEP
    // ==================================================

    @Bean
    public Step interesesWorkerStep(

            JobRepository jobRepository,

            PlatformTransactionManager transactionManager,

            FlatFileItemReader<InteresCuenta> interesReader,

            ItemProcessor<InteresCuenta, InteresCuenta>
                    interesProcessor,

            ItemWriter<InteresCuenta> interesWriter,

            CustomSkipPolicy customSkipPolicy,

            CustomRetryPolicy customRetryPolicy,

            CustomRetryListener customRetryListener,

            InteresSkipListener interesSkipListener,

            InteresStepExecutionListener
                    interesStepExecutionListener) {


        return new ChunkOrientedStepBuilder
                <InteresCuenta, InteresCuenta>(

                "interesesWorkerStep",
                jobRepository,
                100
        )

                .reader(
                        interesReader
                )

                .processor(
                        interesProcessor
                )

                .writer(
                        interesWriter
                )

                .transactionManager(
                        transactionManager
                )


                // ======================================
                // TOLERANCIA A FALLOS
                // ======================================

                .faultTolerant()

                .skipPolicy(
                        customSkipPolicy
                )

                .listener(
                        interesSkipListener
                )

                .retryPolicy(
                        customRetryPolicy
                                .crearRetryPolicy()
                )

                .retryListener(
                        customRetryListener
                )

                .listener(
                        interesStepExecutionListener
                )

                /*
                 * Importante:
                 * aquí NO usamos taskExecutor.
                 *
                 * El paralelismo ahora lo controla
                 * el PartitionHandler.
                 */

                .build();
    }



    // ==================================================
    // PARTITIONED STEP
    // ==================================================

    @Bean
    public Step interesesPartitionedStep(

            JobRepository jobRepository,

            @Qualifier("interesesPartitioner")
            MultiResourcePartitioner partitioner,

            @Qualifier("interesesWorkerStep")
            Step interesesWorkerStep,

            @Qualifier("batchTaskExecutor")
            ThreadPoolTaskExecutor taskExecutor,

            @Value("${app.partition.grid-size:4}")
            int gridSize)

            throws Exception {


        TaskExecutorPartitionHandler handler =
                new TaskExecutorPartitionHandler();

        handler.setTaskExecutor(
                taskExecutor
        );

        handler.setStep(
                interesesWorkerStep
        );

        handler.setGridSize(
                gridSize
        );


        return new StepBuilder(
                "interesesPartitionedStep",
                jobRepository
        )

                .partitioner(
                        "interesesWorkerStep",
                        partitioner
                )

                .partitionHandler(
                        handler
                )

                .build();
    }



    // ==================================================
    // JOB
    // ==================================================

    @Bean
    public Job interesesJob(

            JobRepository jobRepository,

            @Qualifier("interesesPartitionedStep")
            Step interesesPartitionedStep,

            JobCompletionListener jobCompletionListener,

            CustomDecider customDecider) {


        return new JobBuilder(
                "interesesJob",
                jobRepository
        )

                .incrementer(
                        new RunIdIncrementer()
                )

                .listener(
                        jobCompletionListener
                )

                .start(
                        interesesPartitionedStep
                )

                .next(
                        customDecider
                )

                .on("COMPLETED_WITH_SKIPS")
                .end()

                .from(customDecider)
                .on("COMPLETED_CLEAN")
                .end()

                .from(customDecider)
                .on("*")
                .fail()

                .end()
                .build();
    }
}