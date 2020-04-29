package com.gentics.mesh.core.rest.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.StreamUtil.unique;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import io.vertx.core.json.JsonObject;

public final class LanguageOverrideUtil {
	public static final String LANGUAGE_OVERRIDE_KEY = "_meshLanguageOverride";
	public static final Pattern LANGUAGE_SPLIT_PATTERN = Pattern.compile(",");

	private LanguageOverrideUtil() {
	}

	/**
	 * Validates the language override for Elasticsearch settings / mappings.
	 * @param settings
	 */
	public static void validateLanguageOverrides(JsonObject settings) {
		findLanguages(settings)
			.filter(unique().negate())
			.findFirst()
			.ifPresent(duplicateLanguage -> {
				throw error(BAD_REQUEST, "error_language_duplicate_override", duplicateLanguage);
			});
	}

	/**
	 * Finds all overridden languages for Elasticsearch settings / mappings.
	 * Duplicate languages will not be filtered out.
	 * @param settings
	 * @return
	 */
	public static Stream<String> findLanguages(JsonObject settings) {
		return Optional.ofNullable(settings)
			.map(json -> json.getJsonObject(LANGUAGE_OVERRIDE_KEY))
			.map(Stream::of)
			.orElseGet(Stream::empty)
			.flatMap(languageOverrides -> languageOverrides.fieldNames().stream())
			.flatMap(LANGUAGE_SPLIT_PATTERN::splitAsStream)
			.map(String::trim)
			.filter(StringUtils::isNotEmpty);
	}
}
