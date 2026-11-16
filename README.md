# QueuePilot — Backend Job Processing & Orchestration System

**QueuePilot** is a backend job processing system built using Java, Spring Boot, and PostgreSQL. It provides a structured mechanism for submitting jobs, processing them asynchronously through background workers, tracking execution attempts, and handling failures through retry-aware scheduling.

The project explores backend engineering concepts such as asynchronous processing, database transactions, concurrent job execution, persistence, and reliable failure handling.

**Project Status:** In Progress

## 1. Overview

In backend applications, certain operations can take significant time to complete. Executing these operations directly within an HTTP request can increase response times and negatively affect application responsiveness.

QueuePilot addresses this problem by separating job submission from job execution.

Instead of performing a task immediately when a request arrives, the application stores the job in PostgreSQL. A background worker retrieves eligible jobs and processes them independently of the original request.

The system also maintains execution-attempt records and supports retry-aware scheduling to handle processing failures.

## 2. Key Features

- **RESTful Job Management:** Provides backend APIs for submitting jobs and managing job information and execution status.
- **Asynchronous Processing:** Executes submitted jobs through a background worker rather than blocking the request-processing flow.
- **Scheduled Job Execution:** Retrieves jobs that are eligible for execution based on their scheduled time.
- **Concurrency-Safe Job Claiming:** Uses PostgreSQL transactions and `FOR UPDATE SKIP LOCKED` to prevent multiple workers from claiming the same job simultaneously.
- **Retry-Aware Scheduling:** Supports scheduling failed jobs for subsequent execution attempts.
- **Execution Attempt Tracking:** Maintains individual records for job execution attempts, including attempt numbers and statuses.
- **Job Result Management:** Stores processing results and maintains job execution information.
- **Extensible Handler Architecture:** Uses a handler registry to associate job types with their respective processing implementations.
- **Database Versioning:** Uses Flyway migrations to manage database schema changes.
- **Automated Testing:** Includes tests for application and repository-level functionality.

## 3. Technology Stack

| Category | Technologies |
|---|---|
| Programming Language | Java |
| Backend Framework | Spring Boot |
| Data Access | Spring Data JPA |
| Database | PostgreSQL |
| Database Migrations | Flyway |
| Build Tool | Maven |
| Testing | JUnit, Spring Boot Test |
| Version Control | Git, GitHub |

## 4. System Architecture

QueuePilot follows a layered backend architecture that separates API handling, job claiming, job execution, and database operations.

### High-Level Architecture

![QueuePilot System Architecture](docs/system-architecture.jpeg)

### Architecture Components

**1. JobController**

Handles incoming HTTP requests related to job management and communicates with the backend services.

**2. JobRepository**

Manages job persistence and provides database queries for retrieving eligible jobs.

**3. JobClaimService**

Responsible for claiming eligible jobs while using transactional database operations to ensure safe job claiming.

**4. JobWorker**

Processes eligible jobs in the background and coordinates execution using the registered job handlers.

**5. JobHandlerRegistry**

Maintains the mapping between supported job types and their corresponding processing implementations.

**6. JobResultService**

Handles job execution results and the associated processing information.

**7. JobAttemptRepository**

Provides persistence and retrieval operations for individual execution-attempt records.

## 5. Job Processing Lifecycle

QueuePilot separates job submission from background execution.

The general processing workflow is:
![QueuePilot Job Execution Lifecycle](docs/job-lifecycle.jpeg)

1. **Job Submission:** A client submits a job through the REST API.
2. **Job Persistence:** The submitted job is stored in PostgreSQL with its initial status.
3. **Job Selection:** The background worker identifies jobs eligible for execution.
4. **Job Claiming:** The job claim service retrieves and claims an eligible job using transactional database locking.
5. **Handler Resolution:** The handler registry identifies the appropriate handler based on the job type.
6. **Job Execution:** The worker executes the selected job handler.
7. **Result Management:** The processing result and execution-attempt information are persisted.
8. **Failure Handling:** When processing fails, retry-aware scheduling determines whether another execution attempt should be scheduled.

This workflow allows job execution to operate independently of the original API request.

## 6. Concurrency and Job Claiming

A key backend engineering challenge in job processing systems is ensuring that multiple workers do not execute the same job simultaneously.

QueuePilot addresses this using PostgreSQL row-level locking.

The job repository uses:

```sql
FOR UPDATE SKIP LOCKED
```

The query selects eligible jobs while locking the selected database rows within a transaction.

The `SKIP LOCKED` behavior allows concurrent workers to skip jobs already locked by another transaction and claim other eligible jobs instead.

This approach improves the safety of concurrent job claiming and helps avoid duplicate claims during parallel processing.

Jobs are selected according to their scheduled execution time.

## 7. Failure Handling and Retry Management

Job execution can fail because of processing errors or other runtime issues.

QueuePilot incorporates retry-aware scheduling to handle these failures.

The system:

- Maintains individual execution-attempt records.
- Associates attempts with their corresponding jobs.
- Tracks attempt numbers and execution statuses.
- Supports retry scheduling for jobs that can be executed again.
- Maintains job execution results.

This provides a structured way to track execution history and manage failures rather than treating every job as a one-time operation.

## 8. Database Design

QueuePilot uses PostgreSQL for persistent storage.

The database design separates job information from individual execution attempts.

### Jobs

Stores the primary job records and associated processing information.

### Job Attempts

Stores individual execution-attempt records associated with a job.

This separation allows the system to maintain an execution history independently of the primary job record.

### Database Migrations

Flyway manages database schema changes through version-controlled migration scripts.

Current migrations:

- `V1__create_jobs_tables.sql` — Creates the initial job-related database tables.
- `V2__add_job_result.sql` — Introduces job result persistence.

Hibernate automatic schema generation is disabled, keeping database structure management under Flyway migrations.

## 9. API Layer

QueuePilot exposes its job management functionality through REST APIs.

The current backend includes:

- Job submission through `CreateJobRequest`.
- Job management through `JobController`.
- Persistence and retrieval through `JobRepository`.
- Background processing through `JobWorker`.

The exact endpoint mappings and request fields are maintained in the corresponding controller and request classes.

## 10. Project Structure

```text
QueuePilot/
|
└── backend/
    └── queuepilot-backend/
        |
        ├── src/
        │   ├── main/
        │   │   ├── java/com/queuepilot/
        │   │   │   ├── CreateJobRequest.java
        │   │   │   ├── HealthController.java
        │   │   │   ├── Job.java
        │   │   │   ├── JobAttempt.java
        │   │   │   ├── JobAttemptRepository.java
        │   │   │   ├── JobAttemptStatus.java
        │   │   │   ├── JobClaimService.java
        │   │   │   ├── JobController.java
        │   │   │   ├── JobHandler.java
        │   │   │   ├── JobHandlerRegistry.java
        │   │   │   ├── JobRepository.java
        │   │   │   ├── JobResultService.java
        │   │   │   ├── JobStatus.java
        │   │   │   ├── JobWorker.java
        │   │   │   ├── UppercaseTextJobHandler.java
        │   │   │   └── QueuepilotBackendApplication.java
        │   │   │
        │   │   └── resources/
        │   │       ├── application.properties
        │   │       └── db/migration/
        │   │
        │   └── test/
        │       └── java/com/queuepilot/
        │           └── QueuepilotBackendApplicationTests.java
        │
        └── pom.xml
```

## 11. Running the Project Locally

### Prerequisites

- Java Development Kit (JDK 21)
- Maven
- PostgreSQL
- Git

### Step 1: Clone the Repository

```bash
git clone https://github.com/magarsanchita1110697-arch/QueuePilot.git

cd QueuePilot
```

### Step 2: Navigate to the Backend

```bash
cd backend/queuepilot-backend
```

### Step 3: Configure PostgreSQL

Create a PostgreSQL database and user:

```sql
CREATE ROLE queuepilot_user
WITH LOGIN PASSWORD 'your_password';

CREATE DATABASE queuepilot
OWNER queuepilot_user;
```

### Step 4: Configure Environment Variable

Set the database password as an environment variable.

On Ubuntu / WSL:

```bash
export QUEUEPILOT_DB_PASSWORD='your_password'
```

The application uses the following database configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/queuepilot

spring.datasource.username=queuepilot_user

spring.datasource.password=${QUEUEPILOT_DB_PASSWORD}
```

Do not commit actual database credentials to GitHub.

### Step 5: Build and Run Tests

```bash
mvn clean test
```

### Step 6: Start the Application

```bash
mvn spring-boot:run
```

Flyway manages the database migrations when the application starts.

## 12. Testing

QueuePilot includes automated backend tests using JUnit and Spring Boot Test.

The tests cover application-level and repository-level behavior.

Run the test suite using:

```bash
mvn test
```

## 13. Future Enhancements

The following improvements are planned as the project evolves:

- Redis integration for improved job scheduling and coordination.
- AWS-based deployment and cloud infrastructure integration.
- React-based dashboard for monitoring jobs and execution history.
- LLM integration for intelligent failure analysis and job insights.
- Improved monitoring, logging, and observability.
- Additional job handlers and configurable job types.
- Performance evaluation under concurrent workloads.

These enhancements are part of the future development roadmap and are not represented as completed features.

## 14. Project Goals

QueuePilot is being developed to explore practical backend engineering concepts, including:

- Asynchronous job processing.
- Database transactions and concurrency control.
- Background worker architecture.
- Retry mechanisms and failure handling.
- REST API development.
- Database persistence and schema migration.
- Modular and extensible application design.

The goal is to build a reliable, maintainable, and extensible backend job processing system while gaining practical experience with real-world backend engineering challenges.

---

**Project:** QueuePilot  
**Status:** In Progress  
**Repository:** https://github.com/magarsanchita1110697-arch/QueuePilot