package com.gentics.mesh.contentrag.service.config;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Stellt den Low-Level-Elasticsearch-RestClient bereit (Konzept 5.6/8). Bewusst der
 * Low-Level-Client: stabile, versionsrobuste API; Queries werden als JSON gebaut
 * (siehe {@code ElasticsearchGateway}). Der Client baut nur den Transport auf und
 * verbindet sich erst beim ersten Request – der Service startet daher auch ohne ES.
 */
@Configuration
public class ElasticsearchConfig {

    /**
     * Erzeugt den Low-Level-RestClient aus der Konfiguration (optional mit Basic-Auth).
     *
     * @param properties RAG-Konfiguration (Elasticsearch-URL/Credentials)
     * @return konfigurierter, noch nicht verbundener RestClient
     */
    @Bean(destroyMethod = "close")
    public RestClient restClient(RagProperties properties) {
        RagProperties.Elasticsearch es = properties.getElasticsearch();
        var builder = RestClient.builder(HttpHost.create(es.getUrl()));

        if (es.getUsername() != null && !es.getUsername().isBlank()) {
            String token = Base64.getEncoder().encodeToString(
                    (es.getUsername() + ":" + (es.getPassword() == null ? "" : es.getPassword()))
                            .getBytes(StandardCharsets.UTF_8));
            Header[] headers = {new BasicHeader("Authorization", "Basic " + token)};
            builder.setDefaultHeaders(headers);
        }
        return builder.build();
    }
}
