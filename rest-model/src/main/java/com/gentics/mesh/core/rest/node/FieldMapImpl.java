package com.gentics.mesh.core.rest.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.json.JsonUtil;
import com.google.common.collect.Lists;

/**
 * Implementation of a fieldmap which uses a central JsonNode to access the field specific data. Fields will be mapped during runtime.
 * 
 * @see FieldMap
 *
 */
public class FieldMapImpl implements FieldMap {

	private JsonNode node;

	public FieldMapImpl(JsonNode node) {
		this.node = node;
	}

	public FieldMapImpl() {
		this(JsonNodeFactory.instance.objectNode());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Field> T getField(String key, FieldTypes type, String listType, boolean expand) {

		try {
			JsonNode jsonNode = node.get(key);
			if (!node.has(key)) {
				return null;
			}
			if (jsonNode == null || jsonNode.isNull()) {
				return null;
			}
			// Handle each field type
			switch (type) {
			case HTML:
				return (T) transformHtmlFieldJsonNode(jsonNode, key);
			case STRING:
				return (T) transformStringFieldJsonNode(jsonNode, key);
			case BINARY:
				return (T) transformBinaryFieldJsonNode(jsonNode, key);
			case NUMBER:
				return (T) transformNumberFieldJsonNode(jsonNode, key);
			case BOOLEAN:
				return (T) transformBooleanFieldJsonNode(jsonNode, key);
			case DATE:
				return (T) transformDateFieldJsonNode(jsonNode, key);
			case LIST:
				return (T) transformListFieldJsonNode(jsonNode, key, listType);
			case NODE:
				return (T) transformNodeFieldJsonNode(jsonNode, key, expand);
			case MICRONODE:
				return (T) transformMicronodeFieldJsonNode(jsonNode, key);
			default:
				// TODO i18n
				throw error(INTERNAL_SERVER_ERROR, "Unknown field type {" + type + "}");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private FieldList<?> transformListFieldJsonNode(JsonNode jsonNode, String key, String listType) throws JsonProcessingException {

		ObjectMapper mapper = JsonUtil.getMapper();
		// ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
		switch (listType) {
		case "node":
			// Unwrap stored pojos
			if (jsonNode.isPojo()) {
				return pojoNodeToValue(jsonNode, NodeFieldList.class, key);
			}
			// TODO use NodeFieldListItemDeserializer to deserialize the item in expanded form
			NodeFieldListImpl nodeListField = new NodeFieldListImpl();
			// NodeFieldListItemDeserializer deser = new NodeFieldListItemDeserializer();
			for (JsonNode node : jsonNode) {
				nodeListField.getItems().add(mapper.treeToValue(node, NodeFieldListItem.class));
			}
			// NodeFieldListItem[] itemsArray = oc.treeToValue(jsonNode, NodeFieldListItemImpl[].class);
			// nodeListField.getItems().addAll(Arrays.asList(itemsArray));

			// NodeFieldListImpl nodeListField = null;
			// try {
			// nodeListField = JsonUtil.readNode(jsonNode.toString(), NodeFieldListImpl.class);
			// } catch (MeshJsonException e) {
			// if (log.isDebugEnabled()) {
			// log.debug(
			// "Could not deserialize json to expanded Node Response this is normal when the json does not contain expanded fields: "
			// + e.getMessage());
			// }
			// nodeListField = oc.treeToValue(jsonNode, NodeFieldListImpl.class);
			// } catch (IOException e) {
			// throw new MeshJsonException("Could not read node field for key {" + fieldKey + "}", e);
			// }
			return nodeListField;
		case "micronode":
			// Unwrap stored pojos
			if (jsonNode.isPojo()) {
				return pojoNodeToValue(jsonNode, MicronodeFieldList.class, key);
			}
			MicronodeFieldList micronodeFieldList = new MicronodeFieldListImpl();
			for (JsonNode node : jsonNode) {
				micronodeFieldList.getItems().add(JsonUtil.readValue(node.toString(), MicronodeResponse.class));
			}
			return micronodeFieldList;
		// Basic types
		case "string":
			// Unwrap stored pojos
			if (jsonNode.isPojo()) {
				return pojoNodeToValue(jsonNode, StringFieldListImpl.class, key);
			}
			String[] itemsStringArray = mapper.treeToValue(jsonNode, String[].class);
			return getBasicList(key, String[].class, new StringFieldListImpl(), String.class, itemsStringArray);
		case "html":
			// Unwrap stored pojos
			if (jsonNode.isPojo()) {
				return pojoNodeToValue(jsonNode, HtmlFieldListImpl.class, key);
			}
			String[] itemsHtmlArray = mapper.treeToValue(jsonNode, String[].class);
			return getBasicList(key, String[].class, new HtmlFieldListImpl(), String.class, itemsHtmlArray);
		case "date":
			// Unwrap stored pojos
			if (jsonNode.isPojo()) {
				return pojoNodeToValue(jsonNode, DateFieldListImpl.class, key);
			}
			String[] itemsDateArray = mapper.treeToValue(jsonNode, String[].class);
			return getBasicList(key, String[].class, new DateFieldListImpl(), String.class, itemsDateArray);
		case "number":
			// Unwrap stored pojos
			if (jsonNode.isPojo()) {
				return pojoNodeToValue(jsonNode, NumberFieldListImpl.class, key);
			}
			Number[] itemsNumberArray = mapper.treeToValue(jsonNode, Number[].class);
			return getBasicList(key, Number[].class, new NumberFieldListImpl(), Number.class, itemsNumberArray);
		case "boolean":
			// Unwrap stored pojos
			if (jsonNode.isPojo()) {
				return pojoNodeToValue(jsonNode, BooleanFieldListImpl.class, key);
			}
			Boolean[] itemsBooleanArray = mapper.treeToValue(jsonNode, Boolean[].class);
			return getBasicList(key, Boolean[].class, new BooleanFieldListImpl(), Boolean.class, itemsBooleanArray);
		default:
			// TODO i18n
			throw error(BAD_REQUEST, "Unknown list type {" + listType + "}");
		}
		// } else {
		// //TODO handle unexpected error
		// throw new MeshJsonException("Unknown field list class type {" + fieldSchema.getClass().getName() + "}");
		// }
	}

	/**
	 * Transform the JSON node field into html field POJO.
	 * 
	 * @param jsonNode
	 * @param key
	 * @return
	 */
	private HtmlField transformHtmlFieldJsonNode(JsonNode jsonNode, String key) {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			HtmlField field = pojoNodeToValue(jsonNode, HtmlField.class, key);
			if (field == null || field.getHTML() == null) {
				return null;
			} else {
				return field;
			}
		}
		HtmlField htmlField = new HtmlFieldImpl();
		if (!jsonNode.isNull() && jsonNode.isTextual()) {
			htmlField.setHTML(jsonNode.textValue());
		}
		if (!jsonNode.isNull() && !jsonNode.isTextual()) {
			throw error(BAD_REQUEST, "field_html_error_invalid_type", key, jsonNode.asText());
		}
		return htmlField;
	}

	/**
	 * Transform the JSON node into a micronode POJO.
	 * 
	 * @param jsonNode
	 * @param key
	 * @return
	 */
	private MicronodeResponse transformMicronodeFieldJsonNode(JsonNode jsonNode, String key) {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			return pojoNodeToValue(jsonNode, MicronodeResponse.class, key);
		}
		try {
			if (jsonNode.isNull()) {
				return null;
			} else {
				MicronodeResponse field = JsonUtil.getMapper().treeToValue(jsonNode, MicronodeResponse.class);
				return field;
			}
		} catch (IOException e) {
			// TODO i18n
			throw error(INTERNAL_SERVER_ERROR, "Could not read node field for key {" + key + "}", e);
		}

	}

	/**
	 * Transform the JSON node into a response POJO.
	 * 
	 * @param jsonNode
	 * @param key
	 * @return
	 */
	private DateField transformDateFieldJsonNode(JsonNode jsonNode, String key) {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			DateField field = pojoNodeToValue(jsonNode, DateField.class, key);
			if (field == null || field.getDate() == null) {
				return null;
			} else {
				return field;
			}
		}

		DateField dateField = new DateFieldImpl();
		if (!jsonNode.isNull() && jsonNode.isTextual()) {
			dateField.setDate(jsonNode.textValue());
		}
		if (!jsonNode.isNull() && !jsonNode.isTextual()) {
			throw error(BAD_REQUEST, "The field value for date field {" + key + "} is not a string value. The value was {" + jsonNode.asText() + "}");
		}
		return dateField;
	}

	/**
	 * Transform the JSON node into a node response POJO.
	 * 
	 * @param jsonNode
	 * @param key
	 * @param expand
	 * @return
	 * @throws IOException
	 */
	private NodeField transformNodeFieldJsonNode(JsonNode jsonNode, String key, boolean expand) throws IOException {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			Object pojo = ((POJONode) jsonNode).getPojo();
			if (pojo != null) {
				if (pojo instanceof NodeFieldImpl) {
					return pojoNodeToValue(jsonNode, NodeFieldImpl.class, key);
				} else {
					return pojoNodeToValue(jsonNode, NodeResponse.class, key);
				}
			} else {
				return null;
			}
		}

		ObjectMapper mapper = JsonUtil.getMapper();
		if (expand) {
			return JsonUtil.readValue(jsonNode.toString(), NodeResponse.class);
		} else {
			if (jsonNode.isNull()) {
				return null;
			}
			return mapper.treeToValue(jsonNode, NodeFieldImpl.class);
		}
	}

	/**
	 * Transform the JSON node in a boolean field (if possible)
	 * 
	 * @param jsonNode
	 * @param key
	 * @return
	 */
	private BooleanField transformBooleanFieldJsonNode(JsonNode jsonNode, String key) {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			BooleanField field = pojoNodeToValue(jsonNode, BooleanField.class, key);
			if (field == null || field.getValue() == null) {
				return null;
			} else {
				return field;
			}
		}

		BooleanField booleanField = new BooleanFieldImpl();
		if (!jsonNode.isNull() && jsonNode.isBoolean()) {
			booleanField.setValue(jsonNode.booleanValue());
		}
		if (!jsonNode.isNull() && !jsonNode.isBoolean()) {
			throw error(BAD_REQUEST, "The field value for {" + key + "} is not a boolean value. The value was {" + jsonNode.asText() + "}");
		}
		return booleanField;
	}

	private NumberField transformNumberFieldJsonNode(JsonNode jsonNode, String key) {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			NumberField field = pojoNodeToValue(jsonNode, NumberField.class, key);
			if (field == null || field.getNumber() == null) {
				return null;
			} else {
				return field;
			}
		}
		NumberField numberField = new NumberFieldImpl();
		if (!jsonNode.isNull() && jsonNode.isNumber()) {
			Number number = jsonNode.numberValue();
			numberField.setNumber(number);
		}
		if (!jsonNode.isNull() && !jsonNode.isNumber()) {
			throw error(BAD_REQUEST, "field_number_error_invalid_type", key, jsonNode.asText());
		}
		return numberField;
	}

	private BinaryField transformBinaryFieldJsonNode(JsonNode jsonNode, String key) throws JsonProcessingException {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			return pojoNodeToValue(jsonNode, BinaryField.class, key);
		}
		return JsonUtil.getMapper().treeToValue(jsonNode, BinaryFieldImpl.class);
	}

	private StringField transformStringFieldJsonNode(JsonNode jsonNode, String key) throws JsonProcessingException {
		// Unwrap stored pojos
		if (jsonNode.isPojo()) {
			StringField field = pojoNodeToValue(jsonNode, StringField.class, key);
			if (field == null || field.getString() == null) {
				return null;
			} else {
				return field;
			}
		}
		StringField stringField = new StringFieldImpl();
		if (!jsonNode.isNull() && jsonNode.isTextual()) {
			stringField.setString(jsonNode.textValue());
		}
		if (!jsonNode.isNull() && !jsonNode.isTextual()) {
			throw error(BAD_REQUEST, "The field value for {" + key + "} is not a text value. The value was {" + jsonNode.asText() + "}");
		}
		return stringField;
	}

	/**
	 * Unwrapped the POJO within the JSON node.
	 * 
	 * @param jsonNode
	 * @param clazz
	 * @param key
	 * @return
	 */
	private <T extends Field> T pojoNodeToValue(JsonNode jsonNode, Class<T> clazz, String key) {
		Object pojo = ((POJONode) jsonNode).getPojo();
		if (pojo == null) {
			return null;
		}
		if (clazz.isAssignableFrom(pojo.getClass())) {
			return clazz.cast(pojo);
		} else {
			throw error(BAD_REQUEST, "The field value for {" + key + "} is not of correct type. Stored POJO was of class {" + pojo.getClass()
					.getName() + "}");
		}
	}

	private <I, AT> FieldList<I> getBasicList(String fieldKey, Class<AT> clazzOfJsonArray, FieldList<I> list, Class<I> classOfItem, I[] itemsArray) {
		if (itemsArray != null) {
			list.getItems().addAll((List<I>) Arrays.asList(itemsArray));
		}
		return list;
	}

	@Override
	public int size() {
		return node.size();
	}

	@Override
	public Collection<String> keySet() {
		return Lists.newArrayList(node.fieldNames());
	}

	@Override
	public DateFieldListImpl getDateFieldList(String key) {
		return getField(key, FieldTypes.LIST, "date");
	}

	private <T extends Field> T getField(String key, FieldTypes type, String listType) {
		return getField(key, type, listType, false);
	}

	@Override
	public HtmlFieldListImpl getHtmlFieldList(String key) {
		return getField(key, FieldTypes.LIST, "html");
	}

	@Override
	public HtmlFieldImpl getHtmlField(String key) {
		return getField(key, FieldTypes.HTML);
	}

	@Override
	public BinaryField getBinaryField(String key) {
		return getField(key, FieldTypes.BINARY);
	}

	@Override
	public BooleanFieldImpl getBooleanField(String key) {
		return getField(key, FieldTypes.BOOLEAN);
	}

	@Override
	public DateFieldImpl getDateField(String key) {
		return getField(key, FieldTypes.DATE);
	}

	@Override
	public MicronodeResponse getMicronodeField(String key) {
		return getField(key, FieldTypes.MICRONODE);
	}

	@Override
	public NumberFieldImpl getNumberField(String key) {
		return getField(key, FieldTypes.NUMBER);
	}

	@Override
	public StringFieldImpl getStringField(String key) {
		return getField(key, FieldTypes.STRING);
	}

	/**
	 * Return the field with the given key.
	 * 
	 * @param key
	 *            Field key
	 * @param type
	 *            Field type
	 * @return
	 */
	private <T extends Field> T getField(String key, FieldTypes type) {
		return getField(key, type, null);
	}

	@Override
	public NodeField getNodeField(String key) {
		return getField(key, FieldTypes.NODE);
	}

	@JsonIgnore
	@Override
	public boolean isEmpty() {
		return node.size() == 0;
	}

	@Override
	public boolean hasField(String key) {
		return node.has(key);
	}

	@Override
	public NumberFieldListImpl getNumberFieldList(String key) {
		return getField(key, FieldTypes.LIST, "number");
	}

	@Override
	public BooleanFieldListImpl getBooleanFieldList(String key) {
		return getField(key, FieldTypes.LIST, "boolean");
	}

	@Override
	public StringFieldListImpl getStringFieldList(String key) {
		return getField(key, FieldTypes.LIST, "string");
	}

	@Override
	public MicronodeFieldList getMicronodeFieldList(String key) {
		return getField(key, FieldTypes.LIST, "micronode");
	}

	@Override
	public Field put(String fieldKey, Field field) {
		ObjectNode objectNode = ((ObjectNode) node);
		objectNode.putPOJO(fieldKey, field);
		return field;
	}

	@Override
	public FieldMap putAll(Map<String, Field> fieldMap) {
		ObjectNode objectNode = ((ObjectNode) node);
		fieldMap.forEach((key, field) -> {
			objectNode.putPOJO(key, field);
		});
		return this;
	}

	@Override
	public boolean remove(String fieldKey) {
		ObjectNode objectNode = ((ObjectNode) node);
		JsonNode object = objectNode.remove(fieldKey);
		return object != null;
	}

	@Override
	public void clear() {
		((ObjectNode) node).removeAll();
	}

	@Override
	public NodeResponse getNodeFieldExpanded(String key) {
		return getField(key, FieldTypes.NODE, null, true);
	}

	@Override
	public boolean isExpandedNodeField(String fieldKey) {
		JsonNode field = node.get(fieldKey);
		if (field == null) {
			return false;
		}
		if (field.isObject() && field.has("fields")) {
			return true;
		}
		if (field.isPojo()) {
			return ((POJONode) field).getPojo() instanceof NodeResponse;
		}
		return false;
	}

	@Override
	public NodeFieldList getNodeFieldList(String key) {
		return getField(key, FieldTypes.LIST, "node");
	}

	@Override
	public Field getField(String key, FieldSchema fieldSchema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		String listType = null;
		if (fieldSchema instanceof ListFieldSchema) {
			listType = ((ListFieldSchema) fieldSchema).getListType();
		}
		return getField(key, type, listType);
	}

	public JsonNode getNode() {
		return node;
	}

	@Override
	public String toString() {
		return node.toString();
	}

	@Override
	public Set<String> getUrlFieldValues(Schema schema) {
		Set<String> urlFieldValues = new HashSet<>();
		for (String urlField : schema.getUrlFields()) {
			FieldSchema fieldSchema = schema.getField(urlField);
			Field field = getField(urlField, fieldSchema);
			if (field instanceof StringFieldImpl) {
				StringFieldImpl stringField = (StringFieldImpl) field;
				String value = stringField.getString();
				if (value != null) {
					urlFieldValues.add(value);
				}
			}
			if (field instanceof StringFieldListImpl) {
				StringFieldListImpl stringListField = (StringFieldListImpl) field;
				for (String value : stringListField.getItems()) {
					if (value != null) {
						urlFieldValues.add(value);
					}
				}
			}
		}
		return urlFieldValues;
	}

}