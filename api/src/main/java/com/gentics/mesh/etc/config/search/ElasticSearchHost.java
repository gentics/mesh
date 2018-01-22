package com.gentics.mesh.etc.config.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.MeshOptions;
import static io.netty.util.internal.StringUtil.isNullOrEmpty;

@GenerateDocumentation
public class ElasticSearchHost {

	public static final String DEFAULT_HOSTNAME = "localhost";

	private static final int DEFAULT_PORT = 9200;

	private static final String DEFAULT_PROTOCOL = "http";

	@JsonProperty(required = true)
	@JsonPropertyDescription("Elasticsearch hostname. Default: " + DEFAULT_HOSTNAME)
	private String hostname = DEFAULT_HOSTNAME;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Elasticsearch port. Default: " + DEFAULT_PORT)
	private int port = DEFAULT_PORT;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Elasticsearch port. Default: " + DEFAULT_PORT)
	private String protocol = DEFAULT_PROTOCOL;

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void validate(MeshOptions meshOptions) {
		if (isNullOrEmpty(hostname)) {
			throw new NullPointerException("The search hostname must not be empty.");
		}
		if (isNullOrEmpty(protocol)) {
			throw new NullPointerException("The search host protocol must not be empty");
		}
	}

}
