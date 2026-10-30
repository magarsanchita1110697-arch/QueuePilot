
package com.queuepilot;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private Map<String, Object> result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.QUEUED;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public Job() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        if (payload == null) payload = new HashMap<>();
        if (status == null) status = JobStatus.QUEUED;
        if (createdAt == null) createdAt = now;
        if (scheduledAt == null) scheduledAt = now;

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getJobType() { return jobType; }
    public Map<String, Object> getPayload() { return payload; }
    public JobStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Map<String, Object> getResult() {
    return result;
}

   public void setJobType(String jobType) {
    this.jobType = jobType;
}

public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
}

public void setStatus(JobStatus status) {
    this.status = status;
}

public void setAttemptCount(int attemptCount) {
    this.attemptCount = attemptCount;
}

public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
}

public void setLastError(String lastError) {
    this.lastError = lastError;
}

public void setResult(Map<String, Object> result) {
    this.result = result;
}

public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
}

public void setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
}

public void setScheduledAt(Instant scheduledAt) {
    this.scheduledAt = scheduledAt;
}
}

