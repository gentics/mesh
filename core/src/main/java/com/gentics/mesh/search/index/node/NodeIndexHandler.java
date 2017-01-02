package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;

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
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

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
 * <li>Document Type: -</li>
 * </ul>
 * 
 * <ul>
 * <li>Document Index: [node-:projectUuid-:releaseUuid-:schemaVersionUuid-:versionType]</li>
 * <li>Example: node-934ef7f2210e4d0e8ef7f2210e0d0ec5-fd26b3cf20fb4f6ca6b3cf20fbdf6cd6-draft</li>
 * </ul>
 * <p>
 */
@Singleton
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	public final static String DOCUMENT_ID_NAME = "documentId";

	public final static String FIELD_CONTAINER_UUID_NAME = "fieldContainerUuid";

	/**
	 * Key of the custom property of SearchQueueEntry containing the language tag. Value: {@value #CUSTOM_LANGUAGE_TAG}
	 */
	public final static String CUSTOM_LANGUAGE_TAG = "languageTag";

	/**
	 * Key of the custom property of SearchQueueEntry containing the release uuid. Value: {@value #CUSTOM_RELEASE_UUID}
	 */
	public final static String CUSTOM_RELEASE_UUID = "releaseUuid";

	/**
	 * Key of the custom property of SearchQueueEntry containing the version ("draft" or "published"). Value: {@value #CUSTOM_VERSION}
	 */
	public final static String CUSTOM_VERSION = "version";

	/**
	 * Key of the custom property of SearchQueueEntry containing the project uuid. Value: {@value #CUSTOM_PROJECT_UUID}
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	/**
	 * Key of the custom property of SearchQueueEntry containing the schema version uuid. Value: {@value #CUSTOM_SCHEMAVERSION_UUID}
	 */
	public final static String CUSTOM_SCHEMAVERSION_UUID = "schemaVersionUuid";

	@Inject
	NodeGraphFieldContainerTransformator transformator;

	@Inject
	public NodeIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	public Completable init() {
		Completable superCompletable = super.init();
		return superCompletable.andThen(Completable.create(sub -> {
			db.noTx(() -> {
				updateNodeIndexMappings();
				sub.onCompleted();
				return null;
			});
		}));
	}

	public NodeGraphFieldContainerTransformator getTransformator() {
		return transformator;
	}

	/**
	 * Get the document type for node documents.
	 * 
	 * @return document type
	 */
	public static String getDocumentType() {
		return Node.TYPE;
	}

	@Override
	protected String getDocumentType(SearchQueueEntry entry) {
		return getDocumentType();
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return Node.TYPE;
	}

	@Override
	public Map<String, Set<String>> getIndices() {
		return db.noTx(() -> {
			Map<String, Set<String>> indexInfo = new HashMap<>();

			//Iterate over all projects and construct the index names
			boot.meshRoot().getProjectRoot().reload();
			List<? extends Project> projects = boot.meshRoot().getProjectRoot().findAll();
			for (Project project : projects) {
				List<? extends Release> releases = project.getReleaseRoot().findAll();
				// Add the draft and published index names per release to the map 
				for (Release release : releases) {
					// Each release specific index has also document type specific mappings
					for (SchemaContainerVersion containerVersion : release.findAllSchemaVersions()) {
						String draftIndexName = getIndexName(project.getUuid(), release.getUuid(), containerVersion.getUuid(), DRAFT);
						String publishIndexName = getIndexName(project.getUuid(), release.getUuid(), containerVersion.getUuid(), PUBLISHED);
						Set<String> documentTypes = new HashSet<>();
						documentTypes.add(getDocumentType());
						indexInfo.put(draftIndexName, documentTypes);
						indexInfo.put(publishIndexName, documentTypes);
					}
				}
			}
			return indexInfo;
		});
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return db.noTx(() -> {
			Set<String> indices = new HashSet<>();
			Project project = ac.getProject();
			if (project != null) {
				Release release = ac.getRelease(null);
				for (SchemaContainerVersion version : release.findAllSchemaVersions()) {
					indices.add(getIndexName(project.getUuid(), release.getUuid(), version.getUuid(),
							ContainerType.forVersion(ac.getVersioningParameters().getVersion())));
				}
			} else {
				// The project was not specified. Maybe a global search wants to know which indices must be searched. In that case we just iterate over all projects and collect index names per release.
				List<? extends Project> projects = boot.meshRoot().getProjectRoot().findAll();
				for (Project currentProject : projects) {
					for (Release release : currentProject.getReleaseRoot().findAll()) {
						for (SchemaContainerVersion version : release.findAllSchemaVersions()) {
							indices.add(getIndexName(currentProject.getUuid(), release.getUuid(), version.getUuid(),
									ContainerType.forVersion(ac.getVersioningParameters().getVersion())));
						}
					}
				}
			}
			return indices;
		});
	}

	/**
	 * Get the index name for the given project/release/version and container type.
	 * 
	 * @param projectUuid
	 * @param releaseUuid
	 * @param schemaVersionUuid
	 * @param versionType
	 *            Version type of the document (e.g: published, draft)
	 * @return index name
	 */
	public static String getIndexName(String projectUuid, String releaseUuid, String schemaVersionUuid, ContainerType versionType) {
		// TODO check that only "draft" and "publisheD" are used for version
		StringBuilder indexName = new StringBuilder("node");
		indexName.append("-").append(projectUuid).append("-").append(releaseUuid).append("-").append(schemaVersionUuid).append("-")
				.append(versionType.toString().toLowerCase());
		return indexName.toString();
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
	public Completable store(Node node, String documentType, SearchQueueEntry entry) {
		return Completable.defer(() -> {
			try (NoTx noTrx = db.noTx()) {
				String languageTag = entry.get(CUSTOM_LANGUAGE_TAG);
				String releaseUuid = entry.get(CUSTOM_RELEASE_UUID);
				ContainerType type = ContainerType.forVersion(entry.get(CUSTOM_VERSION));

				// TODO check consistency

				Set<Completable> obs = new HashSet<>();
				Completable deleteObs = Completable.complete();

				String indexName = null;
				
				// Store all containers if no language was specified
				if (languageTag == null) {
					for (NodeGraphFieldContainer container : node.getGraphFieldContainers()) {
						indexName = getIndexName(node.getProject().getUuid(), releaseUuid, container.getSchemaContainerVersion().getUuid(),
								type);
						if (log.isDebugEnabled()) {
							log.debug("Storing node {" + node.getUuid() + "} of type {" + type.name() + "} into index {" + indexName + "}");
						}
						obs.add(storeContainer(container, indexName, releaseUuid));
					}
				} else {

					NodeGraphFieldContainer container = node.getGraphFieldContainer(languageTag, releaseUuid, type);
					if (container == null) {
						log.warn("Node {" + node.getUuid() + "} has no language container for languageTag {" + languageTag
								+ "}. I can't store the search index document. This may be normal in cases if mesh is handling an outdated search queue batch entry.");
						return Completable.complete();
					}
					indexName = getIndexName(node.getProject().getUuid(), releaseUuid, container.getSchemaContainerVersion().getUuid(), type);

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
							JSONObject mustNotTerm = new JSONObject().put("term", new JSONObject().put("_type", getDocumentType()));
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
					if (log.isDebugEnabled()) {
						log.debug("Stored node in index {" + indexName + "}");
					}
				}

				String name = indexName;
				return Completable.merge(obs).andThen(deleteObs).doOnCompleted(() -> {
					MeshInternal.get().searchProvider().refreshIndex(name);
				});

			}
		});

	}

	@Override
	public Completable delete(SearchQueueEntry entry) {
		String languageTag = entry.get(CUSTOM_LANGUAGE_TAG);
		String releaseUuid = entry.get(CUSTOM_RELEASE_UUID);
		String projectUuid = entry.get(CUSTOM_PROJECT_UUID);
		String schemaVersionUuid = entry.get(CUSTOM_SCHEMAVERSION_UUID);
		ContainerType type = ContainerType.forVersion(entry.get(CUSTOM_VERSION));
		String indexName = getIndexName(projectUuid, releaseUuid, schemaVersionUuid, type);
		String documentType = getDocumentType(entry);
		String uuid = entry.getElementUuid();
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
		JsonObject doc = transformator.toDocument(container, releaseUuid);
		return searchProvider.storeDocument(indexName, getDocumentType(), composeDocumentId(container.getParentNode(), doc.getString("language")),
				doc);
	}

	/**
	 * Compose the document ID for the index document.
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
	 * Update the mapping for the schema. The schema information will be used to determine the correct index type.
	 * 
	 * @param schema
	 *            schema
	 * @return observable
	 */
	public Completable updateNodeIndexMapping(Schema schema) {
		Set<Completable> obs = new HashSet<>();
		for (String indexName : getIndices().keySet()) {
			obs.add(updateNodeIndexMapping(indexName, schema));
		}
		return Completable.merge(obs);
	}

	@Override
	public Completable updateMapping(SearchQueueEntry entry) {
		String containerVersionUuid = entry.get("schemaContainerVersionUuuid");
		String containerUuid = entry.get("schemaContainerUuid");
		SchemaContainerVersion version = boot.findSchemaContainerRoot().findByUuid(containerUuid).findVersionByUuid(containerVersionUuid);
		Schema schema = version.getSchema();
		String indexName = entry.getElementUuid();
		return updateNodeIndexMapping(indexName, schema);
	}

	/**
	 * Update the mapping for the given type in the given index for the schema.
	 *
	 * @param indexName
	 *            index name
	 * @param schema
	 *            schema
	 * @return
	 */
	public Completable updateNodeIndexMapping(String indexName, Schema schema) {

		//String type = schema.getName() + "-" + schema.getVersion();
		String type = getDocumentType();
		// Check whether the search provider is a dummy provider or not
		if (searchProvider.getNode() != null) {
			return Completable.create(sub -> {
				//TODO Add trigram filter - https://www.elastic.co/guide/en/elasticsearch/guide/current/ngrams-compound-words.html 
//				UpdateSettingsRequestBuilder updateSettingsBuilder = searchProvider.getNode().client().admin().indices().prepareUpdateSettings(indexName);
//				updateSettingsBuilder.set
//				updateSettingsBuilder.execute();
				PutMappingRequestBuilder mappingRequestBuilder = searchProvider.getNode().client().admin().indices().preparePutMapping(indexName);
				mappingRequestBuilder.setType(type);

				try {
					JsonObject mappingJson = transformator.getMapping(schema, type);
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
	public GraphPermission getReadPermission(InternalActionContext ac) {
		switch (ContainerType.forVersion(ac.getVersioningParameters().getVersion())) {
		case PUBLISHED:
			return GraphPermission.READ_PUBLISHED_PERM;
		default:
			return GraphPermission.READ_PERM;
		}
	}

	/**
	 * Update all node specific index mappings for all projects and all releases.
	 */
	public void updateNodeIndexMappings() {
		List<? extends Project> projects = boot.meshRoot().getProjectRoot().findAll();
		projects.forEach((project) -> {
			List<? extends Release> releases = project.getReleaseRoot().findAll();
			// Add the draft and published index names per release to the map
			for (Release release : releases) {
				// Each release specific index has also document type specific mappings
				for (SchemaContainerVersion containerVersion : release.findAllSchemaVersions()) {
					updateNodeIndexMapping(getIndexName(project.getUuid(), release.getUuid(), containerVersion.getUuid(), DRAFT),
							containerVersion.getSchema()).await();
					updateNodeIndexMapping(getIndexName(project.getUuid(), release.getUuid(), containerVersion.getUuid(), PUBLISHED),
							containerVersion.getSchema()).await();
				}
			}
		});

	}
}
