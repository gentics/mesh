package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	 * Set the multivalue query parameter
	 * @param <T> value type
	 * @param name parameter name
	 * @param values parameter values
	 * @param converter function to convert the values into strings
	 */
	default <T> void setMultivalueParameter(String name, Collection<T> values, Function<T, String> converter) {
		if (values == null) {
			setParameter(name, null);
		} else {
			setParameter(name, values.stream().map(converter::apply).collect(Collectors.joining(",")));
		}
	}

	/**
	 * Set the multivalue query parameter to string values
	 * @param name name
	 * @param values string values
	 */
	default void setMultivalueParameter(String name, Collection<String> values) {
		setMultivalueParameter(name, values, Function.identity());
	}

	/**
	 * Return the query parameter value for the given name.
	 * 
	 * @param name
	 *            Parameter name
	 * @return Loaded value or null
	 */
	String getParameter(String name);

	/**
	 * Return the query parameter values for the given name.
	 * @param <T> value type
	 * @param name parameter name
	 * @param converter function that converts the stores String into the expected value
	 * @return Loaded values or empty set
	 */
	default <T> Set<T> getMultivalueParameter(String name, Function<String, T> converter) {
		String value = getParameter(name);
		if (value == null || value.isEmpty()) {
			return Collections.emptySet();
		} else {
			return Arrays.asList(value.split(",")).stream().map(converter::apply).collect(Collectors.toSet());
		}
	}

	/**
	 * Return the query parameter values for the given name as set of strings
	 * @param name parameter name
	 * @return Loaded values or empty set
	 */
	default Set<String> getMultivalueParameter(String name) {
		return getMultivalueParameter(name, Function.identity());
	}

	/**
	 * Return the query parameters as a map.
	 * 
	 * @return
	 */
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
