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

        long totalOmitidos =
                jobExecution.getStepExecutions().stream()
                        .mapToLong(execution ->
                                execution.getReadSkipCount()
                                        + execution.getProcessSkipCount()
                                        + execution.getWriteSkipCount())
                        .sum();

        if (totalOmitidos > 0) {

            logger.warn(
                    "[DECIDER] El Job termino con {} registros omitidos.",
                    totalOmitidos
            );

            return new FlowExecutionStatus(
                    "COMPLETED_WITH_SKIPS"
            );
        }

        logger.info(
                "[DECIDER] El Job termino sin registros omitidos."
        );

        return new FlowExecutionStatus(
                "COMPLETED_CLEAN"
        );
    }
}
