package com.gentics.mesh.search.impl;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
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
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public abstract class AbstractSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	@Override
	public void addFields(JsonObject document, GraphFieldContainer container, List<? extends FieldSchema> fields) {
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
								addFields(itemMap, micronode, micronode.getSchemaContainerVersion().getSchema().getFields());
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
						addFields(micronodeMap, micronode, micronode.getSchemaContainerVersion().getSchema().getFields());
						fieldsMap.put(fieldSchema.getName(), micronodeMap);
					}
				}
				break;
			default:
				// TODO error?
				break;
			}

		}
		document.put("fields", fieldsMap);

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

	@Override
	public JsonObject getMappingInfo(FieldSchema fieldSchema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());

		JsonObject fieldInfo = new JsonObject();

		switch (type) {
		case STRING:
			fieldInfo.put("type", "string");
			addRawInfo(fieldInfo, "string");
			break;
		case HTML:
			fieldInfo.put("type", "string");
			addRawInfo(fieldInfo, "string");
			break;
		case BOOLEAN:
			fieldInfo.put("type", "boolean");
			//addRawInfo(fieldInfo, "boolean");
			break;
		case DATE:
			fieldInfo.put("type", "date");
			//addRawInfo(fieldInfo, "date");
			break;
		case BINARY:
			fieldInfo.put("type", "object");
			JsonObject binaryProps = new JsonObject();
			fieldInfo.put("properties", binaryProps);

			// filename
			JsonObject filenameInfo = new JsonObject();
			filenameInfo.put("type", "string");
			filenameInfo.put("index", "not_analyzed");
			binaryProps.put("filename", filenameInfo);

			// filesize
			JsonObject filesizeInfo = new JsonObject();
			filesizeInfo.put("type", "long");
			filesizeInfo.put("index", "not_analyzed");
			binaryProps.put("filesize", filesizeInfo);

			// mimeType
			JsonObject mimeTypeInfo = new JsonObject();
			mimeTypeInfo.put("type", "string");
			mimeTypeInfo.put("index", "not_analyzed");
			binaryProps.put("mimeType", mimeTypeInfo);

			// imageWidth
			JsonObject imageWidthInfo = new JsonObject();
			imageWidthInfo.put("type", "long");
			imageWidthInfo.put("index", "not_analyzed");
			binaryProps.put("width", imageWidthInfo);

			// imageHeight
			JsonObject imageHeightInfo = new JsonObject();
			imageHeightInfo.put("type", "long");
			imageHeightInfo.put("index", "not_analyzed");
			binaryProps.put("height", imageHeightInfo);

			// dominantColor
			JsonObject dominantColorInfo = new JsonObject();
			dominantColorInfo.put("type", "string");
			dominantColorInfo.put("index", "not_analyzed");
			binaryProps.put("dominantColor", dominantColorInfo);
			break;
		case NUMBER:
			// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
			// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
			fieldInfo.put("type", "double");
			break;
		case NODE:
			fieldInfo.put("type", "string");
			fieldInfo.put("index", "not_analyzed");
			break;
		case LIST:
			if (fieldSchema instanceof ListFieldSchemaImpl) {
				ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
				switch (listFieldSchema.getListType()) {
				case "node":
					fieldInfo.put("type", "nested");
					break;
				case "date":
					fieldInfo.put("type", "nested");
					break;
				case "number":
					fieldInfo.put("type", "nested");
					break;
				case "boolean":
					fieldInfo.put("type", "nested");
					break;
				case "micronode":
					fieldInfo.put("type", "nested");
					//fieldProps.put(field.getName(), fieldInfo);
					break;
				case "string":
					fieldInfo.put("type", "nested");
					break;
				case "html":
					fieldInfo.put("type", "nested");
					break;
				default:
					log.error("Unknown list type {" + listFieldSchema.getListType() + "}");
					throw new RuntimeException("Mapping type  for field type {" + type + "} unknown.");
				}
			}
			break;
		case MICRONODE:
			fieldInfo.put("type", "object");
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
		document.put("microschema", info);
	}

}
