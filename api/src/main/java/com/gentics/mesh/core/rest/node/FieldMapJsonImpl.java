package com.gentics.mesh.core.rest.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.micronode.NullMicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
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
import com.gentics.mesh.json.JsonUtil;
import com.google.common.collect.Lists;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FieldMapJsonImpl implements FieldMap {

	private static final Logger log = LoggerFactory.getLogger(FieldMapJsonImpl.class);

	private JsonNode node;

	public FieldMapJsonImpl(JsonNode node) {
		this.node = node;
	}

	@Override
	public <T extends Field> T getField(String key, FieldTypes type, String listType, boolean expand) {

		try {
			ObjectMapper mapper = JsonUtil.getMapper();
			JsonNode jsonNode = node.get(key);
			if (!node.has(key)) {
				return null;
			}
			if (jsonNode == null) {
				return null;
			}
			// Handle each field type
			switch (type) {
			case HTML:
				HtmlField htmlField = new HtmlFieldImpl();
				if (!jsonNode.isNull() && jsonNode.isTextual()) {
					htmlField.setHTML(jsonNode.textValue());
				}
				if (!jsonNode.isNull() && !jsonNode.isTextual()) {
					throw error(BAD_REQUEST, "field_html_error_invalid_type", key, jsonNode.asText());
				}
				return (T) htmlField;
			case STRING:
				StringField stringField = new StringFieldImpl();
				if (!jsonNode.isNull() && jsonNode.isTextual()) {
					stringField.setString(jsonNode.textValue());
				}
				if (!jsonNode.isNull() && !jsonNode.isTextual()) {
					throw error(BAD_REQUEST, "The field value for {" + key + "} is not a text value. The value was {" + jsonNode.asText() + "}");
				}
				return (T) stringField;
			case BINARY:
				BinaryField binaryField = JsonUtil.readValue(jsonNode.toString(), BinaryFieldImpl.class);
				return (T) binaryField;
			case NUMBER:
				NumberField numberField = new NumberFieldImpl();
				if (!jsonNode.isNull() && jsonNode.isNumber()) {
					Number number = jsonNode.numberValue();
					numberField.setNumber(number);
				}
				if (!jsonNode.isNull() && !jsonNode.isNumber()) {
					throw error(BAD_REQUEST, "field_number_error_invalid_type", key, jsonNode.asText());
				}
				return (T) numberField;
			case BOOLEAN:
				BooleanField booleanField = new BooleanFieldImpl();
				if (!jsonNode.isNull() && jsonNode.isBoolean()) {
					booleanField.setValue(jsonNode.booleanValue());
				}
				if (!jsonNode.isNull() && !jsonNode.isBoolean()) {
					throw error(BAD_REQUEST, "The field value for {" + key + "} is not a boolean value. The value was {" + jsonNode.asText() + "}");
				}
				return (T) booleanField;
			case DATE:
				DateField dateField = new DateFieldImpl();
				if (!jsonNode.isNull() && jsonNode.isNumber()) {
					dateField.setDate(jsonNode.numberValue().longValue());
				}
				if (!jsonNode.isNull() && !jsonNode.isNumber()) {
					throw error(BAD_REQUEST, "The field value for {" + key + "} is not a number value. The value was {" + jsonNode.asText() + "}");
				}
				return (T) dateField;
			case LIST:
				// ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
				switch (listType) {
				case "node":
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
					return (T) nodeListField;
				case "micronode":
					MicronodeFieldList micronodeFieldList = new MicronodeFieldListImpl();
					for (JsonNode node : jsonNode) {
						try {
							micronodeFieldList.getItems().add(JsonUtil.readValue(node.toString(), MicronodeResponse.class));
						} catch (IOException e) {
							throw error(BAD_REQUEST, "Could not read list item", e);
						}
					}
					return (T) micronodeFieldList;
				// Basic types
				case "string":
					String[] itemsStringArray = mapper.treeToValue(jsonNode, String[].class);
					return (T) getBasicList(key, String[].class, new StringFieldListImpl(), String.class, itemsStringArray);
				case "html":
					String[] itemsHtmlArray = mapper.treeToValue(jsonNode, String[].class);
					return (T) getBasicList(key, String[].class, new HtmlFieldListImpl(), String.class, itemsHtmlArray);
				case "date":
					Long[] itemsDateArray = mapper.treeToValue(jsonNode, Long[].class);
					return (T) getBasicList(key, Long[].class, new DateFieldListImpl(), Long.class, itemsDateArray);
				case "number":
					Number[] itemsNumberArray = mapper.treeToValue(jsonNode, Number[].class);
					return (T) getBasicList(key, Number[].class, new NumberFieldListImpl(), Number.class, itemsNumberArray);
				case "boolean":
					Boolean[] itemsBooleanArray = mapper.treeToValue(jsonNode, Boolean[].class);
					return (T) getBasicList(key, Boolean[].class, new BooleanFieldListImpl(), Boolean.class, itemsBooleanArray);
				default:
					// TODO i18n
					throw error(BAD_REQUEST, "Unknown list type {" + listType + "}");
				}
				// } else {
				// //TODO handle unexpected error
				// throw new MeshJsonException("Unknown field list class type {" + fieldSchema.getClass().getName() + "}");
				// }
			case NODE:
				if (expand) {
					NodeResponse expandedField = JsonUtil.readValue(jsonNode.toString(), NodeResponse.class);
					return (T) expandedField;
				} else {
					if (jsonNode.isNull()) {
						return (T) new NodeResponse();
					}
					NodeFieldImpl collapsedField = mapper.treeToValue(jsonNode, NodeFieldImpl.class);
					NodeResponse restNode = new NodeResponse();
					restNode.setUuid(collapsedField.getUuid());
					restNode.setPath(collapsedField.getPath());
					return (T) restNode;
				}

			case MICRONODE:
				try {
					if (jsonNode.isNull()) {
						return (T) new NullMicronodeResponse();
					} else {
						MicronodeResponse field = JsonUtil.readValue(jsonNode.toString(), MicronodeResponse.class);
						return (T) field;
					}
				} catch (IOException e) {
					// TODO i18n
					throw error(INTERNAL_SERVER_ERROR, "Could not read node field for key {" + key + "}", e);
				}
			default:
				// TODO i18n
				throw error(INTERNAL_SERVER_ERROR, "Unknown field type {" + type + "}");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <I, AT> Field getBasicList(String fieldKey, Class<AT> clazzOfJsonArray, FieldList<I> list, Class<I> classOfItem, I[] itemsArray)
			throws JsonProcessingException {
		if (itemsArray != null) {
			list.getItems().addAll((List<I>) Arrays.asList(itemsArray));
		}
		return list;
	}

	@Override
	public <T extends Field> T get(String key, Class<T> classOfT) {
		throw new NotImplementedException("not implemented");
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
	public boolean containsKey(String key) {
		return node.has(key);
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
		try {
			return getField(key, FieldTypes.STRING);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

	@Override
	public NodeFieldListImpl getNodeListField(String key) {
		return getField(key, FieldTypes.LIST, "node");
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
	public BooleanFieldListImpl getBooleanListField(String key) {
		return getField(key, FieldTypes.LIST, "boolean");
	}

	@Override
	public StringFieldListImpl getStringFieldList(String key) {
		return getField(key, FieldTypes.LIST, "string");
	}

	@Override
	public FieldList<MicronodeField> getMicronodeFieldList(String key) {
		return getField(key, FieldTypes.LIST, "micronode");
	}

	@Override
	public Field put(String fieldKey, Field field) {
		ObjectNode objectNode = ((ObjectNode) node);
		objectNode.putPOJO(fieldKey, field);
		return field;
	}

	@Override
	public NodeResponse getNodeFieldExpanded(String key) {
		return getField(key, FieldTypes.NODE, null, true);
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

}
