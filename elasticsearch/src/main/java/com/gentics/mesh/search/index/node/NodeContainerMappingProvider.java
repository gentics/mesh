package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static com.gentics.mesh.search.index.MappingHelper.ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.BOOLEAN;
import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.DOUBLE;
import static com.gentics.mesh.search.index.MappingHelper.LONG;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NESTED;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.TRIGRAM_ANALYZER;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class NodeContainerMappingProvider extends AbstractMappingProvider {

	private static final Logger log = LoggerFactory.getLogger(NodeContainerMappingProvider.class);

	@Inject
	public NodeContainerMappingProvider() {
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
	public JsonObject getMapping(Schema schema) {
		// 1. Get the common type specific mapping
		JsonObject mapping = getMapping();

		// 2. Enhance the type specific mapping
		JsonObject typeMapping = mapping.getJsonObject(DEFAULT_TYPE);
		typeMapping.put("dynamic", "strict");
		JsonObject typeProperties = typeMapping.getJsonObject("properties");

		// .project
		JsonObject projectMapping = new JsonObject();
		projectMapping.put("type", OBJECT);
		JsonObject projectMappingProps = new JsonObject();
		projectMappingProps.put("name", trigramStringType());
		projectMappingProps.put("uuid", notAnalyzedType(STRING));
		projectMapping.put("properties", projectMappingProps);
		typeProperties.put("project", projectMapping);

		// .tags
		JsonObject tagsMapping = new JsonObject();
		tagsMapping.put("type", NESTED);
		JsonObject tagsMappingProps = new JsonObject();
		tagsMappingProps.put("name", trigramStringType());
		tagsMappingProps.put("uuid", notAnalyzedType(STRING));
		tagsMapping.put("properties", tagsMappingProps);
		typeProperties.put("tags", tagsMapping);

		// .tagFamilies
		typeProperties.put("tagFamilies", new JsonObject().put("type", "object").put("dynamic", true));

		typeMapping.put("dynamic_templates", new JsonArray().add(new JsonObject().put("tagFamilyUuid", new JsonObject().put("path_match",
				"tagFamilies.*.uuid").put("match_mapping_type", "*").put("mapping", notAnalyzedType(STRING)))).add(new JsonObject().put(
						"tagFamilyTags", new JsonObject().put("path_match", "tagFamilies.*.tags").put("match_mapping_type", "*").put("mapping",
								new JsonObject().put("type", "nested").put("properties", new JsonObject().put("name", trigramStringType()).put("uuid",
										notAnalyzedType(STRING)))))));

		// .language
		typeProperties.put("language", notAnalyzedType(STRING));

		// .schema
		JsonObject schemaMapping = new JsonObject();
		schemaMapping.put("type", OBJECT);
		JsonObject schemaMappingProperties = new JsonObject();
		schemaMappingProperties.put("uuid", notAnalyzedType(STRING));
		schemaMappingProperties.put("name", trigramStringType());
		schemaMappingProperties.put("version", notAnalyzedType(STRING));
		schemaMapping.put("properties", schemaMappingProperties);
		typeProperties.put("schema", schemaMapping);

		// .displayField
		JsonObject displayFieldMapping = new JsonObject();
		displayFieldMapping.put("type", OBJECT);
		JsonObject displayFieldMappingProperties = new JsonObject();
		displayFieldMappingProperties.put("key", trigramStringType());
		displayFieldMappingProperties.put("value", trigramStringType());
		displayFieldMapping.put("properties", displayFieldMappingProperties);
		typeProperties.put("displayField", displayFieldMapping);

		// .parentNode
		JsonObject parentNodeMapping = new JsonObject();
		parentNodeMapping.put("type", OBJECT);
		JsonObject parentNodeMappingProperties = new JsonObject();
		parentNodeMappingProperties.put("uuid", notAnalyzedType(STRING));
		parentNodeMapping.put("properties", parentNodeMappingProperties);
		typeProperties.put("parentNode", parentNodeMapping);

		// Add field properties
		JsonObject fieldProps = new JsonObject();
		JsonObject fieldJson = new JsonObject();
		fieldJson.put("properties", fieldProps);
		typeProperties.put("fields", fieldJson);
		mapping.put(DEFAULT_TYPE, typeMapping);

		for (FieldSchema field : schema.getFields()) {
			JsonObject fieldInfo = getFieldMapping(field);
			fieldProps.put(field.getName(), fieldInfo);
		}
		return mapping;

	}

	/**
	 * Return the mapping JSON info for the field.
	 * 
	 * @param fieldSchema
	 *            Field schema which will be used to construct the mapping info
	 * @return JSON object which contains the mapping info
	 */
	public JsonObject getFieldMapping(FieldSchema fieldSchema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		JsonObject customIndexOptions = fieldSchema.getSearchIndex();
		JsonObject fieldInfo = new JsonObject();

		switch (type) {
		case STRING:
		case HTML:
			fieldInfo.put("type", STRING);
			fieldInfo.put("index", ANALYZED);
			fieldInfo.put("analyzer", TRIGRAM_ANALYZER);
			if (customIndexOptions != null) {
				fieldInfo.put("fields", customIndexOptions);
			}
			break;
		case BOOLEAN:
			fieldInfo.put("type", BOOLEAN);
			break;
		case DATE:
			fieldInfo.put("type", DATE);
			break;
		case BINARY:
			fieldInfo.put("type", OBJECT);
			JsonObject binaryProps = new JsonObject();
			fieldInfo.put("properties", binaryProps);

			binaryProps.put("filename", notAnalyzedType(STRING));
			binaryProps.put("filesize", notAnalyzedType(LONG));
			binaryProps.put("mimeType", notAnalyzedType(STRING));
			binaryProps.put("width", notAnalyzedType(LONG));
			binaryProps.put("height", notAnalyzedType(LONG));
			binaryProps.put("dominantColor", notAnalyzedType(STRING));
			break;
		case NUMBER:
			// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
			// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
			fieldInfo.put("type", DOUBLE);
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
					fieldInfo.put("type", STRING);
					fieldInfo.put("index", NOT_ANALYZED);
					break;
				case "date":
					fieldInfo.put("type", DATE);
					break;
				case "number":
					fieldInfo.put("type", DOUBLE);
					break;
				case "boolean":
					fieldInfo.put("type", BOOLEAN);
					break;
				case "micronode":
					fieldInfo.put("type", NESTED);
					fieldInfo.put("dynamic", true);

					// TODO
					// for(String microschema : listFieldSchema.getAllowedSchemas()) {
					// 1. Load the microschema version by name from the release to which this schema belongs
					// 2. Add the fields for the microschema to the mapping using the microschema name as a prefix for the fields
					// }
					// TODO Also add index creation to MicronodeMigrationHandler

					// fieldProps.put(field.getName(), fieldInfo);
					break;
				case "string":
					fieldInfo.put("type", STRING);
					if (customIndexOptions != null) {
						fieldInfo.put("fields", customIndexOptions);
					}
					break;
				case "html":
					fieldInfo.put("type", STRING);
					if (customIndexOptions != null) {
						fieldInfo.put("fields", customIndexOptions);
					}
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
			microschemaMappingProperties.put(NAME_KEY, trigramStringType());
			microschemaMappingProperties.put(UUID_KEY, notAnalyzedType(STRING));
			microschemaMapping.put("properties", microschemaMappingProperties);
			fieldInfo.put("dynamic", true);

			// TODO
			// for(String microschema : ((MicronodeFieldSchema)fieldSchema).getAllowedMicroSchemas()) {
			// 1. Load the microschema version by name from the release to which this schema belongs
			// 2. Add the fields for the microschema to the mapping using the microschema name as a prefix for the fields
			// }
			// TODO Also add index creation to MicronodeMigrationHandler

			// TODO add version
			fieldInfo.put("properties", micronodeMappingProperties);
			break;
		default:
			throw new RuntimeException("Mapping type  for field type {" + type + "} unknown.");
		}
		return fieldInfo;
	}
}
