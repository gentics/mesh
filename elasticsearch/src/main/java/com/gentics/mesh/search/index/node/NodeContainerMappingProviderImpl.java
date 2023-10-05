package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static com.gentics.mesh.search.index.MappingHelper.BOOLEAN;
import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.DOUBLE;
import static com.gentics.mesh.search.index.MappingHelper.GEOPOINT;
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
import static com.gentics.mesh.search.index.node.NodeIndexUtil.getLanguageOverride;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.search.index.node.NodeContainerMappingProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.etc.config.search.MappingMode;
import com.gentics.mesh.search.index.AbstractMappingProvider;
import com.google.common.collect.Sets;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see NodeContainerMappingProvider
 */
@Singleton
public class NodeContainerMappingProviderImpl extends AbstractMappingProvider implements NodeContainerMappingProvider {

	private static final Logger log = LoggerFactory.getLogger(NodeContainerMappingProviderImpl.class);

	private final boolean isStrictMode;

	@Inject
	public NodeContainerMappingProviderImpl(MeshOptions options) {
		super(options);
		this.isStrictMode = MappingMode.STRICT == options.getSearchOptions().getMappingMode();
	}

	@Override
	public JsonObject getMappingProperties() {
		return new JsonObject();
	}

	@Override
	public Optional<JsonObject> getMapping(SchemaModel schema) {
		return getMapping(schema, null, null);
	}

	@Override
	public Optional<JsonObject> getMapping(SchemaModel schema, HibBranch branch, String language) {
		// 0. 'no index' schema flag check
		if (schema.getNoIndex() != null && schema.getNoIndex()) {
			return Optional.empty();
		}

		// 1. Get the common type specific mapping
		JsonObject mapping = getMapping();

		// 2. Enhance the type specific mapping
		JsonObject typeMapping = mapping;

		// In ES 6.x the mapping is typed
		if (complianceMode == ComplianceMode.ES_6) {
			typeMapping = mapping.getJsonObject(DEFAULT_TYPE);
		}
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
		typeProperties.put("branchUuid", notAnalyzedType(KEYWORD));

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

		for (FieldSchema field : schema.getFields()) {
			getFieldMapping(field, branch, language).ifPresent(info -> {
				fieldProps.put(field.getName(), info);
			});
		}

		switch (complianceMode) {
		case ES_7:
			return Optional.of(typeMapping);
		case ES_6:
			mapping.put(DEFAULT_TYPE, typeMapping);
			return Optional.of(mapping);
		default:
			throw new RuntimeException("Unknown compliance mode {" + complianceMode + "}");
		}
	}

	/**
	 * Return the mapping JSON info for the field.
	 * 
	 * @param fieldSchema
	 *            Field schema which will be used to construct the mapping info
	 * @return Optional with the JSON object which contains the mapping info or it can be empty if the mapping should be omitted.
	 */
	public Optional<JsonObject> getFieldMapping(FieldSchema fieldSchema, HibBranch branch, String language) {

		// Create the mapping if the field is required.
		// It may be required if the mapping mode is set to dynamic
		// of if the schema contains a custom mapping and the mode is
		// set to strict.
		boolean mappingRequired = fieldSchema.isMappingRequired(options.getSearchOptions());
		if (!mappingRequired) {
			return Optional.empty();
		}

		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
		JsonObject customIndexOptions = getLanguageOverride(fieldSchema.getElasticsearch(), language);
		JsonObject fieldInfo = new JsonObject();

		switch (type) {
		case STRING:
		case HTML:
			addStringFieldMapping(fieldInfo, customIndexOptions);
			break;
		case BOOLEAN:
			addBooleanFieldMapping(fieldInfo, customIndexOptions);
			break;
		case DATE:
			addDataFieldMapping(fieldInfo, customIndexOptions);
			break;
		case S3BINARY:
			addS3BinaryFieldMapping(fieldInfo, customIndexOptions);
			break;
		case BINARY:
			addBinaryFieldMapping(fieldInfo, customIndexOptions);
			break;
		case NUMBER:
			addNumberFieldMapping(fieldInfo, customIndexOptions);
			break;
		case NODE:
			addNodeMapping(fieldInfo, customIndexOptions);
			break;
		case LIST:
			if (fieldSchema instanceof ListFieldSchemaImpl) {
				addListFieldMapping(fieldInfo, branch, (ListFieldSchemaImpl) fieldSchema, language, customIndexOptions);
			}
			break;
		case MICRONODE:
			addMicronodeMapping(fieldInfo, fieldSchema, branch, language, customIndexOptions);
			break;
		default:
			throw new RuntimeException("Mapping type for field type {" + type + "} unknown.");
		}
		return Optional.of(fieldInfo);
	}

	private void addBooleanFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
			fieldInfo.put("type", BOOLEAN);
		}
	}

	private void addDataFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
			fieldInfo.put("type", DATE);
		}
	}

	private void addNumberFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
			// Note: Lucene does not support BigDecimal/Decimal. It is not possible to store such values. ES will fallback to string in those cases.
			// The mesh json parser will not deserialize numbers into BigDecimal at this point. No need to check for big decimal is therefore needed.
			fieldInfo.put("type", DOUBLE);
		}
	}

	private void addMicronodeMapping(JsonObject fieldInfo, FieldSchema fieldSchema, HibBranch branch, String language,
		JsonObject customIndexOptions) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
			fieldInfo.put("type", OBJECT);

			// Cast to MicronodeFieldSchema should be safe as it's a Micronode-Field
			String[] allowed = ((MicronodeFieldSchema) fieldSchema).getAllowedMicroSchemas();

			// Merge the options into the info
			fieldInfo.mergeIn(getMicroschemaMappingOptions(allowed, branch, language));
		}
	}

	private void addNodeMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
			fieldInfo.put("type", KEYWORD);
			fieldInfo.put("index", INDEX_VALUE);
		}
	}

	private void addListFieldMapping(JsonObject fieldInfo, HibBranch branch, ListFieldSchemaImpl fieldSchema, String language,
		JsonObject customIndexOptions) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
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
				fieldInfo.mergeIn(getMicroschemaMappingOptions(allowed, branch, language));

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
	}

	private void addStringFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
			fieldInfo.put("type", TEXT);
			fieldInfo.put("index", INDEX_VALUE);
			fieldInfo.put("analyzer", TRIGRAM_ANALYZER);
			if (customIndexOptions != null) {
				fieldInfo.put("fields", customIndexOptions);
			}
		}
	}

	private void addS3BinaryFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		if (!isStrictMode) {
			JsonObject binaryProps = new JsonObject();

			// .s3ObjectKey
			binaryProps.put("s3ObjectKey", notAnalyzedType(KEYWORD));
			addCommonBinaryFieldMapping(fieldInfo, customIndexOptions, binaryProps);
		}
	}

	private void addBinaryFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions) {
		if (!isStrictMode) {
			JsonObject binaryProps = new JsonObject();

			// .sha512sum
			binaryProps.put("sha512sum", notAnalyzedType(KEYWORD));
			addCommonBinaryFieldMapping(fieldInfo, customIndexOptions, binaryProps);
		}
	}

	private void addCommonBinaryFieldMapping(JsonObject fieldInfo, JsonObject customIndexOptions, JsonObject binaryProps) {
		if (isStrictMode) {
			fieldInfo.mergeIn(customIndexOptions);
		} else {
			fieldInfo.put("type", OBJECT);
			fieldInfo.put("properties", binaryProps);

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

			if (options.getSearchOptions().isIncludeBinaryFields()) {
				// Add mapping for plain text fields
				addBinaryFieldPlainTextMapping(binaryProps, customIndexOptions);

				// .metadata - Add metadata properties which are mostly dynamic string values
				addMetadataMapping(binaryProps);
			}
		}
	}

	private void addBinaryFieldPlainTextMapping(JsonObject binaryProps, JsonObject customIndexOptions) {
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

		JsonObject contentFieldInfo = new JsonObject();
		contentFieldInfo.put("type", OBJECT);
		contentFieldInfo.put("properties", contentProps);
		binaryProps.put("file", contentFieldInfo);
	}

	private void addMetadataMapping(JsonObject binaryProps) {
		JsonObject metadataProps = new JsonObject();

		// .location
		JsonObject locationInfo = new JsonObject();
		locationInfo.put("type", GEOPOINT);
		metadataProps.put("location", locationInfo);

		JsonObject metadataInfo = new JsonObject();
		metadataInfo.put("type", OBJECT);
		metadataInfo.put("properties", metadataProps);
		metadataInfo.put("dynamic", true);
		binaryProps.put("metadata", metadataInfo);
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
	public JsonObject getMicroschemaMappingOptions(String[] allowed, HibBranch branch, String language) {
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
			for (HibBranchMicroschemaVersion assignment : branch.findAllLatestMicroschemaVersionEdges()) {
				HibMicroschemaVersion version = assignment.getMicroschemaContainerVersion();
				MicroschemaVersionModel microschema = version.getSchema();
				String microschemaName = microschema.getName();

				// Check if the microschema is contained in the whitelist
				// and ignore it if it isn't
				if (!whitelist.contains(microschemaName) || (microschema.getNoIndex() != null && microschema.getNoIndex())) {
					continue;
				}

				// Create and save a mapping for all microschema fields
				JsonObject fields = new JsonObject();
				microschema.getFields().stream()
					.forEach(microschemaField -> {
						Optional<JsonObject> mapping = getFieldMapping(microschemaField, branch, language);
						mapping.ifPresent(info -> {
							fields.put(microschemaField.getName(), info);
						});
					});

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
