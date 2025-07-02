package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.VersioningParameters;

public class VersioningParametersImpl extends BranchParametersImpl implements VersioningParameters {

	public VersioningParametersImpl(ActionContext ac) {
		super(ac);
	}

	public VersioningParametersImpl() {
	}

	@Override
	public void validate() {
		// TODO validate version pattern
	}

	@Override
	public String getName() {
		return "Versioning parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// version
		QueryParameter versionParameter = new QueryParameter();
		versionParameter.setDefaultValue("draft");
		versionParameter.setDescription(
				"Specifies the version to be loaded. Can either be published/draft or version number. e.g.: _0.1_, _1.0_, _draft_, _published_.");
		versionParameter.setExample("1.1");
		versionParameter.setRequired(false);
		versionParameter.setType(ParamType.STRING);
		parameters.put(VERSION_QUERY_PARAM_KEY, versionParameter);
		return parameters;
	}

}
