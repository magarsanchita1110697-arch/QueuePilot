
package com.queuepilot;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobResultService {

    private static final long INITIAL_RETRY_DELAY_SECONDS = 5;
    private static final long MAX_RETRY_DELAY_SECONDS = 300;

    private final JobRepository jobRepository;
    private final JobAttemptRepository jobAttemptRepository;

    public JobResultService(
            JobRepository jobRepository,
            JobAttemptRepository jobAttemptRepository) {
        this.jobRepository = jobRepository;
        this.jobAttemptRepository = jobAttemptRepository;
    }

    @Transactional
    public void completeSuccess(
            UUID jobId,
            int attemptNumber,
            Map<String, Object> result) {

        Instant now = Instant.now();

        Job job = findJob(jobId);
        JobAttempt attempt = findAttempt(jobId, attemptNumber);

        validateRunning(job, attempt);

        job.setResult(result);
        job.setStatus(JobStatus.SUCCEEDED);
        job.setFinishedAt(now);
        job.setLastError(null);

        attempt.setStatus(JobAttemptStatus.SUCCEEDED);
        attempt.setFinishedAt(now);
        attempt.setErrorMessage(null);

        jobRepository.save(job);
        jobAttemptRepository.save(attempt);
    }

    @Transactional
    public void completeFailure(
            UUID jobId,
            int attemptNumber,
            String errorMessage) {

        Instant now = Instant.now();

        Job job = findJob(jobId);
        JobAttempt attempt = findAttempt(jobId, attemptNumber);

        validateRunning(job, attempt);

        String error = (errorMessage == null || errorMessage.isBlank())
                ? "Job execution failed"
                : errorMessage;

        attempt.setStatus(JobAttemptStatus.FAILED);
        attempt.setFinishedAt(now);
        attempt.setErrorMessage(error);

        job.setLastError(error);

        if (job.getAttemptCount() < job.getMaxAttempts()) {
            job.setStatus(JobStatus.RETRY_WAIT);
            job.setScheduledAt(
                    now.plusSeconds(calculateRetryDelay(attemptNumber))
            );
            job.setFinishedAt(null);
        } else {
            job.setStatus(JobStatus.FAILED);
            job.setFinishedAt(now);
        }

        jobRepository.save(job);
        jobAttemptRepository.save(attempt);
    }

    private Job findJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Job not found: " + jobId
                        )
                );
    }

    private JobAttempt findAttempt(UUID jobId, int attemptNumber) {
        return jobAttemptRepository
                .findByJobIdOrderByAttemptNumberAsc(jobId)
                .stream()
                .filter(attempt ->
                        attempt.getAttemptNumber() == attemptNumber
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Job attempt not found: " + attemptNumber
                        )
                );
    }

   private void validateRunning(Job job, JobAttempt attempt) {
    if (job.getStatus() != JobStatus.RUNNING
            || attempt.getStatus() != JobAttemptStatus.RUNNING) {
        throw new IllegalStateException(
                "Job and attempt must both be RUNNING"
        );
    }

    if (job.getAttemptCount() != attempt.getAttemptNumber()) {
        throw new IllegalStateException(
                "Attempt number does not match current job attempt"
        );
    }
}
    private long calculateRetryDelay(int attemptNumber) {
        int exponent = Math.min(attemptNumber - 1, 10);

        long delay = INITIAL_RETRY_DELAY_SECONDS * (1L << exponent);

        return Math.min(delay, MAX_RETRY_DELAY_SECONDS);
    }
}
