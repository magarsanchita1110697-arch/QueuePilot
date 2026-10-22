
# QueuePilot

**Cloud Job Orchestration & AI Failure Assistant**

QueuePilot is a backend-focused project for submitting, executing, monitoring,
and troubleshooting asynchronous background jobs.

## Project Status

Early development. The Git repository and development environment are being
prepared. Features will be marked complete only after implementation and testing.

## Problem Statement

Applications often need to process work outside the lifetime of an HTTP request,
such as generating reports, processing files, or sending notifications.

QueuePilot aims to help developers submit jobs, monitor progress, inspect failures,
and safely retry eligible work.

## Planned Features

- Submit jobs through a REST API.
- Track job status, execution attempts, results, and failures.
- Execute jobs using background workers.
- Apply bounded retries and record attempt history.
- Design safeguards against duplicate processing.
- Monitor queue activity through a web dashboard.
- Investigate failures using logs, documentation, and AI-assisted explanations.
- Deploy to AWS with operational logs and metrics.

## Technology Stack

### Backend
- Java 21
- Spring Boot
- Maven

### Database and Messaging
- PostgreSQL
- Flyway
- Amazon SQS
- Redis

### Frontend
- React
- TypeScript
- Node.js and npm

### Cloud and AI
- AWS
- CloudWatch
- Amazon Bedrock

### Testing and Packaging
- JUnit
- Spring Boot Test
- Integration tests
- Docker

## Initial Scope

We will start with one controlled job type, such as report generation. First,
we will implement persistent job records, execution, status tracking, and basic
failure handling. Cloud services and AI troubleshooting will follow once the
local core works.

## Engineering Principles

- PostgreSQL will be the source of truth for persisted job records.
- Workers must tolerate duplicate message delivery.
- Retries must be bounded and observable.
- Partial failures between database updates and message publishing must be handled.
- AI explanations must distinguish observed evidence from hypotheses.
- The AI assistant will recommend fixes rather than automatically execute risky actions.
- Secrets and credentials must never be committed to Git.

## Development Roadmap

1. Verify the development environment and repository.
2. Define the architecture, domain model, and job lifecycle.
3. Create and test the Spring Boot backend.
4. Configure PostgreSQL and database migrations.
5. Implement job submission and retrieval.
6. Add background workers, retries, and failure handling.
7. Integrate queue delivery and reliability safeguards.
8. Build the React and TypeScript dashboard.
9. Add AI-assisted failure investigation.
10. Test, deploy, document, and prepare for interviews.

## Development Environment

- Operating system: Ubuntu 22.04.5 LTS under WSL2.
- Architecture: ARM64 Linux.
- Editor: Visual Studio Code.
- Version control: Git.
- Backend runtime: OpenJDK 21.
- Frontend runtime: Node.js 24 and npm 11.
- Planned backend framework: Spring Boot.
- Planned frontend: React and TypeScript.

Maven, PostgreSQL, Redis, Docker, and AWS integration still need to be
configured or verified as the project progresses.

## License

No license has been selected yet.
