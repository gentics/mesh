package com.gentics.mesh.contentrag.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Konfiguration des RAG-Service. Bindet an Praefix {@code rag}; Werte siehe application.yml.
 *
 * <p>Der Service ist reiner Leser: befuellt werden die Indizes von Mesh ueber dessen
 * Standard-Sync. Entsprechend gibt es hier keine Ingestion-, Chunking- oder
 * Embedding-Batch-Parameter mehr – das Chunking konfiguriert die Mesh-Seite, die Vektorisierung
 * uebernimmt Elasticsearch.
 */
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Inference inference = new Inference();
    private final Mesh mesh = new Mesh();
    private final Llm llm = new Llm();
    private final Retrieval retrieval = new Retrieval();

    /** @return Elasticsearch-Verbindungseinstellungen */
    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    /** @return Einstellungen der Vektorisierung in Elasticsearch */
    public Inference getInference() {
        return inference;
    }

    /** @return Mesh-Verbindungseinstellungen (nur optionaler Live-Permission-Check) */
    public Mesh getMesh() {
        return mesh;
    }

    /** @return LLM-Provider-Einstellungen */
    public Llm getLlm() {
        return llm;
    }

    /** @return Retrieval-Parameter */
    public Retrieval getRetrieval() {
        return retrieval;
    }

    /** Verbindung zu der Elasticsearch-Instanz, die auch die Mesh-Volltextsuche traegt. */
    public static class Elasticsearch {
        private String url = "http://localhost:9200";
        private String username;
        private String password;
        /** Installations-Praefix; muss mit dem Praefix der Mesh-Suchkonfiguration uebereinstimmen. */
        private String indexPrefix = "mesh";

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

        /** @return Installations-Praefix der Indexnamen */
        public String getIndexPrefix() {
            return indexPrefix;
        }

        /** @param indexPrefix Installations-Praefix der Indexnamen */
        public void setIndexPrefix(String indexPrefix) {
            this.indexPrefix = indexPrefix;
        }
    }

    /**
     * Vektorisierung innerhalb von Elasticsearch: ein Inference-Endpunkt zeigt auf den
     * Text-Embeddings-Server, eine Ingest-Pipeline haengt den Vektor beim Indexieren an.
     * Derselbe Endpunkt erzeugt zur Query-Zeit den Frage-Vektor.
     */
    public static class Inference {
        /** ID des Inference-Endpunkts in Elasticsearch. */
        private String id = "mesh-content-rag-embeddings";
        /** ID der Ingest-Pipeline. */
        private String pipelineId = "mesh-content-rag-embed";
        /** URL des Embed-Endpunkts des Inference-Servers (TEI). */
        private String url = "http://localhost:8081/embed";
        /** Von TEI ignoriert, von der Elasticsearch-Konfiguration jedoch verlangt. */
        private String apiKey;
        /** Vektordimension des Modells; muss zum Modell des Endpunkts passen. */
        private int dimension = 384;
        /** Primaer-Shards je RAG-Index. */
        private int shards = 1;
        /** Replicas je RAG-Index. */
        private int replicas = 1;
        /** Legt Endpunkt, Pipeline und Index-Template beim Start an, falls nicht vorhanden. */
        private boolean bootstrap = true;

        /** @return ID des Inference-Endpunkts */
        public String getId() {
            return id;
        }

        /** @param id ID des Inference-Endpunkts */
        public void setId(String id) {
            this.id = id;
        }

        /** @return ID der Ingest-Pipeline */
        public String getPipelineId() {
            return pipelineId;
        }

        /** @param pipelineId ID der Ingest-Pipeline */
        public void setPipelineId(String pipelineId) {
            this.pipelineId = pipelineId;
        }

        /** @return URL des Embed-Endpunkts */
        public String getUrl() {
            return url;
        }

        /** @param url URL des Embed-Endpunkts */
        public void setUrl(String url) {
            this.url = url;
        }

        /** @return API-Schluessel des Inference-Servers (oder {@code null}) */
        public String getApiKey() {
            return apiKey;
        }

        /** @param apiKey API-Schluessel des Inference-Servers */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /** @return Vektordimension des Modells */
        public int getDimension() {
            return dimension;
        }

        /** @param dimension Vektordimension des Modells */
        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        /** @return Anzahl Primaer-Shards je RAG-Index */
        public int getShards() {
            return shards;
        }

        /** @param shards Anzahl Primaer-Shards je RAG-Index */
        public void setShards(int shards) {
            this.shards = shards;
        }

        /** @return Anzahl Replicas je RAG-Index */
        public int getReplicas() {
            return replicas;
        }

        /** @param replicas Anzahl Replicas je RAG-Index */
        public void setReplicas(int replicas) {
            this.replicas = replicas;
        }

        /** @return ob beim Start Endpunkt, Pipeline und Template sichergestellt werden */
        public boolean isBootstrap() {
            return bootstrap;
        }

        /** @param bootstrap aktiviert das Anlegen von Endpunkt, Pipeline und Template beim Start */
        public void setBootstrap(boolean bootstrap) {
            this.bootstrap = bootstrap;
        }
    }

    /**
     * Anbindung an Gentics Mesh. Wird ausschliesslich vom optionalen Live-Permission-Check
     * benoetigt (K3, default aus); der regulaere Betrieb kommt ohne Mesh-Zugriff aus.
     */
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

        /** @return API-Token/JWT (oder {@code null}) */
        public String getApiToken() {
            return apiToken;
        }

        /** @param apiToken API-Token/JWT */
        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }
    }

    /** LLM-Provider (Konzept 5.5). Default-Profil "cloud-hybrid" (EU-gehostet). */
    public static class Llm {
        /** cloud-hybrid | sovereign-onprem */
        private String profile = "cloud-hybrid";
        private String model = "eu-managed-llm";
        private String endpoint;
        private String apiKey;

        /** @return Deployment-Profil */
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
