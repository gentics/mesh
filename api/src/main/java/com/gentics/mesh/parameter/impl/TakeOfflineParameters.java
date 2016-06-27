package com.gentics.mesh.parameter.impl;

import org.apache.commons.lang.BooleanUtils;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.ParameterProvider;

public class TakeOfflineParameters extends AbstractParameters {

	public static final String RECURSIVE_PARAMETER_KEY = "recursive";

	public TakeOfflineParameters(ActionContext ac) {
		super(ac);
	}

	public TakeOfflineParameters() {
	}

	public ParameterProvider setRecursive(boolean flag) {
		setParameter(RECURSIVE_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	public boolean isRecursive() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(RECURSIVE_PARAMETER_KEY)), false);
	}

	@Override
	public void validate() {
		// TODO validate query parameter value
	}

}
