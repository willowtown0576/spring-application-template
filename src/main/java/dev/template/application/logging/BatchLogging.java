package dev.template.application.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
class BatchLogging implements JobExecutionListener {
  private static final Logger LOG = LoggerFactory.getLogger(BatchLogging.class);

  @Override
  public void beforeJob(JobExecution execution) {
    LOG.info("Batch start executionId={}", execution.getId());
  }

  @Override
  public void afterJob(JobExecution execution) {
    LOG.info("Batch end executionId={} status={}", execution.getId(), execution.getStatus());
    execution
        .getAllFailureExceptions()
        .forEach(exception -> TechnicalErrors.log(LOG, "Batch failure", exception));
  }
}
