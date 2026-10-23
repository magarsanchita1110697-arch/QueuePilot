
# QueuePilot Architecture

## 1. Project Overview

QueuePilot is a background job orchestration platform with an AI-assisted
failure investigation feature.

Users will be able to submit jobs, monitor execution, inspect results,
understand failures, and receive evidence-based explanations.

The project is under development. This document describes the planned design,
not features that have already been implemented.

## 2. Initial Scope

The first supported job type will be report generation.

The initial system will support:
- Submitting jobs through a REST API.
- Persisting job information in PostgreSQL.
- Executing jobs using a background worker.
- Tracking job status, results, and execution attempts.
- Retrying eligible failures up to a configured limit.
- Preventing multiple workers from claiming the same job concurrently.
- Retrieving job information through API endpoints.
- Automated tests for successful and failed execution.

The React dashboard, AWS integrations, and AI assistant will follow after the
local backend works.

## 3. Technology Stack

### Backend
- Java 21
- Spring Boot
- Maven Wrapper

### Database
- PostgreSQL
- Flyway for database migrations

### Job Execution
- Database-backed job queue initially
- Background worker inside the Spring Boot application
- Amazon SQS considered for a later AWS-based version

### Frontend
- React
- TypeScript
- Node.js and npm

### AI and Cloud
- Amazon Bedrock for the planned AI failure assistant
- Amazon CloudWatch for planned logging and monitoring
- AWS deployment in a later phase

### Testing and Delivery
- JUnit
- Spring Boot Test
- Integration tests
- Docker when the local application and database workflow are ready

Redis is excluded from the initial version. It will be considered only if a
specific requirement justifies adding it.

## 4. High-Level Architecture

React Dashboard
       |
       | HTTP / JSON
       |
Spring Boot REST API
       |
       +------ PostgreSQL
       |       - Job records
       |       - Input payloads and results
       |       - Execution attempts and errors
       |
       +------ Background Worker
               - Claims eligible jobs
               - Executes report generation
               - Records success or failure
               - Schedules bounded retries

Later integrations:
- Amazon SQS for distributed job delivery
- Amazon CloudWatch for logging and monitoring
- Amazon Bedrock for failure explanations

Initially, the API and worker will run as one application.
PostgreSQL will be the source of truth for job state.

## 5. Job Lifecycle

Planned statuses:

- QUEUED: The job is waiting to be claimed.
- RUNNING: A worker is executing the job.
- RETRY_WAIT: A retry is scheduled after an eligible failure.
- SUCCEEDED: Execution completed successfully.
- FAILED: Execution failed permanently or exhausted its retry limit.

Normal execution:

QUEUED -> RUNNING -> SUCCEEDED

Retryable failure:

QUEUED -> RUNNING -> RETRY_WAIT -> QUEUED

Exhausted retries or a non-retryable failure:

RUNNING -> FAILED

The backend will validate status transitions rather than allowing clients
to change job statuses arbitrarily.

## 6. Data Model

### Job

The jobs table is planned to contain:
- id: UUID primary key
- job_type: supported operation
- status: current lifecycle status
- payload: JSONB input data
- result: JSONB output data
- attempt_count: number of execution attempts
- max_attempts: configured retry limit
- next_attempt_at: earliest eligible retry time
- last_error: latest failure summary
- created_at and updated_at
- started_at and completed_at

The exact SQL schema will be defined during database implementation.

### Job Attempt

A separate job_attempts table will record:
- id
- job_id
- attempt_number
- status
- started_at and finished_at
- error_type and error_message

This history will support debugging and the future AI assistant.

## 7. API Design

Planned endpoints:

- POST /api/jobs
  Submit a report-generation job.

- GET /api/jobs
  List jobs with pagination and optional status filtering.

- GET /api/jobs/{id}
  Retrieve status, timestamps, result, and latest error.

- GET /api/jobs/{id}/attempts
  Retrieve execution history for a job.

The backend will validate requests and return appropriate HTTP status codes.

## 8. Worker and Reliability

The worker will claim eligible jobs inside a database transaction using a
safe locking strategy, such as SELECT FOR UPDATE SKIP LOCKED.

Job claiming and status updates must prevent workers from intentionally
claiming the same job concurrently.

Retries will be bounded and use a controlled delay. Permanent failures will
not be retried indefinitely.

The system will record execution attempts and provide a recovery strategy
for jobs left RUNNING after an interrupted worker.

QueuePilot will aim for reliable, idempotent processing. It will not claim
that distributed job execution is exactly-once.

## 9. AI Failure Assistant

This feature will follow the core job workflow.

The assistant will use relevant job metadata, attempt history, and error
evidence to provide:
- A plain-language explanation of the failure
- Relevant supporting evidence
- Possible causes marked as hypotheses when uncertain
- Suggested investigation or recovery steps

The assistant must not invent logs, expose secrets, or automatically execute
risky fixes. Its output will be advisory.

## 10. Security and Observability

- Never commit credentials, API keys, or secrets.
- Validate incoming job payloads.
- Avoid storing sensitive data unnecessarily in logs.
- Record useful execution errors and timestamps.
- Keep PostgreSQL as the initial source of truth.
- Use environment variables for deployment-specific configuration.

## 11. Implementation Roadmap

Phase 1: Architecture and project structure
Phase 2: Spring Boot application and health endpoint
Phase 3: PostgreSQL connection and Flyway migration
Phase 4: Job entity, repository, and service layer
Phase 5: Job submission and retrieval APIs
Phase 6: Background worker and report-generation handler
Phase 7: Retry rules, attempt history, and recovery handling
Phase 8: Automated tests and API documentation
Phase 9: React and TypeScript dashboard
Phase 10: AI-assisted failure investigation
Phase 11: Docker, AWS deployment, CloudWatch, and SQS evaluation
Phase 12: Final documentation and interview preparation

Each phase will be verified before moving to the next.
