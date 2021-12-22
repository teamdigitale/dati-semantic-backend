package it.gov.innovazione.ndc.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.utility.DockerImageName.parse;

public class Containers {
    public static final int ELASTICSEARCH_PORT = 9200;
    public static final String CLUSTER_NAME = "cluster.name";
    public static final DockerImageName ELASTICSEARCH_IMAGE =
            parse("docker.elastic.co/elasticsearch/elasticsearch")
                    .withTag("7.12.0");
    public static final int VIRTUOSO_PORT = 8890;

    public static ElasticsearchContainer buildElasticsearchContainer() {
        return new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
                .withReuse(true)
                .withExposedPorts(ELASTICSEARCH_PORT)
                .withEnv("discovery.type", "single-node")
                .withEnv(CLUSTER_NAME, "elasticsearch");
    }

    public static GenericContainer buildVirtuosoContainer() {
        return new GenericContainer(parse("tenforce/virtuoso"))
                .withReuse(true)
                .withExposedPorts(VIRTUOSO_PORT)
                .withEnv("DBA_PASSWORD", "dba")
                .withEnv("SPARQL_UPDATE", "true");
    }
}
