# 4. single oas document for all the vocabularies

Date: 2021-11-16

## Status

Accepted

## Context

As one of the core feature of the NDC, we will be publishing the (flattened) content of Controlled Vocabularies via REST
APIs.

A developer who is willing to consume these APIs, will need to have access to the API's OpenAPI document. Usually such
documents illustrate a lot of information around the API; among other details, we can find `paths`
(/Endpoints). Path specify what endpoints are available; within a Path definition, we can also find the shape of its
response, expressed as references to the Component section. The Component section defines the schema of such elements.

Given that we're publishing a **family** of APIs, two endpoints per Vocabulary, we face this challenge:

* All of our APIs will follow the same _pattern_ for URLs; however these patterns include tokens which are
  Vocabulary-specific (namely an `agency_id` and a `core_vocab_concept`). So each API will be available at a **different
  URL**
  .
* The objects returned by the APIs will have some predefined common properties (`key`, `label_it`, `label_en`), but
  could potentially vary in extra fields available on some specific Vocabularies.

## Decision

After discussing with [Roberto](https://github.com/ioggstream), we have agreed on the following:

* There will only be one OAS specification, which will apply for all of the different vocabularies. In other words, a
  single OAS file that will look at lot
  like [this one](https://editor.swagger.io/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fioggstream%2Fjson-semantic-playground%2Fmaster%2Fopenapi%2Fvocabularies.yaml)
  , with these differences:
    * Instead of `vocabulary_id` we will have the pair `<agency_id, core_vocab_concept>`
    * Pagination is supported over there, but we will support it

## Consequences

* For the MVP, this introduces a simplification, as no Vocabulary specific OAS doccument needs to be generated
* This allows for easy Contract First approach
