package com.gentics.mesh.query.impl;

import org.apache.commons.lang.BooleanUtils;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.query.QueryParameterProvider;

public class TakeOfflineParameter implements QueryParameterProvider {

	public static final String RECURSIVE_PARAMETER_KEY = "recursive";

	private Boolean recursive;

	public QueryParameterProvider setRecursive(boolean flag) {
		this.recursive = flag;
		return this;
	}

	public boolean isRecursive() {
		return BooleanUtils.toBooleanDefaultIfNull(recursive, false);
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		if (recursive != null && recursive.booleanValue()) {
			query.append(RECURSIVE_PARAMETER_KEY + "=");
			query.append(true);
		}
		return query.toString();
	}

	public static TakeOfflineParameter createFrom(ActionContext ac) {
		TakeOfflineParameter parameter = new TakeOfflineParameter();
		parameter.setRecursive(Boolean.valueOf(ac.getParameter(TakeOfflineParameter.RECURSIVE_PARAMETER_KEY)));
		return parameter;
	}

}
