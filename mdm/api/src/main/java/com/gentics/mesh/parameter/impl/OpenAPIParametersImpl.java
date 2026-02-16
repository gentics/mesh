package com.gentics.mesh.parameter.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.core.rest.openapi.Format;
import com.gentics.mesh.core.rest.openapi.Version;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.OpenAPIParameters;

/**
 * Parameters implementation of {@link OpenAPIParameters}
 */
public class OpenAPIParametersImpl extends AbstractParameters implements OpenAPIParameters {

	/**
	 * Create instance on given action context
	 * @param ac action context
	 */
	public OpenAPIParametersImpl(ActionContext ac) {
		super(ac);
	}

	/**
	 * Create empty instance
	 */
	public OpenAPIParametersImpl() {
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// version
		QueryParameter versionParameter = new QueryParameter();
		versionParameter.setDefaultValue(Version.V30.name());
		versionParameter.setDescription("Specify, whether the OpenAPI standard version 3.1 should be generated. If false or unset, a version 3.0 of standard will be used.");
		versionParameter.setExample(Version.V31.name());
		versionParameter.setRequired(false);
		versionParameter.setEnumeration(Arrays.asList(Version.values()).stream().map(e -> e.toString().toLowerCase()).collect(Collectors.toList()));
		versionParameter.setType(ParamType.STRING);
		parameters.put(VERSION_PARAMETER_KEY, versionParameter);

		// format
		QueryParameter formatParameter = new QueryParameter();
		formatParameter.setDefaultValue(Format.JSON.name());
		formatParameter.setDescription("Specify the output format. Default is JSON.");
		formatParameter.setExample(Format.YAML.name());
		formatParameter.setRequired(false);
		formatParameter.setEnumeration(Arrays.asList(Format.values()).stream().map(e -> e.toString().toLowerCase()).collect(Collectors.toList()));
		formatParameter.setType(ParamType.STRING);
		parameters.put(FORMAT_PARAMETER_KEY, formatParameter);
				
		return parameters;
	}

	@Override
	public String getName() {
		return "OpenAPI request parameters";
	}
}
