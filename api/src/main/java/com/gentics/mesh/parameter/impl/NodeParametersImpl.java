package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NodeParameters;

public class NodeParametersImpl extends AbstractParameters implements NodeParameters {

	public NodeParametersImpl(ActionContext ac) {
		super(ac);
	}

	public NodeParametersImpl() {
		super();
	}

	/**
	 * Set the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @param languages
	 * @return Fluent API
	 */
	public NodeParametersImpl setLanguages(String... languages) {
		setParameter(LANGUAGES_QUERY_PARAM_KEY, convertToStr(languages));
		return this;
	}

	/**
	 * Return the <code>{@value #LANGUAGES_QUERY_PARAM_KEY}</code> request parameter values.
	 * 
	 * @return
	 */
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

	/**
	 * @see #getLanguages()
	 * @return
	 */
	public List<String> getLanguageList() {
		return Arrays.asList(getLanguages());
	}

	/**
	 * Set a list of field names which should be expanded.
	 * 
	 * @param fieldNames
	 * @return
	 */
	public NodeParametersImpl setExpandedFieldNames(String... fieldNames) {
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

	/**
	 * @see #getExpandedFieldNames()
	 * @return
	 */
	public List<String> getExpandedFieldnameList() {
		return Arrays.asList(getExpandedFieldNames());
	}

	/**
	 * Set the <code>{@value #EXPANDALL_QUERY_PARAM_KEY}</code> request parameter flag.
	 * 
	 * @param flag
	 * @return
	 */
	public NodeParametersImpl setExpandAll(boolean flag) {
		setParameter(EXPANDALL_QUERY_PARAM_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Return the <code>{@value #EXPANDALL_QUERY_PARAM_KEY}</code> query parameter flag value.
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

	/**
	 * Return the <code>{@value #RESOLVE_LINKS_QUERY_PARAM_KEY}</code> query parameter flag value.
	 * 
	 * @return
	 */
	public LinkType getResolveLinks() {
		String value = getParameter(RESOLVE_LINKS_QUERY_PARAM_KEY);
		if (value != null) {
			return LinkType.valueOf(value.toUpperCase());
		}
		return LinkType.OFF;
	}

	/**
	 * Set the <code>{@value #RESOLVE_LINKS_QUERY_PARAM_KEY}</code> request parameter.
	 * 
	 * @param type
	 * @return Fluent API
	 */
	public NodeParametersImpl setResolveLinks(LinkType type) {
		setParameter(RESOLVE_LINKS_QUERY_PARAM_KEY, type.name().toLowerCase());
		return this;
	}

	@Override
	public void validate() {
		// Check whether all given language tags exists
		for (String languageTag : getLanguages()) {
			Iterator<?> it = Database.getThreadLocalGraph().getVertices("LanguageImpl.languageTag", languageTag).iterator();
			if (!it.hasNext()) {
				throw error(BAD_REQUEST, "error_language_not_found", languageTag);
			}
		}
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// lang
		QueryParameter langParameter = new QueryParameter();
		langParameter.setDescription(
				"Name of the language which should be loaded. Fallback handling can be applied by specifying multiple languages. The first matching language will be returned.");
		langParameter.setExample("en,de");
		langParameter.setRequired(false);
		langParameter.setType(ParamType.STRING);
		parameters.put(LANGUAGES_QUERY_PARAM_KEY, langParameter);

		// expand
		QueryParameter expandParameter = new QueryParameter();
		expandParameter.setDescription(
				"Specifies the name of fields which should be expanded. By default node fields are not expanded and just contain the node reference information. These fields fields can be expanded such that node fields are part of the response. Please note that expanding nodes stops at a certain depth in order to prevent endless recursions.");
		expandParameter.setExample("nodeField, nodeList");
		expandParameter.setRequired(false);
		expandParameter.setType(ParamType.STRING);
		parameters.put(EXPANDFIELDS_QUERY_PARAM_KEY, expandParameter);

		// expandAll
		QueryParameter expandAllParameter = new QueryParameter();
		expandAllParameter.setDescription(
				"All fields that can be expanded will be expanded when setting the parameter to true. Please note that expanding nodes stops at a certain depth in order to prevent endless recursions.");
		expandAllParameter.setExample("true");
		expandAllParameter.setRequired(false);
		expandAllParameter.setType(ParamType.BOOLEAN);
		parameters.put(EXPANDALL_QUERY_PARAM_KEY, expandAllParameter);

		// resolveLinks
		QueryParameter resolveLinksParameter = new QueryParameter();
		resolveLinksParameter.setDescription(
				"The resolve links parameter can be set to either short, medium or full. Stored mesh links will automatically be resolved and replaced by the resolved webroot link. No resolving occures if no link has been specified.");
		resolveLinksParameter.setExample("medium");
		resolveLinksParameter.setRequired(false);
		resolveLinksParameter.setType(ParamType.STRING);
		parameters.put(RESOLVE_LINKS_QUERY_PARAM_KEY, resolveLinksParameter);

		return parameters;
	}

}
