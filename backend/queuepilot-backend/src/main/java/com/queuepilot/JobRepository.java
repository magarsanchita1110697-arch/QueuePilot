package com.queuepilot;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query(value = """
        SELECT *
        FROM jobs
        WHERE status IN ('QUEUED', 'RETRY_WAIT')
          AND scheduled_at <= :now
        ORDER BY scheduled_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Job> findNextRunnableJob(@Param("now") Instant now);
}
