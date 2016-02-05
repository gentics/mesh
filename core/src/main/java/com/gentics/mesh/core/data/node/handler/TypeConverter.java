package com.gentics.mesh.core.data.node.handler;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Type converter for the script engine used by the node migration handler
 * TODO: convert lists to non-lists
 */
public class TypeConverter {
	private NumberFormat format = NumberFormat.getInstance();

	/**
	 * Convert the given value to a string
	 *
	 * @param value
	 * @return
	 */
	public String toString(Object value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	/**
	 * Convert the given value to a string array
	 *
	 * @param value
	 * @return
	 */
	public String[] toStringList(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Object[]) {
			List<String> list = Arrays.asList((Object[]) value).stream().map(e -> toString(e))
					.collect(Collectors.toList());
			return list.toArray(new String[list.size()]);
		} else {
			String stringValue = toString(value);
			if (stringValue == null) {
				return null;
			} else {
				return new String[] {stringValue};
			}
		}
	}

	/**
	 * Convert the given value to a boolean
	 *
	 * @param value
	 * @return
	 */
	public Boolean toBoolean(Object value) {
		if (value == null) {
			return null;
		}

		if (Arrays.asList("true", "1").contains(value.toString().toLowerCase())) {
			return true;
		} else if (Arrays.asList("false", "0").contains(value.toString().toLowerCase())) {
			return false;
		} else {
			return null;
		}
	}

	/**
	 * Convert the given value to a boolean array
	 *
	 * @param value
	 * @return
	 */
	public Boolean[] toBooleanList(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Object[]) {
			List<Boolean> list = Arrays.asList((Object[]) value).stream().map(e -> toBoolean(e))
					.collect(Collectors.toList());
			return list.toArray(new Boolean[list.size()]);
		} else {
			Boolean booleanValue = toBoolean(value);
			if (booleanValue == null) {
				return null;
			} else {
				return new Boolean[] {booleanValue};
			}
		}
	}

	/**
	 * Convert the given value to a number
	 *
	 * @param value
	 * @return
	 */
	public Number toNumber(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Number) {
			return (Number)value;
		} else {
			try {
				return format.parse(value.toString());
			} catch (ParseException e) {
				return null;
			}
		}
	}

	/**
	 * Convert the given value to a number array
	 *
	 * @param value
	 * @return
	 */
	public Number[] toNumberList(Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof Object[]) {
			List<Number> list = Arrays.asList((Object[]) value).stream().map(e -> toNumber(e))
					.collect(Collectors.toList());
			return list.toArray(new Number[list.size()]);
		} else {
			Number numberValue = toNumber(value);
			if (numberValue == null) {
				return null;
			} else {
				return new Number[] {numberValue};
			}
		}
	}
}
