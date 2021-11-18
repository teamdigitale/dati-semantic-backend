# 5. Elasticsearch for Controlled Vocabulary Data

Date: 2021-11-17

## Status

Accepted

## Context

To support one of the core features of NDC to publish the flattened data of the Controlled Vocabularies (CV) via REST APIs, 
we need to store the flattened data in our system.

The CV data will not be having a structured schema. Each CV can have schema different from the others, 
and the stored data should be easy to retrieve via dynamic APIs.

The two options are between Elasticsearch and MongoDB (document store).

## Decision

We had a discussion around this and decided to go ahead with Elasticsearch.

This is because we will anyways be having Elasticsearch to store the semantic assets (including CV) metadata.
It makes sense to store the data also along with the metadata even though Elasticsearch is not primarily a document store.

We reduce the type of databases we have to support as Elasticsearch is already present. We do not need to 
add one more type of DB.

Storing the CV data in Elasticsearch can also help in the future, if we ever need to search in the CV data too as
Elasticsearch will help us in indexing and efficiently searching in the data.

## Consequences

- For MVP, we can easily store and retrieve the CV data
- We reduce the type of databases we have to support. No need for additional MongoDB.