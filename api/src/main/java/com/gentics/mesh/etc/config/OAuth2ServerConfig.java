package com.gentics.mesh.etc.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.Option;

import io.vertx.core.json.JsonObject;

public class OAuth2ServerConfig implements Option {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Realm name to be used.")
	private String realm = "master";

	@JsonProperty(required = true)
	@JsonPropertyDescription("URL to the authentication server.")
	private String authServerUrl = "http://localhost:3000/auth";

	@JsonProperty(required = false)
	@JsonPropertyDescription("SSL Required flag of the realm.")
	private String sslRequired = "external";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the resource to be used.")
	private String resource;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional credentials for client-server communication.")
	private Map<String, String> credentials = new HashMap<>();

	@JsonProperty(required = false)
	private int confidentialPort = 0;

	/**
	 * Return the realm name.
	 * 
	 * @return
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * Set the realm name.
	 * 
	 * @param realm
	 * @return Fluent API
	 */
	public OAuth2ServerConfig setRealm(String realm) {
		this.realm = realm;
		return this;
	}

	/**
	 * Return the authentication server url.
	 * 
	 * @return
	 */
	public String getAuthServerUrl() {
		return authServerUrl;
	}

	/**
	 * Return the authentication server url.
	 * 
	 * @param authServerUrl
	 * @return Fluent API
	 */
	public OAuth2ServerConfig setAuthServerUrl(String authServerUrl) {
		this.authServerUrl = authServerUrl;
		return this;
	}

	public String getSslRequired() {
		return sslRequired;
	}

	public OAuth2ServerConfig setSslRequired(String sslRequired) {
		this.sslRequired = sslRequired;
		return this;
	}

	public String getResource() {
		return resource;
	}

	public OAuth2ServerConfig setResource(String resource) {
		this.resource = resource;
		return this;
	}

	public int getConfidentialPort() {
		return confidentialPort;
	}

	public OAuth2ServerConfig setConfidentialPort(int confidentialPort) {
		this.confidentialPort = confidentialPort;
		return this;
	}

	/**
	 * Return the configured credentials.
	 * 
	 * @return
	 */
	public Map<String, String> getCredentials() {
		return credentials;
	}

	public OAuth2ServerConfig setCredentials(Map<String, String> credentials) {
		this.credentials = credentials;
		return this;
	}

	/**
	 * Add the given credential pair to the map of credentials.
	 * 
	 * @param key
	 * @param value
	 * @return Fluent API
	 */
	public OAuth2ServerConfig addCredential(String key, String value) {
		this.credentials.put(key, value);
		return this;
	}

	@JsonIgnore
	public JsonObject toJson() {
		JsonObject config = new JsonObject();
		config.put("realm", getRealm());
		config.put("auth-server-url", getAuthServerUrl());
		config.put("ssl-required", getSslRequired());
		config.put("resource", getResource());
		config.put("credentials", getCredentials());
		config.put("confidential-port", getConfidentialPort());
		return config;
	}

}
