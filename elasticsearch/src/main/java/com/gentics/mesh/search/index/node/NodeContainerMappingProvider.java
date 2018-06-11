package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static com.gentics.mesh.search.index.MappingHelper.BINARY;
import static com.gentics.mesh.search.index.MappingHelper.BOOLEAN;
import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.DONT_INDEX_VALUE;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
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
	 * @param schema
	 *            Schema from which the mapping should be constructed
	 * @return An ES-Mapping for the given Schema
	 */
	public JsonObject getMapping(Schema schema) {
		return getMapping(schema, null);
	}

	/**
	 * Return the type specific mapping which is constructed using the provided schema.
	 * 
	 * @param schema
	 *            Schema from which the mapping should be constructed
	 * @param branch
	 *            The branch-version which should be used for the construction
	 * @return An ES-Mapping for the given Schema in the branch
	 */
	public JsonObject getMapping(Schema schema, Branch branch) {
		// 1. Get the common type specific mapping
		JsonObject mapping = getMapping();

		addBinaryFieldExcludes(mapping, schema);

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
			JsonObject fieldInfo = getFieldMapping(field, branch);
			fieldProps.put(field.getName(), fieldInfo);
		}
		return mapping;

	}

	/**
	 * Add custom exclude for the source document. We don't want to store the base64 encoded data fields.
	 * @param mapping
	 * @param schema
	 */
	private void addBinaryFieldExcludes(JsonObject mapping, Schema schema) {
		JsonObject sourceInfo = new JsonObject();

		JsonArray excludes = new JsonArray();
		List<String> binaryFields = schema.getFields().stream().filter(f -> f.getType().equals("binary")).map(f -> f.getType())
			.collect(Collectors.toList());
		for (String field : binaryFields) {
			excludes.add("fields." + field + ".data");
		}
		sourceInfo.put("excludes", excludes);
		mapping.getJsonObject(DEFAULT_TYPE).put("_source", sourceInfo);
	}

	/**
	 * Return the mapping JSON info for the field.
	 * 
	 * @param fieldSchema
	 *            Field schema which will be used to construct the mapping info
	 * @return JSON object which contains the mapping info
	 */
	public JsonObject getFieldMapping(FieldSchema fieldSchema, Branch branch) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		JsonObject customIndexOptions = fieldSchema.getElasticsearch();
		JsonObject fieldInfo = new JsonObject();

		switch (type) {
		case STRING:
		case HTML:
			addStringFieldMapping(fieldInfo, customIndexOptions);
			break;
		case BOOLEAN:
			addBooleanFieldMapping(fieldInfo);
			break;
		case DATE:
			addDataFieldMapping(fieldInfo);
			break;
		case BINARY:
			addBinaryFieldMapping(fieldInfo, customIndexOptions);
			break;
		case NUMBER:
			addNumberFieldMapping(fieldInfo);
			break;
		case NODE:
			addNodeMapping(fieldInfo);
			break;
		case LIST:
			if (fieldSchema instanceof ListFieldSchemaImpl) {
				addListFieldMapping(fieldInfo, branch, (ListFieldSchemaImpl) fieldSchema, customIndexOptions);
			}
			break;
		case MICRONODE:
			addMicronodeMapping(fieldInfo, fieldSchema, branch);
			break;
		default:
			throw new RuntimeException("Mapping type  for field type {" + type + "} unknown.");
		}
		return fieldInfo;
	}

	private void addBooleanFieldMapping(JsonObject fieldInfo) {
		fieldInfo.put("type", BOOLEAN);
	}

	private void addDataFieldMapping(JsonObject fieldInfo) {
		fieldInfo.put("type", DATE);
	}

	private void addNumberFieldMapping(JsonObject fieldInfo) {
		// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
		// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
		fieldInfo.put("type", DOUBLE);
	}

	private void addMicronodeMapping(JsonObject fieldInfo, FieldSchema fieldSchema, Branch branch) {
		fieldInfo.put("type", OBJECT);

		// Cast to MicronodeFieldSchema should be safe as it's a Micronode-Field
		String[] allowed = ((MicronodeFieldSchema) fieldSchema).getAllowedMicroSchemas();

		// Merge the options into the info
		fieldInfo.mergeIn(getMicroschemaMappingOptions(allowed, branch));
	}

	private void addNodeMapping(JsonObject fieldInfo) {
		fieldInfo.put("type", KEYWORD);
		fieldInfo.put("index", INDEX_VALUE);
	}

	private void addListFieldMapping(JsonObject fieldInfo, Branch branch, ListFieldSchemaImpl fieldSchema, JsonObject customIndexOptions) {
		ListFieldSchemaImpl listFieldSchema = (ListFieldSchemaImpl) fieldSchema;
		String type = listFieldSchema.getListType();
		switch (type) {
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
			fieldInfo.mergeIn(getMicroschemaMappingOptions(allowed, branch));

			// fieldProps.put(field.getName(), fieldInfo);
			break;
		case "string":
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

	private void addStringFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		fieldInfo.put("type", TEXT);
		fieldInfo.put("index", INDEX_VALUE);
		fieldInfo.put("analyzer", TRIGRAM_ANALYZER);
		if (customIndexOptions != null) {
			fieldInfo.put("fields", customIndexOptions);
		}
	}

	private void addBinaryFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		fieldInfo.put("type", OBJECT);
		JsonObject binaryProps = new JsonObject();
		fieldInfo.put("properties", binaryProps);

		// .sha512sum
		binaryProps.put("sha512sum", notAnalyzedType(KEYWORD));

		// .filename
		JsonObject customFilenameMapping = null;
		if (customIndexOptions != null && customIndexOptions.containsKey("filename")) {
			customFilenameMapping = customIndexOptions.getJsonObject("filename");
		}
		binaryProps.put("filename", notAnalyzedType(KEYWORD, customFilenameMapping));

		// .filesize
		binaryProps.put("filesize", notAnalyzedType(LONG));

		// .mimeType
		JsonObject customMimeTypeMapping = null;
		if (customIndexOptions != null && customIndexOptions.containsKey("mimeType")) {
			customMimeTypeMapping = customIndexOptions.getJsonObject("mimeType");
		}
		binaryProps.put("mimeType", notAnalyzedType(KEYWORD, customMimeTypeMapping));

		// .width
		binaryProps.put("width", notAnalyzedType(LONG));

		// .height
		binaryProps.put("height", notAnalyzedType(LONG));

		// .dominantColor
		binaryProps.put("dominantColor", notAnalyzedType(KEYWORD));

		// Add mapping for fields which were added by the ingest plugin
		addBinaryFieldIngestMapping(binaryProps, customIndexOptions);
	}

	private void addBinaryFieldIngestMapping(JsonObject binaryProps, JsonObject customIndexOptions) {
		// The data field must not be indexed. It is just provided for the ingest pipeline
		// .data (for input only)
		JsonObject ignoreField = new JsonObject();
		ignoreField.put("type", BINARY);
		ignoreField.put("index", DONT_INDEX_VALUE);
		binaryProps.put("data", ignoreField);

		JsonObject contentProps = new JsonObject();

		// .file.content
		JsonObject contentTextInfo = new JsonObject();
		contentTextInfo.put("type", TEXT);
		contentTextInfo.put("index", INDEX_VALUE);
		contentTextInfo.put("analyzer", TRIGRAM_ANALYZER);
		if (customIndexOptions != null && customIndexOptions.containsKey("file.content")) {
			contentTextInfo.put("fields", customIndexOptions.getJsonObject("file.content"));
		}
		contentProps.put("content", contentTextInfo);

		// .file.language
		contentProps.put("language", notAnalyzedType(KEYWORD));

		// .file.title
		contentProps.put("title", notAnalyzedType(KEYWORD));

		// .file.author
		contentProps.put("author", notAnalyzedType(KEYWORD));

		// .file.date
		contentProps.put("date", notAnalyzedType(DATE));

		JsonObject contentFieldInfo = new JsonObject();
		contentFieldInfo.put("type", OBJECT);
		contentFieldInfo.put("properties", contentProps);

		binaryProps.put("file", contentFieldInfo);
	}

	/**
	 * Creates an Elasticsearch mapping for all allowed microschemas in the given branch. When the allowed are empty/null, it'll generate the mapping for all
	 * microschemas in the branch.
	 * 
	 * @param allowed
	 *            Restriction to which microschemas are allowed to be saved in the field
	 * @param branch
	 *            The branch for which the mapping should be created
	 * @return An Properties-mapping for a microschema field.
	 */
	public JsonObject getMicroschemaMappingOptions(String[] allowed, Branch branch) {
		// Prevent Null-Pointers
		if (allowed == null) {
			allowed = new String[0];
		}

		// General options
		JsonObject properties = new JsonObject();

		// Microschema options
		properties.put("microschema", new JsonObject()
			.put("type", OBJECT)
			.put("properties", new JsonObject()
				.put(NAME_KEY, trigramTextType())
				.put(UUID_KEY, notAnalyzedType(KEYWORD))
				.put("version", notAnalyzedType(KEYWORD))));

		// Final Object which will be returned
		JsonObject options = new JsonObject().put("properties", properties);

		// A Set-Instance of the allowed microschema-names
		Set<String> whitelist = Sets.newHashSet(allowed);

		// If the branch is given and the whitelist has entries.
		// Otherwise the index would be empty and not dynamic which prevents every
		// kind of proper search.
		boolean shouldFilter = branch != null && !whitelist.isEmpty();

		if (shouldFilter) {
			for (BranchMicroschemaEdge edge : branch.findAllLatestMicroschemaVersionEdges()) {
				MicroschemaContainerVersion version = edge.getMicroschemaContainerVersion();
				MicroschemaModel microschema = version.getSchema();
				String microschemaName = microschema.getName();

				// Check if the microschema is contained in the whitelist
				// and ignore it if it isn't
				if (!whitelist.contains(microschemaName)) {
					continue;
				}

				// Create and save a mapping for all microschema fields
				JsonObject fields = new JsonObject(microschema
					.getFields()
					.stream()
					.collect(Collectors.toMap(FieldSchema::getName, field -> this.getFieldMapping(field, branch))));

				// Save the created mapping to the properties
				properties.put("fields-" + microschemaName, new JsonObject()
					.put("type", OBJECT)
					.put("properties", fields));
			}
		} else {
			// Set the options to dynamic as no proper mapping could be generated
			options.put("dynamic", true);
		}

		return options;
	}
}
