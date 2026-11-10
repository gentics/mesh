package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.vertx.openapi.model.ParamType;
import com.gentics.vertx.openapi.model.parameters.QueryParameter;

import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.ImageManipulationRetrievalParameters;
import com.gentics.mesh.parameter.ParameterProviderContext;

/**
 * @see ImageManipulationRetrievalParameters
 * 
 * @author plyhun
 *
 */
public class ImageManipulationRetrievalParametersImpl extends AbstractParameters implements ImageManipulationRetrievalParameters {

	public ImageManipulationRetrievalParametersImpl() {
	}

	public ImageManipulationRetrievalParametersImpl(ParameterProviderContext parameterProviderContext) {
		super(parameterProviderContext);
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getParameterDefinitions() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter filesizeParameter = new QueryParameter();
		filesizeParameter.setDescription(
				"Specifies a need to retrieve filesizes for the returned image manipulation variants.");
		filesizeParameter.setExample("true");
		filesizeParameter.setRequired(false);
		filesizeParameter.setType(ParamType.BOOLEAN);

		QueryParameter originalParameter = new QueryParameter();
		originalParameter.setDescription(
				"Specifies a need to retrieve original image along with its manipulation variants.");
		originalParameter.setExample("true");
		originalParameter.setRequired(false);
		originalParameter.setType(ParamType.BOOLEAN);

		parameters.put(FILESIZE_QUERY_PARAM_KEY, filesizeParameter);
		parameters.put(ORIGINAL_QUERY_PARAM_KEY, originalParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Image manipulation retrieval parameters.";
	}
}
