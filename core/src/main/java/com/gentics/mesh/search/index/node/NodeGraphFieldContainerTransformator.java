package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.search.index.MappingHelper.BOOLEAN;
import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.LONG;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NESTED;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * Transformator which can be used to transform a {@link NodeGraphFieldContainer} into a elasticsearch document. Additionally the matching mapping can also be
 * generated using this class.
 */
public class NodeGraphFieldContainerTransformator extends AbstractTransformator<NodeGraphFieldContainer> {

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerTransformator.class);

	private static final String VERSION_KEY = "version";

	/**
	 * Transform the given schema and add it to the source map.
	 * 
	 * @param document
	 * @param schemaContainerVersion
	 */
	private void addSchema(JsonObject document, SchemaContainerVersion schemaContainerVersion) {
		String name = schemaContainerVersion.getName();
		String uuid = schemaContainerVersion.getSchemaContainer().getUuid();
		Map<String, String> schemaFields = new HashMap<>();
		schemaFields.put(NAME_KEY, name);
		schemaFields.put(UUID_KEY, uuid);
		schemaFields.put(VERSION_KEY, String.valueOf(schemaContainerVersion.getVersion()));
		document.put("schema", schemaFields);
	}

	/**
	 * Use the given node to populate the parent node fields within the source map.
	 * 
	 * @param document
	 * @param parentNode
	 */
	private void addParentNodeInfo(JsonObject document, Node parentNode) {
		JsonObject info = new JsonObject();
		info.put(UUID_KEY, parentNode.getUuid());
		// TODO check whether nesting of nested elements would also work
		// TODO FIXME MIGRATE: How to add this reference info? The schema is now linked to the node. Should we add another reference:
		// (n:Node)->(sSchemaContainer) ?
		// parentNodeInfo.put("schema.name", parentNode.getSchemaContainer().getName());
		// parentNodeInfo.put("schema.uuid", parentNode.getSchemaContainer().getUuid());
		document.put("parentNode", info);
	}

	/**
	 * Add node fields to the given source map.
	 * 
	 * @param document
	 *            Search index document
	 * @param fieldKey
	 *            Key to be used to store the fields (e.g.: fields)
	 * @param container
	 *            Node field container
	 * @param fields
	 *            List of schema fields that should be handled
	 */
	public void addFields(JsonObject document, String fieldKey, GraphFieldContainer container, List<? extends FieldSchema> fields) {
		Map<String, Object> fieldsMap = new HashMap<>();
		for (FieldSchema fieldSchema : fields) {
			String name = fieldSchema.getName();
			FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());

			switch (type) {
			case STRING:
				StringGraphField stringField = container.getString(name);
				if (stringField != null) {
					fieldsMap.put(name, stringField.getString());
				}
				break;
			case HTML:
				HtmlGraphField htmlField = container.getHtml(name);
				if (htmlField != null) {
					fieldsMap.put(name, htmlField.getHTML());
				}
				break;
			case BINARY:
				BinaryGraphField binaryField = container.getBinary(name);
				if (binaryField != null) {
					JsonObject binaryFieldInfo = new JsonObject();
					fieldsMap.put(name, binaryFieldInfo);
					binaryFieldInfo.put("filename", binaryField.getFileName());
					binaryFieldInfo.put("filesize", binaryField.getFileSize());
					binaryFieldInfo.put("width", binaryField.getImageWidth());
					binaryFieldInfo.put("height", binaryField.getImageHeight());
					binaryFieldInfo.put("mimeType", binaryField.getMimeType());
					binaryFieldInfo.put("dominantColor", binaryField.getImageDominantColor());
				}
				break;
			case BOOLEAN:
				BooleanGraphField booleanField = container.getBoolean(name);
				if (booleanField != null) {
					fieldsMap.put(name, booleanField.getBoolean());
				}
				break;
			case DATE:
				DateGraphField dateField = container.getDate(name);
				if (dateField != null) {
					fieldsMap.put(name, dateField.getDate());
				}
				break;
			case NUMBER:
				NumberGraphField numberField = container.getNumber(name);
				if (numberField != null) {

					// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
					// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
					fieldsMap.put(name, numberField.getNumber());
				}
				break;
			case NODE:
				NodeGraphField nodeField = container.getNode(name);
				if (nodeField != null) {
					fieldsMap.put(name, nodeField.getNode().getUuid());
				}
				break;
			case LIST:
				if (fieldSchema instanceof ListFieldSchemaImpl) {
					ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
					switch (listFieldSchema.getListType()) {
					case "node":
						NodeGraphFieldList graphNodeList = container.getNodeList(fieldSchema.getName());
						if (graphNodeList != null) {
							List<String> nodeItems = new ArrayList<>();
							for (NodeGraphField listItem : graphNodeList.getList()) {
								nodeItems.add(listItem.getNode().getUuid());
							}
							fieldsMap.put(fieldSchema.getName(), nodeItems);
						}
						break;
					case "date":
						DateGraphFieldList graphDateList = container.getDateList(fieldSchema.getName());
						if (graphDateList != null) {
							List<Long> dateItems = new ArrayList<>();
							for (DateGraphField listItem : graphDateList.getList()) {
								dateItems.add(listItem.getDate());
							}
							fieldsMap.put(fieldSchema.getName(), dateItems);
						}
						break;
					case "number":
						NumberGraphFieldList graphNumberList = container.getNumberList(fieldSchema.getName());
						if (graphNumberList != null) {
							List<Number> numberItems = new ArrayList<>();
							for (NumberGraphField listItem : graphNumberList.getList()) {
								// TODO Number can also be a big decimal. We need to convert those special objects into basic numbers or else ES will not be
								// able to store them
								numberItems.add(listItem.getNumber());
							}
							fieldsMap.put(fieldSchema.getName(), numberItems);
						}
						break;
					case "boolean":
						BooleanGraphFieldList graphBooleanList = container.getBooleanList(fieldSchema.getName());
						if (graphBooleanList != null) {
							List<String> booleanItems = new ArrayList<>();
							for (BooleanGraphField listItem : graphBooleanList.getList()) {
								booleanItems.add(String.valueOf(listItem.getBoolean()));
							}
							fieldsMap.put(fieldSchema.getName(), booleanItems);
						}
						break;
					case "micronode":
						MicronodeGraphFieldList micronodeGraphFieldList = container.getMicronodeList(fieldSchema.getName());
						if (micronodeGraphFieldList != null) {
							// add list of micronode objects
							fieldsMap.put(fieldSchema.getName(), Observable.from(micronodeGraphFieldList.getList()).map(item -> {
								JsonObject itemMap = new JsonObject();
								Micronode micronode = item.getMicronode();
								addMicroschema(itemMap, micronode.getSchemaContainerVersion());
								addFields(itemMap, "fields-" + micronode.getSchemaContainerVersion().getName(), micronode,
										micronode.getSchemaContainerVersion().getSchema().getFields());
								return itemMap;
							}).toList().toBlocking().single());
						}
						break;
					case "string":
						StringGraphFieldList graphStringList = container.getStringList(fieldSchema.getName());
						if (graphStringList != null) {
							List<String> stringItems = new ArrayList<>();
							for (StringGraphField listItem : graphStringList.getList()) {
								stringItems.add(listItem.getString());
							}
							fieldsMap.put(fieldSchema.getName(), stringItems);
						}
						break;
					case "html":
						HtmlGraphFieldList graphHtmlList = container.getHTMLList(fieldSchema.getName());
						if (graphHtmlList != null) {
							List<String> htmlItems = new ArrayList<>();
							for (HtmlGraphField listItem : graphHtmlList.getList()) {
								htmlItems.add(listItem.getHTML());
							}
							fieldsMap.put(fieldSchema.getName(), htmlItems);
						}
						break;
					default:
						log.error("Unknown list type {" + listFieldSchema.getListType() + "}");
						break;
					}
				}
				// container.getStringList(fieldKey)
				// ListField listField = container.getN(name);
				// fieldsMap.put(name, htmlField.getHTML());
				break;
			case MICRONODE:

				MicronodeGraphField micronodeGraphField = container.getMicronode(fieldSchema.getName());
				if (micronodeGraphField != null) {
					Micronode micronode = micronodeGraphField.getMicronode();
					if (micronode != null) {
						JsonObject micronodeMap = new JsonObject();
						addMicroschema(micronodeMap, micronode.getSchemaContainerVersion());
						// Micronode field can't be stored. The datastructure is dynamic
						addFields(micronodeMap, "fields-" + micronode.getSchemaContainerVersion().getName(), micronode,
								micronode.getSchemaContainerVersion().getSchema().getFields());
						fieldsMap.put(fieldSchema.getName(), micronodeMap);
					}
				}
				break;
			default:
				// TODO error?
				break;
			}

		}
		document.put(fieldKey, fieldsMap);

	}

	/**
	 * Add the raw field info to the given mapping element.
	 * 
	 * @param fieldInfo
	 * @param mappingType
	 */
	private void addRawInfo(JsonObject fieldInfo, String mappingType) {
		JsonObject rawInfo = new JsonObject();
		rawInfo.put("type", mappingType);
		rawInfo.put("index", "not_analyzed");
		JsonObject rawFieldInfo = new JsonObject();
		rawFieldInfo.put("raw", rawInfo);
		fieldInfo.put("fields", rawFieldInfo);
	}

	/**
	 * Return the mapping JSON info for the field.
	 * 
	 * @param fieldSchema
	 *            Field schema which will be used to construct the mapping info
	 * @return JSON object which contains the mapping info
	 */
	public JsonObject getMappingInfo(FieldSchema fieldSchema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());

		JsonObject fieldInfo = new JsonObject();

		switch (type) {
		case STRING:
			fieldInfo.put("type", STRING);
			addRawInfo(fieldInfo, "string");
			break;
		case HTML:
			fieldInfo.put("type", STRING);
			addRawInfo(fieldInfo, "string");
			break;
		case BOOLEAN:
			fieldInfo.put("type", BOOLEAN);
			// addRawInfo(fieldInfo, "boolean");
			break;
		case DATE:
			fieldInfo.put("type", DATE);
			// addRawInfo(fieldInfo, "date");
			break;
		case BINARY:
			fieldInfo.put("type", OBJECT);
			JsonObject binaryProps = new JsonObject();
			fieldInfo.put("properties", binaryProps);

			// filename
			JsonObject filenameInfo = new JsonObject();
			filenameInfo.put("type", "string");
			filenameInfo.put("index", NOT_ANALYZED);
			binaryProps.put("filename", filenameInfo);

			// filesize
			JsonObject filesizeInfo = new JsonObject();
			filesizeInfo.put("type", "long");
			filesizeInfo.put("index", NOT_ANALYZED);
			binaryProps.put("filesize", filesizeInfo);

			// mimeType
			JsonObject mimeTypeInfo = new JsonObject();
			mimeTypeInfo.put("type", "string");
			mimeTypeInfo.put("index", NOT_ANALYZED);
			binaryProps.put("mimeType", mimeTypeInfo);

			// imageWidth
			JsonObject imageWidthInfo = new JsonObject();
			imageWidthInfo.put("type", "long");
			imageWidthInfo.put("index", NOT_ANALYZED);
			binaryProps.put("width", imageWidthInfo);

			// imageHeight
			JsonObject imageHeightInfo = new JsonObject();
			imageHeightInfo.put("type", LONG);
			imageHeightInfo.put("index", NOT_ANALYZED);
			binaryProps.put("height", imageHeightInfo);

			// dominantColor
			JsonObject dominantColorInfo = new JsonObject();
			dominantColorInfo.put("type", STRING);
			dominantColorInfo.put("index", NOT_ANALYZED);
			binaryProps.put("dominantColor", dominantColorInfo);
			break;
		case NUMBER:
			// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
			// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
			fieldInfo.put("type", "double");
			break;
		case NODE:
			fieldInfo.put("type", STRING);
			fieldInfo.put("index", NOT_ANALYZED);
			break;
		case LIST:
			if (fieldSchema instanceof ListFieldSchemaImpl) {
				ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
				switch (listFieldSchema.getListType()) {
				case "node":
					fieldInfo.put("type", NESTED);
					break;
				case "date":
					fieldInfo.put("type", NESTED);
					break;
				case "number":
					fieldInfo.put("type", NESTED);
					break;
				case "boolean":
					fieldInfo.put("type", NESTED);
					break;
				case "micronode":
					fieldInfo.put("type", NESTED);
					fieldInfo.put("dynamic", true);
					// fieldProps.put(field.getName(), fieldInfo);
					break;
				case "string":
					fieldInfo.put("type", NESTED);
					break;
				case "html":
					fieldInfo.put("type", NESTED);
					break;
				default:
					log.error("Unknown list type {" + listFieldSchema.getListType() + "}");
					throw new RuntimeException("Mapping type  for field type {" + type + "} unknown.");
				}
			}
			break;
		case MICRONODE:
			fieldInfo.put("type", OBJECT);
			JsonObject micronodeMappingProperties = new JsonObject();

			// microschema
			JsonObject microschemaMapping = new JsonObject();
			micronodeMappingProperties.put("microschema", microschemaMapping);

			JsonObject microschemaMappingProperties = new JsonObject();
			microschemaMappingProperties.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
			microschemaMappingProperties.put(UUID_KEY, fieldType(STRING, NOT_ANALYZED));
			microschemaMapping.put("properties", microschemaMappingProperties);
			fieldInfo.put("dynamic", true);
			// TODO add version
			fieldInfo.put("properties", micronodeMappingProperties);
			break;
		default:
			throw new RuntimeException("Mapping type  for field type {" + type + "} unknown.");
		}

		return fieldInfo;
	}

	/**
	 * Transform the given microschema container and add it to the source map.
	 * 
	 * @param map
	 * @param microschemaContainerVersion
	 */
	private void addMicroschema(JsonObject document, MicroschemaContainerVersion microschemaContainerVersion) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, microschemaContainerVersion.getName());
		info.put(UUID_KEY, microschemaContainerVersion.getUuid());
		// TODO add version
		document.put("microschema", info);
	}

	/**
	 * It is required to specify the releaseUuid in order to transform containers.
	 * 
	 * @deprecated
	 */
	@Override
	@Deprecated
	public JsonObject toDocument(NodeGraphFieldContainer object) {
		throw new NotImplementedException("Use toDocument(container, releaseUuid) instead");
	}

	public JsonObject toDocument(NodeGraphFieldContainer container, String releaseUuid) {
		Node node = container.getParentNode();
		JsonObject document = new JsonObject();
		document.put("uuid", node.getUuid());
		addUser(document, "editor", container.getEditor());
		document.put("edited", toISO8601(container.getLastEditedTimestamp()));
		addUser(document, "creator", node.getCreator());
		document.put("created", toISO8601(node.getCreationTimestamp()));

		addProject(document, node.getProject());
		addTags(document, node.getTags(node.getProject().getLatestRelease()));

		// The basenode has no parent.
		if (node.getParentNode(releaseUuid) != null) {
			addParentNodeInfo(document, node.getParentNode(releaseUuid));
		}

		String language = container.getLanguage().getLanguageTag();
		document.put("language", language);
		addSchema(document, container.getSchemaContainerVersion());

		addFields(document, "fields", container, container.getSchemaContainerVersion().getSchema().getFields());
		if (log.isTraceEnabled()) {
			String json = document.toString();
			log.trace("Search index json:");
			log.trace(json);
		}

		// Add display field value
		JsonObject displayField = new JsonObject();
		displayField.put("key", container.getSchemaContainerVersion().getSchema().getDisplayField());
		displayField.put("value", container.getDisplayFieldValue());
		return document.put("displayField", displayField);
	}

	@Override
	public JsonObject getMappingProperties() {
		return new JsonObject();
	}

	/**
	 * Return the type specific mapping which is constructed using the provided schema.
	 * 
	 * @param schema
	 * @param type
	 * @return
	 */
	public JsonObject getMapping(Schema schema, String type) {
		// 1. Get the common type specific mapping
		JsonObject mapping = getMapping(type);

		// 2. Enhance the type specific mapping
		JsonObject typeMapping = mapping.getJsonObject(type);
		typeMapping.put("dynamic", "strict");
		JsonObject typeProperties = typeMapping.getJsonObject("properties");

		// project
		JsonObject projectMapping = new JsonObject();
		projectMapping.put("type", OBJECT);
		JsonObject projectMappingProps = new JsonObject();
		projectMappingProps.put("name", fieldType(STRING, NOT_ANALYZED));
		projectMappingProps.put("uuid", fieldType(STRING, NOT_ANALYZED));
		projectMapping.put("properties", projectMappingProps);
		typeProperties.put("project", projectMapping);

		// tags
		JsonObject tagsMapping = new JsonObject();
		tagsMapping.put("type", "nested");
		tagsMapping.put("dynamic", true);
		typeProperties.put("tags", tagsMapping);

		// language
		JsonObject languageMapping = fieldType(STRING, NOT_ANALYZED);
		typeProperties.put("language", languageMapping);

		// schema
		JsonObject schemaMapping = new JsonObject();
		schemaMapping.put("type", OBJECT);
		JsonObject schemaMappingProperties = new JsonObject();
		schemaMappingProperties.put("uuid", fieldType(STRING, NOT_ANALYZED));
		schemaMappingProperties.put("name", fieldType(STRING, NOT_ANALYZED));
		schemaMappingProperties.put("version", fieldType(LONG, NOT_ANALYZED));
		schemaMapping.put("properties", schemaMappingProperties);
		typeProperties.put("schema", schemaMapping);

		// displayField
		JsonObject displayFieldMapping = new JsonObject();
		displayFieldMapping.put("type", OBJECT);
		JsonObject displayFieldMappingProperties = new JsonObject();
		displayFieldMappingProperties.put("key", fieldType(STRING, NOT_ANALYZED));
		displayFieldMappingProperties.put("value", fieldType(STRING, NOT_ANALYZED));
		displayFieldMapping.put("properties", displayFieldMappingProperties);
		typeProperties.put("displayField", displayFieldMapping);

		// parentNode
		JsonObject parentNodeMapping = new JsonObject();
		parentNodeMapping.put("type", OBJECT);
		JsonObject parentNodeMappingProperties = new JsonObject();
		parentNodeMappingProperties.put("uuid", fieldType(STRING, NOT_ANALYZED));
		parentNodeMapping.put("properties", parentNodeMappingProperties);
		typeProperties.put("parentNode", parentNodeMapping);

		// Add field properties
		JsonObject fieldProps = new JsonObject();
		JsonObject fieldJson = new JsonObject();
		fieldJson.put("properties", fieldProps);
		typeProperties.put("fields", fieldJson);
		mapping.put(type, typeMapping);

		for (FieldSchema field : schema.getFields()) {
			JsonObject fieldInfo = getMappingInfo(field);
			fieldProps.put(field.getName(), fieldInfo);
		}
		return mapping;

	}

}
