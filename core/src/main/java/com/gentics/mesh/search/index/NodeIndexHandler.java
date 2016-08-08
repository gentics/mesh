package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;

/**
 * Handler for the node specific search index.
 * 
 * <p>
 * Format:
 * <ul>
 * <li>Document Id: [:uuid-:languageTag]</li>
 * <li>Example: 234ef7f2510e4d0e8ef9f2210e0d0ec2-en</li>
 * </ul>
 * 
 * <ul>
 * <li>Document Type: [:schemaName-:schemaVersion]</li>
 * <li>Example: content-1</li>
 * </ul>
 * 
 * <ul>
 * <li>Document Index: [node-:projectUuid-:releaseUuid-:versionType]</li>
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
	 * Name of the custom property of SearchQueueEntry containing the release uuid.
	 */
	public final static String CUSTOM_RELEASE_UUID = "releaseUuid";

	/**
	 * Name of the custom property of SearchQueueEntry containing the version ("draft" or "published")
	 */
	public final static String CUSTOM_VERSION = "version";

	/**
	 * Name of the custom property of SearchQueueEntry containing the project uuid.
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
		return db.noTx(() -> {
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
		return db.noTx(() -> {
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
	protected JsonObject transformToDocument(Node object) {
		throw new NotImplementedException("Nodes can't be directly transformed due to i18n support.");
	}

	@Override
	public Completable store(Node node, String documentType, SearchQueueEntry entry) {
		return Completable.defer(() -> {
			try (NoTx noTrx = db.noTx()) {
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
						return Completable.complete();
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
					// A node migration for example requires old node documents to be removed from the index since the migration itself may create new node
					// versions.
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
					// Don't store the container if it is null.
					if (container != null) {
						obs.add(storeContainer(container, indexName, releaseUuid));
					}
					deleteObs = searchProvider.deleteDocumentsViaQuery(indexName, query).toCompletable();
				}

				if (log.isDebugEnabled()) {
					log.debug("Stored node in index {" + indexName + "}");
				}
				return Completable.merge(obs).andThen(deleteObs).doOnCompleted(() -> {
					MeshSpringConfiguration.getInstance().searchProvider().refreshIndex();
				});

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
	 * Generate an elasticsearch document object from the given container and stores it in the search index.
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
		JsonObject document = new JsonObject();
		addUser(document, "editor", container.getEditor());
		document.put("edited", container.getLastEditedTimestamp());
		addBasicReferences(document, node);
		addProject(document, node.getProject());
		addTags(document, node.getTags(node.getProject().getLatestRelease()));

		// The basenode has no parent.
		if (node.getParentNode(releaseUuid) != null) {
			addParentNodeInfo(document, node.getParentNode(releaseUuid));
		}

		document.remove("language");
		String language = container.getLanguage().getLanguageTag();
		document.put("language", language);
		addSchema(document, container.getSchemaContainerVersion());

		searchProvider.addFields(document, container, container.getSchemaContainerVersion().getSchema().getFields());
		if (log.isTraceEnabled()) {
			String json = document.toString();
			log.trace("Search index json:");
			log.trace(json);
		}

		// Add display field value
		Map<String, String> displayFieldMap = new HashMap<>();
		displayFieldMap.put("key", container.getSchemaContainerVersion().getSchema().getDisplayField());
		displayFieldMap.put("value", container.getDisplayFieldValue());
		document.put("displayField", displayFieldMap);
		return searchProvider.storeDocument(indexName, getDocumentType(container.getSchemaContainerVersion()),
				composeDocumentId(container.getParentNode(), language), document);

	}

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
		Map<String, Object> parentNodeInfo = new HashMap<>();
		parentNodeInfo.put(UUID_KEY, parentNode.getUuid());
		// TODO check whether nesting of nested elements would also work
		// TODO FIXME MIGRATE: How to add this reference info? The schema is now linked to the node. Should we add another reference:
		// (n:Node)->(sSchemaContainer) ?
		// parentNodeInfo.put("schema.name", parentNode.getSchemaContainer().getName());
		// parentNodeInfo.put("schema.uuid", parentNode.getSchemaContainer().getUuid());
		document.put("parentNode", parentNodeInfo);
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
	public Completable setNodeIndexMapping(String indexName, String type, Schema schema) {
		// Check whether the search provider is a dummy provider or not
		if (searchProvider.getNode() != null) {
			return Completable.create(sub -> {
				PutMappingRequestBuilder mappingRequestBuilder = searchProvider.getNode().client().admin().indices().preparePutMapping(indexName);
				mappingRequestBuilder.setType(type);

				try {
					JsonObject mappingJson = new JsonObject();
					JsonObject typeJson = new JsonObject();
					JsonObject fieldJson = new JsonObject();
					JsonObject fieldProps = new JsonObject();
					fieldJson.put("properties", fieldProps);
					JsonObject typeProperties = new JsonObject();
					typeJson.put("properties", typeProperties);
					typeProperties.put("fields", fieldJson);
					mappingJson.put(type, typeJson);

					for (FieldSchema field : schema.getFields()) {
						JsonObject fieldInfo = searchProvider.getMappingInfo(field);
						fieldProps.put(field.getName(), fieldInfo);
					}

					if (log.isDebugEnabled()) {
						log.debug(mappingJson.toString());
					}
					mappingRequestBuilder.setSource(mappingJson.toString());
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
