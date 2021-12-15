[![License](https://img.shields.io/github/license/italia/bootstrap-italia.svg)](https://github.com/italia/bootstrap-italia/blob/master/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/italia/bootstrap-italia.svg)](https://github.com/italia/bootstrap-italia/issues)
[![Join the #design channel](https://img.shields.io/badge/Slack%20channel-%23design-blue.svg)](https://developersitalia.slack.com/messages/C7VPAUVB3/)
[![Get invited](https://slack.developers.italia.it/badge.svg)](https://slack.developers.italia.it/)
[![18app on forum.italia.it](https://img.shields.io/badge/Forum-18app-blue.svg)](https://forum.italia.it/c/18app-carta-docente)

# Backend for the National Data Catalog (NDC) for Semantic Interoperability

This is the backend for the National Data Catalog (NDC) for Semantic Interoperability, a component of the PDND (Piattaforma Digitale Nazionale Dati) project.

PDND is composed of:
* a system to streamline the signing of "interoperability agreements" between API producers and consumers, by standardizing the process and authenticating participants
* a centralized API catalog, which enables the discovery and usage of eServices
* a centralized system which enables controlled and authorized access by consumers to the APIs, along with logging of the operations
* the National Data Catalog (NDC) for Semantic Interoperability, whose purpose is to store and index the semantic material (ontologies and controlled vocabularies) to be used as metadata with which to describe the APIs and the data they exchange

The backend is composed:
* of databases in which the semantic material is stored, which can be queried with a SPARQL endpoint
* of indexes which enable fast lookups of data and definitions
* of a crawler which is responsible for exploring the repositories published by the different participants to the Catalog, downloading the semantic material and storing it into the databases and indexes

# Index

- [How to start](#how-to-start)
- [How to contribute](#how-to-contribute)
- [Maintenance](#maintenance)
- [License](#license)


# How to start

## Installing

- Install JDK 11 in your machine
- Run  `mkdir .git/hooks`
- Build service using `./gradlew clean build`
OR Run tests locally using `docker-compose -f docker-compose-test.yaml up`   
- Start service using `./gradlew clean bootRun`

## Documentation

# How to contribute

## Community

### Code of conduct

### Responsible Disclosure

### Segnalazione bug e richieste di aiuto

# Maintenance 

# License

This work is licensed under the GNU Affero General Public License (AGPL), version 3 or later. You can find a copy of the license in the `LICENSE` file
