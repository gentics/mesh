package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.HandleContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Handler for the node specific search index.
 * 
 * This handler can process {@link UpdateDocumentEntry} objects which may contain additional {@link HandleContext} information. The handler will use the context
 * information in order to determine which elements need to be stored in or removed from the index.
 * 
 * Additionally the handler may infer the scope of store actions if the context information is lacking certain information. A context which does not include the
 * target language will result in multiple store actions. Each language container will be loaded and stored. This behaviour will also be applied to releases and
 * project information.
 */
@Singleton
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	@Inject
	NodeContainerTransformator transformator;

	@Inject
	public NodeIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	protected Class<Node> getElementClass() {
		return Node.class;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return NodeGraphFieldContainer.composeDocumentId(entry.getElementUuid(), entry.getContext().getLanguageTag());
	}

	@Override
	protected String composeIndexTypeFromEntry(UpdateDocumentEntry entry) {
		return NodeGraphFieldContainer.composeIndexType();
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		HandleContext context = entry.getContext();
		String projectUuid = context.getProjectUuid();
		String releaseUuid = context.getReleaseUuid();
		String schemaContainerVersionUuid = context.getSchemaContainerVersionUuid();
		ContainerType type = context.getContainerType();
		return NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, schemaContainerVersionUuid, type);
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

	@Override
	public NodeContainerTransformator getTransformator() {
		return transformator;
	}

	@Override
	public Map<String, String> getIndices() {
		return db.noTx(() -> {
			Map<String, String> indexInfo = new HashMap<>();

			// Iterate over all projects and construct the index names
			boot.meshRoot().getProjectRoot().reload();
			List<? extends Project> projects = boot.meshRoot().getProjectRoot().findAll();
			for (Project project : projects) {
				List<? extends Release> releases = project.getReleaseRoot().findAll();
				// Add the draft and published index names per release to the map
				for (Release release : releases) {
					// Each release specific index has also document type specific mappings
					for (SchemaContainerVersion containerVersion : release.findAllSchemaVersions()) {
						String draftIndexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), release.getUuid(),
								containerVersion.getUuid(), DRAFT);
						String publishIndexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), release.getUuid(),
								containerVersion.getUuid(), PUBLISHED);
						String documentType = NodeGraphFieldContainer.composeIndexType();
						indexInfo.put(draftIndexName, documentType);
						indexInfo.put(publishIndexName, documentType);
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
					indices.add(NodeGraphFieldContainer.composeIndexName(project.getUuid(), release.getUuid(), version.getUuid(),
							ContainerType.forVersion(ac.getVersioningParameters().getVersion())));
				}
			} else {
				// The project was not specified. Maybe a global search wants to
				// know which indices must be searched. In that case we just
				// iterate over all projects and collect index names per
				// release.
				List<? extends Project> projects = boot.meshRoot().getProjectRoot().findAll();
				for (Project currentProject : projects) {
					for (Release release : currentProject.getReleaseRoot().findAll()) {
						for (SchemaContainerVersion version : release.findAllSchemaVersions()) {
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
	protected RootVertex<Node> getRootVertex() {
		return boot.meshRoot().getNodeRoot();
	}

	@Override
	public Completable store(Node node, UpdateDocumentEntry entry) {
		return Completable.defer(() -> {
			HandleContext context = entry.getContext();
			Set<Single<String>> obs = new HashSet<>();
			try (NoTx noTrx = db.noTx()) {
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
	private void store(Set<Single<String>> obs, Node node, HandleContext context) {
		if (context.getReleaseUuid() == null) {
			for (Release release : node.getProject().getReleaseRoot().findAll()) {
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
	private void store(Set<Single<String>> obs, Node node, String releaseUuid, HandleContext context) {
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
	private void store(Set<Single<String>> obs, Node node, String releaseUuid, ContainerType type, HandleContext context) {
		if (context.getLanguageTag() != null) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer(context.getLanguageTag(), releaseUuid, type);
			if (container == null) {
				log.warn("Node {" + node.getUuid() + "} has no language container for languageTag {" + context.getLanguageTag()
						+ "}. I can't store the search index document. This may be normal in cases if mesh is handling an outdated search queue batch entry.");
			} else {
				obs.add(storeContainer(container, releaseUuid, type));
			}
			//			obs.add(sanitizeIndex(node, container, context.getLanguageTag()).toCompletable());
		} else {
			for (NodeGraphFieldContainer container : node.getGraphFieldContainers(releaseUuid, type)) {
				obs.add(storeContainer(container, releaseUuid, type));
				//				obs.add(sanitizeIndex(node, container, context.getLanguageTag()).toCompletable());
			}
		}

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
		JsonObject doc = transformator.toDocument(container, releaseUuid);
		String projectUuid = container.getParentNode().getProject().getUuid();
		String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, container.getSchemaContainerVersion().getUuid(), type);
		if (log.isDebugEnabled()) {
			log.debug("Storing node {" + container.getParentNode().getUuid() + "} into index {" + indexName + "}");
		}
		String languageTag = container.getLanguage().getLanguageTag();
		String documentId = NodeGraphFieldContainer.composeDocumentId(container.getParentNode().getUuid(), languageTag);
		return searchProvider.storeDocument(indexName, NodeGraphFieldContainer.composeIndexType(), documentId, doc).andThen(Single.just(indexName));
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

	/**
	 * Update the node mapping for the index which is identified using the provided elements.
	 * 
	 * @param project
	 * @param release
	 * @param schemaVersion
	 * @param containerType
	 * @param schema
	 * @return
	 */
	public Completable updateNodeIndexMapping(Project project, Release release, SchemaContainerVersion schemaVersion, ContainerType containerType,
			Schema schema) {
		String indexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), release.getUuid(), schemaVersion.getUuid(), containerType);
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

		// String type = schema.getName() + "-" + schema.getVersion();
		String type = NodeGraphFieldContainer.composeIndexType();
		// Check whether the search provider is a dummy provider or not
		if (searchProvider.getNode() == null) {
			return Completable.complete();
		}
		return Completable.create(sub -> {
			org.elasticsearch.node.Node esNode = getESNode();
			PutMappingRequestBuilder mappingRequestBuilder = esNode.client().admin().indices().preparePutMapping(indexName);
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
				// Each release specific index has also document type specific
				// mappings
				for (SchemaContainerVersion containerVersion : release.findAllSchemaVersions()) {
					updateNodeIndexMapping(project, release, containerVersion, DRAFT, containerVersion.getSchema()).await();
					updateNodeIndexMapping(project, release, containerVersion, PUBLISHED, containerVersion.getSchema()).await();
				}
			}
		});

	}

}
