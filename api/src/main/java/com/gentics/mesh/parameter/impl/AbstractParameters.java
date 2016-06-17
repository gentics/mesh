package com.gentics.mesh.parameter.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.ParameterProvider;

public abstract class AbstractParameters implements ParameterProvider {

	/**
	 * Read the {@link ActionContext} parameters and set the needed values within the parameter object.
	 * 
	 * @param ac
	 */
	protected abstract void constructFrom(ActionContext ac);

	public AbstractParameters(ActionContext ac) {
		constructFrom(ac);
	}

	public AbstractParameters() {
	}

	protected abstract Map<String, Object> getParameters();

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		Map<String, Object> params = getParameters();
		for (String key : params.keySet()) {
			Object value = params.get(key);
			if (value != null) {
				if (query.length() != 0) {
					query.append("&");
				}
				String valueStr = convertToStr(value);
				try {
					query.append(key + "=" + URLEncoder.encode(valueStr, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
		return query.toString();
	}

	private String convertToStr(Object value) {
		if (value instanceof String[]) {
			String stringVal = null;
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
