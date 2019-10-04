package com.gentics.mesh.plugin.keycloak.config;

public class KeycloakPluginConfig {

	/**
	 * URL to keycloak server
	 */
	private String url;

	/**
	 * Realm to be used
	 */
	private String realmName;

	public String getUrl() {
		return url;
	}

	public KeycloakPluginConfig setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getRealmName() {
		return realmName;
	}

	public KeycloakPluginConfig setRealmName(String realmName) {
		this.realmName = realmName;
		return this;
	}
}
