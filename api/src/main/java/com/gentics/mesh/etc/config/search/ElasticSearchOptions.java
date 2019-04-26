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

	public static final int DEFAULT_EVENT_BUFFER_SIZE = 1000;
	public static final int DEFAULT_BULK_DEBOUNCE_TIME = 2000;
	public static final int DEFAULT_IDLE_DEBOUNCE_TIME = 100;
	public static final int DEFAULT_RETRY_INTERVAL = 5000;
	public static final boolean DEFAULT_WAIT_FOR_IDLE = true;

	public static final String DEFAULT_PREFIX = "mesh-";

	public static final String DEFAULT_ARGS = "-Xms1g -Xmx1g -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+AlwaysPreTouch -client -Xss1m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djna.nosys=true -XX:-OmitStackTraceInFastThrow -Dio.netty.noUnsafe=true -Dio.netty.noKeySetOptimization=true -Dio.netty.recycler.maxCapacityPerThread=0 -Dlog4j.shutdownHookEnabled=false -Dlog4j2.disable.jmx=true -XX:+HeapDumpOnOutOfMemoryError";

	public static final String MESH_ELASTICSEARCH_URL_ENV = "MESH_ELASTICSEARCH_URL";
	public static final String MESH_ELASTICSEARCH_TIMEOUT_ENV = "MESH_ELASTICSEARCH_TIMEOUT";
	public static final String MESH_ELASTICSEARCH_STARTUP_TIMEOUT_ENV = "MESH_ELASTICSEARCH_STARTUP_TIMEOUT";
	public static final String MESH_ELASTICSEARCH_START_EMBEDDED_ENV = "MESH_ELASTICSEARCH_START_EMBEDDED";
	public static final String MESH_ELASTICSEARCH_PREFIX_ENV = "MESH_ELASTICSEARCH_PREFIX";
	public static final String MESH_ELASTICSEARCH_BULK_LIMIT_ENV = "MESH_ELASTICSEARCH_BULK_LIMIT";
	public static final String MESH_ELASTICSEARCH_EVENT_BUFFER_SIZE_ENV = "MESH_ELASTICSEARCH_EVENT_BUFFER_SIZE";
	public static final String MESH_ELASTICSEARCH_BULK_DEBOUNCE_TIME_ENV = "MESH_ELASTICSEARCH_BULK_DEBOUNCE_TIME";
	public static final String MESH_ELASTICSEARCH_IDLE_DEBOUNCE_TIME_ENV = "MESH_ELASTICSEARCH_IDLE_DEBOUNCE_TIME";
	public static final String MESH_ELASTICSEARCH_RETRY_INTERVAL_ENV = "MESH_ELASTICSEARCH_RETRY_INTERVAL";
	public static final String MESH_ELASTICSEARCH_WAIT_FOR_IDLE_ENV = "MESH_ELASTICSEARCH_WAIT_FOR_IDLE";

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
	@JsonPropertyDescription("Search server prefix for this installation. Choosing different prefixes for each Gentics Mesh instance will allow you to use a single Elasticsearch cluster for multiple Gentics Mesh instances. Default: "
		+ DEFAULT_PREFIX)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_PREFIX_ENV, description = "Override the configured elasticsearch prefix.")
	private String prefix = DEFAULT_PREFIX;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Upper limit for the size of bulk requests. Default: " + DEFAULT_BULK_LIMIT)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_BULK_LIMIT_ENV, description = "Override the batch bulk limit. Default: " + DEFAULT_BULK_LIMIT)
	private int bulkLimit = DEFAULT_BULK_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Upper limit for mesh events that are to be mapped to elastic search requests. Default: "
		+ DEFAULT_EVENT_BUFFER_SIZE)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_EVENT_BUFFER_SIZE_ENV, description = "Override the configured event buffer size.")
	private int eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The maximum amount of time in milliseconds between two bulkable requests before they are sent. Default: "
		+ DEFAULT_BULK_DEBOUNCE_TIME)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_BULK_DEBOUNCE_TIME_ENV, description = "Override the bulk debounce time.")
	private int bulkDebounceTime = DEFAULT_BULK_DEBOUNCE_TIME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The maximum amount of time in milliseconds between two successful requests before the idle event is emitted. Default: "
		+ DEFAULT_IDLE_DEBOUNCE_TIME)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_IDLE_DEBOUNCE_TIME_ENV, description = "Override the idle debounce time.")
	private int idleDebounceTime = DEFAULT_IDLE_DEBOUNCE_TIME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The time in milliseconds between retries of elastic search requests in case of a failure. Default: "
		+ DEFAULT_RETRY_INTERVAL)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_RETRY_INTERVAL_ENV, description = "Override the retry interval.")
	private int retryInterval = DEFAULT_RETRY_INTERVAL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("If true, search endpoints wait for elasticsearch to be idle before sending a response. Default: "
		+ DEFAULT_WAIT_FOR_IDLE)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_WAIT_FOR_IDLE_ENV, description = "Override the search idle wait flag.")
	private boolean waitForIdle = DEFAULT_WAIT_FOR_IDLE;

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

	public int getEventBufferSize() {
		return eventBufferSize;
	}

	public ElasticSearchOptions setEventBufferSize(int eventBufferSize) {
		this.eventBufferSize = eventBufferSize;
		return this;
	}

	public int getBulkDebounceTime() {
		return bulkDebounceTime;
	}

	public ElasticSearchOptions setBulkDebounceTime(int bulkDebounceTime) {
		this.bulkDebounceTime = bulkDebounceTime;
		return this;
	}

	public int getIdleDebounceTime() {
		return idleDebounceTime;
	}

	public ElasticSearchOptions setIdleDebounceTime(int idleDebounceTime) {
		this.idleDebounceTime = idleDebounceTime;
		return this;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	public ElasticSearchOptions setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
		return this;
	}

	public boolean isWaitForIdle() {
		return waitForIdle;
	}

	public ElasticSearchOptions setWaitForIdle(boolean waitForIdle) {
		this.waitForIdle = waitForIdle;
		return this;
	}
}
