package cl.duoc.formativa1.jobs;

import cl.duoc.formativa1.business.Transaccion;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;

import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;

import org.springframework.batch.core.repository.JobRepository;

import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.io.Resource;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.formativa1.advanced.TransaccionSkipListener;
import cl.duoc.formativa1.advanced.TransaccionStepExecutionListener;
import cl.duoc.formativa1.advanced.JobCompletionListener;
import cl.duoc.formativa1.advanced.CustomDecider;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;

import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.time.format.DateTimeParseException;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
public class TransaccionesJobConfig {


    // ==================================================
    // PARTICIONADOR
    // ==================================================

    @Bean
    public MultiResourcePartitioner transaccionesPartitioner(

            @Value("classpath:transacciones-*.csv")
            Resource[] resources) {


        MultiResourcePartitioner partitioner =
                new MultiResourcePartitioner();

        partitioner.setResources(resources);

        /*
         * El nombre "file" se enviará al
         * stepExecutionContext.
         *
         * El Reader lo recibe mediante:
         *
         * stepExecutionContext['file']
         */
        partitioner.setKeyName("file");

        return partitioner;
    }



    // ==================================================
    // WORKER STEP
    // ==================================================

    @Bean
    public Step transaccionesWorkerStep(

            JobRepository jobRepository,

            PlatformTransactionManager transactionManager,

            ItemReader<Transaccion> transaccionReader,

            ItemProcessor<Transaccion, Transaccion>
                    transaccionProcessor,

            ItemWriter<Transaccion> transaccionWriter,

            @Qualifier("transaccionesSkipPolicy")
            SkipPolicy transaccionesSkipPolicy,

            TransaccionSkipListener transaccionSkipListener,

            TransaccionStepExecutionListener
                    transaccionStepExecutionListener) {


        return new StepBuilder(
                "transaccionesWorkerStep",
                jobRepository
        )

                .<Transaccion, Transaccion>chunk(
                        100,
                        transactionManager
                )

                .reader(
                        transaccionReader
                )

                .processor(
                        transaccionProcessor
                )

                .writer(
                        transaccionWriter
                )


                // ======================================
                // TOLERANCIA A FALLOS
                // ======================================

                .faultTolerant()

                .skipPolicy(
                        transaccionesSkipPolicy
                )

                .listener(
                        transaccionSkipListener
                )

                // Listener de cada Worker
                .listener(
                        transaccionStepExecutionListener
                )

                .build();
    }



    // ==================================================
    // MANAGER / PARTITION STEP
    // ==================================================

    @Bean
    public Step transaccionesPartitionedStep(

            JobRepository jobRepository,

            @Qualifier("transaccionesPartitioner")
            MultiResourcePartitioner partitioner,

            @Qualifier("transaccionesWorkerStep")
            Step transaccionesWorkerStep,

            @Qualifier("batchTaskExecutor")
            ThreadPoolTaskExecutor taskExecutor,

            @Value("${app.partition.grid-size:4}")
            int gridSize)

            throws Exception {


        TaskExecutorPartitionHandler handler =
                new TaskExecutorPartitionHandler();

        /*
         * Executor encargado de ejecutar
         * las particiones en paralelo.
         */
        handler.setTaskExecutor(
                taskExecutor
        );

        /*
         * Step que ejecutará cada partición.
         */
        handler.setStep(
                transaccionesWorkerStep
        );

        /*
         * Cantidad de particiones.
         */
        handler.setGridSize(
                gridSize
        );


        return new StepBuilder(
                "transaccionesPartitionedStep",
                jobRepository
        )

                .partitioner(
                        "transaccionesWorkerStep",
                        partitioner
                )

                .partitionHandler(
                        handler
                )

                .build();
    }

    @Bean
    public Step resumenTransaccionesStep(

            JobRepository jobRepository,

            PlatformTransactionManager transactionManager,

            JdbcTemplate jdbcTemplate) {

        return new StepBuilder(
                "resumenTransaccionesStep",
                jobRepository)

                .tasklet(
                        (contribution, chunkContext) -> {

                            /*
                             * Limpiar resumen anterior
                             * para permitir nuevas ejecuciones.
                             */
                            jdbcTemplate.update(
                                    "DELETE FROM resumen_transacciones_diarias");

                            /*
                             * Generar el resumen utilizando
                             * solamente los datos que ya fueron
                             * procesados correctamente.
                             */
                            String sql = """
                                    INSERT INTO resumen_transacciones_diarias (
                                        fecha,
                                        total_transacciones,
                                        total_creditos,
                                        total_debitos,
                                        total_anomalias,
                                        monto_creditos,
                                        monto_debitos
                                    )
                                    SELECT
                                        fecha,

                                        COUNT(*) AS total_transacciones,

                                        SUM(
                                            CASE
                                                WHEN LOWER(tipo) = 'credito'
                                                     AND estado = 'VALIDA'
                                                THEN 1
                                                ELSE 0
                                            END
                                        ) AS total_creditos,

                                        SUM(
                                            CASE
                                                WHEN LOWER(tipo) = 'debito'
                                                     AND estado = 'VALIDA'
                                                THEN 1
                                                ELSE 0
                                            END
                                        ) AS total_debitos,

                                        SUM(
                                            CASE
                                                WHEN estado = 'ANOMALIA'
                                                THEN 1
                                                ELSE 0
                                            END
                                        ) AS total_anomalias,

                                        SUM(
                                            CASE
                                                WHEN LOWER(tipo) = 'credito'
                                                     AND estado = 'VALIDA'
                                                THEN monto
                                                ELSE 0
                                            END
                                        ) AS monto_creditos,

                                        SUM(
                                            CASE
                                                WHEN LOWER(tipo) = 'debito'
                                                     AND estado = 'VALIDA'
                                                THEN monto
                                                ELSE 0
                                            END
                                        ) AS monto_debitos

                                    FROM transacciones_procesadas

                                    GROUP BY fecha
                                    """;

                            int filas = jdbcTemplate.update(sql);

                            System.out.println(
                                    "[RESUMEN] "
                                            + filas
                                            + " resúmenes diarios generados.");

                            return RepeatStatus.FINISHED;
                        },

                        transactionManager)

                .build();
    }



    @Bean
    public SimpleRetryPolicy transaccionesRetryPolicy() {

        Map<Class<? extends Throwable>, Boolean>
                retryableExceptions =
                new HashMap<>();

        retryableExceptions.put(
                CannotAcquireLockException.class,
                true
        );

        retryableExceptions.put(
                TransientDataAccessException.class,
                true
        );

        return new SimpleRetryPolicy(
                3,
                retryableExceptions,
                true
        );
    }

    @Bean
    public ExponentialBackOffPolicy transaccionesBackoffPolicy() {

        ExponentialBackOffPolicy backoffPolicy =
                new ExponentialBackOffPolicy();

        // Primer reintento después de 1 segundo
        backoffPolicy.setInitialInterval(1000);

        // Cada espera se duplica
        backoffPolicy.setMultiplier(2.0);

        // Máximo 10 segundos
        backoffPolicy.setMaxInterval(10000);

        return backoffPolicy;
    }

    // ==================================================
    // JOB
    // ==================================================

    @Bean
    public Job transaccionesJob(

            JobRepository jobRepository,

            @Qualifier("transaccionesPartitionedStep")
            Step transaccionesPartitionedStep,

            @Qualifier("resumenTransaccionesStep")
            Step resumenTransaccionesStep,

            JobCompletionListener jobCompletionListener,

            CustomDecider customDecider) {


        return new JobBuilder(
                "transaccionesJob",
                jobRepository)

                .incrementer(
                        new RunIdIncrementer())

                .listener(
                        jobCompletionListener)

                /*
                 * El Job ahora comienza con
                 * el Partitioned Step.
                 */
                .start(
                        transaccionesPartitionedStep)

                .next(
                        resumenTransaccionesStep)

                .next(
                        customDecider)


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

    @Bean
    public SkipPolicy transaccionesSkipPolicy() {

        return (throwable, skipCount) -> {

            if (throwable instanceof FlatFileParseException) {
                return true;
            }

            if (throwable instanceof DateTimeParseException) {
                return true;
            }

            if (throwable instanceof NumberFormatException) {
                return true;
            }

            return false;
        };
    }
}