package com.gentics.mesh.plugin.keycloak.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class KeycloakPluginConfig {

	/**
	 * URL to keycloak server
	 */
	private String url;

	/**
	 * Realm to be used
	 */
	private String realmName;

	/** Whether to load the realms public keys from the Keycloak server. */
	@JsonProperty(defaultValue = "false")
	@JsonPropertyDescription("Whether to load the realms public keys from the Keycloak server")
	private boolean loadPublicKeys = false;

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

	public boolean isLoadPublicKeys() {
		return loadPublicKeys;
	}

	public KeycloakPluginConfig setLoadPublicKeys(boolean loadPublicKeys) {
		this.loadPublicKeys = loadPublicKeys;
		return this;
	}
}
