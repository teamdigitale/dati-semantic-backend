# 6. loading ttl files in memory

Date: 2021-11-19

## Status

Accepted

## Context

We need to parse TTL files and extract metadata around the main semantic asset. 
We can do this by using an approach which loads the whole model in memory (similar to the DOM approach for XML) or 
by using a streaming API (similar to SAX or StAX).

The in-memory model has a simpler programming model, whereas the streaming approach dictates that the code keeps 
track of the parsing state.

We've estimated that even big TTL files won't consume too much memory, with respect to standard JVM based process 
resources.

## Decision

We're approaching the parsing with the full in memory model; if we encounter much bigger files in the future, 
this decision might need to be reviewed.

## Consequences

We can use the simpler programming model, but we need to watch out for resource consumption for bigger files.
