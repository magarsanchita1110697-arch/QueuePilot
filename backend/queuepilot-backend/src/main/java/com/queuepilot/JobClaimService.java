package com.queuepilot;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;
    private final JobAttemptRepository jobAttemptRepository;

    public JobClaimService(
            JobRepository jobRepository,
            JobAttemptRepository jobAttemptRepository) {
        this.jobRepository = jobRepository;
        this.jobAttemptRepository = jobAttemptRepository;
    }

    @Transactional
    public Optional<Job> claimNextJob() {

        Instant now = Instant.now();

        Optional<Job> nextJob =
                jobRepository.findNextRunnableJob(now);

        if (nextJob.isEmpty()) {
            return Optional.empty();
        }

        Job job = nextJob.get();

        int nextAttemptNumber = job.getAttemptCount() + 1;

        job.setStatus(JobStatus.RUNNING);
        job.setAttemptCount(nextAttemptNumber);
        job.setStartedAt(now);
        job.setFinishedAt(null);
        job.setLastError(null);

        jobRepository.save(job);

        JobAttempt attempt = new JobAttempt();

        attempt.setJobId(job.getId());
        attempt.setAttemptNumber(nextAttemptNumber);
        attempt.setStatus(JobAttemptStatus.RUNNING);
        attempt.setStartedAt(now);

        jobAttemptRepository.save(attempt);

        return Optional.of(job);
    }
}
