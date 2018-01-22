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

	@JsonProperty(required = true)
	@JsonPropertyDescription("Elasticsearch hosts to be used.")
	private List<ElasticSearchHost> hosts = new ArrayList<>();

	public List<ElasticSearchHost> getHosts() {
		return hosts;
	}

	public void validate(MeshOptions meshOptions) {
		for (ElasticSearchHost host : hosts) {
			host.validate(meshOptions);
		}
	}

}
