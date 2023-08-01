package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.ImageManipulationRetrievalParameters;

/**
 * @see ImageManipulationRetrievalParameters
 * 
 * @author plyhun
 *
 */
public class ImageManipulationRetrievalParametersImpl extends AbstractParameters implements ImageManipulationRetrievalParameters {

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter pageParameter = new QueryParameter();
		pageParameter.setDescription(
				"Specifies a need to retrieve filesizes for the returned image manipulation variants.");
		pageParameter.setExample("true");
		pageParameter.setRequired(false);
		pageParameter.setType(ParamType.BOOLEAN);
		parameters.put(FILESIZE_QUERY_PARAM_KEY, pageParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Image manipulation retrieval parameters.";
	}
}
