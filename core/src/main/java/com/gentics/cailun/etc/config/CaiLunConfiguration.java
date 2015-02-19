package com.gentics.cailun.etc.config;

import io.vertx.ext.graph.neo4j.Neo4VertxConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;

import com.gentics.cailun.etc.CaiLunVerticleConfiguration;

public class CaiLunConfiguration {

	// private static final boolean ENABLED = true;
	private static final boolean DISABLED = false;

	public static final String HTTP_PORT_KEY = "httpPort";
	public static final int DEFAULT_HTTP_PORT = 8080;
	public static final boolean DEFAULT_CLUSTER_MODE = DISABLED;
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	public static final String DEFAULT_NEO4VERTX_BASE_ADDRESS = "graph";
	public static final String DEFAULT_NEO4VERTX_MODE = "gui";
	public static final String DEFAULT_NEO4J_WEB_SERVER_BIND_ADDRESS = "0.0.0.0";

	@JsonProperty("neo4j_config")
	private Neo4VertxConfiguration neo4jConfiguration;

	@JsonProperty("http_port")
	private int httpPort = DEFAULT_HTTP_PORT;

	@JsonProperty("cluster_mode")
	private boolean clusterMode = DEFAULT_CLUSTER_MODE;

	@JsonProperty("verticles")
	private Map<String, CaiLunVerticleConfiguration> verticles = new HashMap<>();

	public CaiLunConfiguration() {
		neo4jConfiguration = new Neo4VertxConfiguration();
		neo4jConfiguration.setMode(DEFAULT_NEO4VERTX_MODE);
		neo4jConfiguration.setWebServerBindAddress(DEFAULT_NEO4J_WEB_SERVER_BIND_ADDRESS);
		// Check for target directory and use it as a subdirectory if possible
		File targetDir = new File("target");
		if (targetDir.exists()) {
			neo4jConfiguration.setPath(new File(targetDir, DEFAULT_DIRECTORY_NAME).getAbsolutePath());
		} else {
			neo4jConfiguration.setPath(new File(DEFAULT_DIRECTORY_NAME).getAbsolutePath());
		}
	}

	public Map<String, CaiLunVerticleConfiguration> getVerticles() {
		return verticles;
	}

	public boolean isClusterMode() {
		return clusterMode;
	}

	public Neo4VertxConfiguration getNeo4jConfiguration() {
		return neo4jConfiguration;
	}
}
