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
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;

import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.io.Resource;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.transaction.PlatformTransactionManager;

import cl.duoc.formativa1.advanced.CustomDecider;
import cl.duoc.formativa1.advanced.CustomRetryListener;
import cl.duoc.formativa1.advanced.CustomRetryPolicy;
import cl.duoc.formativa1.advanced.CustomSkipPolicy;
import cl.duoc.formativa1.advanced.JobCompletionListener;
import cl.duoc.formativa1.advanced.MovimientoAnualSkipListener;
import cl.duoc.formativa1.advanced.MovimientoAnualStepExecutionListener;

import cl.duoc.formativa1.business.MovimientoAnual;


@Configuration
public class EstadosCuentaJobConfig {


    // ==================================================
    // PARTICIONADOR
    // ==================================================

    @Bean
    public MultiResourcePartitioner movimientosPartitioner(

            @Value("classpath:cuentas_anuales-*.csv")
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
    public Step movimientosWorkerStep(

            JobRepository jobRepository,

            PlatformTransactionManager transactionManager,

            FlatFileItemReader<MovimientoAnual>
                    movimientoAnualReader,

            ItemProcessor<MovimientoAnual, MovimientoAnual>
                    movimientoAnualProcessor,

            ItemWriter<MovimientoAnual>
                    movimientoAnualWriter,

            CustomSkipPolicy customSkipPolicy,

            CustomRetryPolicy customRetryPolicy,

            CustomRetryListener customRetryListener,

            MovimientoAnualSkipListener
                    movimientoAnualSkipListener,

            MovimientoAnualStepExecutionListener
                    movimientoAnualStepExecutionListener) {


        return new ChunkOrientedStepBuilder
                <MovimientoAnual, MovimientoAnual>(

                "movimientosWorkerStep",
                jobRepository,
                100
        )

                .reader(
                        movimientoAnualReader
                )

                .processor(
                        movimientoAnualProcessor
                )

                .writer(
                        movimientoAnualWriter
                )

                .transactionManager(
                        transactionManager
                )

                .faultTolerant()

                .skipPolicy(
                        customSkipPolicy
                )

                .listener(
                        movimientoAnualSkipListener
                )

                .retryPolicy(
                        customRetryPolicy
                                .crearRetryPolicy()
                )

                .retryListener(
                        customRetryListener
                )

                .listener(
                        movimientoAnualStepExecutionListener
                )

                .build();
    }



    // ==================================================
    // PARTITIONED STEP
    // ==================================================

    @Bean
    public Step estadosCuentaPartitionedStep(

            JobRepository jobRepository,

            @Qualifier("movimientosPartitioner")
            MultiResourcePartitioner partitioner,

            @Qualifier("movimientosWorkerStep")
            Step movimientosWorkerStep,

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
                movimientosWorkerStep
        );

        handler.setGridSize(
                gridSize
        );


        return new StepBuilder(
                "estadosCuentaPartitionedStep",
                jobRepository
        )

                .partitioner(
                        "movimientosWorkerStep",
                        partitioner
                )

                .partitionHandler(
                        handler
                )

                .build();
    }



    // ==================================================
    // STEP SECUENCIAL PARA GENERAR RESUMEN
    // ==================================================

    @Bean
    public Step generarEstadosCuentaStep(

            JobRepository jobRepository,

            PlatformTransactionManager transactionManager,

            JdbcTemplate jdbcTemplate) {


        return new StepBuilder(
                "generarEstadosCuentaStep",
                jobRepository
        )

                .tasklet(
                        (contribution, chunkContext) -> {

                            jdbcTemplate.update(
                                    "DELETE FROM estados_cuenta_anuales"
                            );


                            String sql = """
                                INSERT INTO estados_cuenta_anuales (
                                    cuenta_id,
                                    total_ingresos,
                                    total_egresos,
                                    saldo_neto,
                                    cantidad_movimientos,
                                    cantidad_anomalias
                                )

                                SELECT
                                    cuenta_id,

                                    SUM(
                                        CASE
                                            WHEN estado = 'VALIDO'
                                             AND transaccion = 'deposito'
                                            THEN monto
                                            ELSE 0
                                        END
                                    ) AS total_ingresos,

                                    SUM(
                                        CASE
                                            WHEN estado = 'VALIDO'
                                             AND transaccion IN (
                                                 'retiro',
                                                 'compra'
                                             )
                                            THEN ABS(monto)
                                            ELSE 0
                                        END
                                    ) AS total_egresos,

                                    SUM(
                                        CASE
                                            WHEN estado = 'VALIDO'
                                            THEN monto
                                            ELSE 0
                                        END
                                    ) AS saldo_neto,

                                    COUNT(*) AS cantidad_movimientos,

                                    SUM(
                                        CASE
                                            WHEN estado = 'ANOMALIA'
                                            THEN 1
                                            ELSE 0
                                        END
                                    ) AS cantidad_anomalias

                                FROM movimientos_anuales

                                GROUP BY cuenta_id
                                """;


                            int filas =
                                    jdbcTemplate.update(sql);


                            System.out.println(
                                    "[ESTADOS-CUENTA] "
                                    + filas
                                    + " estados de cuenta generados."
                            );


                            return RepeatStatus.FINISHED;
                        },

                        transactionManager
                )

                .build();
    }



    // ==================================================
    // JOB
    // ==================================================

    @Bean
    public Job estadosCuentaJob(

            JobRepository jobRepository,

            @Qualifier("estadosCuentaPartitionedStep")
            Step estadosCuentaPartitionedStep,

            @Qualifier("generarEstadosCuentaStep")
            Step generarEstadosCuentaStep,

            JobCompletionListener jobCompletionListener,

            CustomDecider customDecider) {


        return new JobBuilder(
                "estadosCuentaJob",
                jobRepository
        )

                .incrementer(
                        new RunIdIncrementer()
                )

                .listener(
                        jobCompletionListener
                )

                .start(
                        estadosCuentaPartitionedStep
                )

                .next(
                        generarEstadosCuentaStep
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