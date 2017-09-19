package com.gentics.mesh.etc.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;

/**
 * Search engine options POJO.
 */
@GenerateDocumentation
public class ElasticSearchOptions {

	public static final String DEFAULT_DIRECTORY = "data" + File.separator + "searchindex";

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the elasticsearch data directory.")
	private String directory = DEFAULT_DIRECTORY;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether to enable or disable the elasticsearch http server. Please note that the HTTP server is not secured and thus this option should only be enabled if the server has been protected properly.")
	private boolean httpEnabled = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configures the elasticsarch transport.tcp.port setting.")
	private String transportPort = "9300-9400";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional set of search parameters.")
	private Map<String, Object> parameters = new HashMap<>();

	/**
	 * Check whether the http server should be enabled.
	 * 
	 * @return
	 */
	public boolean isHttpEnabled() {
		return httpEnabled;
	}

	/**
	 * Set http server flag
	 * 
	 * @param httpEnabled
	 *            Server flag
	 * @return Fluent API
	 */
	public ElasticSearchOptions setHttpEnabled(boolean httpEnabled) {
		this.httpEnabled = httpEnabled;
		return this;
	}

	/**
	 * 
	 * Return the search index filesystem directory.
	 * 
	 * @return Path to the search index filesystem directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * Set the search index filesystem directory.
	 * 
	 * @param directory
	 *            Path to the search index filesystem directory
	 * @return Fluent API
	 */
	public ElasticSearchOptions setDirectory(String directory) {
		this.directory = directory;
		return this;
	}

	/**
	 * Return the transport.tcp.port configured value.
	 * 
	 * @return
	 */
	public String getTransportPort() {
		return transportPort;
	}

	/**
	 * Set the transport.tcp.port value.
	 * 
	 * @param transportPort
	 * @return
	 */
	public ElasticSearchOptions setTransportPort(String transportPort) {
		this.transportPort = transportPort;
		return this;
	}

	/**
	 * Return the set of custom parameters.
	 * 
	 * @return
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void validate(MeshOptions meshOptions) {
		if (meshOptions.getClusterOptions() != null && meshOptions.getClusterOptions().isEnabled() && getTransportPort() == null) {
			throw new NullPointerException("The searchprovider transport port setting must be configured when clustering is enabled.");
		}
	}

}
