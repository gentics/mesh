package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.NodeParameters;

public class NodeParametersImpl extends AbstractParameters implements NodeParameters {

	public NodeParametersImpl(ActionContext ac) {
		super(ac);
	}

	public NodeParametersImpl() {
		super();
	}

	@Override
	public void validate() {

	}

	@Override
	public String getName() {
		return "Node parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// lang
		QueryParameter langParameter = new QueryParameter();
		langParameter.setDescription(
			"ISO 639-1 language tag of the language which should be loaded. Fallback handling can be applied by specifying multiple languages in a comma-separated list. The first matching language will be returned.  If omitted or the requested language is not available then the _defaultLanguage_ as configured in _mesh.yml_ will be returned.");
		langParameter.setExample("en,de");
		langParameter.setRequired(false);
		langParameter.setType(ParamType.STRING);
		parameters.put(LANGUAGES_QUERY_PARAM_KEY, langParameter);

		// resolveLinks
		QueryParameter resolveLinksParameter = new QueryParameter();
		resolveLinksParameter.setDescription(
			"The resolve links parameter can be set to either _short_, _medium_ or _full_. Stored mesh links will automatically be resolved and replaced by the resolved webroot link. With the parameter set the _path_ property as well as the _languagesPath_ property (for available language variants) will be included in the response. Gentics Mesh links in any HTML-typed field will automatically be resolved and replaced by the resolved WebRoot path. No resolving occurs if no link has been specified.");
		resolveLinksParameter.setExample("medium");
		resolveLinksParameter.setRequired(false);
		resolveLinksParameter.setType(ParamType.STRING);
		parameters.put(RESOLVE_LINKS_QUERY_PARAM_KEY, resolveLinksParameter);

		return parameters;
	}

}
