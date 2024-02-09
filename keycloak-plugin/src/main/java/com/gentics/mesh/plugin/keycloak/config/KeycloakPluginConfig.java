package com.gentics.mesh.plugin.keycloak.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

public class KeycloakPluginConfig implements Option {

	public static final String ENV_PREFIX = "MESH_PLUGIN_KEYCLOAK_";

	private static final String ENV_URL = ENV_PREFIX + "URL";
	private static final String ENV_REALM = ENV_PREFIX + "REALM_NAME";
	private static final String ENV_LOAD_PUBLIC_KEYS = ENV_PREFIX + "LOAD_PUBLIC_KEYS";
	private static final String ENV_REALM_URL_FORMAT = ENV_PREFIX + "REALM_URL_FORMAT";

	public static final String DEFAULT_REALM_URL_FORMAT = KeycloakUtils.DEFAULT_REALM_URL_FORMAT; 
	
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

	/**
	 * Realm to be used
	 */
	@JsonProperty(required = false)
	@JsonPropertyDescription("Keycloak realm URL format. Default value is: " + DEFAULT_REALM_URL_FORMAT)
	@EnvironmentVariable(name = ENV_REALM_URL_FORMAT, description = "Override the keycloak realm URL format")
	private String realmUrlFormat = DEFAULT_REALM_URL_FORMAT;

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

	public String getRealmUrlFormat() {
		return realmUrlFormat;
	}

	public KeycloakPluginConfig setRealmUrlFormat(String realmUrlFormat) {
		this.realmUrlFormat = realmUrlFormat;
		return this;
	}
}
