package cl.duoc.formativa1.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.StepExecution;

import org.springframework.stereotype.Component;

@Component
public class CustomDecider implements JobExecutionDecider {

    private static final Logger logger =
            LoggerFactory.getLogger(CustomDecider.class);


    @Override
    public FlowExecutionStatus decide(
            JobExecution jobExecution,
            StepExecution stepExecution) {


        /*
         * Buscar solamente los Worker Steps
         * correspondientes a las particiones.
         *
         * Ejemplos:
         * transaccionesWorkerStep:partition0
         * transaccionesWorkerStep:partition1
         * interesesWorkerStep:partition0
         * movimientosWorkerStep:partition0
         */
        boolean existenParticiones =
                jobExecution
                        .getStepExecutions()
                        .stream()
                        .anyMatch(
                                execution ->
                                        execution
                                                .getStepName()
                                                .contains(":partition")
                        );


        long totalOmitidos;


        if (existenParticiones) {

            /*
             * Sumar los skips de todas las particiones.
             */
            totalOmitidos =
                    jobExecution
                            .getStepExecutions()
                            .stream()

                            .filter(
                                    execution ->
                                            execution
                                                    .getStepName()
                                                    .contains(":partition")
                            )

                            .mapToLong(
                                    execution ->
                                            execution.getReadSkipCount()
                                            + execution.getProcessSkipCount()
                                            + execution.getWriteSkipCount()
                            )

                            .sum();

        } else {

            /*
             * Fallback para un Job que no utilice
             * particiones.
             */
            if (stepExecution == null) {

                logger.warn(
                        "[DECIDER] No existe información de ejecución."
                );

                return new FlowExecutionStatus(
                        "UNKNOWN"
                );
            }


            totalOmitidos =
                    stepExecution.getReadSkipCount()
                    + stepExecution.getProcessSkipCount()
                    + stepExecution.getWriteSkipCount();
        }


        if (totalOmitidos > 0) {

            logger.warn(
                    "[DECIDER] El procesamiento terminó con {} registros omitidos.",
                    totalOmitidos
            );

            return new FlowExecutionStatus(
                    "COMPLETED_WITH_SKIPS"
            );
        }


        logger.info(
                "[DECIDER] El procesamiento terminó sin registros omitidos."
        );


        return new FlowExecutionStatus(
                "COMPLETED_CLEAN"
        );
    }
}