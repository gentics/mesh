package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;

import com.gentics.mesh.plugin.common.license.LicenseConfiguration;
import java.util.ArrayList;
import java.util.List;

public class PluginConfiguration extends LicenseConfiguration {

	public static final Long DEFAULT_CACHE_TIME = 600_000L;

	@JsonProperty()
	@JsonPropertyDescription("The webroot paths of the configuration nodes")
	private List<WebrootPathConfiguration> webrootPaths = new ArrayList<>();

	@JsonProperty()
	@JsonPropertyDescription("The cache time (in milliseconds) for the config nodes. Defaults to: `600.000`")
	@EnvironmentVariable(name = "MESH_PLUGIN_EXPORT_CACHE_TIME", description = "The cache time (in milliseconds) for the config nodes. Defaults to: `600.000`")
	private Long cacheTime = DEFAULT_CACHE_TIME;

	@JsonProperty()
	@JsonPropertyDescription("If it should check and create the role on startup")
	@EnvironmentVariable(name = "MESH_PLUGIN_EXPORT_ROLE_CHECK", description = "If it should check and create the role on startup")
	private boolean roleCheck = false;

	@JsonProperty()
	@JsonPropertyDescription("The role-name used for permitting exporting of data. Defaults to 'plugin_export'")
	@EnvironmentVariable(name = "MESH_PLUGIN_EXPORT_ROLE_NAME", description = "The role-name used for permitting exporting of data. Defaults to 'plugin_export'")
	private String roleName = "plugin_export";

	public PluginConfiguration() {}

	public List<WebrootPathConfiguration> getWebrootPaths() {
		return webrootPaths;
	}

	public void setWebrootPaths(List<WebrootPathConfiguration> webrootPaths) {
		this.webrootPaths = webrootPaths;
	}

	public Long getCacheTime() {
		return cacheTime;
	}

	public void setCacheTime(Long cacheTime) {
		this.cacheTime = cacheTime;
	}

	public boolean isRoleCheck() {
		return roleCheck;
	}

	public PluginConfiguration setRoleCheck(boolean roleCheck) {
		this.roleCheck = roleCheck;
		return this;
	}

	public String getRoleName() {
		return roleName;
	}

	public PluginConfiguration setRoleName(String roleName) {
		this.roleName = roleName;
		return this;
	}
}

