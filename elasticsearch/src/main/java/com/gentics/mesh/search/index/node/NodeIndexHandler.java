package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.search.index.node.NodeIndexUtil.getDefaultSetting;
import static com.gentics.mesh.search.index.node.NodeIndexUtil.getLanguageOverride;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactivestreams.Publisher;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.search.BucketableElement;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.Bucket;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import io.reactivex.Completable;
import io.reactivex.Flowable;
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
	public NodeIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetersFactory, BucketManager bucketManager) {
		super(searchProvider, db, boot, helper, options, syncMetersFactory, bucketManager);
	}

	@Override
	public Class<? extends BucketableElement> getElementClass() {
		return Node.class;
	}

	@Override
	public String getType() {
		return "node";
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
			Map<String, IndexInfo> indexInfos = new HashMap<>();
			SchemaModel schema = containerVersion.getSchema();

			// Add all language specific indices (might be none)
			schema.findOverriddenSearchLanguages().forEach(language -> Stream.of(DRAFT, PUBLISHED).forEach(version -> {
				String indexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(), containerVersion
					.getUuid(), version, language, containerVersion.getMicroschemaVersionHash(branch));
				log.debug("Adding index to map of known indices {" + indexName + "}");
				// Load the index mapping information for the index
				indexInfos.put(indexName, createIndexInfo(branch, schema, language, indexName, schema.getName() + "@" + schema.getVersion()));
			}));

			// And all default indices
			Stream.of(DRAFT, PUBLISHED).forEach(version -> {
				String indexName = NodeGraphFieldContainer.composeIndexName(project.getUuid(), branch.getUuid(), containerVersion
					.getUuid(), version, containerVersion.getMicroschemaVersionHash(branch));
				log.debug("Adding index to map of known indices {" + indexName + "}");
				// Load the index mapping information for the index
				indexInfos.put(indexName, createIndexInfo(branch, schema, null, indexName, schema.getName() + "@" + schema.getVersion()));
			});
			return indexInfos;
		});
	}

	public IndexInfo createIndexInfo(Branch branch, Schema schema, String language, String indexName, String sourceInfo) {
		JsonObject mapping = getMappingProvider().getMapping(schema, branch, language);
		JsonObject settings = language == null
			? getDefaultSetting(schema.getElasticsearch())
			: getLanguageOverride(schema.getElasticsearch(), language);
		return new IndexInfo(indexName, settings, mapping, sourceInfo);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		Set<String> activeIndices = new HashSet<>();
		db.tx(() -> {
			for (Project currentProject : boot.meshRoot().getProjectRoot().findAll()) {
				for (Branch branch : currentProject.getBranchRoot().findAll()) {
					for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
						if (log.isDebugEnabled()) {
							log.debug("Found active schema version {}-{} in branch {}", version.getSchema().getName(), version.getVersion(),
								branch.getName());
						}

						// add all language specific indices
						version.getSchema().findOverriddenSearchLanguages()
								.forEach(language -> Stream.of(DRAFT, PUBLISHED).forEach(type -> {
									activeIndices.add(NodeGraphFieldContainer.composeIndexName(currentProject.getUuid(),
											branch.getUuid(), version.getUuid(), type, language, version.getMicroschemaVersionHash(branch)));
								}));

						Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED).forEach(type -> {
							activeIndices
								.add(NodeGraphFieldContainer.composeIndexName(currentProject.getUuid(), branch.getUuid(), version.getUuid(),
									type, version.getMicroschemaVersionHash(branch)));
						});
					}
				}
			}
		});

		if (log.isDebugEnabled()) {
			log.debug(
				"All indices:\n" +
					String.join("\n", indices) + "\n" +
					"Active indices: \n" +
					String.join("\n", activeIndices));
		}
		return indices.stream()
			// Only handle indices of the handler's type
			.filter(i -> i.startsWith(getType()))
			// Filter out indices which are active
			.filter(i -> !activeIndices.contains(i))
			.collect(Collectors.toSet());
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return Flowable.defer(() -> db.tx(() -> {
			return boot.meshRoot().getProjectRoot().findAll().stream()
				.flatMap(project -> project.getBranchRoot().findAll().stream()
					.flatMap(branch -> branch.findActiveSchemaVersions().stream()
						.flatMap(version -> Stream.of(DRAFT, PUBLISHED)
							.map(type -> diffAndSync(project, branch, version, type, indexPattern)))))
				.collect(Collectors.collectingAndThen(Collectors.toList(), Flowable::merge));
		}));
	}

	/**
	 *
	 * @param branch
	 * @param version
	 * @param type
	 * @param bucketId
	 * @return indexName -> documentName -> NodeGraphFieldContainer
	 */
	private Map<String, Map<String, NodeGraphFieldContainer>> loadVersionsFromGraph(Branch branch, SchemaContainerVersion version, ContainerType type,
		Bucket bucket) {
		return db.tx(() -> {
			String branchUuid = branch.getUuid();
			List<String> indexLanguages = version.getSchema().findOverriddenSearchLanguages().collect(Collectors.toList());

			Map<String, Map<String, NodeGraphFieldContainer>> map = version.getFieldContainers(branchUuid, bucket)
				.filter(c -> c.isType(type, branchUuid))
				.collect(Collectors.groupingBy(content -> {
					String languageTag = content.getLanguageTag();
					return NodeGraphFieldContainer.composeIndexName(
						branch.getProject().getUuid(),
						branchUuid,
						version.getUuid(),
						type,
						indexLanguages.contains(languageTag)
							? languageTag
							: null, version.getMicroschemaVersionHash(branch));
				},
					Collectors.toMap(c -> c.getParentNode().getUuid() + "-" + c.getLanguageTag(), Function.identity())));
			return map;
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

	private Flowable<SearchRequest> diffAndSync(Project project, Branch branch, SchemaContainerVersion version, ContainerType type, Optional<Pattern> indexPattern) {
		log.info("Handling index sync on handler {" + getClass().getName() + "}");
		// Sync each bucket individually
		Flowable<Bucket> buckets = bucketManager.getBuckets(NodeGraphFieldContainer.class);
		return buckets.flatMap(bucket -> {
			log.info("Handling sync of {" + bucket + "}");
			return diffAndSync(project, branch, version, type, bucket, indexPattern);
		}, 1);
	}

	private Publisher<? extends SearchRequest> diffAndSync(Project project, Branch branch, SchemaContainerVersion version, ContainerType type,
		Bucket bucket, Optional<Pattern> indexPattern) {
		return Flowable.defer(() -> {
			Map<String, Map<String, NodeGraphFieldContainer>> sourceNodesPerIndex = loadVersionsFromGraph(branch, version, type, bucket);
			return Flowable.fromIterable(getIndexNames(project, branch, version, type))
				.filter(indexName -> {
					boolean match = indexPattern.orElse(MATCH_ALL).matcher(indexName).matches();
					if (!match && log.isDebugEnabled()) {
						log.debug("Index {} does not match pattern {} and will be omitted from sync", indexName, indexPattern);
					}
					return match;
				})
				.flatMap(indexName -> loadVersionsFromIndex(indexName, bucket).flatMapPublisher(sinkVersions -> {
					log.debug("Handling index sync on handler {" + getClass().getName() + "} for bucket {" + bucket + "}");
					Map<String, NodeGraphFieldContainer> sourceNodes = sourceNodesPerIndex.getOrDefault(indexName, Collections.emptyMap());
					String branchUuid = branch.getUuid();

					Map<String, String> sourceVersions = db.tx(() -> sourceNodes.entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, x -> generateVersion(x.getValue(), branchUuid, type))));

					// 3. Diff the maps
					MapDifference<String, String> diff = Maps.difference(sourceVersions, sinkVersions);
					if (diff.areEqual()) {
						return Flowable.empty();
					}
					Set<String> needInsertionInES = diff.entriesOnlyOnLeft().keySet();
					Set<String> needRemovalInES = diff.entriesOnlyOnRight().keySet();
					Set<String> needUpdateInEs = diff.entriesDiffering().keySet();

					log.debug("Pending insertions on {" + indexName + "}:" + needInsertionInES.size());
					log.debug("Pending removals on {" + indexName + "}:" + needRemovalInES.size());
					log.debug("Pending updates on {" + indexName + "}:" + needUpdateInEs.size());

					meters.getInsertMeter().addPending(needInsertionInES.size());
					meters.getDeleteMeter().addPending(needRemovalInES.size());
					meters.getUpdateMeter().addPending(needUpdateInEs.size());

					io.reactivex.functions.Function<Action, io.reactivex.functions.Function<String, CreateDocumentRequest>> toCreateRequest = action -> uuid -> {
						JsonObject doc = db.tx(() -> getTransformer().toDocument(sourceNodes.get(uuid), branchUuid, type));
						return helper.createDocumentRequest(indexName, uuid, doc, complianceMode, action);
					};

					Flowable<SearchRequest> toInsert = Flowable.fromIterable(needInsertionInES)
						.map(toCreateRequest.apply(meters.getInsertMeter()::synced));

					Flowable<SearchRequest> toUpdate = Flowable.fromIterable(needUpdateInEs)
						.map(toCreateRequest.apply(meters.getUpdateMeter()::synced));

					Flowable<SearchRequest> toDelete = Flowable.fromIterable(needRemovalInES)
						.map(uuid -> helper.deleteDocumentRequest(indexName, uuid, complianceMode, meters.getDeleteMeter()::synced));

					return Flowable.merge(toInsert, toUpdate, toDelete);
				}));
		});
	}

	private List<String> getIndexNames(Project project, Branch branch, SchemaContainerVersion version, ContainerType type) {
		return db.tx(() -> {
			Stream<String> languageIndices = version.getSchema().findOverriddenSearchLanguages()
				.map(lang -> NodeGraphFieldContainer.composeIndexName(
					project.getUuid(),
					branch.getUuid(),
					version.getUuid(),
					type,
					lang, version.getMicroschemaVersionHash(branch)));
			Stream<String> defaultIndex = Stream.of(NodeGraphFieldContainer.composeIndexName(
				project.getUuid(),
				branch.getUuid(),
				version.getUuid(),
				type, version.getMicroschemaVersionHash(branch)));
			return Stream.concat(languageIndices, defaultIndex)
				.collect(Collectors.toList());
		});
	}

	public Set<String> getIndicesForSearch(InternalActionContext ac, ContainerType type) {
		return db.tx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				Branch branch = ac.getBranch();
				return Collections.singleton(NodeGraphFieldContainer.composeIndexPattern(
					project.getUuid(),
					branch.getUuid(),
					type));
			} else {
				return Collections.singleton(NodeGraphFieldContainer.composeIndexPattern(type));
			}
		});
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return getIndicesForSearch(ac, ContainerType.forVersion(ac.getVersioningParameters().getVersion()));
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
	 * Validate the schema by creating an index template.
	 * 
	 * @param schema
	 */
	public Completable validate(Schema schema) {
		return Flowable.defer(() -> {
			schema.validate();
			return Flowable.fromIterable(schema.findOverriddenSearchLanguages()::iterator);
		}).map(language -> createDummyIndexInfo(schema, language))
			.startWith(createDummyIndexInfo(schema, null))
			.flatMapCompletable(searchProvider::validateCreateViaTemplate);
	}

	/**
	 * Construct the full index settings using the provided schema and language as a source.
	 *
	 * @param schema
	 * @return
	 */
	public JsonObject createIndexSettings(Schema schema) {
		return createIndexSettings(schema, null);
	}

	/**
	 * Construct the full index settings using the provided schema and language as a source.
	 *
	 * @param schema
	 * @return
	 */
	public JsonObject createIndexSettings(Schema schema, String language) {
		return searchProvider.createIndexSettings(createDummyIndexInfo(schema, language));
	}

	private IndexInfo createDummyIndexInfo(Schema schema, String language) {
		String indexName = language != null
			? "validationDummy-" + language
			: "validationDummy";
		return createIndexInfo(null, schema, language, indexName, schema.getName());
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

	@Override
	public Function<String, Node> elementLoader() {
		return (uuid) -> db.index().findByUuid(Node.class, uuid);
	}

	@Override
	public Stream<? extends Node> loadAllElements() {
		return db.type().findAll(Node.class);
	}

}
