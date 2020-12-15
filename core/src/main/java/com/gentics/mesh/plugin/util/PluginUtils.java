package com.gentics.mesh.plugin.util;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.graphql.GraphQLPlugin;

/**
 * Utils for plugins.
 */
public final class PluginUtils {

	public static final String NAME_REGEX = "^[_a-zA-Z][_a-zA-Z0-9]*$";

	private PluginUtils() {
	}

	/**
	 * Validate the manifest.
	 */
	public static void validate(PluginManifest manifest) {
		if (StringUtils.isEmpty(manifest.getId())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "id");
		}
		if (StringUtils.isEmpty(manifest.getName())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "name");
		}
		if (StringUtils.isEmpty(manifest.getVersion())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "version");
		}
		if (StringUtils.isEmpty(manifest.getAuthor())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "author");
		}
		if (StringUtils.isEmpty(manifest.getLicense())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "license");
		}
		if (StringUtils.isEmpty(manifest.getInception())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "inception");
		}
		if (StringUtils.isEmpty(manifest.getDescription())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "description");
		}
		if (StringUtils.isEmpty(manifest.getVersion())) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "version");
		}
	}

	public static void validate(RestPlugin plugin) {
		String apiName = plugin.restApiName();
		if (StringUtils.isEmpty(apiName)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "apiName");
		}
		if (apiName.contains(" ") || apiName.contains("/")) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_apiname_invalid", plugin.getManifest().getId());
		}
	}

	public static void validate(GraphQLPlugin plugin) {
		String name = plugin.gqlApiName();
		if (name != null && !name.matches(NAME_REGEX)) {
			throw error(BAD_REQUEST, "admin_plugin_error_invalid_gql_name", name);
		}
	}

	public static void validate(MeshPlugin plugin) {
		if (plugin instanceof RestPlugin) {
			validate((RestPlugin) plugin);
		}
		if (plugin instanceof GraphQLPlugin) {
			validate((GraphQLPlugin) plugin);
		}
	}

}
