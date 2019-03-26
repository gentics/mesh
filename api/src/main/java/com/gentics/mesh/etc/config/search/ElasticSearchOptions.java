package com.gentics.mesh.etc.config.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Search engine options POJO.
 */
@GenerateDocumentation
public class ElasticSearchOptions implements Option {

	/**
	 * Default ES connection details.
	 */
	public static final String DEFAULT_URL = "http://localhost:9200";
	public static final long DEFAULT_TIMEOUT = 8000L;

	public static final int DEFAULT_STARTUP_TIMEOUT = 45;

	public static final int DEFAULT_BULK_LIMIT = 2000;

	public static final String DEFAULT_PREFIX = "mesh-";

	public static final String DEFAULT_ARGS = "-Xms1g -Xmx1g -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+AlwaysPreTouch -client -Xss1m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djna.nosys=true -XX:-OmitStackTraceInFastThrow -Dio.netty.noUnsafe=true -Dio.netty.noKeySetOptimization=true -Dio.netty.recycler.maxCapacityPerThread=0 -Dlog4j.shutdownHookEnabled=false -Dlog4j2.disable.jmx=true -XX:+HeapDumpOnOutOfMemoryError";

	public static final String MESH_ELASTICSEARCH_URL_ENV = "MESH_ELASTICSEARCH_URL";
	public static final String MESH_ELASTICSEARCH_TIMEOUT_ENV = "MESH_ELASTICSEARCH_TIMEOUT";
	public static final String MESH_ELASTICSEARCH_STARTUP_TIMEOUT_ENV = "MESH_ELASTICSEARCH_STARTUP_TIMEOUT";
	public static final String MESH_ELASTICSEARCH_START_EMBEDDED_ENV = "MESH_ELASTICSEARCH_START_EMBEDDED";
	public static final String MESH_ELASTICSEARCH_PREFIX_ENV = "MESH_ELASTICSEARCH_PREFIX";
	public static final String MESH_ELASTICSEARCH_BULK_LIMIT_ENV = "MESH_ELASTICSEARCH_BULK_LIMIT";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Elasticsearch connection url to be used. Set this setting to null will disable the Elasticsearch support.")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_URL_ENV, description = "Override the configured elasticsearch server url. The value can be set to null in order to disable the Elasticsearch support.")
	private String url = DEFAULT_URL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Timeout for Elasticsearch operations. Default: " + DEFAULT_TIMEOUT + "ms")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_TIMEOUT_ENV, description = "Override the configured elasticsearch server timeout.")
	private Long timeout = DEFAULT_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Timeout for Elasticsearch startup. Default: " + DEFAULT_STARTUP_TIMEOUT + "sec")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_STARTUP_TIMEOUT_ENV, description = "Override the configured elasticsearch server timeout.")
	private Integer startupTimeout = DEFAULT_STARTUP_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether to deploy and start the included Elasticsearch server.")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_START_EMBEDDED_ENV, description = "Override the start embedded elasticsearch server flag.")
	private boolean startEmbedded = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("String of arguments which will be used for starting the Elasticsearch server instance")
	private String embeddedArguments = DEFAULT_ARGS;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Upper limit for the size of bulk requests.")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_BULK_LIMIT_ENV, description = "Override the batch bulk limit. Default: " + DEFAULT_BULK_LIMIT)
	private int bulkLimit = DEFAULT_BULK_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Search server prefix for this installation. Choosing different prefixes for each Gentics Mesh instance will allow you to use a single Elasticsearch cluster for multiple Gentics Mesh instances. Default: "
		+ DEFAULT_PREFIX)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_PREFIX_ENV, description = "Override the configured elasticsearch prefix.")
	private String prefix = DEFAULT_PREFIX;

	public ElasticSearchOptions() {

	}

	/**
	 * Flag which indicates whether the embedded ES should be started.
	 * 
	 * @return
	 */
	public boolean isStartEmbedded() {
		return startEmbedded;
	}

	/**
	 * Set the flag to start the embedded ES server.
	 * 
	 * @param startEmbedded
	 * @return Fluent API
	 */
	public ElasticSearchOptions setStartEmbedded(boolean startEmbedded) {
		this.startEmbedded = startEmbedded;
		return this;
	}

	/**
	 * Return the operation timeout in milliseconds.
	 * 
	 * @return
	 */
	public Long getTimeout() {
		return timeout;
	}

	/**
	 * Set the operation timeout.
	 * 
	 * @param timeout
	 * @return Fluent API
	 */
	public ElasticSearchOptions setTimeout(Long timeout) {
		this.timeout = timeout;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public ElasticSearchOptions setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getEmbeddedArguments() {
		return embeddedArguments;
	}

	public ElasticSearchOptions setEmbeddedArguments(String embeddedArguments) {
		this.embeddedArguments = embeddedArguments;
		return this;
	}

	public void validate(MeshOptions meshOptions) {

	}

	public long getStartupTimeout() {
		return startupTimeout;
	}

	public ElasticSearchOptions setStartupTimeout(Integer startupTimeout) {
		this.startupTimeout = startupTimeout;
		return this;
	}

	public int getBulkLimit() {
		return bulkLimit;
	}

	public ElasticSearchOptions setBulkLimit(int bulkLimit) {
		this.bulkLimit = bulkLimit;
		return this;
	}

	public String getPrefix() {
		return prefix;
	}

	public ElasticSearchOptions setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

}
