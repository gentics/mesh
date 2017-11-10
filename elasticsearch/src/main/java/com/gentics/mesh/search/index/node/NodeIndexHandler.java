package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

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
import com.gentics.mesh.core.data.search.MoveDocumentEntry;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Handler for the node specific search index.
 * 
 * This handler can process {@link UpdateDocumentEntry} objects which may contain additional {@link GenericEntryContextImpl} information. The handler will use
 * the context information in order to determine which elements need to be stored in or removed from the index.
 * 
 * Additionally the handler may infer the scope of store actions if the context information is lacking certain information. A context which does not include the
 * target language will result in multiple store actions. Each language container will be loaded and stored. This behaviour will also be applied to releases and
 * project information.
 */
@Singleton
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	@Inject
	public NodeContainerTransformer transformer;

	@Inject
	public NodeContainerMappingProvider mappingProvider;

	@Inject
	public NodeIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	public Class<Node> getElementClass() {
		return Node.class;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return NodeGraphFieldContainer.composeDocumentId(entry.getElementUuid(), entry.getContext().getLanguageTag());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		GenericEntryContext context = entry.getContext();
		String projectUuid = context.getProjectUuid();
		String releaseUuid = context.getReleaseUuid();
		String schemaContainerVersionUuid = context.getSchemaContainerVersionUuid();
		ContainerType type = context.getContainerType();
		return NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, schemaContainerVersionUuid, type);
	}

	@Override
	public NodeContainerTransformer getTransformer() {
		return transformer;
	}

	@Override
	public NodeContainerMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	// @Override
	// public Completable init() {
	// return super.init().andThen(Completable.create(sub -> {
	// db.tx(() -> {
	// updateNodeIndexMappings();
	// sub.onCompleted();
	// });
	// }));
	// }

	@Override
	public Map<String, IndexInfo> getIndices() {
		return db.tx(() -> {
			Map<String, IndexInfo> indexInfo = new HashMap<>();

			// Iterate over all projects and construct the index names
			for (Project project : boot.meshRoot().getProjectRoot().findAllIt()) {
				// Add the draft and published index names per release to the map
				for (Release release : project.getReleaseRoot().findAllIt()) {
					// Each release specific index has also document type specific mappings
					for (SchemaContainerVersion containerVersion : release.findActiveSchemaVersions()) {
						String draftIndexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), release.getUuid(),
								containerVersion.getUuid(), DRAFT);
						String publishIndexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), release.getUuid(),
								containerVersion.getUuid(), PUBLISHED);
						if (log.isDebugEnabled()) {
							log.debug("Adding index to map of known idices {" + draftIndexName + "");
							log.debug("Adding index to map of known idices {" + publishIndexName + "");
						}
						// Load the index mapping information for the index
						SchemaModel schema = containerVersion.getSchema();
						JsonObject mapping = getMappingProvider().getMapping(schema);
						JsonObject settings = schema.getElasticsearch();
						indexInfo.put(draftIndexName, new IndexInfo(draftIndexName, settings, mapping));
						indexInfo.put(publishIndexName, new IndexInfo(publishIndexName, settings, mapping));
					}
				}
			}
			return indexInfo;
		});
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return db.tx(() -> {
			Set<String> indices = new HashSet<>();
			Project project = ac.getProject();
			if (project != null) {
				Release release = ac.getRelease();
				// Locate all schema versions which need to be taken into consideration when choosing the indices
				for (SchemaContainerVersion version : release.findActiveSchemaVersions()) {
					indices.add(NodeGraphFieldContainer.composeIndexName(project.getUuid(), release.getUuid(), version.getUuid(),
							ContainerType.forVersion(ac.getVersioningParameters().getVersion())));
				}
			} else {
				// The project was not specified. Maybe a global search wants to know which indices must be searched.
				// In that case we just iterate over all projects and collect index names per release.
				for (Project currentProject : boot.meshRoot().getProjectRoot().findAllIt()) {
					for (Release release : currentProject.getReleaseRoot().findAllIt()) {
						for (SchemaContainerVersion version : release.findActiveSchemaVersions()) {
							indices.add(NodeGraphFieldContainer.composeIndexName(currentProject.getUuid(), release.getUuid(), version.getUuid(),
									ContainerType.forVersion(ac.getVersioningParameters().getVersion())));
						}
					}
				}
			}
			return indices;
		});
	}

	@Override
	public RootVertex<Node> getRootVertex() {
		return boot.meshRoot().getNodeRoot();
	}

	@Override
	public Completable store(Node node, UpdateDocumentEntry entry) {
		return Completable.defer(() -> {
			GenericEntryContext context = entry.getContext();
			Set<Single<String>> obs = new HashSet<>();
			try (Tx tx = db.tx()) {
				store(obs, node, context);
			}

			// Now merge all store actions and refresh the affected indices
			return Observable.from(obs).map(x -> x.toObservable()).flatMap(x -> x).distinct()
					.doOnNext(indexName -> searchProvider.refreshIndex(indexName)).toCompletable();
		});
	}

	/**
	 * Step 1 - Check whether we need to handle all releases.
	 * 
	 * @param obs
	 * @param node
	 * @param context
	 */
	private void store(Set<Single<String>> obs, Node node, GenericEntryContext context) {
		if (context.getReleaseUuid() == null) {
			for (Release release : node.getProject().getReleaseRoot().findAllIt()) {
				store(obs, node, release.getUuid(), context);
			}
		} else {
			store(obs, node, context.getReleaseUuid(), context);
		}
	}

	/**
	 * Step 2 - Check whether we need to handle all container types.
	 * 
	 * Add the possible store actions to the set of observables. This method will utilise as much of the provided context data if possible. It will also handle
	 * fallback options and invoke store for all types if the container type has not been specified.
	 * 
	 * @param obs
	 * @param node
	 * @param releaseUuid
	 * @param context
	 */
	private void store(Set<Single<String>> obs, Node node, String releaseUuid, GenericEntryContext context) {
		if (context.getContainerType() == null) {
			for (ContainerType type : ContainerType.values()) {
				// We only want to store DRAFT and PUBLISHED Types
				if (type == DRAFT || type == PUBLISHED) {
					store(obs, node, releaseUuid, type, context);
				}
			}
		} else {
			store(obs, node, releaseUuid, context.getContainerType(), context);
		}
	}

	/**
	 * Step 3 - Check whether we need to handle all languages.
	 * 
	 * Invoke store for the possible set of containers. Utilise the given context settings as much as possible.
	 * 
	 * @param obs
	 * @param node
	 * @param releaseUuid
	 * @param type
	 * @param context
	 */
	private void store(Set<Single<String>> obs, Node node, String releaseUuid, ContainerType type, GenericEntryContext context) {
		if (context.getLanguageTag() != null) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer(context.getLanguageTag(), releaseUuid, type);
			if (container == null) {
				log.warn("Node {" + node.getUuid() + "} has no language container for languageTag {" + context.getLanguageTag()
						+ "}. I can't store the search index document. This may be normal in cases if mesh is handling an outdated search queue batch entry.");
			} else {
				obs.add(storeContainer(container, releaseUuid, type));
			}
			// obs.add(sanitizeIndex(node, container, context.getLanguageTag()).toCompletable());
		} else {
			for (NodeGraphFieldContainer container : node.getGraphFieldContainers(releaseUuid, type)) {
				obs.add(storeContainer(container, releaseUuid, type));
				// obs.add(sanitizeIndex(node, container, context.getLanguageTag()).toCompletable());
			}
		}

	}

	/**
	 * Remove the old container from its index and add the new container to the new index.
	 * 
	 * @param entry
	 * @return
	 */
	public Completable move(MoveDocumentEntry entry) {
		MoveEntryContext context = entry.getContext();
		ContainerType type = context.getContainerType();
		String releaseUuid = context.getReleaseUuid();
		return storeContainer(context.getNewContainer(), releaseUuid, type).toCompletable()
				.andThen(deleteContainer(context.getOldContainer(), releaseUuid, type));
	}

	/**
	 * Deletes the container for the index in which it should reside.
	 * 
	 * @param container
	 * @param releaseUuid
	 * @param type
	 * @return
	 */
	private Completable deleteContainer(NodeGraphFieldContainer container, String releaseUuid, ContainerType type) {
		String projectUuid = container.getParentNode().getProject().getUuid();
		return searchProvider.deleteDocument(container.getIndexName(projectUuid, releaseUuid, type), container.getDocumentId());
	}

	/**
	 * Generate an elasticsearch document object from the given container and stores it in the search index.
	 * 
	 * @param container
	 * @param releaseUuid
	 * @param type
	 * @return Single with affected index name
	 */
	public Single<String> storeContainer(NodeGraphFieldContainer container, String releaseUuid, ContainerType type) {
		JsonObject doc = transformer.toDocument(container, releaseUuid, type);
		String projectUuid = container.getParentNode().getProject().getUuid();
		String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, container.getSchemaContainerVersion().getUuid(), type);
		if (log.isDebugEnabled()) {
			log.debug("Storing node {" + container.getParentNode().getUuid() + "} into index {" + indexName + "}");
		}
		String languageTag = container.getLanguage().getLanguageTag();
		String documentId = NodeGraphFieldContainer.composeDocumentId(container.getParentNode().getUuid(), languageTag);
		return searchProvider.storeDocument(indexName, documentId, doc).andThen(Single.just(indexName));
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
	 * We need to handle permission update requests for nodes here since the action must affect multiple documents in multiple indices .
	 */
	@Override
	public Completable updatePermission(UpdateDocumentEntry entry) {
		String uuid = entry.getElementUuid();
		Node node = getRootVertex().findByUuid(uuid);
		if (node == null) {
			throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, DEFAULT_TYPE);
		} else {
			Project project = node.getProject();

			Set<Observable<String>> obs = new HashSet<>();

			// Determine which documents need to be updated. The node could have multiple documents in various indices.
			for (Release release : project.getReleaseRoot().findAllIt()) {
				for (ContainerType type : Arrays.asList(DRAFT, PUBLISHED)) {
					JsonObject json = getTransformer().toPermissionPartial(node, type);
					for (NodeGraphFieldContainer container : node.getGraphFieldContainers(release, type)) {
						String indexName = container.getIndexName(project.getUuid(), release.getUuid(), type);
						String documentId = container.getDocumentId();
						obs.add(searchProvider.updateDocument(indexName, documentId, json, true).andThen(Observable.just(indexName)));
					}
				}
			}
			return Observable.merge(obs).toList().doOnNext(list -> {
				if (log.isDebugEnabled()) {
					log.debug("Updated object in index.");
				}
				searchProvider.refreshIndex(list.stream().toArray(String[]::new));
			}).toCompletable();
		}
	}

	/**
	 * Validate the schema by creating an index template.
	 * 
	 * @param schema
	 */
	public Completable validate(Schema schema) {
		String indexName = "validationDummy";
		JsonObject mapping = getMappingProvider().getMapping(schema);
		JsonObject settings = schema.getElasticsearch();
		IndexInfo info = new IndexInfo(indexName, settings, mapping);
		return Completable.create(sub -> {
			try {
				schema.validate();
				sub.onCompleted();
			} catch (Exception e) {
				sub.onError(e);
			}
		}).andThen(searchProvider.validateCreateViaTemplate(info));
	}

	/**
	 * Construct the full index settings using the provided schema as a source.
	 * 
	 * @param schema
	 * @return
	 */
	public JsonObject createIndexSettings(Schema schema) {
		JsonObject mapping = getMappingProvider().getMapping(schema);
		JsonObject settings = schema.getElasticsearch();
		IndexInfo info = new IndexInfo("validationDummy", settings, mapping);
		JsonObject fullSettings = searchProvider.createIndexSettings(info);
		return fullSettings;
	}

}
