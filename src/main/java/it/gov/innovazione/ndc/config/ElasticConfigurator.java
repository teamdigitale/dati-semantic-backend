package it.gov.innovazione.ndc.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;

@RequiredArgsConstructor
public class ElasticConfigurator {

    private final String host;
    private final int port;
    private final String scheme;
    private final String username;
    private final String password;

    public RestClient client() {
        try {
            return RestClient.builder(new HttpHost(host, port, scheme))
                    .setHttpClientConfigCallback(this::getHttpAsyncClientBuilder)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create an elasticsearch client", e);
        }
    }

    @SneakyThrows
    private HttpAsyncClientBuilder getHttpAsyncClientBuilder(HttpAsyncClientBuilder httpClientBuilder) {

        if (StringUtils.equalsIgnoreCase(scheme, "https")) {
            httpClientBuilder.setSSLContext(SSLContexts.custom()
                            .loadTrustMaterial(null, (x509Certificates, s) -> true)
                            .build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        return httpClientBuilder;
    }
}
