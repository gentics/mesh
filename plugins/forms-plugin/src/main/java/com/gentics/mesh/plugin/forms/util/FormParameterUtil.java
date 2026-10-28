package com.gentics.mesh.plugin.forms.util;

import java.util.Optional;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.export.ExportFormat;
import com.gentics.mesh.plugin.forms.export.Exporter;
import com.gentics.mesh.plugin.forms.service.FormDataService;

/**
 * Utilities for handling of parameters
 */
public final class FormParameterUtil {
	/**
	 * Extract export related options from the request.
	 *
	 * @param context The current request context.
	 * @param configuration The plugin configuration.
	 * @return A string describing the export options for the current request.
	 */
	public static String getExportOptions(PluginContext context, FormsConfig configuration) {
		boolean publishedOnly = Optional.ofNullable(context.queryParams().get(FormDataService.PARAM_PUBLISHED_ONLY))
			.map(Boolean::parseBoolean)
			.orElse(true);
		ExportFormat format = ReflectionUtils.getEnumValue(
			ExportFormat.class,
			context.request().getParam(Exporter.PARAM_FORMAT),
			configuration.getDefaultExportFormat());

		return new StringBuilder("publishedOnly=").append(publishedOnly)
			.append(",format=").append(format.toString())
			.toString();
	}
}
