package it.gov.innovazione.ndc.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.testcontainers.utility.DockerImageName.parse;

public class Containers {
    public static final int ELASTICSEARCH_PORT = 9200;
    public static final String CLUSTER_NAME = "cluster.name";
    public static final DockerImageName ELASTICSEARCH_IMAGE =
            parse("docker.elastic.co/elasticsearch/elasticsearch")
                    .withTag("8.14.3");
    public static final int VIRTUOSO_PORT = 8890;

    public static final String ELASTIC_USERNAME = "elastic";
    public static final String ELASTIC_PASSWORD = "s3cret";

    public static ElasticsearchContainer buildElasticsearchContainer() {
        ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
                .withReuse(true)
                .withExposedPorts(ELASTICSEARCH_PORT)
                .withPassword(ELASTIC_PASSWORD)
                .withEnv("discovery.type", "single-node")
                .withEnv(CLUSTER_NAME, "elasticsearch");

        container.getEnvMap().remove("xpack.security.enabled");

        new HttpWaitStrategy()
                .forPort(9200)
                .usingTls()
                .forStatusCode(HTTP_OK)
                .forStatusCode(HTTP_UNAUTHORIZED)
                .withBasicCredentials(ELASTIC_USERNAME, ELASTIC_PASSWORD)
                .withStartupTimeout(Duration.ofMinutes(2));

        return container;

    }

    public static GenericContainer buildVirtuosoContainer() {
        return new GenericContainer(parse("tenforce/virtuoso"))
                .withReuse(true)
                .withExposedPorts(VIRTUOSO_PORT)
                .withEnv("DBA_PASSWORD", "dba")
                .withEnv("SPARQL_UPDATE", "true");
    }
}
