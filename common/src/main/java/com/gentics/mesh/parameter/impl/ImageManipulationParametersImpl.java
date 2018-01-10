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
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ImageRect;

/**
 * Crop and resize parameters for image manipulation.
 */
public class ImageManipulationParametersImpl extends AbstractParameters implements ImageManipulationParameters {

	public ImageManipulationParametersImpl(ActionContext ac) {
		super(ac);
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
			throw error(BAD_REQUEST, "image_error_parameter_positive", WIDTH_QUERY_PARAM_KEY, String.valueOf(width));
		}

		Integer height = getHeight();
		if (height != null && height < 1) {
			throw error(BAD_REQUEST, "image_error_parameter_positive", HEIGHT_QUERY_PARAM_KEY, String.valueOf(height));
		}

		ImageRect rect = getRect();
		if (rect != null) {
			rect.validate();
		}

		validateFocalPointParameter();

	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// w
		QueryParameter widthParameter = new QueryParameter();
		widthParameter.setDescription("Set image target width. The height will automatically be calculated if the width was omitted.");
		widthParameter.setExample("1280");
		widthParameter.setRequired(false);
		widthParameter.setType(ParamType.NUMBER);
		parameters.put(WIDTH_QUERY_PARAM_KEY, widthParameter);

		// h
		QueryParameter heightParameter = new QueryParameter();
		heightParameter.setDescription("Set image target height. The width will automatically be calculated if the height was omitted.");
		heightParameter.setExample("720");
		heightParameter.setRequired(false);
		heightParameter.setType(ParamType.NUMBER);
		parameters.put(HEIGHT_QUERY_PARAM_KEY, heightParameter);

		// fpx
		QueryParameter fpxParameter = new QueryParameter();
		fpxParameter.setDescription("Set the focal point x coordinate.");
		fpxParameter.setRequired(false);
		fpxParameter.setExample("120");
		fpxParameter.setType(ParamType.NUMBER);
		parameters.put(FOCAL_POINT_X_QUERY_PARAM_KEY, fpxParameter);

		// fpy
		QueryParameter fpyParameter = new QueryParameter();
		fpyParameter.setDescription("Set the focal point y coordinate.");
		fpyParameter.setRequired(false);
		fpyParameter.setExample("140");
		fpyParameter.setType(ParamType.NUMBER);
		parameters.put(FOCAL_POINT_Y_QUERY_PARAM_KEY, fpyParameter);

		// rect
		QueryParameter rectParameter = new QueryParameter();
		rectParameter.setDescription("Set image crop area.");
		rectParameter.setExample("20,20,128,128");
		rectParameter.setRequired(false);
		rectParameter.setType(ParamType.STRING);
		parameters.put(RECT_QUERY_PARAM_KEY, rectParameter);

		// crop
		QueryParameter cropParameter = new QueryParameter();
		cropParameter.setDescription("Set the crop mode. Possible modes: " + CropMode.description());
		cropParameter.setExample("rect");
		cropParameter.setRequired(false);
		cropParameter.setType(ParamType.STRING);
		parameters.put(CROP_MODE_QUERY_PARAM_KEY, cropParameter);

		return parameters;
	}

}