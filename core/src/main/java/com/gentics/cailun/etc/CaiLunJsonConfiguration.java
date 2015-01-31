package com.gentics.cailun.etc;

import io.vertx.core.json.JsonObject;

public class CaiLunJsonConfiguration implements CaiLunConfiguration {

	private final JsonObject jsonConfig;

	public CaiLunJsonConfiguration(JsonObject jsonConfig) {
		this.jsonConfig = jsonConfig;
	}

	@Override
	public int getPort() {
		return DEFAULT_HTTP_PORT;
	}

	@Override
	public void setPort(int port) {
		jsonConfig.put(HTTP_PORT_KEY, port);
		
	}

}
