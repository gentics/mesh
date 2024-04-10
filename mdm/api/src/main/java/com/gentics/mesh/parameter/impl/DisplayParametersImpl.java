package com.gentics.mesh.parameter.impl;

import java.util.Collections;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.DisplayParameters;

/**
 * @see DisplayParameters
 * 
 * @author plyhun
 *
 */
public class DisplayParametersImpl extends AbstractParameters implements DisplayParameters {

	public DisplayParametersImpl(ActionContext ac) {
		super(ac);
	}

	public DisplayParametersImpl() {
		super();
	}

	@Override
	public void validate() {
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		QueryParameter minifyParameter = new QueryParameter();
		minifyParameter.setDescription(
			"Flag, indicating the requirement of a minification of an input or output value, to save the payload resources. May override the globally set value.");
		minifyParameter.setExample("true");
		minifyParameter.setRequired(false);
		minifyParameter.setType(ParamType.BOOLEAN);
		return Collections.singletonMap(MINIFY_PARAM_KEY, minifyParameter);
	}

	@Override
	public String getName() {
		return "Input/Output content display parameters";
	}
}
