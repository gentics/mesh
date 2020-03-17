package com.gentics.mesh.plugin.gcms.config;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Configuration POJO
 */
public class GCMSPluginConfig implements Option {
	public final static String PUBLIC_KEY_PATH_ENV = "MESH_GCMS_PLUGIN_PUBLIC_KEY_PATH";

	public final static String PREFIX_CONFIG_ENV = "MESH_GCMS_PLUGIN_PREFIX";

	public final static String PREFIX_USER_CONFIG_ENV = "MESH_GCMS_PLUGIN_PREFIX_USER";

	public final static String PREFIX_GROUP_CONFIG_ENV = "MESH_GCMS_PLUGIN_PREFIX_GROUP";

	public final static String PREFIX_ROLE_CONFIG_ENV = "MESH_GCMS_PLUGIN_PREFIX_ROLE";

	public final static String ISSUER_CONFIG_ENV = "MESH_GCMS_PLUGIN_ISSUER";

	@JsonProperty(defaultValue = "public-key.jwk")
	@JsonPropertyDescription("Path to the JWK file containing the public key")
	@EnvironmentVariable(name = PUBLIC_KEY_PATH_ENV, description = "Override the path to the public key")
	private String publicKeyPath = "public-key.jwk";

	@JsonProperty(defaultValue = "")
	@JsonPropertyDescription("Prefix for mapped users, groups and roles")
	@EnvironmentVariable(name = PREFIX_CONFIG_ENV, description = "Override the prefix")
	private String prefix = "";

	@JsonProperty
	@JsonPropertyDescription("Prefix for mapped users")
	@EnvironmentVariable(name = PREFIX_USER_CONFIG_ENV, description = "Override the user prefix")
	private String prefixUser;

	@JsonProperty
	@JsonPropertyDescription("Prefix for mapped groups")
	@EnvironmentVariable(name = PREFIX_GROUP_CONFIG_ENV, description = "Override the group prefix")
	private String prefixGroup;

	@JsonProperty
	@JsonPropertyDescription("Prefix for mapped roles")
	@EnvironmentVariable(name = PREFIX_ROLE_CONFIG_ENV, description = "Override the role prefix")
	private String prefixRole;

	@JsonProperty(defaultValue = "Gentics CMS")
	@JsonPropertyDescription("Value the iss claim (issuer claim) must have for the mapping to be done")
	@EnvironmentVariable(name = ISSUER_CONFIG_ENV, description = "Override the expected issuer claim")
	private String issuer = "Gentics CMS";

	public String getPublicKeyPath() {
		return publicKeyPath;
	}

	public GCMSPluginConfig setPublicKeyPath(String publicKeyPath) {
		this.publicKeyPath = publicKeyPath;
		return this;
	}

	public String getPrefix() {
		return prefix;
	}

	public GCMSPluginConfig setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public String getPrefixUser() {
		return prefixUser;
	}

	public GCMSPluginConfig setPrefixUser(String prefixUser) {
		this.prefixUser = prefixUser;
		return this;
	}

	public String getPrefixGroup() {
		return prefixGroup;
	}

	public GCMSPluginConfig setPrefixGroup(String prefixGroup) {
		this.prefixGroup = prefixGroup;
		return this;
	}

	public String getPrefixRole() {
		return prefixRole;
	}

	public GCMSPluginConfig setPrefixRole(String prefixRole) {
		this.prefixRole = prefixRole;
		return this;
	}

	public String getIssuer() {
		return issuer;
	}

	public GCMSPluginConfig setIssuer(String issuer) {
		this.issuer = issuer;
		return this;
	}

	/**
	 * Prefix the given user name. Use either the {@link #prefixUser} or the
	 * {@link prefix}
	 * 
	 * @param name name
	 * @return prefixed name
	 */
	@JsonIgnore
	public String prefixUser(String name) {
		return String.format("%s%s", StringUtils.isBlank(prefixUser) ? prefix : prefixUser, name);
	}

	/**
	 * Prefix the given group name. Use either the {@link #prefixGroup} or the
	 * {@link prefix}
	 * 
	 * @param name name
	 * @return prefixed name
	 */
	@JsonIgnore
	public String prefixGroup(String name) {
		return String.format("%s%s", StringUtils.isBlank(prefixGroup) ? prefix : prefixGroup, name);
	}

	/**
	 * Prefix the given role name. Use either the {@link #prefixRole} or the
	 * {@link prefix}
	 * 
	 * @param name name
	 * @return prefixed name
	 */
	@JsonIgnore
	public String prefixRole(String name) {
		return String.format("%s%s", StringUtils.isBlank(prefixRole) ? prefix : prefixRole, name);
	}

	/**
	 * Validate configuration
	 */
	@JsonIgnore
	public void validate() {
	}
}
