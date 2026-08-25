package cl.duoc.formativa1.jobs;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.formativa1.advanced.CustomDecider;
import cl.duoc.formativa1.advanced.BatchProperties;
import cl.duoc.formativa1.advanced.CustomRetryListener;
import cl.duoc.formativa1.advanced.CustomRetryPolicy;
import cl.duoc.formativa1.advanced.CustomSkipPolicy;
import cl.duoc.formativa1.advanced.JobCompletionListener;
import cl.duoc.formativa1.business.InteresCuenta;
import cl.duoc.formativa1.advanced.InteresSkipListener;
import cl.duoc.formativa1.advanced.InteresStepExecutionListener;


@Configuration
public class InteresesJobConfig {

    @Bean
    public Step interesesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<InteresCuenta> interesReader,
            ItemProcessor<InteresCuenta, InteresCuenta> interesProcessor,
            ItemWriter<InteresCuenta> interesWriter,

            @Qualifier("batchTaskExecutor")
            ThreadPoolTaskExecutor batchTaskExecutor,

            CustomSkipPolicy customSkipPolicy,
            CustomRetryPolicy customRetryPolicy,
            CustomRetryListener customRetryListener,
            InteresSkipListener interesSkipListener,
            InteresStepExecutionListener interesStepExecutionListener,
            BatchProperties batchProperties) {

        return new ChunkOrientedStepBuilder<InteresCuenta, InteresCuenta>(
                "interesesStep",
                jobRepository,
                batchProperties.getChunkSize())

                .reader(interesReader)
                .processor(interesProcessor)
                .writer(interesWriter)

                .transactionManager(transactionManager)

                // Tolerancia a fallos
                .faultTolerant()

                // Registros CSV mal formados
                .skipPolicy(customSkipPolicy)

                // Listener para los registros omitidos
                .listener(interesSkipListener)
                .listener(interesStepExecutionListener)

                // Errores temporales
                .retryPolicy(customRetryPolicy.crearRetryPolicy())
                .retryListener(customRetryListener)

                // Procesamiento con 3 hilos
                .taskExecutor(batchTaskExecutor)

                .build();
    }

    @Bean
    public Job interesesJob(
            JobRepository jobRepository,
            Step interesesStep,
            JobCompletionListener jobCompletionListener,
            CustomDecider customDecider) {

        return new JobBuilder(
                "interesesJob",
                jobRepository)

                .incrementer(new RunIdIncrementer())

                // Listener general del Job
                .listener(jobCompletionListener)

                // Step principal
                .start(interesesStep)

                // Evaluación del resultado
                .next(customDecider)

                // Finaliza correctamente si hubo skips
                .on("COMPLETED_WITH_SKIPS")
                .end()

                // Finaliza correctamente si no hubo skips
                .from(customDecider)
                .on("COMPLETED_CLEAN")
                .end()

                // Cualquier estado inesperado
                .from(customDecider)
                .on("*")
                .fail()

                .end()
                .build();
    }
}
