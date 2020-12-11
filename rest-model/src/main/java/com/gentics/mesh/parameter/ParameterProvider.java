package com.gentics.mesh.parameter;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.BooleanUtils;
import org.raml.model.parameter.QueryParameter;

/**
 * Common interface for query parameters.
 */
public interface ParameterProvider {

	/**
	 * * Validate the parameters and throw an exception when an invalid set of parameters has been detected.
	 */
	default void validate() {
	}

	/**
	 * Return the RAML parameters for this provider.
	 * 
	 * @return
	 */
	Map<? extends String, ? extends QueryParameter> getRAMLParameters();

	/**
	 * Set the query parameter.
	 * 
	 * @param name
	 *            Parameter name
	 * @param value
	 *            Parameter value
	 */
	void setParameter(String name, String value);

	/**
	 * Return the query parameter value for the given name.
	 * 
	 * @param name
	 *            Parameter name
	 * @return Loaded value or null
	 */
	String getParameter(String name);

	Map<String, String> getParameters();

	/**
	 * Convert the provides object to a string representation.
	 * 
	 * @param value
	 * @return String representation of value
	 */
	default String convertToStr(Object value) {
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

	/**
	 * Return the query parameters which do not include the the first &amp; or ? character.
	 * 
	 * @return Query string
	 */
	default String getQueryParameters() {
		StringBuilder query = new StringBuilder();
		Map<String, String> params = getParameters();
		for (Entry<String, String> entry : params.entrySet()) {
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

}
