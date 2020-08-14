package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.util.DateUtils.toISO8601;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;
import org.jsoup.Jsoup;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
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
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Transformer which can be used to transform a {@link NodeGraphFieldContainer} into a elasticsearch document. Additionally the matching mapping can also be
 * generated using this class.
 */
@Singleton
public class NodeContainerTransformer extends AbstractTransformer<NodeGraphFieldContainer> {

	private static final Logger log = LoggerFactory.getLogger(NodeContainerTransformer.class);

	private static final String VERSION_KEY = "version";

	private final MeshOptions options;

	@Inject
	public NodeContainerTransformer(MeshOptions options) {
		this.options = options;
	}

	/**
	 * Transform the given schema and add it to the source map.
	 * 
	 * @param document
	 * @param schemaVersion
	 */
	private void addSchema(JsonObject document, SchemaVersion schemaVersion) {
		String name = schemaVersion.getName();
		String uuid = schemaVersion.getSchemaContainer().getUuid();
		Map<String, String> schemaFields = new HashMap<>();
		schemaFields.put(NAME_KEY, name);
		schemaFields.put(UUID_KEY, uuid);
		schemaFields.put(VERSION_KEY, schemaVersion.getVersion());
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
	 * Generate the node container specific permission info. Node containers need to store also the read publish perm roles for published containers.
	 * 
	 * @param document
	 * @param node
	 * @param type
	 */
	private void addPermissionInfo(JsonObject document, Node node, ContainerType type) {
		List<String> roleUuids = new ArrayList<>();

		for (HibRole role : node.getRolesWithPerm(READ_PERM)) {
			roleUuids.add(role.getUuid());
		}

		// Also add the roles which would grant read on published nodes if the container is published.
		if (type == PUBLISHED) {
			for (HibRole role : node.getRolesWithPerm(READ_PUBLISHED_PERM)) {
				roleUuids.add(role.getUuid());
			}
		}
		document.put("_roleUuids", roleUuids);
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
			// Check whether the field is needed
			if (!fieldSchema.isMappingRequired(options.getSearchOptions())) {
				continue;
			}
			String name = fieldSchema.getName();
			FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
			JsonObject customIndexOptions = fieldSchema.getElasticsearch();
			if (customIndexOptions == null) {
				customIndexOptions = new JsonObject();
			}
			// Check whether we need to a raw field property.
			// TODO: This will not work if the field has a different name.
			boolean addRaw = customIndexOptions.containsKey("raw");

			switch (type) {
			case STRING:
				StringGraphField stringField = container.getString(name);
				if (stringField != null) {
					String value = stringField.getString();
					if (addRaw) {
						value = truncateRawFieldValue(value);
					}
					fieldsMap.put(name, value);
				}
				break;
			case HTML:
				HtmlGraphField htmlField = container.getHtml(name);
				if (htmlField != null) {
					String value = htmlField.getHTML();
					if (value != null) {
						String plainValue = Jsoup.parse(value).text();
						if (addRaw) {
							plainValue = truncateRawFieldValue(plainValue);
						}
						fieldsMap.put(name, plainValue);
					} else {
						fieldsMap.put(name, value);
					}
				}
				break;
			case BINARY:
				BinaryGraphField binaryField = container.getBinary(name);
				if (binaryField != null) {
					JsonObject binaryFieldInfo = new JsonObject();
					fieldsMap.put(name, binaryFieldInfo);
					binaryFieldInfo.put("filename", binaryField.getFileName());
					binaryFieldInfo.put("mimeType", binaryField.getMimeType());
					binaryFieldInfo.put("dominantColor", binaryField.getImageDominantColor());

					Binary binary = binaryField.getBinary();
					if (binary != null) {
						binaryFieldInfo.put("filesize", binary.getSize());
						binaryFieldInfo.put("sha512sum", binary.getSHA512Sum());
						binaryFieldInfo.put("width", binary.getImageWidth());
						binaryFieldInfo.put("height", binary.getImageHeight());
					}

					if (options.getSearchOptions().isIncludeBinaryFields()) {
						// Add the metadata
						BinaryMetadata metadata = binaryField.getMetadata();
						if (metadata != null) {
							JsonObject binaryFieldMetadataInfo = new JsonObject();
							binaryFieldInfo.put("metadata", binaryFieldMetadataInfo);

							for (Entry<String, String> entry : metadata.getMap().entrySet()) {
								binaryFieldMetadataInfo.put(entry.getKey(), entry.getValue());
							}

							Location loc = metadata.getLocation();
							if (loc != null) {
								JsonObject locationInfo = new JsonObject();
								binaryFieldMetadataInfo.put("location", locationInfo);
								locationInfo.put("lon", loc.getLon());
								locationInfo.put("lat", loc.getLat());
								// Add height outside of object to prevent ES error
								binaryFieldMetadataInfo.put("location-z", loc.getAlt());
							}
						}

						// Plain text
						String plainText = binaryField.getPlainText();
						if (plainText != null) {
							JsonObject file = new JsonObject();
							binaryFieldInfo.put("file", file);
							file.put("content", plainText);
						}
					}

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
							// Add list of micronode objects
							fieldsMap.put(fieldSchema.getName(), Observable.fromIterable(micronodeGraphFieldList.getList()).map(item -> {
								JsonObject itemMap = new JsonObject();
								Micronode micronode = item.getMicronode();
								MicroschemaVersion microschameContainerVersion = micronode.getSchemaContainerVersion();
								addMicroschema(itemMap, microschameContainerVersion);
								addFields(itemMap, "fields-" + microschameContainerVersion.getName(), micronode,
									microschameContainerVersion.getSchema().getFields());
								return itemMap;
							}).toList().blockingGet());
						}
						break;
					case "string":
						StringGraphFieldList graphStringList = container.getStringList(fieldSchema.getName());
						if (graphStringList != null) {
							List<String> stringItems = new ArrayList<>();
							for (StringGraphField listItem : graphStringList.getList()) {
								String value = listItem.getString();
								if (addRaw) {
									value = truncateRawFieldValue(value);
								}
								stringItems.add(value);
							}
							fieldsMap.put(fieldSchema.getName(), stringItems);
						}
						break;
					case "html":
						HtmlGraphFieldList graphHtmlList = container.getHTMLList(fieldSchema.getName());
						if (graphHtmlList != null) {
							List<String> htmlItems = new ArrayList<>();
							for (HtmlGraphField listItem : graphHtmlList.getList()) {
								String value = listItem.getHTML();
								if (value != null) {
									String plainValue = Jsoup.parse(value).text();
									if (addRaw) {
										plainValue = truncateRawFieldValue(plainValue);
									}
									htmlItems.add(plainValue);
								} else {
									htmlItems.add(value);
								}
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
	 * Transform the given microschema container and add it to the source map.
	 * 
	 * @param map
	 * @param microschemaVersion
	 */
	private void addMicroschema(JsonObject document, MicroschemaVersion microschemaVersion) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, microschemaVersion.getName());
		info.put(UUID_KEY, microschemaVersion.getUuid());
		// TODO add version
		document.put("microschema", info);
	}

	/**
	 * Transforms tags grouped by tag families
	 * 
	 * @param document
	 * @param tags
	 */
	private void addTagFamilies(JsonObject document, Iterable<? extends HibTag> tags) {
		JsonObject familiesObject = new JsonObject();

		for (HibTag tag : tags) {
			HibTagFamily family = tag.getTagFamily();
			JsonObject familyObject = familiesObject.getJsonObject(family.getName());
			if (familyObject == null) {
				familyObject = new JsonObject();
				familyObject.put("uuid", family.getUuid());
				familyObject.put("tags", new JsonArray());
				familiesObject.put(family.getName(), familyObject);
			}
			familyObject.getJsonArray("tags").add(new JsonObject().put("name", tag.getName()).put("uuid", tag.getUuid()));
		}

		document.put("tagFamilies", familiesObject);
	}

	/**
	 * It is required to specify the branchUuid in order to transform containers.
	 *
	 * @deprecated
	 */
	@Override
	@Deprecated
	public JsonObject toDocument(NodeGraphFieldContainer object) {
		throw new NotImplementedException("Use toDocument(container, branchUuid) instead");
	}

	/**
	 * Generate the node specific permission info partial which is used to update node container documents in the indices.
	 * 
	 * @param node
	 * @param type
	 * @return
	 */
	public JsonObject toPermissionPartial(Node node, ContainerType type) {
		JsonObject document = new JsonObject();
		addPermissionInfo(document, node, type);
		return document;
	}

	public String generateVersion(NodeGraphFieldContainer container, String branchUuid, ContainerType type) {
		Node node = container.getParentNode();
		HibProject project = node.getProject();

		StringBuilder builder = new StringBuilder();
		builder.append(container.getElementVersion());
		builder.append("|");
		builder.append(branchUuid);
		builder.append("|");
		builder.append(type.name());
		builder.append("|");
		builder.append(project.getUuid() + project.getName());
		builder.append("|");
		builder.append(node.getElementVersion());

		return ETag.hash(builder.toString());
	}

	/**
	 * @deprecated Use generateVersion(container, branchUuid) instead
	 */
	@Override
	@Deprecated
	public String generateVersion(NodeGraphFieldContainer element) {
		throw new NotImplementedException("Use generateVersion(container, branchUuid) instead");
	}

	/**
	 * Transform the role to the document which can be stored in ES.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	public JsonObject toDocument(NodeGraphFieldContainer container, String branchUuid, ContainerType type) {
		Node node = container.getParentNode();
		JsonObject document = new JsonObject();
		document.put("uuid", node.getUuid());
		addUser(document, "editor", container.getEditor());
		document.put("edited", toISO8601(container.getLastEditedTimestamp()));
		addUser(document, "creator", node.getCreator());
		document.put("created", toISO8601(node.getCreationTimestamp()));

		addProject(document, node.getProject());
		TraversalResult<? extends HibTag> tags = node.getTags(node.getProject().getLatestBranch());
		addTags(document, tags);
		addTagFamilies(document, node.getTags(node.getProject().getLatestBranch()));
		addPermissionInfo(document, node, type);

		// The basenode has no parent.
		if (node.getParentNode(branchUuid) != null) {
			addParentNodeInfo(document, node.getParentNode(branchUuid));
		}

		String language = container.getLanguageTag();
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
		document.put("displayField", displayField);
		document.put("branchUuid", branchUuid);
		document.put(VERSION_KEY, generateVersion(container, branchUuid, type));
		return document;
	}

}
