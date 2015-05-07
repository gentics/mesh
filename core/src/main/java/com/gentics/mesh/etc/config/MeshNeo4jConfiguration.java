package com.gentics.mesh.etc.config;

import io.vertx.core.json.JsonObject;

import java.io.File;

import org.neo4j.cluster.ClusterSettings;
import org.neo4j.kernel.ha.HaSettings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Configuration interface.
 */
public class MeshNeo4jConfiguration {
	
    public final static String DEFAULT_MODE = "default";
    public final static String CLUSTER_MODE = "cluster";
    public final static String GUI_MODE = "gui";

    public static final String MODE_KEY = "mode";
    public static final String PATH_KEY = "path";
    public static final String REST_URL_KEY = "rest_url";
    public static final String BASE_ADDR_KEY = "base_address";
    public static final String HA_INITIAL_HOSTS_KEY = "ha_initial_hosts";
    public static final String HA_CLUSTER_SERVER_KEY = "ha_cluster_server";
    public static final String HA_SLAVE_ONLY_KEY = "ha_slave_only";
    public static final String HA_SERVER_KEY = "ha_server";
    public static final String WEB_SERVER_BIND_ADDRESS_KEY = "webserver_bind_address";
    public static final String HA_SERVER_ID_KEY = "ha_server_id";

    public final static String DEFAULT_WEBSERVER_BIND_ADDRESS = "localhost";
    public final static String DEFAULT_PATH = System.getProperty("user.dir") + File.separator + "db";
    public final static String DEFAULT_REST_URL = "http://localhost:7474/db/data/cypher";
    public final static String DEFAULT_BASE_ADDRESS = "neo4j-graph";

    private String mode;
    private String path;
    
    @JsonProperty(WEB_SERVER_BIND_ADDRESS_KEY)
    private String webServerBindAddress;

    @JsonProperty(REST_URL_KEY)
    private String restUrl;

    @JsonProperty(BASE_ADDR_KEY)
    private String baseAddress;

    @JsonProperty(HA_INITIAL_HOSTS_KEY)
    private String haInitialHosts;

    @JsonProperty(HA_CLUSTER_SERVER_KEY)
    private String haClusterServer;

    @JsonProperty(HA_SLAVE_ONLY_KEY)
    private boolean haSlaveOnly;

    @JsonProperty(HA_SERVER_KEY)
    private String haServer;

    @JsonProperty(HA_SERVER_ID_KEY)
    private String haServerId;

    public MeshNeo4jConfiguration() {

    }

    public MeshNeo4jConfiguration(JsonObject jsonConfig) {
        webServerBindAddress = jsonConfig.getString(WEB_SERVER_BIND_ADDRESS_KEY, DEFAULT_WEBSERVER_BIND_ADDRESS);
        mode = jsonConfig.getString(MODE_KEY, DEFAULT_MODE);
        path = jsonConfig.getString(PATH_KEY, DEFAULT_PATH);
        restUrl = jsonConfig.getString(REST_URL_KEY, DEFAULT_REST_URL);
        baseAddress = jsonConfig.getString(BASE_ADDR_KEY, DEFAULT_BASE_ADDRESS);
        haInitialHosts = jsonConfig.getString(HA_INITIAL_HOSTS_KEY, null);
        haClusterServer = jsonConfig.getString(HA_CLUSTER_SERVER_KEY, ClusterSettings.cluster_server.getDefaultValue());
        haSlaveOnly = jsonConfig.getBoolean(HA_SLAVE_ONLY_KEY, false);
        haServer = jsonConfig.getString(HA_SERVER_KEY, HaSettings.ha_server.getDefaultValue());
        haServerId = jsonConfig.getString(HA_SERVER_ID_KEY, null);
    }

    public String getMode() {
        return mode;
    }

    /**
     * Return the storage path for the neo4j database files.
     *
     * @return storage path
     */
    public String getPath() {
        return path;
    }

    /**
     * Return the url for the cypher REST service.
     *
     * @return the cypher rest url.
     */
    @JsonProperty(REST_URL_KEY)
    public String getRestUrl() {
        return restUrl;
    }

    /**
     * Return the prefix for the vertex event bus address.
     *
     * @return base address prefix
     */
    @JsonProperty(BASE_ADDR_KEY)
    public String getBaseAddress() {
        return baseAddress;
    }

    /**
     * Return the webserver address. The address is used to bind the rest server on an ip/interface. You may use 0.0.0.0 to bind on all network devices.
     * 
     * @return webserver address
     */
    @JsonProperty(WEB_SERVER_BIND_ADDRESS_KEY)
    public String getWebServerBindAddress() {
        return webServerBindAddress;
    }

    /**
     * Return the ha.initial_hosts setting.
     *
     * @return ha.initial_hosts property value
     */
    @JsonProperty(HA_INITIAL_HOSTS_KEY)
    public String getHAInitialHosts() {
        return haInitialHosts;
    }

    /**
     * Return the ha.server_id setting.
     *
     * @return ha.server_id property value
     */
    @JsonProperty(HA_SERVER_ID_KEY)
    public String getHAServerID() {
        return haServerId;
    }

    /**
     * Return the ha.slave_only setting.
     *
     * @return ha.slave_only flag
     */
    @JsonProperty(HA_SLAVE_ONLY_KEY)
    public boolean getHASlaveOnly() {
        return haSlaveOnly;
    }

    /**
     * Return the ha.server setting.
     *
     * @return ha.server property value
     */
    @JsonProperty(HA_SERVER_KEY)
    public String getHAServer() {
        return haServer;
    }

    /**
     * Return the ha.server_cluster setting.
     *
     * @return ha.server_cluster property value
     */
    @JsonProperty(HA_CLUSTER_SERVER_KEY)
    public String getHAClusterServer() {
        return haClusterServer;
    }

    public void setWebServerBindAddress(String webServerBindAddress) {
        this.webServerBindAddress = webServerBindAddress;
    }

    public void setHaInitialHosts(String haInitialHosts) {
        this.haInitialHosts = haInitialHosts;
    }

    public void setHaClusterServer(String haClusterServer) {
        this.haClusterServer = haClusterServer;
    }

    public boolean isHaSlaveOnly() {
        return haSlaveOnly;
    }

    public void setHaSlaveOnly(boolean haSlaveOnly) {
        this.haSlaveOnly = haSlaveOnly;
    }

    public void setHaServer(String haServer) {
        this.haServer = haServer;
    }

    public void setHaServerId(String haServerId) {
        this.haServerId = haServerId;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    /**
     * Wrap the configuration into a vertx json object.
     * 
     * @return
     */
    @JsonIgnore
    public JsonObject getJsonConfig() {
        JsonObject object = new JsonObject();
        object.put(MODE_KEY, getMode());
        object.put(PATH_KEY, getPath());
        object.put(REST_URL_KEY, getRestUrl());
        object.put(WEB_SERVER_BIND_ADDRESS_KEY, getWebServerBindAddress());
        object.put(BASE_ADDR_KEY, getBaseAddress());
        object.put(HA_INITIAL_HOSTS_KEY, getHAInitialHosts());
        object.put(HA_CLUSTER_SERVER_KEY, getHAClusterServer());
        object.put(HA_SLAVE_ONLY_KEY, getHASlaveOnly());
        object.put(HA_SERVER_KEY, getHAServer());
        object.put(HA_SERVER_ID_KEY, getHAServerID());
        return object;
    }

}
