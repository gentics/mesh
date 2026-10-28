package com.gentics.mesh.contentrag.service.config;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;
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
        var builder = RestClient.builder(toHttpHost(es.getUrl()));

        if (es.getUsername() != null && !es.getUsername().isBlank()) {
            String token = Base64.getEncoder().encodeToString(
                    (es.getUsername() + ":" + (es.getPassword() == null ? "" : es.getPassword()))
                            .getBytes(StandardCharsets.UTF_8));
            Header[] headers = {new BasicHeader("Authorization", "Basic " + token)};
            builder.setDefaultHeaders(headers);
        }
        return builder.build();
    }

    /**
     * Zerlegt die konfigurierte Basis-URL in Schema, Host und Port.
     *
     * <p>Bewusst nicht {@link HttpHost#create(String)}: das erwartet eine URL <em>ohne</em>
     * Pfadanteil. Ein abschliessender Schraegstrich - den dieselbe URL in der
     * Mesh-Suchkonfiguration tragen darf - laesst es mit
     * {@code Invalid HTTP host: <host>:<port>/} scheitern, weil es {@code 9200/} als Portnummer
     * liest. Ohne Portangabe ist es schlimmer: dann wandert der Schraegstrich wortwoertlich in
     * den Hostnamen, der Start gelingt und erst der erste Request scheitert an der
     * Namensaufloesung.
     *
     * @param url konfigurierte Basis-URL, mit oder ohne Schema und abschliessenden Schraegstrich
     * @return Ziel-Host des RestClient
     */
    static HttpHost toHttpHost(String url) {
        String value = url == null ? "" : url.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("rag.elasticsearch.url ist nicht gesetzt.");
        }
        // Ohne Schema laese URI "host:9200" das "host" als Schema und haette keinen Host.
        if (!value.contains("://")) {
            value = "http://" + value;
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ungueltige Elasticsearch-URL: " + url, e);
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("Ungueltige Elasticsearch-URL: " + url);
        }
        String path = uri.getPath();
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
            // Einen Pfad-Praefix traegt der HttpHost nicht; ihn hier still zu verwerfen wuerde
            // alle Anfragen an die falsche Adresse schicken.
            throw new IllegalArgumentException("Elasticsearch-URL mit Pfad wird nicht "
                    + "unterstuetzt, erwartet werden nur Schema, Host und Port: " + url);
        }
        return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }
}
