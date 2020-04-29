package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.rest.schema.LanguageOverrideUtil.LANGUAGE_OVERRIDE_KEY;
import static com.gentics.mesh.core.rest.schema.LanguageOverrideUtil.LANGUAGE_SPLIT_PATTERN;

import java.util.Optional;

import io.vertx.core.json.JsonObject;

public final class NodeIndexUtil {

	private NodeIndexUtil() {
	}

	/**
	 * Gets the language specific Elasticsearch settings / mappings.
	 * If the language was not found, the default setting / mapping is returned.
	 * If no default setting / mapping is set, null is returned.
	 * @param language
	 * @return
	 */
	public static JsonObject getLanguageOverride(JsonObject settings, String language) {
		if (settings == null) {
			return null;
		}
		if (language == null) {
			return getDefaultSetting(settings);
		}
		return Optional.ofNullable(settings.getJsonObject(LANGUAGE_OVERRIDE_KEY))
			.map(languages -> {
				for (String fieldName : languages.fieldNames()) {
					boolean matches = LANGUAGE_SPLIT_PATTERN.splitAsStream(fieldName)
						.anyMatch(l -> l.equals(language));
					if (matches) {
						return languages.getJsonObject(fieldName);
					}
				}
				return null;
			})
			.orElseGet(() -> getDefaultSetting(settings));
	}

	/**
	 * Gets the default setting / mapping or null if it is empty.
	 * @param settings
	 * @return
	 */
	public static JsonObject getDefaultSetting(JsonObject settings) {
		if (settings == null) {
			return null;
		}
		settings = settings.copy();
		settings.remove(LANGUAGE_OVERRIDE_KEY);
		return settings.size() > 0
			? settings
			: null;
	}
}
