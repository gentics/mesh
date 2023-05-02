package com.gentics.mesh.plugin.keycloak.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

import com.gentics.mesh.plugin.common.license.LicenseConfiguration;
import java.util.ArrayList;
import java.util.List;

public class KeycloakPluginConfig extends LicenseConfiguration {

	public static final String ENV_PREFIX = "MESH_PLUGIN_KEYCLOAK_";

	private static final String ENV_URL = ENV_PREFIX + "URL";
	private static final String ENV_REALM = ENV_PREFIX + "REALM_NAME";
	private static final String ENV_LOAD_PUBLIC_KEYS = ENV_PREFIX + "LOAD_PUBLIC_KEYS";

	/**
	 * URL to keycloak server
	 */
	@JsonProperty
	@JsonPropertyDescription("Url to the keycloak server")
	@EnvironmentVariable(name = ENV_URL, description = "Override the keycloak url")
	private String url;

	/**
	 * Realm to be used
	 */
	@JsonProperty
	@JsonPropertyDescription("Name of the Keycloak realm")
	@EnvironmentVariable(name = ENV_REALM, description = "Override the keycloak realm name")
	private String realmName;

	/**
	 * Managed roles
	 */
	@JsonProperty(defaultValue = "empty List")
	@JsonPropertyDescription("Roles that are managed by this plugin. They and their groups are created/update ond plugin startup")
	private List<String> managedRoles = new ArrayList<>();

	/**
	 * Whether to load the realms public keys from the Keycloak server.
	 */
	@JsonProperty(defaultValue = "false")
	@JsonPropertyDescription("Whether to load the realms public keys from the Keycloak server")
	@EnvironmentVariable(name = ENV_LOAD_PUBLIC_KEYS, description = "Override if the Public keys should be loaded")
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

	public List<String> getManagedRoles() {
		return managedRoles;
	}

	public KeycloakPluginConfig setManagedRoles(List<String> managedRoles) {
		this.managedRoles = managedRoles;
		return this;
	}
}
