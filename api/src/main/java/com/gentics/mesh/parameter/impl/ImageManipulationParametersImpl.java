package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.NumberUtils.toInteger;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;
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
	public Integer getWidth() {
		return toInteger(getParameter(WIDTH_QUERY_PARAM_KEY), null);
	}

	@Override
	public ImageManipulationParameters setWidth(Integer width) {
		setParameter(WIDTH_QUERY_PARAM_KEY, String.valueOf(width));
		return this;
	}

	@Override
	public Integer getHeight() {
		return toInteger(getParameter(HEIGHT_QUERY_PARAM_KEY), null);
	}

	@Override
	public ImageManipulationParameters setHeight(Integer height) {
		setParameter(HEIGHT_QUERY_PARAM_KEY, String.valueOf(height));
		return this;
	}

	@Override
	public Integer getStartx() {
		return toInteger(getParameter(CROP_X_QUERY_PARAM_KEY), null);
	}

	@Override
	public ImageManipulationParameters setStartx(Integer startx) {
		setParameter(CROP_X_QUERY_PARAM_KEY, String.valueOf(startx));
		return this;
	}

	@Override
	public Integer getStarty() {
		return toInteger(getParameter(CROP_Y_QUERY_PARAM_KEY), null);
	}

	@Override
	public ImageManipulationParameters setStarty(Integer starty) {
		setParameter(CROP_Y_QUERY_PARAM_KEY, String.valueOf(starty));
		return this;
	}

	@Override
	public Integer getCroph() {
		return toInteger(getParameter(CROP_HEIGHT_QUERY_PARAM_KEY), null);
	}

	@Override
	public ImageManipulationParameters setCroph(Integer croph) {
		setParameter(CROP_HEIGHT_QUERY_PARAM_KEY, String.valueOf(croph));
		return this;
	}

	@Override
	public Integer getCropw() {
		return toInteger(getParameter(CROP_WIDTH_QUERY_PARAM_KEY), null);
	}

	@Override
	public ImageManipulationParameters setCropw(Integer cropw) {
		setParameter(CROP_WIDTH_QUERY_PARAM_KEY, String.valueOf(cropw));
		return this;
	}

	@Override
	public boolean isSet() {
		return getWidth() != null || getHeight() != null || getCroph() != null || getCropw() != null || getStartx() != null || getStarty() != null;
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
	public boolean hasAllCropParameters() {
		return getCroph() != null && getCropw() != null && getStartx() != null && getStarty() != null;
	}

	@Override
	public void validateCropBounds(int imageWidth, int imageHeight) {
		if (getStartx() + getCropw() > imageWidth || getStarty() + getCroph() > imageHeight) {
			throw error(BAD_REQUEST, "image_error_crop_out_of_bounds", String.valueOf(imageWidth), String.valueOf(imageHeight));
		}
	}

	@Override
	public String getCacheKey() {

		StringBuilder builder = new StringBuilder();

		if (getStartx() != null) {
			builder.append("cx" + getStartx());
		}
		if (getStarty() != null) {
			builder.append("cy" + getStarty());
		}
		if (getCropw() != null) {
			builder.append("cw" + getCropw());
		}
		if (getCroph() != null) {
			builder.append("ch" + getCroph());
		}
		if (getWidth() != null) {
			builder.append("rw" + getWidth());
		}
		if (getHeight() != null) {
			builder.append("rh" + getHeight());
		}
		return builder.toString();
	}

	@Override
	public void validateLimits(ImageManipulatorOptions options) {
		if (getWidth() != null && options.getMaxWidth() != null && options.getMaxWidth() > 0 && getWidth() > options.getMaxWidth()) {
			throw error(BAD_REQUEST, "image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(getWidth()));
		}
		if (getHeight() != null && options.getMaxHeight() != null && options.getMaxHeight() > 0 && getHeight() > options.getMaxHeight()) {
			throw error(BAD_REQUEST, "image_error_height_limit_exceeded", String.valueOf(options.getMaxHeight()), String.valueOf(getHeight()));
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
		cropxParameter.setDescription("Set image crop area x coordinate.");
		cropxParameter.setExample("260");
		cropxParameter.setRequired(false);
		cropxParameter.setType(ParamType.NUMBER);
		parameters.put(CROP_X_QUERY_PARAM_KEY, cropxParameter);

		// cropy
		QueryParameter cropyParameter = new QueryParameter();
		cropyParameter.setDescription("Set image crop area y coordinate.");
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