package it.teamdigitale.ndc.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "it.teamdigitale.ndc")
@ComponentScan(basePackages = {"it.teamdigitale.ndc"})
public class ElasticsearchClientConfig extends AbstractElasticsearchConfiguration {
    @Value("${elasticsearch.host:localhost}")
    private String host;
    @Value("${elasticsearch.port:9200}")
    private String port;

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {

        final ClientConfiguration clientConfiguration =
                ClientConfiguration
                        .builder()
                        .connectedTo(this.host + ":" + this.port)
                        .build();

        return RestClients.create(clientConfiguration).rest();
    }
}
