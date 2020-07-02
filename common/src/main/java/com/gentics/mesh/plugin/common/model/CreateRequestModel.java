package com.gentics.mesh.plugin.common.model;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Model for create requests which can create a {@link FieldMap} from its
 * member fields annotated with {@link RequestField}.
 */
public abstract class CreateRequestModel implements RestModel {

	/**
	 * Get the schema name for this request.
	 *
	 * <p>
	 *     This method only needs to be implemented for classes which
	 *     {@link #toNodeCreateRequest()} methods are called.
	 * </p>
	 *
	 * @return The name of the schema for the {@link NodeCreateRequest}
	 */
	public String getSchemaName() {
		throw new UnsupportedOperationException(
			"Request model does not specify schema name, and cannot be converted to a NodeCreateRequest");
	}

	/**
	 * Create a {@code NodeCreateRequest} for an english node of this models
	 * {@link #getSchemaName() schema name} and {@link #toFieldMap() field map}.
	 *
	 * @return A {@code NodeCreateRequest} corresponding to this model
	 */
	public NodeCreateRequest toNodeCreateRequest() {
		return new NodeCreateRequest()
			.setLanguage("en")
			.setSchemaName(getSchemaName())
			.setFields(toFieldMap());
	}

	/**
	 * Convenience method for {@link #toNodeCreateRequest()} which already sets
	 * the {@code parentUuid} respectively.
	 *
	 * @param parentUuid The UUID of the paren tnode
	 * @return A {@code NodeCreateRequest} corresponding to this model
	 */
	public NodeCreateRequest toNodeCreateRequest(String parentUuid) {
		return toNodeCreateRequest()
			.setParentNodeUuid(parentUuid);
	}

	/**
	 * Convenience method for {@link #toNodeCreateRequest()} which already sets
	 * the language and parent UUID respectively.
	 *
	 * @param lang The language for the new node
	 * @param parentUuid The nodes parent UUID
	 * @return A {@code NodeCreateRequest} corresponding to this model
	 */
	public NodeCreateRequest toNodeCreateRequest(String lang, String parentUuid) {
		return toNodeCreateRequest(parentUuid)
			.setLanguage(lang);
	}

	/**
	 * Create a {@code FieldMap} from all member fields annoated with
	 * {@link RequestField}.
	 *
	 * <p>
	 *     String fields which should be interpreted as dates or HTML should
	 *     set the {@link RequestField#date()} or {@link RequestField#html()}
	 *     parameters accordingly.
	 * </p>
	 *
	 * @return A {@code FieldMap} with all the member fields annotated with
	 * 		{@code @RequestField}
	 */
	public FieldMap toFieldMap() {
		FieldMap fieldMap = new FieldMapImpl();

		for (Field requestField : getClass().getDeclaredFields()) {
			RequestField annotation = requestField.getAnnotation(RequestField.class);

			if (annotation == null) {
				continue;
			}

			requestField.setAccessible(true);
			com.gentics.mesh.core.rest.node.field.Field field = toMeshField(requestField, annotation);

			if (field != null) {
				fieldMap.put(requestField.getName(), field);
			}
		}

		return fieldMap;
	}

	/**
	 * Determine the correct {@link com.gentics.mesh.core.rest.node.field.Field}
	 * implementation for the given memeber.
	 *
	 * @param field The member field
	 * @param annotation The {@code @RequestField} annotation of the field
	 * @return The correct {@code Field} implementation for this member
	 */
	private com.gentics.mesh.core.rest.node.field.Field toMeshField(Field field, RequestField annotation) {
		Object value;

		try {
			value = field.get(this);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not get field value: " + e.getMessage(), e);
		}

		if (value == null) {
			return null;
		}

		if (value instanceof String) {
			if (annotation.html()) {
				return new HtmlFieldImpl().setHTML((String) value);
			} else if (annotation.date()) {
				return new DateFieldImpl().setDate((String) value);
			}

			return new StringFieldImpl().setString((String) value);
		} else if (value instanceof JsonObject) {
			return new StringFieldImpl().setString(((JsonObject) value).encode());
		} else if (value instanceof JsonArray) {
			return new StringFieldImpl().setString(((JsonArray) value).encode());
		} else if (value instanceof Boolean) {
			return new BooleanFieldImpl().setValue((Boolean) value);
		} else if (value instanceof Number) {
			return new NumberFieldImpl().setNumber((Number) value);
		} else if (value instanceof List) {
			List<?> list = (List<?>) value;
			Type type = field.getGenericType();

			if (type instanceof ParameterizedType) {
				return getListField((ParameterizedType) type, annotation, list);
			}

			return getStringFieldList(annotation, list);
		} else {
			throw new RuntimeException(
				"Unexpected field type: " + value.getClass().getCanonicalName());
		}
	}

	/**
	 * Determine the correct {@link FieldList} for the given values.
	 *
	 * @param type The type of the field
	 * @param annotation The members annotation
	 * @param values The list of values
	 *
	 * @return The correct {@code FieldList} for the given values
	 */
	@SuppressWarnings("unchecked")
	private FieldList<?> getListField(ParameterizedType type, RequestField annotation, List<?> values) {
		Type typeArgument = type.getActualTypeArguments()[0];

		if (JsonObject.class.equals(typeArgument)) {
			List<String> strings = values.stream()
				.map(o -> ((JsonObject) o).encode())
				.collect(Collectors.toList());

			return new StringFieldListImpl().setItems(strings);
		} else if (JsonArray.class.equals(typeArgument)) {
			List<String> strings = values.stream()
				.map(o -> ((JsonArray) o).encode())
				.collect(Collectors.toList());

			return new StringFieldListImpl().setItems(strings);
		} else if (Boolean.class.equals(typeArgument)) {
			return new BooleanFieldListImpl().setItems((List<Boolean>) values);
		} else if (typeArgument instanceof Class && Number.class.isAssignableFrom((Class<?>) typeArgument)) {
			return new NumberFieldListImpl().setItems((List<Number>) values);
		}

		return getStringFieldList(annotation, values);
	}

	/**
	 * Make sure the elements in {@code values} are strings and create the
	 * appropriate {@code FieldList} instance.
	 *
	 * @param annotation The members annotation
	 * @param values The list of values
	 * @return The correct {@code FieldList} for the given values
	 */
	private FieldList<String> getStringFieldList(RequestField annotation, List<?> values) {
		List<String> strings = values.stream()
			.map(o -> o == null ? (String) null : o.toString())
			.collect(Collectors.toList());

		if (annotation.html()) {
			return new HtmlFieldListImpl().setItems(strings);
		} else if (annotation.date()) {
			return new DateFieldListImpl().setItems(strings);
		}

		return new StringFieldListImpl().setItems(strings);
	}
}
