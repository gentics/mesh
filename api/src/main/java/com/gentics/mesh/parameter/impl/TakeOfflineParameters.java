package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.ParameterProvider;

public class TakeOfflineParameters extends AbstractParameters {

	public static final String RECURSIVE_PARAMETER_KEY = "recursive";

	private Boolean recursive;

	public TakeOfflineParameters(ActionContext ac) {
		super(ac);
	}

	public TakeOfflineParameters() {
	}

	public ParameterProvider setRecursive(boolean flag) {
		this.recursive = flag;
		return this;
	}

	public boolean isRecursive() {
		return BooleanUtils.toBooleanDefaultIfNull(recursive, false);
	}

	@Override
	protected void constructFrom(ActionContext ac) {
		setRecursive(Boolean.valueOf(ac.getParameter(TakeOfflineParameters.RECURSIVE_PARAMETER_KEY)));
	}

	@Override
	protected Map<String, Object> getParameters() {
		Map<String, Object> map = new HashMap<>();
		map.put(RECURSIVE_PARAMETER_KEY, recursive);
		return map;
	}

}
