package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.util.VersionNumber;

//TODO split this class into two since some actions are not versioning specific but release specific 
public class VersioningParametersImpl extends AbstractParameters implements VersioningParameters {

	public VersioningParametersImpl(ActionContext ac) {
		super(ac);
	}

	public VersioningParametersImpl() {
	}

	@Override
	public String getVersion() {
		String version = "draft";
		String versionParameter = getParameter(VERSION_QUERY_PARAM_KEY);
		if (versionParameter != null) {
			if ("draft".equalsIgnoreCase(versionParameter) || "published".equalsIgnoreCase(versionParameter)) {
				version = versionParameter;
			} else {
				try {
					version = new VersionNumber(versionParameter).toString();
				} catch (IllegalArgumentException e) {
					throw error(BAD_REQUEST, "error_illegal_version", versionParameter);
				}
			}
		}
		return version;
	}

	@Override
	public VersioningParameters setVersion(String version) {
		setParameter(VERSION_QUERY_PARAM_KEY, version);
		return this;
	}

	@Override
	public VersioningParameters draft() {
		return setVersion("draft");
	}

	@Override
	public VersioningParameters published() {
		return setVersion("published");
	}

	@Override
	public String getRelease() {
		return getParameter(RELEASE_QUERY_PARAM_KEY);
	}

	@Override
	public void validate() {
		// TODO validate version pattern and release parameter value
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// release
		QueryParameter pageParameter = new QueryParameter();
		pageParameter.setDescription(
				"Specifies the release to be used for loading data. The latest project release will be used if this parameter is omitted.");
		pageParameter.setExample("24cf92691c7641158f92691c76c115ef");
		pageParameter.setRequired(false);
		pageParameter.setType(ParamType.STRING);
		parameters.put(RELEASE_QUERY_PARAM_KEY, pageParameter);

		// version
		QueryParameter versionParameter = new QueryParameter();
		versionParameter.setDefaultValue("draft");
		versionParameter.setDescription("Specifies the version to be loaded. Can either be published/draft or version number.");
		versionParameter.setExample("1.1");
		versionParameter.setRequired(false);
		versionParameter.setType(ParamType.STRING);
		parameters.put(VERSION_QUERY_PARAM_KEY, versionParameter);
		return parameters;
	}

}
