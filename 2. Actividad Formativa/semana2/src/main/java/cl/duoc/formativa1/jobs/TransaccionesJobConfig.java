package cl.duoc.formativa1.jobs;

import cl.duoc.formativa1.business.Transaccion;

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

import cl.duoc.formativa1.advanced.CustomSkipPolicy;
import cl.duoc.formativa1.advanced.TransaccionSkipListener;
import cl.duoc.formativa1.advanced.CustomRetryPolicy;
import cl.duoc.formativa1.advanced.CustomRetryListener;
import cl.duoc.formativa1.advanced.TransaccionStepExecutionListener;
import cl.duoc.formativa1.advanced.JobCompletionListener;
import cl.duoc.formativa1.advanced.CustomDecider;
import cl.duoc.formativa1.advanced.BatchProperties;


@Configuration
public class TransaccionesJobConfig {

    @Bean
    public Step transaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Transaccion> transaccionReader,
            ItemProcessor<Transaccion, Transaccion> transaccionProcessor,
            ItemWriter<Transaccion> transaccionWriter,
            @Qualifier("batchTaskExecutor")
            ThreadPoolTaskExecutor batchTaskExecutor,
            CustomSkipPolicy customSkipPolicy,
            TransaccionSkipListener transaccionSkipListener,
            CustomRetryPolicy customRetryPolicy,
            CustomRetryListener customRetryListener,
            TransaccionStepExecutionListener transaccionStepExecutionListener,
            BatchProperties batchProperties)
            {
            

        return new ChunkOrientedStepBuilder<Transaccion, Transaccion>(
                "transaccionesStep",
                jobRepository,
                batchProperties.getChunkSize())

                .reader(transaccionReader)
                .processor(transaccionProcessor)
                .writer(transaccionWriter)
                .transactionManager(transactionManager)

                // Tolerancia a fallos
                .faultTolerant()
                
                //procesamiento de registros mal formados desde el CSV
                .skipPolicy(customSkipPolicy)

                // Listener para registrar los registros omitidos
                .listener(transaccionSkipListener)
                
                // Política de reintentos para errores temporales de acceso a datos
                .retryPolicy(customRetryPolicy.crearRetryPolicy())

                // Listener para registrar los reintentos
                .retryListener(customRetryListener)

                // Procesamiento paralelo con 3 hilos
                .taskExecutor(batchTaskExecutor)

                // Listener para registrar el inicio y fin del step
                .listener(transaccionStepExecutionListener)

                .build();
    }

    @Bean
public Job transaccionesJob(
        JobRepository jobRepository,
        Step transaccionesStep,
        Step transaccionesResumenStep,
        JobCompletionListener jobCompletionListener,
        CustomDecider customDecider) {

    return new JobBuilder(
            "transaccionesJob",
            jobRepository)

            .incrementer(new RunIdIncrementer())
            .listener(jobCompletionListener)

            // Ejecutar primero el Step principal
            .start(transaccionesStep)

            // Reconstruir el resumen usando valores absolutos
            .next(transaccionesResumenStep)

            // Evaluar todos los Steps ejecutados
            .next(customDecider)

            // Si hubo registros omitidos
            .on("COMPLETED_WITH_SKIPS")
            .end()

            // Si no hubo registros omitidos
            .from(customDecider)
            .on("COMPLETED_CLEAN")
            .end()

            // Cualquier resultado inesperado
            .from(customDecider)
            .on("*")
            .fail()

            .end()
            .build();
}

}
