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
import cl.duoc.formativa1.business.MovimientoAnual;
import cl.duoc.formativa1.advanced.MovimientoAnualSkipListener;
import cl.duoc.formativa1.advanced.MovimientoAnualStepExecutionListener;

@Configuration
public class EstadosCuentaJobConfig {

    @Bean
    public Step estadosCuentaStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<MovimientoAnual> movimientoAnualReader,
            ItemProcessor<MovimientoAnual, MovimientoAnual> movimientoAnualProcessor,
            ItemWriter<MovimientoAnual> movimientoAnualWriter,

            @Qualifier("batchTaskExecutor")
            ThreadPoolTaskExecutor batchTaskExecutor,

            CustomSkipPolicy customSkipPolicy,
            CustomRetryPolicy customRetryPolicy,
            CustomRetryListener customRetryListener,
            MovimientoAnualSkipListener movimientoAnualSkipListener,
            MovimientoAnualStepExecutionListener movimientoAnualStepExecutionListener,
            BatchProperties batchProperties) {

        return new ChunkOrientedStepBuilder<MovimientoAnual, MovimientoAnual>(
                "estadosCuentaStep",
                jobRepository,
                batchProperties.getChunkSize())

                .reader(movimientoAnualReader)
                .processor(movimientoAnualProcessor)
                .writer(movimientoAnualWriter)

                .transactionManager(transactionManager)

                // Habilita tolerancia a fallos
                .faultTolerant()

                // Registros CSV mal formados
                .skipPolicy(customSkipPolicy)

                // Listener para los registros omitidos
                .listener(movimientoAnualSkipListener)
                .listener(movimientoAnualStepExecutionListener)

                // Errores temporales
                .retryPolicy(customRetryPolicy.crearRetryPolicy())
                .retryListener(customRetryListener)

                // Procesamiento paralelo con 3 hilos
                .taskExecutor(batchTaskExecutor)

                .build();
    }

    @Bean
    public Job estadosCuentaJob(
            JobRepository jobRepository,
            Step estadosCuentaStep,
            Step estadosCuentaResumenStep,
            JobCompletionListener jobCompletionListener,
            CustomDecider customDecider) {

        return new JobBuilder(
                "estadosCuentaJob",
                jobRepository)

                .incrementer(new RunIdIncrementer())

                // Listener general del Job
                .listener(jobCompletionListener)

                // Step principal
                .start(estadosCuentaStep)

                // Reconstruir el resumen usando valores absolutos
                .next(estadosCuentaResumenStep)

                // Evaluar todos los Steps ejecutados
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
