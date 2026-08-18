package com.gentics.mesh.contentrag.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zentrale Konfiguration des RAG-Service (Konzept 5.x/8). Bindet an Praefix {@code rag}.
 * Werte siehe application.yml. Mandantenspezifische Overrides liegen im ConfigStore.
 */
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Mesh mesh = new Mesh();
    private final Embedding embedding = new Embedding();
    private final Llm llm = new Llm();
    private final Chunking chunking = new Chunking();
    private final Retrieval retrieval = new Retrieval();

    /** @return Elasticsearch-Verbindungseinstellungen */
    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    /** @return Mesh-Verbindungseinstellungen */
    public Mesh getMesh() {
        return mesh;
    }

    /** @return Embedding-Provider-Einstellungen */
    public Embedding getEmbedding() {
        return embedding;
    }

    /** @return LLM-Provider-Einstellungen */
    public Llm getLlm() {
        return llm;
    }

    /** @return Chunking-Parameter */
    public Chunking getChunking() {
        return chunking;
    }

    /** @return Retrieval-Parameter */
    public Retrieval getRetrieval() {
        return retrieval;
    }

    /** Verbindung zur (mit der Mesh-Volltextsuche geteilten) Elasticsearch-Plattform. */
    public static class Elasticsearch {
        private String url = "http://localhost:9200";
        private String username;
        private String password;
        /** Praefix fuer RAG-Indizes; Topologie Index-pro-Projekt (V3). */
        private String indexPrefix = "rag";

        /** @return Basis-URL des Elasticsearch-Servers */
        public String getUrl() {
            return url;
        }

        /** @param url Basis-URL des Elasticsearch-Servers */
        public void setUrl(String url) {
            this.url = url;
        }

        /** @return Benutzername fuer Basic-Auth (oder {@code null}) */
        public String getUsername() {
            return username;
        }

        /** @param username Benutzername fuer Basic-Auth */
        public void setUsername(String username) {
            this.username = username;
        }

        /** @return Passwort fuer Basic-Auth (oder {@code null}) */
        public String getPassword() {
            return password;
        }

        /** @param password Passwort fuer Basic-Auth */
        public void setPassword(String password) {
            this.password = password;
        }

        /** @return Praefix fuer die RAG-Indexnamen */
        public String getIndexPrefix() {
            return indexPrefix;
        }

        /** @param indexPrefix Praefix fuer die RAG-Indexnamen */
        public void setIndexPrefix(String indexPrefix) {
            this.indexPrefix = indexPrefix;
        }
    }

    /** Anbindung an Gentics Mesh fuer ergaenzende GraphQL-Reads (Konzept 5.1). */
    public static class Mesh {
        private String url = "http://localhost:8080";
        private String apiToken;

        /** @return Basis-URL der Mesh-Instanz */
        public String getUrl() {
            return url;
        }

        /** @param url Basis-URL der Mesh-Instanz */
        public void setUrl(String url) {
            this.url = url;
        }

        /** @return API-Token/JWT fuer Mesh-Reads (oder {@code null}) */
        public String getApiToken() {
            return apiToken;
        }

        /** @param apiToken API-Token/JWT fuer Mesh-Reads */
        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }
    }

    /** Embedding-Provider (Konzept 5.5). Default lokal/CPU via TEI. */
    public static class Embedding {
        /** local-tei | external */
        private String provider = "local-tei";
        private String model = "intfloat/multilingual-e5-small";
        private int dimension = 384;
        private String endpoint = "http://localhost:8081";
        private int batchSize = 32;

        /** @return Provider-Kennung ({@code local-tei} oder {@code external}) */
        public String getProvider() {
            return provider;
        }

        /** @param provider Provider-Kennung ({@code local-tei} oder {@code external}) */
        public void setProvider(String provider) {
            this.provider = provider;
        }

        /** @return Modellkennung des Embedding-Modells */
        public String getModel() {
            return model;
        }

        /** @param model Modellkennung des Embedding-Modells */
        public void setModel(String model) {
            this.model = model;
        }

        /** @return Vektordimension des Modells */
        public int getDimension() {
            return dimension;
        }

        /** @param dimension Vektordimension des Modells */
        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        /** @return Basis-URL des Embedding-Endpunkts (z. B. TEI) */
        public String getEndpoint() {
            return endpoint;
        }

        /** @param endpoint Basis-URL des Embedding-Endpunkts */
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        /** @return Batchgroesse fuer Embedding-Anfragen */
        public int getBatchSize() {
            return batchSize;
        }

        /** @param batchSize Batchgroesse fuer Embedding-Anfragen */
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    /** LLM-Provider (Konzept 5.5). Default-Profil "cloud-hybrid" (EU-gehostet). */
    public static class Llm {
        /** cloud-hybrid | sovereign-onprem */
        private String profile = "cloud-hybrid";
        private String model = "eu-managed-llm";
        private String endpoint;
        private String apiKey;

        /** @return Deployment-Profil ({@code cloud-hybrid} oder {@code sovereign-onprem}) */
        public String getProfile() {
            return profile;
        }

        /** @param profile Deployment-Profil */
        public void setProfile(String profile) {
            this.profile = profile;
        }

        /** @return Modellkennung des LLM */
        public String getModel() {
            return model;
        }

        /** @param model Modellkennung des LLM */
        public void setModel(String model) {
            this.model = model;
        }

        /** @return Endpunkt-URL des LLM (oder {@code null}) */
        public String getEndpoint() {
            return endpoint;
        }

        /** @param endpoint Endpunkt-URL des LLM */
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        /** @return API-Schluessel des LLM-Providers (oder {@code null}) */
        public String getApiKey() {
            return apiKey;
        }

        /** @param apiKey API-Schluessel des LLM-Providers */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    /** Chunking-Parameter (Konzept 5.4). */
    public static class Chunking {
        private int maxChars = 1200;
        private int overlapChars = 150;

        /** @return maximale Zeichenanzahl je Chunk */
        public int getMaxChars() {
            return maxChars;
        }

        /** @param maxChars maximale Zeichenanzahl je Chunk */
        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }

        /** @return Ueberlappung benachbarter Chunks in Zeichen */
        public int getOverlapChars() {
            return overlapChars;
        }

        /** @param overlapChars Ueberlappung benachbarter Chunks in Zeichen */
        public void setOverlapChars(int overlapChars) {
            this.overlapChars = overlapChars;
        }
    }

    /** Retrieval-Parameter (Konzept 5.7/V5). */
    public static class Retrieval {
        private int topK = 8;
        /** Normalisierter Hybrid-Score-Schwellwert fuer "keine Antwort ohne Basis" (V5). */
        private double minScore = 0.2;
        /** Optionaler Live-Permission-Check (Hardening, default aus, K3). */
        private boolean livePermissionCheck = false;

        /** @return Anzahl der Top-Treffer fuer das Retrieval */
        public int getTopK() {
            return topK;
        }

        /** @param topK Anzahl der Top-Treffer fuer das Retrieval */
        public void setTopK(int topK) {
            this.topK = topK;
        }

        /** @return normalisierter Score-Schwellwert fuer die Nicht-Antwort */
        public double getMinScore() {
            return minScore;
        }

        /** @param minScore normalisierter Score-Schwellwert fuer die Nicht-Antwort */
        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }

        /** @return ob der optionale Live-Permission-Check aktiv ist */
        public boolean isLivePermissionCheck() {
            return livePermissionCheck;
        }

        /** @param livePermissionCheck aktiviert den optionalen Live-Permission-Check */
        public void setLivePermissionCheck(boolean livePermissionCheck) {
            this.livePermissionCheck = livePermissionCheck;
        }
    }
}
