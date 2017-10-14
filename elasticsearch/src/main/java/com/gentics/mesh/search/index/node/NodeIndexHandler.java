package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.MoveDocumentEntry;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.MeshSearchHit;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
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
 * target language will result in multiple store actions. Each language container will be loaded and stored. This behaviour will also be applied to releases and
 * project information.
 */
@Singleton
public class NodeIndexHandler extends AbstractIndexHandler<Node> {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandler.class);

	private static final int INITIAL_BATCH_SIZE = 30;

	@Inject
	NodeContainerTransformer transformer;

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
		GenericEntryContext context = entry.getContext();
		String projectUuid = context.getProjectUuid();
		String releaseUuid = context.getReleaseUuid();
		String schemaContainerVersionUuid = context.getSchemaContainerVersionUuid();
		ContainerType type = context.getContainerType();
		return NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, schemaContainerVersionUuid, type);
	}

	@Override
	public Completable init() {
		return super.init().andThen(Completable.create(sub -> {
			db.tx(() -> {
				updateNodeIndexMappings();
				sub.onComplete();
			});
		}));
	}

	@Override
	public NodeContainerTransformer getTransformer() {
		return transformer;
	}

	@Override
	public Map<String, String> getIndices() {
		return db.tx(() -> {
			Map<String, String> indexInfo = new HashMap<>();

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
						String documentType = NodeGraphFieldContainer.composeIndexType();
						if (log.isDebugEnabled()) {
							log.debug("Adding index to map of known idices {" + draftIndexName + "");
							log.debug("Adding index to map of known idices {" + publishIndexName + "");
						}
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
	protected RootVertex<Node> getRootVertex() {
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
		return searchProvider.deleteDocument(container.getIndexName(projectUuid, releaseUuid, type), container.getIndexType(),
				container.getDocumentId());
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
		JsonObject doc = transformer.toDocument(container, releaseUuid);
		String projectUuid = container.getParentNode().getProject().getUuid();
		String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, container.getSchemaContainerVersion().getUuid(), type);
		if (log.isDebugEnabled()) {
			log.debug("Storing node {" + container.getParentNode().getUuid() + "} into index {" + indexName + "}");
		}
		String languageTag = container.getLanguage().getLanguageTag();
		String documentId = NodeGraphFieldContainer.composeDocumentId(container.getParentNode().getUuid(), languageTag);
		return searchProvider.storeDocument(indexName, NodeGraphFieldContainer.composeIndexType(), documentId, doc).andThen(Single.just(indexName));
	}

	@Override
	public Completable createIndex(CreateIndexEntry entry) {
		String indexName = entry.getIndexName();
		Map<String, String> indexInfo = getIndices();
		// Only create indices which should be existing
		if (indexInfo.containsKey(indexName)) {
			return searchProvider.createIndex(indexName).andThen(updateNodeIndexMapping(indexName, entry.getSchema()));
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Only found indices:");
				for (String idx : indexInfo.keySet()) {
					log.debug("Index name {" + idx + "}");
				}
			}
			throw error(INTERNAL_SERVER_ERROR, "error_index_unknown", indexName);
		}
	}

	/**
	 * Update the mapping for the schema. The schema information will be used to determine the correct index type.
	 * 
	 * @param schema
	 *            schema
	 * @return Completable
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
		return Completable.defer(() -> {
			String type = NodeGraphFieldContainer.composeIndexType();
			JsonObject mappingJson = transformer.getMapping(schema, type);
			return searchProvider.updateMapping(indexName, type, mappingJson);
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
		for (Project project : boot.meshRoot().getProjectRoot().findAllIt()) {
			// Add the draft and published index names per release to the map
			for (Release release : project.getReleaseRoot().findAllIt()) {
				// Each release specific index has also document type specific mappings
				for (SchemaContainerVersion containerVersion : release.findActiveSchemaVersions()) {
					updateNodeIndexMapping(project, release, containerVersion, DRAFT, containerVersion.getSchema()).blockingAwait();
					updateNodeIndexMapping(project, release, containerVersion, PUBLISHED, containerVersion.getSchema()).blockingAwait();
				}
			}
		}
	}

	/**
	 * Invoke the given query and return a page of node containers.
	 * 
	 * @param gc
	 * @param query
	 *            Elasticsearch query
	 * @param pagingInfo
	 * @return
	 * @throws MeshConfigurationException
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public Page<? extends NodeContent> handleContainerSearch(InternalActionContext ac, String query, PagingParameters pagingInfo,
			GraphPermission... permissions) throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException {
		User user = ac.getUser();

		org.elasticsearch.node.Node esNode = null;
		if (searchProvider.getNode() instanceof org.elasticsearch.node.Node) {
			esNode = (org.elasticsearch.node.Node) searchProvider.getNode();
		} else {
			throw new MeshConfigurationException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getNode() + "}");
		}
		Client client = esNode.client();

		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + query + "} for {" + getElementClass().getName() + "}");
		}
		Set<String> indices = getSelectedIndices(ac);

		/*
		 * TODO, FIXME This a very crude hack but we need to handle paging ourself for now. In order to avoid such nasty ways of paging a custom ES plugin has
		 * to be written that deals with Document Level Permissions/Security (commonly known as DLS)
		 */
		SearchRequestBuilder builder = null;
		builder = client.prepareSearch(indices.toArray(new String[indices.size()]));
		try {
			JSONObject queryStringObject = new JSONObject(query);
			builder.setExtraSource(queryStringObject.toString());
		} catch (Exception e) {
			throw new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e);
		}
		// Only load the documentId we don't care about the indexed contents. The graph is our source of truth here.
		builder.setFetchSource(false);
		builder.setSize(INITIAL_BATCH_SIZE);
		builder.setScroll(new TimeValue(60000));
		SearchResponse scrollResp = builder.execute().actionGet();
		long unfilteredCount = scrollResp.getHits().getTotalHits();
		// The scrolling iterator will wrap the current response and query ES for more data if needed.
		ScrollingIterator scrollingIt = new ScrollingIterator(client, scrollResp);
		Page<? extends NodeContent> page = db.tx(() -> {

			// Prepare a stream which applies all needed filtering
			Stream<NodeContent> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(scrollingIt, Spliterator.ORDERED), false)

					.map(hit -> {
						String id = hit.getId();
						int pos = id.indexOf("-");

						String language = pos > 0 ? id.substring(pos + 1) : null;
						String uuid = pos > 0 ? id.substring(0, pos) : id;

						return new MeshSearchHit<Node>(uuid, language);
					})
					// TODO filter by requested language
					.filter(hit -> {
						return hit.language != null;
					})

					.map(hit -> {
						// Load the node
						hit.element = getRootVertex().findByUuid(hit.uuid);
						if (hit.element == null) {
							log.error(
									"Object could not be found for uuid {" + hit.uuid + "} in root vertex {" + getRootVertex().getRootLabel() + "}");
						}

						return hit;
					})

					.filter(hit -> {
						// Only include found elements
						return hit.element != null;
					})

					.filter(hit -> {
						// TODO check permissions without loading the vertex
						for (GraphPermission permission : permissions) {
							boolean hasPerm = user.hasPermission(hit.element, permission);
							if (hasPerm) {
								return true;
							}
						}
						return false;
					})

					.map(hit -> {

						ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
						Language languageTag = boot.languageRoot().findByLanguageTag(hit.language);
						if (languageTag == null) {
							log.debug("Could not find language {" + hit.language + "}");
							return null;
						}

						// Locate the matching container and add it to the list of found containers
						NodeGraphFieldContainer container = hit.element.getGraphFieldContainer(languageTag, ac.getRelease(), type);
						if (container != null) {
							return new NodeContent(hit.element, container);
						}
						return null;
					})

					.filter(hit -> {
						return hit != null;
					});
			DynamicStreamPageImpl<NodeContent> dynamicPage = new DynamicStreamPageImpl<>(stream, pagingInfo);
			dynamicPage.setUnfilteredSearchCount(unfilteredCount);
			return dynamicPage;
		});
		return page;
	}

}
