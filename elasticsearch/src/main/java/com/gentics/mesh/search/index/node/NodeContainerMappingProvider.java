package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static com.gentics.mesh.search.index.MappingHelper.BOOLEAN;
import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.DOUBLE;
import static com.gentics.mesh.search.index.MappingHelper.INDEX_VALUE;
import static com.gentics.mesh.search.index.MappingHelper.KEYWORD;
import static com.gentics.mesh.search.index.MappingHelper.LONG;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NESTED;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.TEXT;
import static com.gentics.mesh.search.index.MappingHelper.TRIGRAM_ANALYZER;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramTextType;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.search.index.AbstractMappingProvider;
import com.google.common.collect.Sets;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class NodeContainerMappingProvider extends AbstractMappingProvider {

	private static final Logger log = LoggerFactory.getLogger(NodeContainerMappingProvider.class);

	@Inject
	public BootstrapInitializer boot;
	
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
	 * @param schema Schema from which the mapping should be constructed
	 * @param release The release-version which should be used for the construction
	 * @return An ES-Mapping for the given Schema in the Release
	 */
	public JsonObject getMapping(Schema schema, Release release) {
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
		projectMappingProps.put("name", trigramTextType());
		projectMappingProps.put("uuid", notAnalyzedType(KEYWORD));
		projectMapping.put("properties", projectMappingProps);
		typeProperties.put("project", projectMapping);

		// .tags
		JsonObject tagsMapping = new JsonObject();
		tagsMapping.put("type", NESTED);
		JsonObject tagsMappingProps = new JsonObject();
		tagsMappingProps.put("name", trigramTextType());
		tagsMappingProps.put("uuid", notAnalyzedType(KEYWORD));
		tagsMapping.put("properties", tagsMappingProps);
		typeProperties.put("tags", tagsMapping);

		// .tagFamilies
		typeProperties.put("tagFamilies", new JsonObject().put("type", "object").put("dynamic", true));

		typeMapping.put("dynamic_templates", new JsonArray().add(new JsonObject().put("tagFamilyUuid", new JsonObject().put("path_match",
			"tagFamilies.*.uuid").put("match_mapping_type", "*").put("mapping", notAnalyzedType(KEYWORD)))).add(new JsonObject().put(
				"tagFamilyTags", new JsonObject().put("path_match", "tagFamilies.*.tags").put("match_mapping_type", "*").put("mapping",
					new JsonObject().put("type", "nested").put("properties", new JsonObject().put("name", trigramTextType()).put("uuid",
						notAnalyzedType(KEYWORD)))))));

		// .language
		typeProperties.put("language", notAnalyzedType(KEYWORD));

		// .schema
		JsonObject schemaMapping = new JsonObject();
		schemaMapping.put("type", OBJECT);
		JsonObject schemaMappingProperties = new JsonObject();
		schemaMappingProperties.put("uuid", notAnalyzedType(KEYWORD));
		schemaMappingProperties.put("name", trigramTextType());
		schemaMappingProperties.put("version", notAnalyzedType(KEYWORD));
		schemaMapping.put("properties", schemaMappingProperties);
		typeProperties.put("schema", schemaMapping);

		// .displayField
		JsonObject displayFieldMapping = new JsonObject();
		displayFieldMapping.put("type", OBJECT);
		JsonObject displayFieldMappingProperties = new JsonObject();
		displayFieldMappingProperties.put("key", trigramTextType());
		displayFieldMappingProperties.put("value", trigramTextType());
		displayFieldMapping.put("properties", displayFieldMappingProperties);
		typeProperties.put("displayField", displayFieldMapping);

		// .parentNode
		JsonObject parentNodeMapping = new JsonObject();
		parentNodeMapping.put("type", OBJECT);
		JsonObject parentNodeMappingProperties = new JsonObject();
		parentNodeMappingProperties.put("uuid", notAnalyzedType(KEYWORD));
		parentNodeMapping.put("properties", parentNodeMappingProperties);
		typeProperties.put("parentNode", parentNodeMapping);

		// Add field properties
		JsonObject fieldProps = new JsonObject();
		JsonObject fieldJson = new JsonObject();
		fieldJson.put("properties", fieldProps);
		typeProperties.put("fields", fieldJson);
		mapping.put(DEFAULT_TYPE, typeMapping);

		for (FieldSchema field : schema.getFields()) {
			JsonObject fieldInfo = getFieldMapping(field, release);
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
	public JsonObject getFieldMapping(FieldSchema fieldSchema, Release release) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		JsonObject customIndexOptions = fieldSchema.getElasticsearch();
		JsonObject fieldInfo = new JsonObject();

		switch (type) {
		case STRING:
		case HTML:
			fieldInfo.put("type", TEXT);
			fieldInfo.put("index", INDEX_VALUE);
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

			binaryProps.put("sha512sum", notAnalyzedType(KEYWORD));
			binaryProps.put("filename", notAnalyzedType(KEYWORD));
			binaryProps.put("filesize", notAnalyzedType(LONG));
			binaryProps.put("mimeType", notAnalyzedType(KEYWORD));
			binaryProps.put("width", notAnalyzedType(LONG));
			binaryProps.put("height", notAnalyzedType(LONG));
			binaryProps.put("dominantColor", notAnalyzedType(KEYWORD));
			break;
		case NUMBER:
			// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
			// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
			fieldInfo.put("type", DOUBLE);
			break;
		case NODE:
			fieldInfo.put("type", KEYWORD);
			fieldInfo.put("index", INDEX_VALUE);
			break;
		case LIST:
			if (fieldSchema instanceof ListFieldSchemaImpl) {
				ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
				switch (listFieldSchema.getListType()) {
				case "node":
					fieldInfo.put("type", KEYWORD);
					fieldInfo.put("index", INDEX_VALUE);
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
					
					// All allowed microschemas
					String[] allowed = listFieldSchema.getAllowedSchemas();

					// Merge the options into the info
					fieldInfo.mergeIn(getMicroschemaMappingOptions(allowed, release));

					// fieldProps.put(field.getName(), fieldInfo);
					break;
				case "string":
					fieldInfo.put("type", TEXT);
					if (customIndexOptions != null) {
						fieldInfo.put("fields", customIndexOptions);
					}
					break;
				case "html":
					fieldInfo.put("type", TEXT);
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
			
			// Cast to MicronodeFieldSchema should be safe as it's a Micronode-Field
			String[] allowed = ((MicronodeFieldSchema) fieldSchema).getAllowedMicroSchemas();	
			
			// Merge the options into the info
			fieldInfo.mergeIn(getMicroschemaMappingOptions(allowed, release));
			break;
		default:
			throw new RuntimeException("Mapping type  for field type {" + type + "} unknown.");
		}
		return fieldInfo;
	}
	
	/**
	 * Creates an Elasticsearch mapping for all allowed microschemas in the given release.
	 * When the allowed are empty/null, it'll generate the mapping for all microschemas in the release.
	 * 
	 * @param allowed Restriction to which microschemas are allowed to be saved in the field
	 * @param release The release for which the mapping should be created
	 * @return An Properties-mapping for a microschema field.
	 */
	public JsonObject getMicroschemaMappingOptions(String[] allowed, Release release) {
		// Properties-Settings
		JsonObject properties = new JsonObject();
		properties.put(NAME_KEY, trigramTextType());
		properties.put(UUID_KEY, notAnalyzedType(KEYWORD));
		properties.put("version", notAnalyzedType(KEYWORD));
		
		// Final Object which will be returned
		JsonObject options = new JsonObject().put("properties", properties);
		
		// A Set-Instance of the allowed microschema-names
		Set<String> whitelist = Sets.newHashSet(allowed);
		
		// If the release is given and the whitelist has entries.
		// Otherwise indexing doesn't make any sense and therefore has to be
		// made dynamically like before.
		boolean shouldFilter = release != null && !whitelist.isEmpty();
		
		if (shouldFilter) {			
			for (MicroschemaContainerVersion version : release.findAllMicroschemaVersions()) {
				MicroschemaModel model = version.getSchema();
				String name = model.getName();
				
				// Check if the model is contained in the whitelist (if it exists)
				// if not, ignore it
				if (!whitelist.contains(name)) {
					continue;
				}
				
				JsonObject fields = new JsonObject();
				for (FieldSchema field : model.getFields()) {
					String fieldName = field.getName();
					// Check if the type is a micronode, and if it is, then skip it
					if (!FieldTypes.valueByName(fieldName).equals(FieldTypes.MICRONODE)) {
						continue;
					}
					fields.put(fieldName, this.getFieldMapping(field, release));
				}
				properties.put("fields-" + name, fields);
			}
		}
		
		options.put("dynamic", shouldFilter);
		
		return options;
	}
}