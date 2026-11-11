package com.queuepilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class QueuepilotBackendApplicationTests {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobAttemptRepository jobAttemptRepository;

    @Autowired
    private JobResultService jobResultService;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void findByJobIdAndAttemptNumberReturnsMatchingAttempt() {

        Job job = new Job();
        job.setJobType("REPOSITORY_TEST");
        job.setStatus(JobStatus.SUCCEEDED);

        Job savedJob = jobRepository.saveAndFlush(job);

        JobAttempt attempt = new JobAttempt();
        attempt.setJobId(savedJob.getId());
        attempt.setAttemptNumber(1);
        attempt.setStatus(JobAttemptStatus.SUCCEEDED);

        JobAttempt savedAttempt =
                jobAttemptRepository.saveAndFlush(attempt);

        Optional<JobAttempt> foundAttempt =
                jobAttemptRepository.findByJobIdAndAttemptNumber(
                        savedJob.getId(),
                        1
                );

        assertTrue(foundAttempt.isPresent());
        assertEquals(savedAttempt.getId(), foundAttempt.get().getId());
        assertEquals(1, foundAttempt.get().getAttemptNumber());
        assertEquals(
                JobAttemptStatus.SUCCEEDED,
                foundAttempt.get().getStatus()
        );

        assertTrue(
                jobAttemptRepository.findByJobIdAndAttemptNumber(
                        savedJob.getId(),
                        2
                ).isEmpty()
        );
    }

    @Test
    @Transactional
    void completeSuccessMarksJobAndAttemptAsSucceeded() {

        Job job = createRunningJobWithAttempt(1, 3);

        Map<String, Object> result =
                Map.of("message", "Processed successfully");

        jobResultService.completeSuccess(job.getId(), 1, result);

        Job updatedJob =
                jobRepository.findById(job.getId()).orElseThrow();

        JobAttempt updatedAttempt =
                jobAttemptRepository
                        .findByJobIdAndAttemptNumber(job.getId(), 1)
                        .orElseThrow();

        assertEquals(JobStatus.SUCCEEDED, updatedJob.getStatus());
        assertEquals(result, updatedJob.getResult());
        assertTrue(updatedJob.getFinishedAt() != null);

        assertEquals(
                JobAttemptStatus.SUCCEEDED,
                updatedAttempt.getStatus()
        );
        assertTrue(updatedAttempt.getFinishedAt() != null);
    }

    @Test
    @Transactional
    void completeFailureSchedulesRetryWhenAttemptsRemain() {

        Job job = createRunningJobWithAttempt(1, 3);

        Instant beforeFailure = Instant.now();

        jobResultService.completeFailure(
                job.getId(),
                1,
                "Temporary execution error"
        );

        Job updatedJob =
                jobRepository.findById(job.getId()).orElseThrow();

        JobAttempt updatedAttempt =
                jobAttemptRepository
                        .findByJobIdAndAttemptNumber(job.getId(), 1)
                        .orElseThrow();

        assertEquals(JobStatus.RETRY_WAIT, updatedJob.getStatus());
        assertEquals(
                "Temporary execution error",
                updatedJob.getLastError()
        );
        assertTrue(updatedJob.getScheduledAt().isAfter(beforeFailure));
        assertTrue(updatedJob.getFinishedAt() == null);

        assertEquals(
                JobAttemptStatus.FAILED,
                updatedAttempt.getStatus()
        );
    }

    @Test
    @Transactional
    void completeFailureMarksJobFailedAfterMaximumAttempts() {

        Job job = createRunningJobWithAttempt(3, 3);

        jobResultService.completeFailure(
                job.getId(),
                3,
                "Maximum attempts reached"
        );

        Job updatedJob =
                jobRepository.findById(job.getId()).orElseThrow();

        JobAttempt updatedAttempt =
                jobAttemptRepository
                        .findByJobIdAndAttemptNumber(job.getId(), 3)
                        .orElseThrow();

        assertEquals(JobStatus.FAILED, updatedJob.getStatus());
        assertEquals(
                "Maximum attempts reached",
                updatedJob.getLastError()
        );
        assertTrue(updatedJob.getFinishedAt() != null);

        assertEquals(
                JobAttemptStatus.FAILED,
                updatedAttempt.getStatus()
        );
    }

    @Test
    @Transactional
    void completeNonRetryableFailureMarksJobFailedImmediately() {

        Job job = createRunningJobWithAttempt(1, 3);

        String error = "Unsupported job type: UNKNOWN";

        jobResultService.completeNonRetryableFailure(
                job.getId(),
                1,
                error
        );

        Job updatedJob =
                jobRepository.findById(job.getId()).orElseThrow();

        JobAttempt updatedAttempt =
                jobAttemptRepository
                        .findByJobIdAndAttemptNumber(job.getId(), 1)
                        .orElseThrow();

        assertEquals(JobStatus.FAILED, updatedJob.getStatus());
        assertEquals(error, updatedJob.getLastError());
        assertTrue(updatedJob.getFinishedAt() != null);

        assertEquals(
                JobAttemptStatus.FAILED,
                updatedAttempt.getStatus()
        );
    }

    private Job createRunningJobWithAttempt(
            int attemptNumber,
            int maxAttempts) {

        Job job = new Job();
        job.setJobType("REPOSITORY_TEST");
        job.setStatus(JobStatus.RUNNING);
        job.setAttemptCount(attemptNumber);
        job.setMaxAttempts(maxAttempts);

        Job savedJob = jobRepository.saveAndFlush(job);

        JobAttempt attempt = new JobAttempt();
        attempt.setJobId(savedJob.getId());
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStatus(JobAttemptStatus.RUNNING);

        jobAttemptRepository.saveAndFlush(attempt);

        return savedJob;
    }
}
