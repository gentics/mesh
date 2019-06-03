package com.gentics.mesh.core.data.node.handler;

import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.AbstractFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Type converter for the script engine used by the node migration handler.
 */
@SuppressWarnings("restriction")
public class TypeConverter {

	private static final Logger log = LoggerFactory.getLogger(TypeConverter.class);

	private NumberFormat format = NumberFormat.getInstance();

	/**
	 * Convert the given value to a string.
	 *
	 * @param value
	 * @return
	 */
	public String toString(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof List) {
			List<?> listValue = (List) value;
			if (listValue.isEmpty()) {
				return null;
			} else {
				return listValue.stream()
					.map(Object::toString)
					.collect(Collectors.joining(","));
			}
		} else {
			return value.toString();
		}
	}

	/**
	 * Convert the given value to a string list
	 *
	 * @param value
	 *            Value to be converted
	 * @return String array
	 */
	public StringFieldListImpl toStringList(Object value) {
		return listField(StringFieldListImpl::new, this::toString, value);
	}

	/**
	 * Convert the given value to an HTML list
	 *
	 * @param value
	 *            Value to be converted
	 * @return String array
	 */
	public HtmlFieldListImpl toHtmlList(Object value) {
		return listField(HtmlFieldListImpl::new, this::toString, value);
	}

	/**
	 * Convert the given value to a boolean.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Boolean value
	 */
	public Boolean toBoolean(Object value) {
		value = firstIfList(value);

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
	 * Convert the given value to a boolean array.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Boolean array
	 */
	public BooleanFieldListImpl toBooleanList(Object value) {
		return listField(BooleanFieldListImpl::new, this::toBoolean, value);
	}

	/**
	 * Convert the given value to a date.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Date string or null if the value could not be converted
	 */
	public String toDate(Object value) {
		if (value == null) {
			return null;
		}

		value = firstIfList(value);

		if (value instanceof Number) {
			// We assume that the input string is timestamp in seconds. Thus we need to multiple by 1000
			return toISO8601(((Number) value).longValue() * 1000);
		} else {
			// 1. Try to parse value as with ISO-8601 format
			try {
				Date date = Date.from(Instant.parse(value.toString()));
				return Instant.from(date.toInstant()).atZone(ZoneId.systemDefault())
						.format(DateTimeFormatter.ISO_INSTANT);
			} catch (DateTimeException e) {
				if (log.isDebugEnabled()) {
					log.debug("Could not convert to time {" + value.toString() + "}", e);
				}
			}
			// 2. Try to parse the value as timestamp
			try {
				// We assume that the input string is timestamp in seconds. Thus we need to multiple by 1000
				return toISO8601(Long.valueOf(value.toString()) * 1000);
			} catch (DateTimeException | NumberFormatException e) {
				log.error("Could not convert to time {" + value.toString() + "}", e);
				return null;
			}
		}
	}

	/**
	 * Convert the given value to a date array.
	 *
	 * @param value
	 *            Value to be converted
	 * @return String array of dates or null if the value could not be converted
	 */
	public DateFieldListImpl toDateList(Object value) {
		return listField(DateFieldListImpl::new, this::toDate, value);
	}

	/**
	 * Convert the given value to a number.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Number or null if the number could not be converted
	 */
	public Number toNumber(Object value) {
		value = firstIfList(value);

		if (value == null) {
			return null;
		}

		if (value instanceof Number) {
			return (Number) value;
		} else if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue() ? 1 : 0;
		} else {
			// The value could be a string value which represents a date
			try {
				Long date = fromISO8601(value.toString());
				if (date != null) {
					return date;
				}
			} catch (DateTimeParseException e) {
				if (log.isDebugEnabled()) {
					log.debug("The provided object did not represent an ISO-8601 date", e);
				}
				// Ignored
			}
			try {
				return format.parse(value.toString());
			} catch (ParseException e) {
				return null;
			}
		}
	}

	/**
	 * Convert the given value to a number array.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Array of numbers or null if the value could not be converted
	 */
	public NumberFieldListImpl toNumberList(Object value) {
		return listField(NumberFieldListImpl::new, this::toNumber, value);
	}

	/**
	 * Convert the given value into a micronode.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Micronode object or null if the value could not be converted
	 */
	public MicronodeField toMicronode(Object value) {
		value = firstIfList(value);
		if (value instanceof MicronodeField) {
			return (MicronodeField) value;
		} else {
			return null;
		}
	}

	/**
	 * Convert the given value into a list of micronodes.
	 *
	 * @param value
	 *            Value to be converted
	 * @return List of micronodes or null if the value could not be converted
	 */
	public MicronodeFieldList toMicronodeList(Object value) {
		return listField(MicronodeFieldListImpl::new, this::toMicronode, value);
	}

	/**
	 * Convert the given value into a node.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Node or null if the value could not be converted
	 */
	public NodeField toNode(Object value) {
		value = firstIfList(value);
		if (value instanceof NodeField) {
			return (NodeField) value;
		} else if (value instanceof NodeFieldListItem) {
			return new NodeFieldImpl().setUuid(((NodeFieldListItem) value).getUuid());
		} else {
			return null;
		}
	}

	private NodeFieldListItem toNodeFieldListItem(Object value) {
		NodeField field = toNode(value);
		if (field == null) {
			return null;
		} else {
			return new NodeFieldListItemImpl().setUuid(field.getUuid());
		}
	}

	/**
	 * Convert the given value into a list of nodes.
	 *
	 * @param value
	 *            Value to be converted
	 * @return Array of nodes or null if the value could not be converted
	 */
	public NodeFieldList toNodeList(Object value) {
		return listField(NodeFieldListImpl::new, this::toNodeFieldListItem, value);
	}

	private Object firstIfList(Object value) {
		return toStream(value).findFirst().orElse(null);
	}

	private <T, L extends AbstractFieldList<T>> L listField(Supplier<L> listFieldSupplier, Function<Object, T> valueMapper, Object value) {
		List<T> list = toList(valueMapper, value);
		if (list == null) {
			return null;
		} else {
			L listField = listFieldSupplier.get();
			listField.setItems(list);
			return listField;
		}
	}

	private <T> List<T> toList(Function<Object, T> valueMapper, Object value) {
		List<T> list = toStream(value)
			.map(valueMapper)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		return list.isEmpty()
			? null
			: list;
	}

	private Stream<Object> toStream(Object value) {
		if (value instanceof ListField) {
			value = ((ListField) value).getItems();
		} else if (value instanceof FieldList) {
			value = ((FieldList) value).getItems();
		}

		if (value == null) {
			return Stream.empty();
		} else if (value instanceof List) {
			return ((List) value).stream();
		} else {
			return Stream.of(value);
		}
	}
}
