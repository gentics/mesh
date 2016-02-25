package com.gentics.mesh.core.data.node.handler;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;

/**
 * Type converter for the script engine used by the node migration handler
 */
@SuppressWarnings("restriction")
public class TypeConverter {
	private NumberFormat format = NumberFormat.getInstance();

	/**
	 * Convert the given value into a binary value
	 *
	 * @param value
	 * @return
	 */
	public Object toBinary(Object value) {
		return isBinary(value) ? value : null;
	}

	/**
	 * Convert the given value to a string
	 *
	 * @param value
	 * @return
	 */
	public String toString(Object value) {
		if (value == null || isJSObject(value)) {
			return null;
		}
		if (isJSArray(value)) {
			String combined = getJSArray(value).stream().map(e -> toString(e)).filter(s -> s != null)
					.collect(Collectors.joining(","));
			return combined.length() > 0 ? combined : null;
		} else {
			return value.toString();
		}
	}

	/**
	 * Convert the given value to a string array
	 *
	 * @param value
	 * @return
	 */
	public String[] toStringList(Object value) {
		if (value == null || isJSObject(value)) {
			return null;
		}

		if (isJSArray(value)) {
			List<String> list = getJSArray(value).stream().map(e -> toString(e)).filter(s -> s != null)
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
		if (value == null || isJSObject(value)) {
			return null;
		}

		if (isJSArray(value)) {
			return getJSArray(value).stream().findFirst().map(e -> toBoolean(e)).orElse(null);
		} else if (Arrays.asList("true", "1").contains(value.toString().toLowerCase())) {
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
		if (value == null || isJSObject(value)) {
			return null;
		}

		if (isJSArray(value)) {
			List<Boolean> list = getJSArray(value).stream().map(e -> toBoolean(e)).filter(b -> b != null)
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
	 * Convert the given value to a date
	 *
	 * @param value
	 * @return
	 */
	public Number toDate(Object value) {
		if (value == null || isJSObject(value)) {
			return null;
		}

		if (isJSArray(value)) {
			return getJSArray(value).stream().findFirst().map(e -> toDate(e)).orElse(null);
		} else if (value instanceof Number) {
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
	 * Convert the given value to a date array
	 *
	 * @param value
	 * @return
	 */
	public Number[] toDateList(Object value) {
		if (value == null || isJSObject(value)) {
			return null;
		}

		if (isJSArray(value)) {
			List<Number> list = getJSArray(value).stream().map(e -> toDate(e)).filter(d -> d != null)
					.collect(Collectors.toList());
			return list.toArray(new Number[list.size()]);
		} else {
			Number dateValue = toDate(value);
			if (dateValue == null) {
				return null;
			} else {
				return new Number[] {dateValue};
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
		if (value == null || isJSObject(value)) {
			return null;
		}

		if (isJSArray(value)) {
			return getJSArray(value).stream().findFirst().map(e -> toNumber(e)).orElse(null);
		} else if (value instanceof Number) {
			return (Number)value;
		} else if (value instanceof Boolean) {
			return ((Boolean)value).booleanValue() ? 1 : 0;
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
		if (value == null || isJSObject(value)) {
			return null;
		}

		if (isJSArray(value)) {
			List<Number> list = getJSArray(value).stream().map(e -> toNumber(e)).filter(n -> n != null)
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

	/**
	 * Convert the given value into a micronode
	 *
	 * @param value
	 * @return
	 */
	public Object toMicronode(Object value) {
		if (isMicronode(value)) {
			return ScriptUtils.unwrap(value);
		} else if (isJSArray(value)) {
			return getJSArray(value).stream().findFirst().filter(o -> isMicronode(o)).map(o -> ScriptUtils.unwrap(o))
					.orElse(null);
		} else {
			return null;
		}
	}

	/**
	 * Convert the given value into a list of micronodes
	 *
	 * @param value
	 * @return
	 */
	public Object[] toMicronodeList(Object value) {
		if (isMicronode(value)) {
			return new Object[] {ScriptUtils.unwrap(value)};
		} else if (isJSArray(value) && getJSArray(value).stream().anyMatch(o -> isMicronode(o))) {
			List<Object> list = getJSArray(value).stream().map(e -> toMicronode(e)).filter(s -> s != null)
					.collect(Collectors.toList());
			return list.toArray(new Object[list.size()]);
		} else {
			return null;
		}
	}

	/**
	 * Convert the given value into a node
	 *
	 * @param value
	 * @return
	 */
	public Object toNode(Object value) {
		if (isNode(value)) {
			return ScriptUtils.unwrap(value);
		} else if (isJSArray(value)) {
			return getJSArray(value).stream().findFirst().filter(o -> isNode(o)).map(o -> ScriptUtils.unwrap(o))
					.orElse(null);
		} else {
			return null;
		}
	}

	/**
	 * Convert the given value into a list of nodes
	 *
	 * @param value
	 * @return
	 */
	public Object[] toNodeList(Object value) {
		if (isNode(value)) {
			return new Object[] {ScriptUtils.unwrap(value)};
		} else if (isJSArray(value) && getJSArray(value).stream().anyMatch(o -> isNode(o))) {
			List<Object> list = getJSArray(value).stream().map(e -> toNode(e)).filter(s -> s != null)
					.collect(Collectors.toList());
			return list.toArray(new Object[list.size()]);
		} else {
			return null;
		}
	}

	/**
	 * Check whether the object represents a javascript array
	 *
	 * @param value
	 * @return
	 */
	protected boolean isJSArray(Object value) {
		if (value instanceof ScriptObjectMirror) {
			return ((ScriptObjectMirror) value).isArray();
		} else {
			return false;
		}
	}

	/**
	 * Check whether the object represents a javascript object
	 *
	 * @param value
	 * @return
	 */
	protected boolean isJSObject(Object value) {
		if (value instanceof ScriptObjectMirror) {
			return !(isJSArray(value));
		} else {
			return false;
		}
	}

	/**
	 * Get array values as list
	 *
	 * @param value
	 * @return
	 */
	protected List<Object> getJSArray(Object value) {
		if (isJSArray(value)) {
			ScriptObjectMirror arrayValue = (ScriptObjectMirror)value;
			return arrayValue.values().stream().collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Check whether the given object is a binary
	 *
	 * @param value
	 * @return
	 */
	protected boolean isBinary(Object value) {
		if (!isJSObject(value)) {
			return false;
		} else {
			ScriptObjectMirror object = ((ScriptObjectMirror)value);
			return object.containsKey("sha512sum") && object.containsKey("fileName");
		}
	}

	/**
	 * Check whether the given object is a node
	 *
	 * @param value
	 * @return
	 */
	protected boolean isNode(Object value) {
		if (!isJSObject(value)) {
			return false;
		} else {
			ScriptObjectMirror object = ((ScriptObjectMirror)value);
			return object.containsKey("uuid") && !object.containsKey("microschema");
		}
	}

	/**
	 * Check whether the given object is a micronode
	 *
	 * @param value
	 * @return
	 */
	protected boolean isMicronode(Object value) {
		if (!isJSObject(value)) {
			return false;
		} else {
			ScriptObjectMirror object = ((ScriptObjectMirror)value);
			return object.containsKey("uuid") && object.containsKey("microschema");
		}
	}
}
