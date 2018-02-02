package com.gentics.mesh.etc.config.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Search engine options POJO.
 */
@GenerateDocumentation
public class ElasticSearchOptions {

	/**
	 * Default ES connection details.
	 */
	private static final String DEFAULT_URL = "http://localhost:9200";

	private static final long DEFAULT_TIMEOUT = 8000L;

	private static final String DEFAULT_ARGS = "-Xms1g -Xmx1g -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:+AlwaysPreTouch -client -Xss1m -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djna.nosys=true -XX:-OmitStackTraceInFastThrow -Dio.netty.noUnsafe=true -Dio.netty.noKeySetOptimization=true -Dio.netty.recycler.maxCapacityPerThread=0 -Dlog4j.shutdownHookEnabled=false -Dlog4j2.disable.jmx=true -XX:+HeapDumpOnOutOfMemoryError";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Elasticsearch connection url to be used. Set this setting to null will disable the Elasticsearch support.")
	private String url = DEFAULT_URL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Timeout for Elasticsearch operations. Default: " + DEFAULT_TIMEOUT + "ms")
	private Long timeout = DEFAULT_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether to deploy and start the included Elasticsearch server.")
	private boolean startEmbedded = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("String of arguments which will be used for starting the Elasticsearch server instance")
	private String embeddedArguments = DEFAULT_ARGS;

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

	public void setUrl(String url) {
		this.url = url;
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

}
