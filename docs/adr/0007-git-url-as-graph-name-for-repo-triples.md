# 7. git url as graph name for repo triples

Date: 2021-11-23

## Status

Accepted

## Context

When we harvest RDF data from a repo, which we had harvested before, we need a way to clean up the triples from
Virtuoso. We set out to used [Named Graphs](https://www.w3.org/2009/07/NamedGraph.html) as a way to enclose a set of
related triples. This way, we can use a `CLEAR GRAPH` statement to efficiently remove all the triples before we import
the latest version of that RDF data.

The questions we had were

* What is the granularity of data that we want to enclose in a single named graph? From broadest to narrowest:
    * An organization (an organization can provide multiple repos)
    * A repository (a repository can hold multiple assets)
    * An asset
* What name do we use to name (/identify) the graph?

## Decision

* We decided to have a named graph per repository; this will be cleared up before we start importing RDF data for that
  repository.
* We will use the git repo URL as the URI for the graph.

This was agreed with [Giorgia Lodi](https://github.com/giorgialodi), too.

## Consequences

We have an easy way to identify a name for the graph. Since we're not giving up on the whole repo when we encounter an
error, the _coarse_ granularity should not be a problem.

Attention point: if a repository is migrated to a different URL, somebody will have to manually remove data associated
with the previous URL.