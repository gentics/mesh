package com.gentics.mesh.core.rest.admin.cluster.coordinator;

import com.gentics.mesh.core.rest.common.RestModel;

public class CoordinatorMasterResponse implements RestModel {

	private String name;
	private int port;
	private String host;

	public CoordinatorMasterResponse() {
	}

	public CoordinatorMasterResponse(String name, int port, String host) {
		this.name = name;
		this.port = port;
		this.host = host;
	}

	public String getName() {
		return name;
	}

	public CoordinatorMasterResponse setName(String name) {
		this.name = name;
		return this;
	}

	public int getPort() {
		return port;
	}

	public CoordinatorMasterResponse setPort(int port) {
		this.port = port;
		return this;
	}

	public String getHost() {
		return host;
	}

	public CoordinatorMasterResponse setHost(String host) {
		this.host = host;
		return this;
	}
}
