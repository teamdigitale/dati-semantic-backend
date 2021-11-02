# 3. java library to interact with RDF and sparql

Date: 2021-10-28

## Status

Accepted

## Context

We will be handling the RDF data in our code. We anticipate below use cases
* Query/Update/Delete the RDF data (Fluent API)
* Load the RDF data from file
* Parse RDF files and extract metadata form them

For this purpose we need to identify the java library to use. Following libraries were evaluated
* RDF4j
* Apache Jena

## Decision

### RDF4J
SPARQL
```
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name
WHERE { ?x foaf:name ?name }
ORDER BY ?name
LIMIT 5
OFFSET 10
```
Java
```java
query.prefix(foaf).select(name)
    .where(x.has(foaf.iri("name"), name))
    .orderBy(name)
    .limit(5)
    .offset(10);
```
- As you can see above the fluent API is easy to understand and simple to
- use, but it is limited as it does not support the full SPARQL 1.1 spec.
```
Currently, the following SPARQL 1.1 features have not yet been implemented in SparqlBuilder:

Property Paths
Values Block
RDF Collection Syntax
BIND
DESCRIBE and ASK Queries
```
- Java interfaces in RDF4J query builder are different than core RDF model as of now.
- Good dependency management, they also provide the BOM for managing versions of the jars.

### Apache Jena
SPARQL
```
SELECT ?age
WHERE {
    :Tex_Avery :born_on ?b ;
        :died_on ?d .
            BIND (year(?b) AS ?bYear) BIND (year(?d) AS ?dYear) BIND (?dYear - ?bYear AS ?age)
            }
```
Java
```java
SelectBuilder builder = new SelectBuilder()
                .addPrefix("lnt", "http://looneytunes-graph.com/");
        ExprFactory exprFactory=builder.getExprFactory();
        Query bind = builder.addVar("?age")
                .addWhere("lnt:Tex_Avery", "lnt:born_on", "?b")
                .addWhere("lnt:Tex_Avery", "lnt:died_on", "?d")
                .addBind(exprFactory.year("?b"), "?bYear")
                .addBind(exprFactory.year("?d"), "?dYear")
                .addBind(exprFactory.subtract("?dYear","?bYear") , "?age")
                .build()  ;
```
- As you can see above the fluent API is easy to understand and simple to use. Above query is quite
- complex but its straightforward to use
- Dependency management does not have a BOM, but we only need two jars, so its manageable
- It supports all the SPARQL 1.1 features

## Hence we have decided to use Apache Jena.

## Consequences
- No advert consequences are expected.