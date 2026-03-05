package com.gentics.mesh.plugin.common.util;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Single;

/**
 * Utilities for handling of parameters
 */
public final class ParameterUtil {
	/**
	 * Pattern for languages
	 */
	public final static Pattern LANGUAGE_PATTERN = Pattern.compile("\\w+", Pattern.CASE_INSENSITIVE);

	/**
	 * Get the languages contained in the request as query parameter "lang". If the
	 * query parameter is not found, an empty array will be returned
	 *
	 * @param context plugin context
	 * @return string array of languages
	 */
	public static Single<String[]> getLanguages(PluginContext context) {
		List<String> languageList = context.queryParam("lang").stream().map(StringUtils::trimToEmpty)
				.collect(Collectors.toList());

		if (!CollectionUtils.isEmpty(languageList) && languageList.stream()
				.filter(lang -> !LANGUAGE_PATTERN.matcher(lang).matches()).findAny().isPresent()) {
			return Single.error(illegalPathParameter("lang"));
		}

		String[] languages = null;
		if (languageList != null) {
			languages = languageList.toArray(new String[languageList.size()]);
		} else {
			languages = new String[0];
		}

		return Single.just(languages);
	}

	public static Single<String> getFirstLanguage(PluginContext context, String def) {
		return getLanguages(context).map(languages -> {
			if (languages.length > 0) {
				return languages[0];
			} else {
				return def;
			}
		});
	}

	/**
	 * Transform the given value into an integer, if possible. If this is not possible, return the default value
	 * @param value value to transform
	 * @param defaultValue default value
	 * @return transformed or default value
	 */
	public static int asInt(Object value, int defaultValue) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else if (value != null) {
			try {
				return Integer.parseInt(value.toString());
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * Transform the given value into a boolean. Return the default value, if the value is null.
	 * @param value value
	 * @param defaultValue default value
	 * @return transformed or default value
	 */
	public static boolean asBoolean(Object value, boolean defaultValue) {
		if (value instanceof Boolean) {
			return ((Boolean)value).booleanValue();
		} else if (value != null) {
			return Boolean.parseBoolean(value.toString());
		} else {
			return defaultValue;
		}
	}

	/**
	 * Get the uuid from the request path with the given path parameter name.
	 * When the value is not a uuid, the returned Single will emit a MeshPluginException (Bad Request)
	 * @param context plugin context
	 * @param pathParamName name of the path parameter
	 * @return single either emiting a valid uuid or an error
	 */
	public static Single<String> getUuidFromPath(PluginContext context, String pathParamName) {
		String uuid = StringUtils.trimToEmpty(context.pathParam(pathParamName));

		if (!UUIDUtil.isUUID(uuid)) {
			return Single.error(illegalPathParameter(pathParamName));
		} else {
			return Single.just(uuid);
		}
	}

	/**
	 * Get the string from the request path with the given path parameter name.
	 * The value will be trimmed to empty and matched with the given pattern before it is returned.
	 * If the value does not match the pattern, the single will emit a MeshPluginException (Bad Request)
	 * @param context plugin context
	 * @param pathParamName name of the path parameter
	 * @return single containing the trimmed value
	 */
	public static Single<String> getFromPath(PluginContext context, String pathParamName, Pattern pattern) {
		String value = StringUtils.trimToEmpty(context.pathParam(pathParamName));

		if (!pattern.matcher(value).matches()) {
			return Single.error(illegalPathParameter(pathParamName));
		} else {
			return Single.just(value);
		}
	}

	/**
	 * Get the long value from the request path with the given path parameter name.
	 * If the path parameter cannot be converted into a long, the single will emit a MeshPluginException (Bad Request)
	 * @param context plugin context
	 * @param pathParamName name of the path parameter
	 * @return single either emiting the long value or an error
	 */
	public static Single<Long> getLongFromPath(PluginContext context, String pathParamName) {
		String value = StringUtils.trimToEmpty(context.pathParam(pathParamName));

		try {
			return Single.just(Long.valueOf(value));
		} catch (NumberFormatException e) {
			return Single.error(illegalPathParameter(pathParamName));
		}
	}

	/**
	 * Create an instance of {@link MeshPluginException} with error code 400 (Bad Request) for
	 * illegal path parameter with given name
	 * @param pathParamName path parameter name
	 * @return exception
	 */
	protected static MeshPluginException illegalPathParameter(String pathParamName) {
		return new MeshPluginException("Illegal path parameter for %s".formatted(pathParamName), 400);
	}
}
