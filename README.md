[![License](https://img.shields.io/github/license/teamdigitale/dati-semantic-backend.svg)](https://github.com/teamdigitale/dati-semantic-backend/blob/main/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/teamdigitale/dati-semantic-backend.svg)](https://github.com/teamdigitale/dati-semantic-backend/issues)
[![Join the #design channel](https://img.shields.io/badge/Slack%20channel-%23design-blue.svg)](https://developersitalia.slack.com/messages/C7VPAUVB3/)
[![Get invited](https://slack.developers.italia.it/badge.svg)](https://slack.developers.italia.it/)
[![Dati on forum.italia.it](https://img.shields.io/badge/forum-dati-blue.svg)](https://forum.italia.it/c/dati/33)

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

#### Without docker
- Install JDK 11 in your machine
- Run  `mkdir .git/hooks`
- Build service using `./gradlew clean build`
- Start service using `./gradlew clean bootRun`
- Run tests using `./gradlew clean test`

#### With Docker
- Run service using `docker-compose up` - this starts the service and its dependencies

## Documentation
[Wiki](https://github.com/teamdigitale/dati-semantic-backend/wiki)

# How to contribute

## Community

### Code of conduct
Please review our [Code of Conduct](CODE_OF_CONDUCT.md) to ensure a great collaboration with the rest of the community.

### Responsible Disclosure

### Segnalazione bug e richieste di aiuto

# Maintenance 

# License

This work is licensed under the GNU Affero General Public License (AGPL), version 3 or later. You can find a copy of 
the license in the [LICENSE](https://github.com/teamdigitale/dati-semantic-backend/blob/main/LICENSE) file
