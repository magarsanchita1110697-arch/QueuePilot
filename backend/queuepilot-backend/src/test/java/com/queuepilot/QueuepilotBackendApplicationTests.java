package com.queuepilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void findByJobIdAndAttemptNumberReturnsMatchingAttempt() {

        // Create and save a parent job.
        Job job = new Job();
        job.setJobType("REPOSITORY_TEST");
        job.setStatus(JobStatus.SUCCEEDED);

        Job savedJob = jobRepository.saveAndFlush(job);

        // Create and save an attempt for that job.
        JobAttempt attempt = new JobAttempt();
        attempt.setJobId(savedJob.getId());
        attempt.setAttemptNumber(1);
        attempt.setStatus(JobAttemptStatus.SUCCEEDED);

        JobAttempt savedAttempt =
                jobAttemptRepository.saveAndFlush(attempt);

        // Find the attempt using job ID and attempt number.
        Optional<JobAttempt> foundAttempt =
                jobAttemptRepository.findByJobIdAndAttemptNumber(
                        savedJob.getId(),
                        1
                );

        // Verify that the correct attempt was returned.
        assertTrue(foundAttempt.isPresent());
        assertEquals(savedAttempt.getId(), foundAttempt.get().getId());
        assertEquals(1, foundAttempt.get().getAttemptNumber());
        assertEquals(
                JobAttemptStatus.SUCCEEDED,
                foundAttempt.get().getStatus()
        );

        // Verify that a nonexistent attempt returns an empty result.
        assertTrue(
                jobAttemptRepository.findByJobIdAndAttemptNumber(
                        savedJob.getId(),
                        2
                ).isEmpty()
        );
    }
}
