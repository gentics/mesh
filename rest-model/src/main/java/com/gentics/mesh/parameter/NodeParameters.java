package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.etc.config.AbstractMeshOptions;

/**
 * Interface for node query parameters.
 */
public interface NodeParameters extends ParameterProvider {

	/**
	 * Query parameter key: {@value #LANGUAGES_QUERY_PARAM_KEY}
	 */
	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";

	/**
	 * Query parameter key: {@value #EXPANDFIELDS_QUERY_PARAM_KEY}
	 */
	public static final String EXPANDFIELDS_QUERY_PARAM_KEY = "expand";

	/**
	 * Query parameter key: {@value #EXPANDALL_QUERY_PARAM_KEY}
	 */
	public static final String EXPANDALL_QUERY_PARAM_KEY = "expandAll";

	/**
	 * Query parameter key: {@value #RESOLVE_LINKS_QUERY_PARAM_KEY}
	 */
	public static final String RESOLVE_LINKS_QUERY_PARAM_KEY = "resolveLinks";

	/**
	 * Set the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @param languageTags
	 * @return Fluent API
	 */
	default NodeParameters setLanguages(String... languageTags) {
		setParameter(LANGUAGES_QUERY_PARAM_KEY, convertToStr(languageTags));
		return this;
	}

	/**
	 * Return the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @return
	 */
	default String[] getLanguages() {
		String value = getParameter(LANGUAGES_QUERY_PARAM_KEY);
		String[] languages = null;
		if (value != null) {
			languages = value.split(",");
		}
		return languages;
	}

	/**
	 * Set the <code>{@value #RESOLVE_LINKS_QUERY_PARAM_KEY}</code> request parameter.
	 * 
	 * @param type
	 * @return Fluent API
	 */
	default NodeParameters setResolveLinks(LinkType type) {
		setParameter(RESOLVE_LINKS_QUERY_PARAM_KEY, type.name().toLowerCase());
		return this;
	}

	/**
	 * Return the <code>{@value #RESOLVE_LINKS_QUERY_PARAM_KEY}</code> query parameter flag value.
	 * 
	 * @return
	 */
	default LinkType getResolveLinks() {
		String value = getParameter(RESOLVE_LINKS_QUERY_PARAM_KEY);
		if (value != null) {
			return LinkType.valueOf(value.toUpperCase());
		}
		return LinkType.OFF;
	}

	/**
	 * Set the <code>{@value #EXPANDALL_QUERY_PARAM_KEY}</code> request parameter flag.
	 * 
	 * @param flag
	 * @return
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	default NodeParameters setExpandAll(boolean flag) {
		setParameter(EXPANDALL_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Return the <code>{@value #EXPANDALL_QUERY_PARAM_KEY}</code> query parameter flag value.
	 * 
	 * @return
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	default boolean getExpandAll() {
		String value = getParameter(EXPANDALL_QUERY_PARAM_KEY);
		if (value != null) {
			return Boolean.valueOf(value);
		}
		return false;
	}

	/**
	 * Set a list of field names which should be expanded.
	 * 
	 * @param fieldNames
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	default NodeParameters setExpandedFieldNames(String... fieldNames) {
		setParameter(EXPANDFIELDS_QUERY_PARAM_KEY, convertToStr(fieldNames));
		return this;
	}

	/**
	 * Return the field names which should be expanded.
	 * 
	 * @return
	 * @deprecated This feature will be removed in a future mesh version due to graphql support
	 */
	@Deprecated
	default String[] getExpandedFieldNames() {
		String fieldNames = getParameter(EXPANDFIELDS_QUERY_PARAM_KEY);
		if (fieldNames != null) {
			return fieldNames.split(",");
		} else {
			return new String[0];
		}
	}

	/**
	 * @see #getLanguages()
	 * @param options
	 *            Mesh options which contains the default language information
	 * @return
	 */
	default List<String> getLanguageList(AbstractMeshOptions options) {
		String[] langs = getLanguages();
		if (langs == null) {
			return Arrays.asList(options.getDefaultLanguage());
		}
		return Arrays.asList(langs);
	}

	/**
	 * @see #getExpandedFieldNames()
	 * @return
	 */
	@Deprecated
	default List<String> getExpandedFieldnameList() {
		return Arrays.asList(getExpandedFieldNames());
	}
}
