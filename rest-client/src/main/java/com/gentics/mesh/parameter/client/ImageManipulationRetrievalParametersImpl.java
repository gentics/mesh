package com.gentics.mesh.parameter.client;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.parameter.ImageManipulationRetrievalParameters;

/**
 * @see ImageManipulationRetrievalParameters
 * 
 * @author plyhun
 *
 */
public class ImageManipulationRetrievalParametersImpl extends AbstractParameters implements ImageManipulationRetrievalParameters {

	public ImageManipulationRetrievalParametersImpl() {
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
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
}
