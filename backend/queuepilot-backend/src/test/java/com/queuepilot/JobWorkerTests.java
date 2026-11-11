
package com.queuepilot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobWorkerTests {

    private JobClaimService jobClaimService;
    private JobHandlerRegistry jobHandlerRegistry;
    private JobResultService jobResultService;
    private JobHandler jobHandler;

    private JobWorker jobWorker;

    @BeforeEach
    void setUp() {

        jobClaimService = mock(JobClaimService.class);
        jobHandlerRegistry = mock(JobHandlerRegistry.class);
        jobResultService = mock(JobResultService.class);
        jobHandler = mock(JobHandler.class);

        jobWorker = new JobWorker(
                jobClaimService,
                jobHandlerRegistry,
                jobResultService
        );
    }

    @Test
    void processNextJobCompletesSuccessfulJob() {

        Job job = createJob("TEST_JOB");

        Map<String, Object> result =
                Map.of("message", "Processed successfully");

        when(jobClaimService.claimNextJob())
                .thenReturn(Optional.of(job));

        when(jobHandlerRegistry.getHandler("TEST_JOB"))
                .thenReturn(jobHandler);

        when(jobHandler.execute(job.getPayload()))
                .thenReturn(result);

        jobWorker.processNextJob();

        verify(jobResultService).completeSuccess(
                job.getId(),
                1,
                result
        );
    }

    @Test
    void processNextJobDoesNotRetryUnsupportedJobType() {

        Job job = createJob("UNKNOWN_JOB");

        when(jobClaimService.claimNextJob())
                .thenReturn(Optional.of(job));

        when(jobHandlerRegistry.getHandler("UNKNOWN_JOB"))
                .thenThrow(new UnsupportedJobTypeException("UNKNOWN_JOB"));

        jobWorker.processNextJob();

        verify(jobResultService).completeNonRetryableFailure(
                job.getId(),
                1,
                "Unsupported job type: UNKNOWN_JOB"
        );

        verify(jobResultService, never()).completeFailure(
                job.getId(),
                1,
                "Unsupported job type: UNKNOWN_JOB"
        );

        verifyNoInteractions(jobHandler);
    }

    @Test
    void processNextJobRetriesOrdinaryExecutionFailure() {

        Job job = createJob("TEST_JOB");

        RuntimeException failure =
                new IllegalStateException("Temporary execution error");

        when(jobClaimService.claimNextJob())
                .thenReturn(Optional.of(job));

        when(jobHandlerRegistry.getHandler("TEST_JOB"))
                .thenReturn(jobHandler);

        when(jobHandler.execute(job.getPayload()))
                .thenThrow(failure);

        jobWorker.processNextJob();

        verify(jobResultService).completeFailure(
                job.getId(),
                1,
                "Temporary execution error"
        );

        verify(jobResultService, never()).completeNonRetryableFailure(
                job.getId(),
                1,
                "Temporary execution error"
        );
    }

    @Test
    void processNextJobDoesNothingWhenQueueIsEmpty() {

        when(jobClaimService.claimNextJob())
                .thenReturn(Optional.empty());

        jobWorker.processNextJob();

        verifyNoInteractions(
                jobHandlerRegistry,
                jobResultService,
                jobHandler
        );
    }

    private Job createJob(String jobType) {

        Job job = new Job();
        job.setJobType(jobType);
        job.setAttemptCount(1);

        // Initialize the entity ID and default timestamps.
        job.onCreate();

        return job;
    }
}
