package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.MoveDocumentEntry;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.bulk.DeleteBulkEntry;
import com.gentics.mesh.core.data.search.bulk.IndexBulkEntry;
import com.gentics.mesh.core.data.search.bulk.UpdateBulkEntry;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetric;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for the node specific search index.
 * 
 * This handler can process {@link UpdateDocumentEntry} objects which may contain additional {@link GenericEntryContextImpl} information. The handler will use
 * the context information in order to determine which elements need to be stored in or removed from the index.
 * 
 * Additionally the handler may infer the scope of store actions if the context information is lacking certain information. A context which does not include the
 * target language will result in multiple store actions. Each language container will be loaded and stored. This behaviour will also be applied to branches and
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
	public NodeIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper) {
		super(searchProvider, db, boot, helper);
	}

	@Override
	public Class<Node> getElementClass() {
		return Node.class;
	}

	@Override
	public String getType() {
		return "node";
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return NodeGraphFieldContainer.composeDocumentId(entry.getElementUuid(), entry.getContext().getLanguageTag());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		GenericEntryContext context = entry.getContext();
		String projectUuid = context.getProjectUuid();
		String branchUuid = context.getBranchUuid();
		String schemaContainerVersionUuid = context.getSchemaContainerVersionUuid();
		ContainerType type = context.getContainerType();
		return NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid, schemaContainerVersionUuid, type);
	}

	@Override
	public NodeContainerTransformer getTransformer() {
		return transformer;
	}

	@Override
	public NodeContainerMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		return db.tx(tx -> {
			Map<String, IndexInfo> indexInfo = new HashMap<>();

			// Iterate over all projects and construct the index names
			for (Project project : boot.meshRoot().getProjectRoot().findAll()) {
				for (Branch branch : project.getBranchRoot().findAll()) {
					indexInfo.putAll(getIndices(project, branch).runInExistingTx(tx));
				}
			}
			return indexInfo;
		});
	}

	public Transactional<Map<String, IndexInfo>> getIndices(Project project, Branch branch) {
		return db.transactional(tx -> {
			Map<String, IndexInfo> indexInfo = new HashMap<>();
			// Each branch specific index has also document type specific mappings
			for (SchemaContainerVersion containerVersion : branch.findActiveSchemaVersions()) {
				indexInfo.putAll(getIndices(project, branch, containerVersion).runInExistingTx(tx));
			}
			return indexInfo;
		});
	}

	public Transactional<Map<String, IndexInfo>> getIndices(Project project, Branch branch, SchemaContainerVersion containerVersion) {
		return db.transactional(tx -> {
			Map<String, IndexInfo> indexInfo = new HashMap<>();
			String draftIndexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(), containerVersion
				.getUuid(), DRAFT);
			String publishIndexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(), containerVersion
				.getUuid(), PUBLISHED);
			if (log.isDebugEnabled()) {
				log.debug("Adding index to map of known indices {" + draftIndexName + "}");
				log.debug("Adding index to map of known indices {" + publishIndexName + "}");
			}
			// Load the index mapping information for the index
			SchemaModel schema = containerVersion.getSchema();
			JsonObject mapping = getMappingProvider().getMapping(schema, branch);
			JsonObject settings = schema.getElasticsearch();
			IndexInfo draftInfo = new IndexInfo(draftIndexName, settings, mapping, schema.getName() + "@" + schema.getVersion());
			IndexInfo publishInfo = new IndexInfo(publishIndexName, settings, mapping, schema.getName() + "@" + schema.getVersion());

			indexInfo.put(draftIndexName, draftInfo);
			indexInfo.put(publishIndexName, publishInfo);
			return indexInfo;
		});
	}


	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		Set<String> activeIndices = new HashSet<>();
		db.tx(() -> {
			for (Project currentProject : boot.meshRoot().getProjectRoot().findAll()) {
				for (Branch branch : currentProject.getBranchRoot().findAll()) {
					for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
						Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED).forEach(type -> {
							activeIndices
								.add(NodeGraphFieldContainer.composeIndexName(currentProject.getUuid(), branch.getUuid(), version.getUuid(),
										type));
						});
					}
				}
			}
		});

		if (log.isDebugEnabled()) {
			for (String name : activeIndices) {
				log.debug("Active index: {" + name + "}");
			}
		}
		return indices.stream()
			// Only handle indices of the handler's type
			.filter(i -> i.startsWith(getType()))
			// Filter out indices which are active
			.filter(i -> !activeIndices.contains(i))
			.collect(Collectors.toSet());
	}

	@Override
	public Flowable<SearchRequest> syncIndices() {
		return Flowable.defer(() -> db.tx(() -> {
			SyncMetric metric = new SyncMetric(getType());
			return boot.meshRoot().getProjectRoot().findAll().stream()
				.flatMap(project -> project.getBranchRoot().findAll().stream()
				.flatMap(branch -> branch.findActiveSchemaVersions().stream()
				.flatMap(version -> Stream.of(DRAFT, PUBLISHED)
				.map(type -> diffAndSync(project, branch, version, type, metric)))))
				.collect(Collectors.collectingAndThen(Collectors.toList(), Flowable::merge));
		}));
	}

	private Map<String, NodeGraphFieldContainer> loadVersionsFromGraph(Branch branch, SchemaContainerVersion version, ContainerType type) {
		return db.tx(() -> {
			String branchUuid = branch.getUuid();
			return version.getFieldContainers(branchUuid)
				.filter(c -> c.getSchemaContainerVersion().equals(version))
				.filter(c -> c.isType(type, branchUuid))
				.collect(Collectors.toMap(c -> c.getParentNode().getUuid() + "-" + c.getLanguageTag(), Function.identity()));
		});
	}

	/**
	 * We need to override the default method since the UUID alone is not enough to id a document in the node index. We also need to append the language.
	 */
	@Override
	protected void processHits(JsonArray hits, Map<String, String> versions) {
		for (int i = 0; i < hits.size(); i++) {
			JsonObject hit = hits.getJsonObject(i);
			JsonObject source = hit.getJsonObject("_source");
			// The id contains the UUID + language
			String uuidAndLang = hit.getString("_id");
			String version = source.getString("version");
			versions.put(uuidAndLang, version);
		}
	}

	private Flowable<SearchRequest> diffAndSync(Project project, Branch branch, SchemaContainerVersion version, ContainerType type, SyncMetric metric) {
		String indexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(),
			version.getUuid(), type);

		return Single.zip(
			loadVersionsFromIndex(indexName),
			Single.fromCallable(() -> loadVersionsFromGraph(branch, version, type)),
			(sinkVersions, sourceNodes) -> {
				log.info("Handling index sync on handler {" + getClass().getName() + "}");
				String branchUuid = branch.getUuid();

				Map<String, String> sourceVersions = db.tx(() -> sourceNodes.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, x -> generateVersion(x.getValue(), branchUuid, type))));

				// 3. Diff the maps
				MapDifference<String, String> diff = Maps.difference(sourceVersions, sinkVersions);
				if (diff.areEqual()) {
					return Flowable.<SearchRequest>empty();
				}
				Set<String> needInsertionInES = diff.entriesOnlyOnLeft().keySet();
				Set<String> needRemovalInES = diff.entriesOnlyOnRight().keySet();
				Set<String> needUpdateInEs = diff.entriesDiffering().keySet();

				log.info("Pending insertions on {" + indexName + "}:" + needInsertionInES.size());
				log.info("Pending removals on {" + indexName + "}:" + needRemovalInES.size());
				log.info("Pending updates on {" + indexName + "}:" + needUpdateInEs.size());

				metric.incInsert(needInsertionInES.size());
				metric.incDelete(needRemovalInES.size());
				metric.incUpdate(needUpdateInEs.size());

				io.reactivex.functions.Function<
					Action,
					io.reactivex.functions.Function<String, CreateDocumentRequest>
					> toCreateRequest = action -> uuid -> {
					JsonObject doc = db.tx(() -> getTransformer().toDocument(sourceNodes.get(uuid), branchUuid, type));
					return helper.createDocumentRequest(indexName, uuid, doc, action);
				};

				Flowable<SearchRequest> toInsert = Flowable.fromIterable(needInsertionInES)
					.map(toCreateRequest.apply(metric::decInsert));

				Flowable<SearchRequest> toUpdate = Flowable.fromIterable(needUpdateInEs)
					.map(toCreateRequest.apply(metric::decUpdate));

				Flowable<SearchRequest> toDelete = Flowable.fromIterable(needRemovalInES)
					.map(uuid -> helper.deleteDocumentRequest(indexName, uuid, metric::decDelete));

				return Flowable.merge(toInsert, toUpdate, toDelete);
		}).flatMapPublisher(x -> x);
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return db.tx(() -> {
			Set<String> indices = new HashSet<>();
			Project project = ac.getProject();
			if (project != null) {
				Branch branch = ac.getBranch();
				// Locate all schema versions which need to be taken into consideration when choosing the indices
				for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
					indices.add(NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(), version.getUuid(), ContainerType
						.forVersion(ac.getVersioningParameters().getVersion())));
				}
			} else {
				// The project was not specified. Maybe a global search wants to know which indices must be searched.
				// In that case we just iterate over all projects and collect index names per branch.
				for (Project currentProject : boot.meshRoot().getProjectRoot().findAll()) {
					for (Branch branch : currentProject.getBranchRoot().findAll()) {
						for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
							indices.add(NodeGraphFieldContainer.composeIndexName(currentProject.getUuid(), branch.getUuid(), version.getUuid(),
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
			return Observable.fromIterable(obs).map(x -> x.toObservable()).flatMap(x -> x).distinct().ignoreElements();
		});
	}

	public Observable<IndexBulkEntry> storeForBulk(Node node, UpdateDocumentEntry entry) {
		GenericEntryContext context = entry.getContext();
		try (Tx tx = db.tx()) {
			return storeForBulk(node, context);
		}
	}

	/**
	 * Step 1 - Check whether we need to handle all branches.
	 * 
	 * @param obs
	 * @param node
	 * @param context
	 */
	private void store(Set<Single<String>> obs, Node node, GenericEntryContext context) {
		if (context.getBranchUuid() == null) {
			for (Branch branch : node.getProject().getBranchRoot().findAll()) {
				store(obs, node, branch.getUuid(), context);
			}
		} else {
			store(obs, node, context.getBranchUuid(), context);
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
	 * @param branchUuid
	 * @param context
	 */
	private void store(Set<Single<String>> obs, Node node, String branchUuid, GenericEntryContext context) {
		if (context.getContainerType() == null) {
			for (ContainerType type : ContainerType.values()) {
				// We only want to store DRAFT and PUBLISHED Types
				if (type == DRAFT || type == PUBLISHED) {
					store(obs, node, branchUuid, type, context);
				}
			}
		} else {
			store(obs, node, branchUuid, context.getContainerType(), context);
		}
	}

	/**
	 * Step 3 - Check whether we need to handle all languages.
	 * 
	 * Invoke store for the possible set of containers. Utilise the given context settings as much as possible.
	 * 
	 * @param obs
	 * @param node
	 * @param branchUuid
	 * @param type
	 * @param context
	 */
	private void store(Set<Single<String>> obs, Node node, String branchUuid, ContainerType type, GenericEntryContext context) {
		if (context.getLanguageTag() != null) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer(context.getLanguageTag(), branchUuid, type);
			if (container == null) {
				log.warn("Node {" + node.getUuid() + "} has no language container for languageTag {" + context.getLanguageTag()
					+ "}. I can't store the search index document. This may be normal in cases if mesh is handling an outdated search queue batch entry.");
			} else {
				obs.add(storeContainer(container, branchUuid, type));
			}
		} else {
			for (NodeGraphFieldContainer container : node.getGraphFieldContainersIt(branchUuid, type)) {
				obs.add(storeContainer(container, branchUuid, type));
			}
		}

	}

	/**
	 * Step 1 - Check whether we need to handle all branches.
	 * 
	 * @param node
	 * @param context
	 * @return
	 */
	private Observable<IndexBulkEntry> storeForBulk(Node node, GenericEntryContext context) {
		if (context.getBranchUuid() == null) {
			Set<Observable<IndexBulkEntry>> obs = new HashSet<>();
			for (Branch branch : node.getProject().getBranchRoot().findAll()) {
				obs.add(storeForBulk(node, branch.getUuid(), context));
			}
			return Observable.merge(obs);
		} else {
			return storeForBulk(node, context.getBranchUuid(), context);
		}
	}

	/**
	 * Step 2 - Check whether we need to handle all container types.
	 * 
	 * Add the possible store actions to the set of observables. This method will utilise as much of the provided context data if possible. It will also handle
	 * fallback options and invoke store for all types if the container type has not been specified.
	 * 
	 * @param node
	 * @param branchUuid
	 * @param context
	 * @return
	 */
	private Observable<IndexBulkEntry> storeForBulk(Node node, String branchUuid, GenericEntryContext context) {
		if (context.getContainerType() == null) {
			Set<Observable<IndexBulkEntry>> obs = new HashSet<>();
			for (ContainerType type : ContainerType.values()) {
				// We only want to store DRAFT and PUBLISHED Types
				if (type == DRAFT || type == PUBLISHED) {
					obs.add(storeForBulk(node, branchUuid, type, context));
				}
			}
			return Observable.merge(obs);
		} else {
			return storeForBulk(node, branchUuid, context.getContainerType(), context);
		}
	}

	/**
	 * Step 3 - Check whether we need to handle all languages.
	 * 
	 * Invoke store for the possible set of containers. Utilise the given context settings as much as possible.
	 * 
	 * @param node
	 * @param branchUuid
	 * @param type
	 * @param context
	 * @return
	 */
	private Observable<IndexBulkEntry> storeForBulk(Node node, String branchUuid, ContainerType type, GenericEntryContext context) {
		if (context.getLanguageTag() != null) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer(context.getLanguageTag(), branchUuid, type);
			if (container == null) {
				log.warn("Node {" + node.getUuid() + "} has no language container for languageTag {" + context.getLanguageTag()
					+ "}. I can't store the search index document. This may be normal in cases if mesh is handling an outdated search queue batch entry.");
			} else {
				return storeContainerForBulk(container, branchUuid, type).toObservable();
			}
		} else {
			Set<Observable<IndexBulkEntry>> obs = new HashSet<>();
			for (NodeGraphFieldContainer container : node.getGraphFieldContainersIt(branchUuid, type)) {
				obs.add(storeContainerForBulk(container, branchUuid, type).toObservable());
			}
			return Observable.merge(obs);
		}
		return Observable.empty();

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
		String branchUuid = context.getBranchUuid();
		return storeContainer(context.getNewContainer(), branchUuid, type).toCompletable().andThen(deleteContainer(context.getOldContainer(),
			branchUuid, type));
	}

	public Observable<? extends BulkEntry> moveForBulk(MoveDocumentEntry entry) {
		MoveEntryContext context = entry.getContext();
		ContainerType type = context.getContainerType();
		String releaseUuid = context.getBranchUuid();

		NodeGraphFieldContainer oldContainer = context.getOldContainer();
		String oldProjectUuid = oldContainer.getParentNode().getProject().getUuid();
		String oldIndexName = NodeGraphFieldContainer.composeIndexName(oldProjectUuid, releaseUuid,
			oldContainer.getSchemaContainerVersion().getUuid(),
			type);
		String oldLanguageTag = oldContainer.getLanguageTag();
		String oldDocumentId = NodeGraphFieldContainer.composeDocumentId(oldContainer.getParentNode().getUuid(), oldLanguageTag);
		Observable<DeleteBulkEntry> deleteEntry = Observable.just(new DeleteBulkEntry(oldIndexName, oldDocumentId));

		NodeGraphFieldContainer newContainer = context.getNewContainer();
		String newProjectUuid = newContainer.getParentNode().getProject().getUuid();
		String newIndexName = NodeGraphFieldContainer.composeIndexName(newProjectUuid, releaseUuid,
			newContainer.getSchemaContainerVersion().getUuid(),
			type);
		String newLanguageTag = newContainer.getLanguageTag();
		String newDocumentId = NodeGraphFieldContainer.composeDocumentId(newContainer.getParentNode().getUuid(), newLanguageTag);
		JsonObject doc = transformer.toDocument(newContainer, releaseUuid, type);
		return 	Observable.just(new IndexBulkEntry(newIndexName, newDocumentId, doc));

	}

	/**
	 * Deletes the container for the index in which it should reside.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	private Completable deleteContainer(NodeGraphFieldContainer container, String branchUuid, ContainerType type) {
		String projectUuid = container.getParentNode().getProject().getUuid();
		return searchProvider.deleteDocument(container.getIndexName(projectUuid, branchUuid, type), container.getDocumentId());
	}

	/**
	 * Generate an elasticsearch document object from the given container and stores it in the search index.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return Single with affected index name
	 */
	public Single<String> storeContainer(NodeGraphFieldContainer container, String branchUuid, ContainerType type) {
		JsonObject doc = transformer.toDocument(container, branchUuid, type);
		String projectUuid = container.getParentNode().getProject().getUuid();
		String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid, container.getSchemaContainerVersion().getUuid(), type);
		if (log.isDebugEnabled()) {
			log.debug("Storing node {" + container.getParentNode().getUuid() + "} into index {" + indexName + "}");
		}
		String languageTag = container.getLanguageTag();
		String documentId = NodeGraphFieldContainer.composeDocumentId(container.getParentNode().getUuid(), languageTag);
		return searchProvider.storeDocument(indexName, documentId, doc).andThen(Single.just(indexName));
	}

	/**
	 * Generate an elasticsearch document object from the given container and stores it in the search index.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return Single with the bulk entry
	 */
	public Single<IndexBulkEntry> storeContainerForBulk(NodeGraphFieldContainer container, String branchUuid, ContainerType type) {
		JsonObject doc = transformer.toDocument(container, branchUuid, type);
		String projectUuid = container.getParentNode().getProject().getUuid();
		String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid, container.getSchemaContainerVersion().getUuid(), type);
		if (log.isDebugEnabled()) {
			log.debug("Storing node {" + container.getParentNode().getUuid() + "} into index {" + indexName + "}");
		}
		String languageTag = container.getLanguageTag();
		String documentId = NodeGraphFieldContainer.composeDocumentId(container.getParentNode().getUuid(), languageTag);

		return Single.just(new IndexBulkEntry(indexName, documentId, doc));
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
	public Observable<UpdateBulkEntry> updatePermissionForBulk(UpdateDocumentEntry entry) {
		String uuid = entry.getElementUuid();
		Node node = getRootVertex().findByUuid(uuid);
		if (node == null) {
			throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, DEFAULT_TYPE);
		} else {
			Project project = node.getProject();

			List<UpdateBulkEntry> entries = new ArrayList<>();

			// Determine which documents need to be updated. The node could have multiple documents in various indices.
			for (Branch branch : project.getBranchRoot().findAll()) {
				for (ContainerType type : Arrays.asList(DRAFT, PUBLISHED)) {
					JsonObject json = getTransformer().toPermissionPartial(node, type);
					for (NodeGraphFieldContainer container : node.getGraphFieldContainersIt(branch, type)) {
						String indexName = container.getIndexName(project.getUuid(), branch.getUuid(), type);
						String documentId = container.getDocumentId();
						entries.add(new UpdateBulkEntry(indexName, documentId, json, true));
					}
				}
			}

			return Observable.fromIterable(entries);
		}
	}

	/**
	 * Validate the schema by creating an index template.
	 * 
	 * @param schema
	 */
	public Completable validate(Schema schema) {
		String indexName = "validationDummy";
		JsonObject mapping = getMappingProvider().getMapping(schema, null);
		JsonObject settings = schema.getElasticsearch();
		IndexInfo info = new IndexInfo(indexName, settings, mapping, schema.getName());
		return Completable.create(sub -> {
			try {
				schema.validate();
				sub.onComplete();
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
		JsonObject mapping = getMappingProvider().getMapping(schema, null);
		JsonObject settings = schema.getElasticsearch();
		IndexInfo info = new IndexInfo("validationDummy", settings, mapping, schema.getName());
		JsonObject fullSettings = searchProvider.createIndexSettings(info);
		return fullSettings;
	}

	/**
	 * Generate the version for the container that should be transformed.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	public String generateVersion(NodeGraphFieldContainer container, String branchUuid, ContainerType type) {
		return getTransformer().generateVersion(container, branchUuid, type);
	}
}
