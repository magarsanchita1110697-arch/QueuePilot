
package com.queuepilot;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobWorker {

    private static final Logger logger =
            LoggerFactory.getLogger(JobWorker.class);

    private final JobClaimService jobClaimService;
    private final JobHandlerRegistry jobHandlerRegistry;
    private final JobResultService jobResultService;

    public JobWorker(
            JobClaimService jobClaimService,
            JobHandlerRegistry jobHandlerRegistry,
            JobResultService jobResultService) {
        this.jobClaimService = jobClaimService;
        this.jobHandlerRegistry = jobHandlerRegistry;
        this.jobResultService = jobResultService;
    }

    @Scheduled(fixedDelayString = "${queuepilot.worker.poll-interval-ms:1000}")
    public void processNextJob() {

        Optional<Job> nextJob = jobClaimService.claimNextJob();

        if (nextJob.isEmpty()) {
            return;
        }

        Job job = nextJob.get();
        int attemptNumber = job.getAttemptCount();

        Map<String, Object> result;

        try {
            JobHandler handler =
                    jobHandlerRegistry.getHandler(job.getJobType());

            result = handler.execute(job.getPayload());

                
                } catch (UnsupportedJobTypeException exception) {

            logger.error(
                    "Job {} has unsupported job type {}",
                    job.getId(),
                    job.getJobType(),
                    exception
            );

            jobResultService.completeNonRetryableFailure(
                    job.getId(),
                    attemptNumber,
                    exception.getMessage()
            );

            return;

        } catch (Exception exception) {

            logger.error(
                    "Job {} failed on attempt {}",
                    job.getId(),
                    attemptNumber,
                    exception
            );

            jobResultService.completeFailure(
                    job.getId(),
                    attemptNumber,
                    exception.getMessage()
            );

            return;
        }

        jobResultService.completeSuccess(
                job.getId(),
                attemptNumber,
                result
        );

        logger.info(
                "Job {} completed successfully on attempt {}",
                job.getId(),
                attemptNumber
        );
    }
}
