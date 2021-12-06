# 8. Use Spring Batch to Schedule Harvesting

Date: 2021-12-06

## Status

Accepted

## Context

The harvesting of the Git repositories of all the agencies onboarded onto NDC will be happening every day.
To facilitate this, we need something to provide the following functionalities
- Schedule jobs
- Prevent parallel runs of jobs
- Provide a way to restart failed jobs

## Decision

All of these functionalities are provided by Spring Batch.

Some advantages of Spring Batch
- The batch framework leverages Spring programming model thus allows developers to concentrate
  on the business logic or the business procedure and framework facilitates the infrastructure.
- Clear separation of concerns between the infrastructure, the batch execution environment,
  the batch application, and the different steps/processes within a batch application.
- Provides common scenario-based, core execution services as interfaces that the applications can implement
  and in addition to that framework provides its default implementation that the developers could use
  or partially override based on their business logic.
- Easily configurable and extendable services across different layers.

In addition, Spring Batch provides scalability (via partitioning and chunks) and big data support.

Spring Batch is backed by Mysql where the job runs are stored.
This provides visibility into the jobs which have been run and their statuses.

## Consequences

It is easier to schedule and execute jobs using Spring Batch, but an additional datasource Mysql will need to be added.
