package com.gentics.mesh.parameter.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.handler.ActionContext;

public class NodeParameters extends AbstractParameters {

	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";

	public static final String EXPANDFIELDS_QUERY_PARAM_KEY = "expand";

	public static final String EXPANDALL_QUERY_PARAM_KEY = "expandAll";

	public static final String RESOLVE_LINKS_QUERY_PARAM_KEY = "resolveLinks";

	private String[] languages;
	private String[] expandedFieldNames;
	private Boolean expandAll;
	private LinkType resolveLinks;

	public NodeParameters(ActionContext ac) {
		super(ac);
	}

	public NodeParameters() {
	}

	@Override
	protected void constructFrom(ActionContext ac) {
		String value = ac.getParameter(EXPANDALL_QUERY_PARAM_KEY);
		if (value != null) {
			expandAll = Boolean.valueOf(value);
		}

		value = ac.getParameter(LANGUAGES_QUERY_PARAM_KEY);
		if (value != null) {
			languages = value.split(",");
		}
		if (languages == null) {
			languages = new String[] { Mesh.mesh().getOptions().getDefaultLanguage() };
		}

		resolveLinks = LinkType.OFF;

		value = ac.getParameter(RESOLVE_LINKS_QUERY_PARAM_KEY);
		if (value != null) {
			resolveLinks = LinkType.valueOf(value.toUpperCase());
		}

		// List<String> fieldList = (List<String>) data().get(EXPANDED_FIELDNAMED_DATA_KEY);
		// if (fieldList == null) {
		// fieldList = new ArrayList<>();
		// Map<String, String> queryPairs = splitQuery();
		// if (queryPairs != null) {
		// String value = queryPairs.get(EXPANDFIELDS_QUERY_PARAM_KEY);
		// if (value != null) {
		// fieldList.addAll(Arrays.asList(value.split(",")));
		// }
		// }
		// data().put(EXPANDED_FIELDNAMED_DATA_KEY, fieldList);
		// }
		//
		// // check whether given language tags exist
		// Database db = MeshSpringConfiguration.getInstance().database();
		// try (NoTrx noTrx = db.noTrx()) {
		// for (String languageTag : languageTags) {
		// if (languageTag != null) {
		// Iterator<Vertex> it = db.getVertices(LanguageImpl.class, new String[] { LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY },
		// new Object[] { languageTag });
		// if (!it.hasNext()) {
		// throw error(BAD_REQUEST, "error_language_not_found", languageTag);
		// }
		// }
		// }
		// }

	}

	/**
	 * Set the <code>lang</code> request parameter values.
	 * 
	 * @param languages
	 * @return Fluent API
	 */
	public NodeParameters setLanguages(String... languages) {
		this.languages = languages;
		return this;
	}

	public String[] getLanguages() {
		return languages;
	}

	public List<String> getLanguageList() {
		return Arrays.asList(languages);
	}

	/**
	 * Set a list of field names which should be expanded.
	 * 
	 * @param fieldNames
	 * @return
	 */
	public NodeParameters setExpandedFieldNames(String... fieldNames) {
		this.expandedFieldNames = fieldNames;
		return this;
	}

	public String[] getExpandedFieldNames() {
		return expandedFieldNames;
	}

	public List<String> getExpandedFieldnames() {
		if (expandedFieldNames != null) {
			return Arrays.asList(expandedFieldNames);
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Set the <code>expandAll</code> request parameter flag.
	 * 
	 * @param flag
	 * @return
	 */
	public NodeParameters setExpandAll(boolean flag) {
		this.expandAll = flag;
		return this;
	}

	/**
	 * Return the <code>expandAll</code> query parameter flag value.
	 * 
	 * @return
	 */
	public boolean getExpandAll() {
		if (this.expandAll == null) {
			return false;
		}
		return this.expandAll;
	}

	public LinkType getResolveLinks() {
		return resolveLinks;
	}

	/**
	 * Set the <code>resolveLinks</code> request parameter.
	 * 
	 * @param type
	 */
	public NodeParameters setResolveLinks(LinkType type) {
		this.resolveLinks = type;
		return this;
	}

	@Override
	protected Map<String, Object> getParameters() {
		Map<String, Object> map = new HashMap<>();
		map.put(LANGUAGES_QUERY_PARAM_KEY, languages);
		map.put(EXPANDALL_QUERY_PARAM_KEY, expandAll);
		map.put(EXPANDFIELDS_QUERY_PARAM_KEY, expandedFieldNames);
		map.put(RESOLVE_LINKS_QUERY_PARAM_KEY, resolveLinks);
		return map;
	}

}
