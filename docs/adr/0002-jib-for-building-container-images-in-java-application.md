# 3. jib - for building container images in java application

Date: 2021-10-28

## Status

Accepted

## Context
We plan to ship our application code in the containers. This requires us to create artifacts in container images.
We need to decide the way for creating the container images.
Below 3 different options were discussed
1. Dockerfile - using docker daemon
2. [Jib](https://github.com/GoogleContainerTools/jib) from GoogleContainerTools
3. Spring Boot inbuilt mechanism using buildpacks

## Decision

We have decided to use `jib` for backend java services due to following reasons
1. `jib` provides sensible defaults for building the container image from spring boot app
2. [OCI](https://opencontainers.org/) complient images
3. We are using [distroless](https://github.com/GoogleContainerTools/distroless) base images
4. It does not require writing Dockerfile and docker daemon (you can build images inside another
container without dind or socket sharing)
5. Jib separates your application into multiple layers, splitting dependencies from classes.
you don’t have to wait for Docker to rebuild your entire Java application - just deploy the layers that changed.

#### Why not Dockerfile?
- Requires docker daemon which makes it difficult to build images in containerised CI/CD runners
- Given recent docker license changes, everybody may not be willing/able to run docker on their machine

#### Why not Spring boot's buildpack based solution?
- Limited configuration options
- Slow build time compared to Jib

## Consequences

We will be able to build images fast due to layered approach. These images will be completely reproducible
including dates on the file system.
