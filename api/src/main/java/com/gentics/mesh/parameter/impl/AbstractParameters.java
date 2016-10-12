package com.gentics.mesh.parameter.impl;

import java.util.Map.Entry;

import org.apache.commons.lang.BooleanUtils;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.MultiMap;

public abstract class AbstractParameters implements ParameterProvider {

	protected MultiMap parameters;

	public AbstractParameters(ActionContext ac) {
		this(ac.getParameters());
		validate();
	}

	public AbstractParameters(MultiMap parameters) {
		this.parameters = parameters;
	}

	public AbstractParameters() {
		this(MultiMap.caseInsensitiveMultiMap());
	}

	protected String getParameter(String name) {
		return parameters.get(name);
	}

	protected MultiMap getParameters() {
		return parameters;
	}

	protected void setParameter(String name, String value) {
		parameters.set(name, value);
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		MultiMap params = getParameters();
		for (Entry<String, String> entry : params.entries()) {
			String value = entry.getValue();
			if (value != null) {
				if (query.length() != 0) {
					query.append("&");
				}
				// try {
				query.append(entry.getKey() + "=" + value);// URLEncoder.encode(value, "UTF-8"));
				// } catch (UnsupportedEncodingException e) {
				// }
			}
		}
		return query.toString();
	}

	/**
	 * Convert the provides object to a string representation.
	 * 
	 * @param value
	 * @return String representation of value
	 */
	protected String convertToStr(Object value) {
		if (value instanceof String[]) {
			String stringVal = "";
			String[] values = (String[]) value;
			for (int i = 0; i < values.length; i++) {
				stringVal += values[i];
				if (i != values.length - 1) {
					stringVal += ',';
				}
			}
			return stringVal;
		} else if (value instanceof Integer) {
			return Integer.toString((int) value);
		} else if (value instanceof Boolean) {
			return BooleanUtils.toStringTrueFalse((Boolean) value);
		} else {
			return value.toString();
		}
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}
}
