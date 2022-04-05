package com.gentics.mesh.plugin.forms.config;

import io.vertx.core.json.JsonObject;

public class ProxyOptions {

	private String host;
	private String port;
	private String user;
	private String password;
	private boolean active = false;


	public String getHost() {
		return host;
	}


	public void setHost(String host) {
		this.host = host;
	}


	public String getPort() {
		return port;
	}


	public void setPort(String port) {
		this.port = port;
	}


	public String getUser() {
		return user;
	}


	public void setUser(String user) {
		this.user = user;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}
	

	public boolean isActive() {
		return active;
	}


	public void setActive(boolean active) {
		this.active = active;
	}
	
	@Override
	public String toString() {
		return JsonObject.mapFrom(this).encodePrettily();
	}


}
