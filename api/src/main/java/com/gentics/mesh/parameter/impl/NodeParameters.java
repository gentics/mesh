package com.gentics.mesh.parameter.impl;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.handler.ActionContext;

public class NodeParameters extends AbstractParameters {

	public static final String LANGUAGES_QUERY_PARAM_KEY = "lang";

	public static final String EXPANDFIELDS_QUERY_PARAM_KEY = "expand";

	public static final String EXPANDALL_QUERY_PARAM_KEY = "expandAll";

	public static final String RESOLVE_LINKS_QUERY_PARAM_KEY = "resolveLinks";

	public NodeParameters(ActionContext ac) {
		super(ac);
	}

	public NodeParameters() {
		super();
	}

	//	@Override
	protected void constructFrom() {

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
		setParameter(LANGUAGES_QUERY_PARAM_KEY, convertToStr(languages));
		return this;
	}

	public String[] getLanguages() {
		String value = getParameter(LANGUAGES_QUERY_PARAM_KEY);
		String[] languages = null;
		if (value != null) {
			languages = value.split(",");
		}
		if (languages == null) {
			languages = new String[] { Mesh.mesh().getOptions().getDefaultLanguage() };
		}
		return languages;
	}

	public List<String> getLanguageList() {
		return Arrays.asList(getLanguages());
	}

	/**
	 * Set a list of field names which should be expanded.
	 * 
	 * @param fieldNames
	 * @return
	 */
	public NodeParameters setExpandedFieldNames(String... fieldNames) {
		setParameter(EXPANDFIELDS_QUERY_PARAM_KEY, convertToStr(fieldNames));
		return this;
	}

	public String[] getExpandedFieldNames() {
		String fieldNames = getParameter(EXPANDFIELDS_QUERY_PARAM_KEY);
		if (fieldNames != null) {
			return fieldNames.split(",");
		} else {
			return new String[0];
		}
	}

	public List<String> getExpandedFieldnameList() {
		return Arrays.asList(getExpandedFieldNames());
	}

	/**
	 * Set the <code>expandAll</code> request parameter flag.
	 * 
	 * @param flag
	 * @return
	 */
	public NodeParameters setExpandAll(boolean flag) {
		setParameter(EXPANDALL_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Return the <code>expandAll</code> query parameter flag value.
	 * 
	 * @return
	 */
	public boolean getExpandAll() {
		String value = getParameter(EXPANDALL_QUERY_PARAM_KEY);
		if (value != null) {
			return Boolean.valueOf(value);
		}
		return false;
	}

	public LinkType getResolveLinks() {
		String value = getParameter(RESOLVE_LINKS_QUERY_PARAM_KEY);
		if (value != null) {
			return LinkType.valueOf(value.toUpperCase());
		}
		return LinkType.OFF;
	}

	/**
	 * Set the <code>resolveLinks</code> request parameter.
	 * 
	 * @param type
	 */
	public NodeParameters setResolveLinks(LinkType type) {
		setParameter(RESOLVE_LINKS_QUERY_PARAM_KEY, type.name().toLowerCase());
		return this;
	}

}
