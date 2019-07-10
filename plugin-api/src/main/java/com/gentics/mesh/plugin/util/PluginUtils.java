package com.gentics.mesh.plugin.util;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.MeshPluginDescriptor;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.impl.MeshPluginDescriptorImpl;

public final class PluginUtils {

	private PluginUtils() {
	}

	/**
	 * Validate the manifest.
	 * 
	 * @return
	 */
	public static void validate(PluginManifest manifest, boolean strict) {
		// if (StringUtils.isEmpty(manifest.getPluginId())) {
		// throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "pluginId");
		// }
		if (strict) {
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
	}

	public static void validate(RestPlugin plugin) {
		String apiName = plugin.apiName();
		if (StringUtils.isEmpty(apiName)) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "apiName");
		}
		if (apiName.contains(" ") | apiName.contains("/")) {
			throw error(BAD_REQUEST, "admin_plugin_error_validation_failed_apiname_invalid", apiName);
		}
	}

	public static void validate(MeshPlugin plugin) {
		if (plugin instanceof RestPlugin) {
			validate((RestPlugin) plugin);
		}

	}

}
