package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.ImageManipulationParameters;

/**
 * Crop and resize parameters for image manipulation.
 */
public class ImageManipulationParametersImpl extends AbstractParameters implements ImageManipulationParameters {

	public ImageManipulationParametersImpl(ActionContext ac) {
		super(ac);
		// TODO validate parameters
	}

	public ImageManipulationParametersImpl() {
	}

	@Override
	public String getName() {
		return "Image manipulation parameters";
	}

	/**
	 * * Validate the set parameters and throw an exception when an invalid set of parameters has been detected.
	 */
	@Override
	public void validate() {
		Integer width = getWidth();
		if (width != null && width < 1) {
			throw error(BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParametersImpl.WIDTH_QUERY_PARAM_KEY, String.valueOf(width));
		}
		Integer height = getHeight();
		if (height != null && height < 1) {
			throw error(BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParametersImpl.HEIGHT_QUERY_PARAM_KEY,
					String.valueOf(height));
		}

		Integer croph = getCroph();
		Integer cropw = getCropw();
		Integer startx = getStartx();
		Integer starty = getStarty();
		// Check whether all required crop parameters have been set when at least one crop parameter has been set.
		boolean hasOneCropParameter = croph != null || cropw != null || startx != null || starty != null;
		if (hasOneCropParameter) {
			// Check whether all required crop parameters have been set.
			if (!hasAllCropParameters()) {
				throw error(BAD_REQUEST, "image_error_incomplete_crop_parameters");
			}

			if (croph != null && croph <= 0) {
				throw error(BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParametersImpl.CROP_HEIGHT_QUERY_PARAM_KEY,
						String.valueOf(croph));
			}

			if (cropw != null && cropw <= 0) {
				throw error(BAD_REQUEST, "image_error_parameter_positive", ImageManipulationParametersImpl.CROP_WIDTH_QUERY_PARAM_KEY,
						String.valueOf(cropw));
			}

			if (startx != null && startx <= -1) {
				throw error(BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParametersImpl.CROP_X_QUERY_PARAM_KEY,
						String.valueOf(startx));
			}

			if (starty != null && starty <= -1) {
				throw error(BAD_REQUEST, "image_error_crop_start_not_negative", ImageManipulationParametersImpl.CROP_Y_QUERY_PARAM_KEY,
						String.valueOf(starty));
			}

		}

	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// width
		QueryParameter widthParameter = new QueryParameter();
		widthParameter.setDescription("Set image target width. The height will automatically be calculated if the width was omitted.");
		widthParameter.setExample("1280");
		widthParameter.setRequired(false);
		widthParameter.setType(ParamType.NUMBER);
		parameters.put(WIDTH_QUERY_PARAM_KEY, widthParameter);

		// height
		QueryParameter heightParameter = new QueryParameter();
		heightParameter.setDescription("Set image target height. The width will automatically be calculated if the height was omitted.");
		heightParameter.setExample("720");
		heightParameter.setRequired(false);
		heightParameter.setType(ParamType.NUMBER);
		parameters.put(HEIGHT_QUERY_PARAM_KEY, heightParameter);

		// cropx
		QueryParameter cropxParameter = new QueryParameter();
		cropxParameter.setDescription("Set image crop area start x coordinate.");
		cropxParameter.setExample("260");
		cropxParameter.setRequired(false);
		cropxParameter.setType(ParamType.NUMBER);
		parameters.put(CROP_X_QUERY_PARAM_KEY, cropxParameter);

		// cropy
		QueryParameter cropyParameter = new QueryParameter();
		cropyParameter.setDescription("Set image crop area start y coordinate.");
		cropyParameter.setExample("260");
		cropyParameter.setRequired(false);
		cropyParameter.setType(ParamType.NUMBER);
		parameters.put(CROP_Y_QUERY_PARAM_KEY, cropyParameter);

		// croph
		QueryParameter crophParameter = new QueryParameter();
		crophParameter.setDescription("Set image crop area height.");
		crophParameter.setExample("35");
		crophParameter.setType(ParamType.NUMBER);
		crophParameter.setRequired(false);
		parameters.put(CROP_HEIGHT_QUERY_PARAM_KEY, crophParameter);

		// cropw
		QueryParameter cropwParameter = new QueryParameter();
		cropwParameter.setDescription("Set image crop area width.");
		cropwParameter.setExample("35");
		cropwParameter.setRequired(false);
		cropwParameter.setType(ParamType.NUMBER);
		parameters.put(CROP_WIDTH_QUERY_PARAM_KEY, cropwParameter);

		return parameters;
	}

}