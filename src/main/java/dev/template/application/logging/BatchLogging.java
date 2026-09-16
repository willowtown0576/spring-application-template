package dev.template.application.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

/** Batch実行の開始・終了・障害を、処理を識別する非機密情報で記録する。 */
@Component
class BatchLogging implements JobExecutionListener {
    /** Batchの開始・終了と秘匿処理済みの障害情報の出力先。 */
    private static final Logger LOG = LoggerFactory.getLogger(BatchLogging.class);

    /** {@inheritDoc} */
    @Override
    public void afterJob(final JobExecution execution) {
        LOG.info("Batch end executionId={} status={}", execution.getId(), execution.getStatus());
        execution.getAllFailureExceptions().forEach(exception -> TechnicalErrors.log(LOG, "Batch failure", exception));
    }

    /** {@inheritDoc} */
    @Override
    public void beforeJob(final JobExecution execution) {
        LOG.info("Batch start executionId={}", execution.getId());
    }
}
