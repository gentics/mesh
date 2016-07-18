package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
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
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RxUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.Observable;

/**
 * Handler for the node specific search index.
 * 
 * <p>
 * Format:
 * <ul>
 * <li>Document Id: [:uuid]-[:languageTag]</li>
 * <li>Example: 234ef7f2510e4d0e8ef9f2210e0d0ec2-en</li>
 * </ul>
 * 
 * <ul>
 * <li>Document Type: [:schemaName]-[:schemaVersion]</li>
 * <li>Example: content-1</li>
 * </ul>
 * 
 * <ul>
 * <li>Document Index: [node-[:projectUuid]-[:releaseUuid]-[:versionType]</li>
 * <li>Example: node-934ef7f2210e4d0e8ef7f2210e0d0ec5-fd26b3cf20fb4f6ca6b3cf20fbdf6cd6-draft</li>
 * </ul>
 * <p>
 */
@Component
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	public final static String DOCUMENT_ID_NAME = "documentId";

	public final static String FIELD_CONTAINER_UUID_NAME = "fieldContainerUuid";

	/**
	 * Name of the custom property of SearchQueueEntry containing the language tag
	 */
	public final static String CUSTOM_LANGUAGE_TAG = "languageTag";

	/**
	 * Name of the custom property of SearchQueueEntry containing the release uuid
	 */
	public final static String CUSTOM_RELEASE_UUID = "releaseUuid";

	/**
	 * Name of the custom property of SearchQueueEntry containing the version ("draft" or "published")
	 */
	public final static String CUSTOM_VERSION = "version";

	/**
	 * Name of the custom property of SearchQueueEntry containing the project uuid
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	private static final String VERSION_KEY = "version";

	private static NodeIndexHandler instance;

	@Autowired
	protected Database db;

	@PostConstruct
	public void setup() {
		NodeIndexHandler.instance = this;
	}

	public static NodeIndexHandler getInstance() {
		return instance;
	}

	/**
	 * Get the document type for documents stored for the given schema
	 * 
	 * @param schemaVersion
	 *            Schema container version
	 * @return document type
	 */
	public static String getDocumentType(SchemaContainerVersion schemaVersion) {
		return new StringBuilder(schemaVersion.getName()).append("-").append(schemaVersion.getVersion()).toString();
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return Node.TYPE;
	}

	@Override
	public Set<String> getIndices() {
		return db.noTrx(() -> {
			Set<String> indexNames = new HashSet<>();
			BootstrapInitializer.getBoot().meshRoot().getProjectRoot().reload();
			List<? extends Project> projects = BootstrapInitializer.getBoot().meshRoot().getProjectRoot().findAll();
			projects.forEach((project) -> {
				List<? extends Release> releases = project.getReleaseRoot().findAll();
				releases.forEach((r) -> {
					indexNames.add(getIndexName(project.getUuid(), r.getUuid(), "draft"));
					indexNames.add(getIndexName(project.getUuid(), r.getUuid(), "published"));
				});
			});
			return indexNames;
		});
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return db.noTrx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				return Collections
						.singleton(getIndexName(project.getUuid(), ac.getRelease(null).getUuid(), ac.getVersioningParameters().getVersion()));
			} else {
				List<? extends Project> projects = BootstrapInitializer.getBoot().meshRoot().getProjectRoot().findAll();
				return projects.stream()
						.map(p -> getIndexName(p.getUuid(), p.getLatestRelease().getUuid(), ac.getVersioningParameters().getVersion()))
						.collect(Collectors.toSet());
			}
		});
	}

	/**
	 * Get the index name for the given project/release/version
	 * 
	 * @param projectUuid
	 * @param releaseUuid
	 * @param version
	 * @return index name
	 */
	public static String getIndexName(String projectUuid, String releaseUuid, String version) {
		// TODO check that only "draft" and "publisheD" are used for version
		StringBuilder indexName = new StringBuilder("node");
		indexName.append("-").append(projectUuid).append("-").append(releaseUuid).append("-").append(version);
		return indexName.toString();
	}

	@Override
	protected String getType() {
		return Node.TYPE;
	}

	@Override
	public String getKey() {
		return Node.TYPE;
	}

	@Override
	protected RootVertex<Node> getRootVertex() {
		return boot.meshRoot().getNodeRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Node object) {
		throw new NotImplementedException("Nodes can't be directly transformed due to i18n support.");
	}

	@Override
	public Completable store(Node node, String documentType, SearchQueueEntry entry) {
		return Completable.defer(() -> {
			try (NoTrx noTrx = db.noTrx()) {
				String languageTag = entry.getCustomProperty(CUSTOM_LANGUAGE_TAG);
				String releaseUuid = entry.getCustomProperty(CUSTOM_RELEASE_UUID);
				ContainerType type = ContainerType.forVersion(entry.getCustomProperty(CUSTOM_VERSION));
				String indexName = getIndexName(node.getProject().getUuid(), releaseUuid, type.toString().toLowerCase());

				if (log.isDebugEnabled()) {
					log.debug("Storing node {" + node.getUuid() + "} of type {" + type.name() + "} into index {" + indexName + "}");
				}
				// TODO check consistency

				Set<Completable> obs = new HashSet<>();
				Completable deleteObs = Completable.complete();

				// Store all containers if no language was specified
				if (languageTag == null) {
					for (NodeGraphFieldContainer container : node.getGraphFieldContainers()) {
						obs.add(storeContainer(container, indexName, releaseUuid));
					}
				} else {

					NodeGraphFieldContainer container = node.getGraphFieldContainer(languageTag, releaseUuid, type);
					if (container == null) {
						log.warn("Node {" + node.getUuid() + "} has no language container for languageTag {" + languageTag
								+ "}. I can't store the search index document. This may be normal in cases if mesh is handling an outdated search queue batch entry.");
					}
					// 1. Sanitize the search index for nodes.
					// We'll need to delete all documents which match the given query:
					// * match node documents with same UUID
					// * match node documents with same language
					// * exclude all documents which have the same document type in order to avoid deletion
					// of the document which will be updated later on
					//
					// This will ensure that only one version of the node document remains in the search index.
					//
					// A node migration for example requires old node documents to be removed from the index since the migration itself may create new node versions.
					// Those documents are stored within a schema container version specific index type.
					// We need to ensure that those documents are purged from the index.
					JSONObject query = new JSONObject();
					try {
						JSONObject queryObject = new JSONObject();

						// Only handle selected language
						JSONObject langTerm = new JSONObject().put("term", new JSONObject().put("language", languageTag));
						// Only handle nodes with the same uuid
						JSONObject uuidTerm = new JSONObject().put("term", new JSONObject().put("uuid", node.getUuid()));

						JSONArray mustArray = new JSONArray();
						mustArray.put(uuidTerm);
						mustArray.put(langTerm);

						JSONObject boolFilter = new JSONObject();
						boolFilter.put("must", mustArray);

						// Only limit the deletion if a container could be found. Otherwise delete all the documents in oder to sanitize the index
						if (container != null) {
							// Don't delete the document which is specific to the language container (via the document type)
							JSONObject mustNotTerm = new JSONObject().put("term",
									new JSONObject().put("_type", getDocumentType(container.getSchemaContainerVersion())));
							boolFilter.put("must_not", mustNotTerm);
						}
						queryObject.put("bool", boolFilter);
						query.put("query", queryObject);
					} catch (Exception e) {
						log.error("Error while building deletion query", e);
						throw new GenericRestException(INTERNAL_SERVER_ERROR, "Could not prepare search query.", e);
					}

					deleteObs = RxUtil.andThenCompletable(searchProvider.deleteDocumentsViaQuery(indexName, query), nDocumentsDeleted -> {
						if (log.isDebugEnabled()) {
							log.debug("Deleted {" + nDocumentsDeleted + "} documents from index {" + indexName + "}");
						}
						// Don't store the container if it is null.
						if (container == null) {
							return Completable.complete();
						} else {
							// 2. Try to store the updated document
							return db.noTrx(() -> {
								return storeContainer(container, indexName, releaseUuid);
							});
						}
					});
				}
				Completable.merge(obs).await();
				MeshSpringConfiguration.getInstance().searchProvider().refreshIndex();
				deleteObs.await();
				return Completable.complete();
//					if (log.isDebugEnabled()) {
//						log.debug("Stored node in index.");
//					}
				
				
			}
		});

	}

	@Override
	public Completable delete(String uuid, String documentType, SearchQueueEntry entry) {
		String languageTag = entry.getCustomProperty(CUSTOM_LANGUAGE_TAG);
		String releaseUuid = entry.getCustomProperty(CUSTOM_RELEASE_UUID);
		String projectUuid = entry.getCustomProperty(CUSTOM_PROJECT_UUID);
		ContainerType type = ContainerType.forVersion(entry.getCustomProperty(CUSTOM_VERSION));
		String indexName = getIndexName(projectUuid, releaseUuid, type.toString().toLowerCase());

		return searchProvider.deleteDocument(indexName, documentType, composeDocumentId(uuid, languageTag));
	}

	/**
	 * Generate a flat property map from the given container and store the map within the search index.
	 * 
	 * @param container
	 * @param indexName
	 *            project name
	 * @param releaseUuid
	 *            release Uuid
	 * @return
	 */
	public Completable storeContainer(NodeGraphFieldContainer container, String indexName, String releaseUuid) {

		Node node = container.getParentNode();
		Map<String, Object> map = new HashMap<>();
		addUser(map, "editor", container.getEditor());
		map.put("edited", container.getLastEditedTimestamp());
		addBasicReferences(map, node);
		addProject(map, node.getProject());
		addTags(map, node.getTags(node.getProject().getLatestRelease()));

		// The basenode has no parent.
		if (node.getParentNode(releaseUuid) != null) {
			addParentNodeInfo(map, node.getParentNode(releaseUuid));
		}

		map.remove("language");
		String language = container.getLanguage().getLanguageTag();
		map.put("language", language);
		addSchema(map, container.getSchemaContainerVersion());

		addFields(map, container, container.getSchemaContainerVersion().getSchema().getFields());
		if (log.isTraceEnabled()) {
			String json = JsonUtil.toJson(map);
			log.trace("Search index json:");
			log.trace(json);
		}

		// Add display field value
		Map<String, String> displayFieldMap = new HashMap<>();
		displayFieldMap.put("key", container.getSchemaContainerVersion().getSchema().getDisplayField());
		displayFieldMap.put("value", container.getDisplayFieldValue());
		map.put("displayField", displayFieldMap);
		return searchProvider.storeDocument(indexName, getDocumentType(container.getSchemaContainerVersion()),
				composeDocumentId(container.getParentNode(), language), map);

	}

	/**
	 * Add node fields to the given source map.
	 * 
	 * @param map
	 *            Search index document source map
	 * @param container
	 *            Node field container
	 * @param fields
	 *            List of schema fields that should be handled
	 */
	private void addFields(Map<String, Object> map, GraphFieldContainer container, List<? extends FieldSchema> fields) {
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
								Map<String, Object> itemMap = new HashMap<>();
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
						Map<String, Object> micronodeMap = new HashMap<>();
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
		map.put("fields", fieldsMap);

	}

	/**
	 * Transform the given schema and add it to the source map.
	 * 
	 * @param map
	 * @param schemaContainerVersion
	 */
	private void addSchema(Map<String, Object> map, SchemaContainerVersion schemaContainerVersion) {
		String name = schemaContainerVersion.getName();
		String uuid = schemaContainerVersion.getSchemaContainer().getUuid();
		Map<String, String> schemaFields = new HashMap<>();
		schemaFields.put(NAME_KEY, name);
		schemaFields.put(UUID_KEY, uuid);
		schemaFields.put(VERSION_KEY, String.valueOf(schemaContainerVersion.getVersion()));
		map.put("schema", schemaFields);
	}

	/**
	 * Transform the given microschema container and add it to the source map.
	 * 
	 * @param map
	 * @param microschemaContainerVersion
	 */
	private void addMicroschema(Map<String, Object> map, MicroschemaContainerVersion microschemaContainerVersion) {
		Map<String, String> microschemaFields = new HashMap<>();
		microschemaFields.put(NAME_KEY, microschemaContainerVersion.getName());
		microschemaFields.put(UUID_KEY, microschemaContainerVersion.getUuid());
		map.put("microschema", microschemaFields);
	}

	/**
	 * Use the given node to populate the parent node fields within the source map.
	 * 
	 * @param map
	 * @param parentNode
	 */
	private void addParentNodeInfo(Map<String, Object> map, Node parentNode) {
		Map<String, Object> parentNodeInfo = new HashMap<>();
		parentNodeInfo.put(UUID_KEY, parentNode.getUuid());
		// TODO check whether nesting of nested elements would also work
		// TODO FIXME MIGRATE: How to add this reference info? The schema is now linked to the node. Should we add another reference:
		// (n:Node)->(sSchemaContainer) ?
		// parentNodeInfo.put("schema.name", parentNode.getSchemaContainer().getName());
		// parentNodeInfo.put("schema.uuid", parentNode.getSchemaContainer().getUuid());
		map.put("parentNode", parentNodeInfo);
	}

	/**
	 * Compose the document ID for the index document
	 * 
	 * @param node
	 *            node
	 * @param language
	 *            language
	 * @return document ID
	 */
	public static String composeDocumentId(Node node, String language) {
		return composeDocumentId(node.getUuid(), language);
	}

	/**
	 * Compose the document ID for the index document.
	 * 
	 * @param nodeUuid
	 *            node UUID
	 * @param languageTag
	 *            language
	 * @return document ID
	 */
	public static String composeDocumentId(String nodeUuid, String languageTag) {
		StringBuilder id = new StringBuilder(nodeUuid);
		id.append("-").append(languageTag);
		return id.toString();
	}

	/**
	 * Set the mapping for the given type in all indices for the schema.
	 * 
	 * @param type
	 *            type name
	 * @param schema
	 *            schema
	 * @return observable
	 */
	public Completable setNodeIndexMapping(String type, Schema schema) {
		Set<Completable> obs = new HashSet<>();
		getIndices().forEach(index -> obs.add(setNodeIndexMapping(index, type, schema)));
		return Completable.merge(obs);
	}

	/**
	 * Set the mapping for the given type in the given index for the schema.
	 *
	 * @param indexName
	 *            index name
	 * @param type
	 *            type name
	 * @param schema
	 *            schema
	 * @return observable
	 */
	protected Completable setNodeIndexMapping(String indexName, String type, Schema schema) {
		// Check whether the search provider is a dummy provider or not
		if (searchProvider.getNode() != null) {
			return Completable.create(sub -> {
				PutMappingRequestBuilder mappingRequestBuilder = searchProvider.getNode().client().admin().indices().preparePutMapping(indexName);
				mappingRequestBuilder.setType(type);

				try {
					XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject() // root object
							.startObject(type) // type
							.startObject("properties") // properties
							.startObject("fields") // fields
							.startObject("properties"); // properties

					for (FieldSchema field : schema.getFields()) {
						if (FieldTypes.valueByName(field.getType()) == FieldTypes.LIST) {
							if ("micronode".equals(((ListFieldSchema) field).getListType())) {
								mappingBuilder.startObject(field.getName()).field("type", "nested").endObject();
							}
						}
					}

					mappingBuilder.endObject() // properties
							.endObject() // fields
							.endObject() // properties
							.endObject() // type
							.endObject(); // root object
					if (log.isDebugEnabled()) {
						log.debug(mappingBuilder.string());
					}
					mappingRequestBuilder.setSource(mappingBuilder);

					mappingRequestBuilder.execute(new ActionListener<PutMappingResponse>() {

						@Override
						public void onResponse(PutMappingResponse response) {
							sub.onCompleted();
						}

						@Override
						public void onFailure(Throwable e) {
							sub.onError(e);
						}
					});

				} catch (Exception e) {
					sub.onError(e);
				}
			});
		} else {
			return Completable.complete();
		}
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();

		return props;
	}

	// TODO Combine updateMapping with setNodeIndexMapping
	@Override
	public Completable init() {
		// Omit regular mapping creation for now.
		return createIndex();
	}

	@Override
	public GraphPermission getReadPermission(InternalActionContext ac) {
		switch (ContainerType.forVersion(ac.getVersioningParameters().getVersion())) {
		case PUBLISHED:
			return GraphPermission.READ_PUBLISHED_PERM;
		default:
			return GraphPermission.READ_PERM;
		}
	}
}
