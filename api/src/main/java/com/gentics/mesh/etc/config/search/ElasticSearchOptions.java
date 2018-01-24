package com.gentics.mesh.etc.config.search;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Search engine options POJO.
 */
@GenerateDocumentation
public class ElasticSearchOptions {

	private final ElasticSearchHost DEFAULT_HOST = new ElasticSearchHost().setHostname("localhost").setPort(9200).setProtocol("http");

	@JsonProperty(required = true)
	@JsonPropertyDescription("Elasticsearch hosts to be used. You can specify multiple hosts in order to loadbalance the requests.")
	private List<ElasticSearchHost> hosts = new ArrayList<>();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether to deploy and start the included Elasticsearch server.")
	private boolean startEmbeddedES = true;

	public ElasticSearchOptions() {
		hosts.add(DEFAULT_HOST);
	}

	public List<ElasticSearchHost> getHosts() {
		return hosts;
	}

	/**
	 * Flag which indicates whether the embedded ES should be started.
	 * 
	 * @return
	 */
	public boolean isStartEmbeddedES() {
		return startEmbeddedES;
	}

	/**
	 * Set the flag to start the embedded ES server.
	 * 
	 * @param startEmbeddedES
	 */
	public void setStartEmbeddedES(boolean startEmbeddedES) {
		this.startEmbeddedES = startEmbeddedES;
	}

	public void validate(MeshOptions meshOptions) {
		for (ElasticSearchHost host : hosts) {
			host.validate(meshOptions);
		}
	}

}
