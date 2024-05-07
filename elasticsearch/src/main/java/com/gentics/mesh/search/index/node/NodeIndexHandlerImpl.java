package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.search.index.node.NodeIndexUtil.getDefaultSetting;
import static com.gentics.mesh.search.index.node.NodeIndexUtil.getLanguageOverride;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.search.index.node.NodeContainerMappingProvider;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import io.reactivex.Completable;
import io.reactivex.Flowable;
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
public class NodeIndexHandlerImpl extends AbstractIndexHandler<HibNode> implements NodeIndexHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeIndexHandlerImpl.class);

	protected final NodeContainerTransformer transformer;

	protected final NodeContainerMappingProviderImpl mappingProvider;

	@Inject
	public NodeIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetersFactory, BucketManager bucketManager, NodeContainerTransformer transformer, NodeContainerMappingProviderImpl mappingProvider) {
		super(searchProvider, db, helper, options, syncMetersFactory, bucketManager);
		this.transformer = transformer;
		this.mappingProvider = mappingProvider;
	}

	@Override
	public Class<? extends HibBucketableElement> getElementClass() {
		return HibNode.class;
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
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.contentDao().globalCount();
		});
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		return db.tx(tx -> {
			Map<String, Optional<IndexInfo>> indexInfo = new HashMap<>();

			// Iterate over all projects and construct the index names
			for (HibProject project : tx.projectDao().findAll()) {
				for (HibBranch branch : tx.branchDao().findAll(project)) {
					indexInfo.putAll(getIndices(project, branch).runInExistingTx(tx));
				}
			}
			return indexInfo;
		});
	}

	/**
	 * Return a map of indices for the given project and branch.
	 * 
	 * This will list all schema version, draft/published and language specific indices for the projct branch arrangement.
	 * 
	 * @param project
	 * @param branch
	 * @return
	 */
	public Transactional<Map<String, Optional<IndexInfo>>> getIndices(HibProject project, HibBranch branch) {
		return db.transactional(tx -> {
			Map<String, Optional<IndexInfo>> indexInfo = new HashMap<>();
			// Each branch specific index has also document type specific mappings
			for (HibSchemaVersion containerVersion : branch.findActiveSchemaVersions()) {
				indexInfo.putAll(getIndices(project, branch, containerVersion).runInExistingTx(tx));
			}
			return indexInfo;
		});
	}

	/**
	 * Return a transactional which produces a map that contains all indices that are needed for the given container version, project, branch arrangement.
	 * 
	 * @param project
	 * @param branch
	 * @param containerVersion
	 * @return
	 */
	public Transactional<Map<String, Optional<IndexInfo>>> getIndices(HibProject project, HibBranch branch, HibSchemaVersion containerVersion) {
		return getIndices(project, branch, containerVersion, Collections.emptyMap());
	}

	/**
	 * Variant of {@link #getIndices(Project, Branch, SchemaContainerVersion)}, which will replace the assigned microschema versions with the microschema versions of the given replacementMap, if the schema version
	 * contains micronodes.
	 * This can be used to get the index names and information for indices, if they would use other versions of microschemas (e.g. the old index names after a micronode migration has happened)
	 * @param project project
	 * @param branch branch
	 * @param containerVersion container version
	 * @param replacementMap map of microschema names to microschema version uuids (may be empty, but not null)
	 * @return transactional
	 */
	public Transactional<Map<String, Optional<IndexInfo>>> getIndices(HibProject project, HibBranch branch, HibSchemaVersion containerVersion, Map<String, String> replacementMap) {
		return db.transactional(tx -> {
			Map<String, Optional<IndexInfo>> indexInfos = new HashMap<>();
			SchemaVersionModel schema = containerVersion.getSchema();

			// Add all language specific indices (might be none)
			schema.findOverriddenSearchLanguages().forEach(language -> Stream.of(DRAFT, PUBLISHED).forEach(version -> {
				String indexName = ContentDao.composeIndexName(project.getUuid(), branch.getUuid(), containerVersion
					.getUuid(), version, language, containerVersion.getMicroschemaVersionHash(branch, replacementMap));
				log.debug("Adding index to map of known indices {" + indexName + "}");
				// Load the index mapping information for the index, if applicable
				indexInfos.put(indexName, createIndexInfo(branch, schema, language, indexName, schema.getName() + "@" + schema.getVersion()));
			}));

			// And all default indices
			Stream.of(DRAFT, PUBLISHED).forEach(version -> {
				String indexName = ContentDao.composeIndexName(project.getUuid(), branch.getUuid(), containerVersion
					.getUuid(), version, null, containerVersion.getMicroschemaVersionHash(branch, replacementMap));
				log.debug("Adding index to map of known indices {" + indexName + "}");
				// Load the index mapping information for the index, if applicable
				indexInfos.put(indexName, createIndexInfo(branch, schema, null, indexName, schema.getName() + "@" + schema.getVersion()));
			});
			return indexInfos;
		});
	}

	/**
	 * Get triples of (old index name, new index name, restriction query) for the indices in the given branch for the given schema version.
	 * These triples will be used to create reindex requests for reindexing documents from old indices to new indices after a micronode migration.
	 * <p>The old index names will be use the replacement map to replace the currently assigned microschema versions with older versions.</p>
	 * <p>The new index names will use the currently assigned microschema versions</p>
	 * <p>The restricting queries will restrict to documents, which do *not* contain micronodes of the given microschema</p>
	 * @param project project
	 * @param branch branch
	 * @param containerVersion schema version
	 * @param microschema microschema
	 * @param replacementMap replacement map
	 * @return transactional
	 */
	public Transactional<List<Triple<String, String, JsonObject>>> getReIndexTriples(HibProject project, HibBranch branch,
			HibSchemaVersion containerVersion, HibMicroschema microschema,
			Map<String, String> replacementMap) {
		return db.transactional(tx -> {
			List<Triple<String, String, JsonObject>> indexTripleList = new ArrayList<>();
			SchemaModel schema = containerVersion.getSchema();

			String oldHash = containerVersion.getMicroschemaVersionHash(branch, replacementMap);
			String newHash = containerVersion.getMicroschemaVersionHash(branch);

			if (!Objects.equals(oldHash, newHash)) {
				JsonArray mustNotArray = new JsonArray();
				JsonObject query = new JsonObject().put("bool", new JsonObject().put("must_not", mustNotArray));
				for (String field : containerVersion.getFieldsUsingMicroschema(microschema)) {
					mustNotArray.add(
							new JsonObject().put("term", new JsonObject().put("fields." + field + ".microschema.uuid",
									new JsonObject().put("value", microschema.getUuid()))));
				}

				// Add all language specific indices (might be none)
				schema.findOverriddenSearchLanguages().forEach(language -> Stream.of(DRAFT, PUBLISHED).forEach(version -> {
					String oldIndexName = ContentDao.composeIndexName(project.getUuid(), branch.getUuid(),
							containerVersion.getUuid(), version, language, oldHash);
					String newIndexName = ContentDao.composeIndexName(project.getUuid(), branch.getUuid(),
							containerVersion.getUuid(), version, language, newHash);
					indexTripleList.add(Triple.of(oldIndexName, newIndexName, query));
				}));

				// And all default indices
				Stream.of(DRAFT, PUBLISHED).forEach(version -> {
					String oldIndexName = ContentDao.composeIndexName(project.getUuid(), branch.getUuid(),
							containerVersion.getUuid(), version, null, oldHash);
					String newIndexName = ContentDao.composeIndexName(project.getUuid(), branch.getUuid(),
							containerVersion.getUuid(), version, null, newHash);
					indexTripleList.add(Triple.of(oldIndexName, newIndexName, query));
				});
			}

			return indexTripleList;
		});
	}

	/**
	 * Create the index information which contains the mapping and elasticsearch settings which are loaded from the provided schema model.
	 * 
	 * @param branch
	 *            The branch is used to select the correct schema versions
	 * @param schema
	 *            Schema to select the correct version
	 * @param language
	 *            Mappings can be language specific
	 * @param indexName
	 *            Name of the search index
	 * @param sourceInfo
	 *            Human readable name of the source of the index settings (often used for debug information)
	 * @return
	 */
	public Optional<IndexInfo> createIndexInfo(HibBranch branch, SchemaModel schema, String language, String indexName, String sourceInfo) {
		Optional<JsonObject> maybeMapping = getMappingProvider().getMapping(schema, branch, language);
		return maybeMapping.map(mapping -> {
			JsonObject settings = language == null
					? getDefaultSetting(schema.getElasticsearch())
					: getLanguageOverride(schema.getElasticsearch(), language);
			return new IndexInfo(indexName, settings, mapping, sourceInfo);
		});
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		Set<String> activeIndices = new HashSet<>();
		db.tx(tx -> {
			ProjectDao projectDao = tx.projectDao();
			BranchDao branchDao = tx.branchDao();
			SchemaDao schemaDao = tx.schemaDao();
			for (HibProject currentProject : projectDao.findAll()) {
				for (HibBranch branch : branchDao.findAll(currentProject)) {
					for (HibSchemaVersion version : schemaDao.findActiveSchemaVersions(branch)) {
						if (log.isDebugEnabled()) {
							log.debug("Found active schema version {}-{} in branch {}", version.getSchema().getName(), version.getVersion(),
								branch.getName());
						}

						// add all language specific indices
						version.getSchema().findOverriddenSearchLanguages()
								.forEach(language -> Stream.of(DRAFT, PUBLISHED).forEach(type -> {
									activeIndices.add(ContentDao.composeIndexName(currentProject.getUuid(),
											branch.getUuid(), version.getUuid(), type, language, version.getMicroschemaVersionHash(branch)));
								}));

						Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED).forEach(type -> {
							activeIndices
								.add(ContentDao.composeIndexName(currentProject.getUuid(), branch.getUuid(), version.getUuid(),
									type, null, version.getMicroschemaVersionHash(branch)));
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
		return Flowable.defer(() -> db.tx(tx -> {
			ProjectDao projectDao = tx.projectDao();
			BranchDao branchDao = tx.branchDao();
			
			return projectDao.findAll().stream()
				.flatMap(project -> branchDao.findAll(project).stream()
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
	 * @param bucket
	 * @return indexName -> documentName -> NodeGraphFieldContainer
	 */
	private Map<String, Map<String, HibNodeFieldContainer>> loadVersionsFromGraph(HibBranch branch, HibSchemaVersion version, ContainerType type,
		Bucket bucket) {
		return db.tx(tx -> {
			CommonTx ctx = (CommonTx) tx;
			ContentDao contentDao = ctx.contentDao();
			PersistingSchemaDao schemaDao = ctx.schemaDao();
			String branchUuid = branch.getUuid();
			List<String> indexLanguages = version.getSchema().findOverriddenSearchLanguages().collect(Collectors.toList());

			return schemaDao.getFieldContainers(version, branchUuid, bucket)
				.filter(c -> contentDao.isType(c, type, branchUuid))
				.map(HibNodeFieldContainer.class::cast)
				.collect(Collectors.groupingBy(content -> {
					String languageTag = content.getLanguageTag();
					return ContentDao.composeIndexName(
						branch.getProject().getUuid(),
						branchUuid,
						version.getUuid(),
						type,
						indexLanguages.contains(languageTag)
							? languageTag
							: null, version.getMicroschemaVersionHash(branch));
				}, Collectors.toMap(c -> contentDao.getNode(c).getUuid() + "-" + c.getLanguageTag(), Function.identity())));
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

	private Flowable<SearchRequest> diffAndSync(HibProject project, HibBranch branch, HibSchemaVersion version, ContainerType type, Optional<Pattern> indexPattern) {
		// if an index pattern is given, check whether any index matches the pattern
		if (indexPattern.isPresent() && !getIndexNames(project, branch, version, type).stream()
				.filter(indexName -> indexPattern.orElse(MATCH_ALL).matcher(indexName).matches()).findFirst()
				.isPresent()) {
			return Flowable.empty();
		}

		String projectName = project.getName();
		String branchName = branch.getName();
		String versionNumber = version.getVersion();
		String schemaName = version.getName();
		String typeName = type.name();
		log.info("Handling index sync on handler {} for project {}, branch {}, version {} of schema {}, type {}",
				getClass().getName(), projectName, branchName, versionNumber, schemaName, typeName);
		// Sync each bucket individually
		Flowable<Bucket> buckets = bucketManager.getBuckets(getTotalCountFromGraph());
		return buckets.flatMap(bucket -> {
			log.info("Handling sync of {} for project {}, branch {}, version {} of schema {}, type {}", bucket,
					projectName, branchName, versionNumber, schemaName, typeName);
			return diffAndSync(project, branch, version, type, bucket, indexPattern);
		}, 1);
	}

	private Flowable<SearchRequest> diffAndSync(HibProject project, HibBranch branch, HibSchemaVersion version, ContainerType type, Bucket bucket, Optional<Pattern> indexPattern) {
		return Flowable.defer(() -> {
			Map<String, Map<String, HibNodeFieldContainer>> sourceNodesPerIndex = loadVersionsFromGraph(branch, version, type, bucket);
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
					Map<String, HibNodeFieldContainer> sourceNodes = sourceNodesPerIndex.getOrDefault(indexName, Collections.emptyMap());
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

	private List<String> getIndexNames(HibProject project, HibBranch branch, HibSchemaVersion version, ContainerType type) {
		return db.tx(() -> {
			Stream<String> languageIndices = version.getSchema().findOverriddenSearchLanguages()
				.map(lang -> ContentDao.composeIndexName(
					project.getUuid(),
					branch.getUuid(),
					version.getUuid(),
					type,
					lang,
					version.getMicroschemaVersionHash(branch)));
			Stream<String> defaultIndex = Stream.of(ContentDao.composeIndexName(
				project.getUuid(),
				branch.getUuid(),
				version.getUuid(),
				type,
				null,
				version.getMicroschemaVersionHash(branch)));
			return Stream.concat(languageIndices, defaultIndex)
				.collect(Collectors.toList());
		});
	}

	/**
	 * Return all content indices that should be selected for the given request (which is scoped to a project) and type.
	 * 
	 * @param ac
	 * @param type
	 * @return
	 */
	public Set<String> getIndicesForSearch(InternalActionContext ac, ContainerType type) {
		return db.tx(tx -> {
			HibProject project = tx.getProject(ac);
			if (project != null) {
				HibBranch branch = tx.getBranch(ac);
				return Collections.singleton(ContentDao.composeIndexPattern(
					project.getUuid(),
					branch.getUuid(),
					type));
			} else {
				return Collections.singleton(ContentDao.composeIndexPattern(type));
			}
		});
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return getIndicesForSearch(ac, ContainerType.forVersion(ac.getVersioningParameters().getVersion()));
	}

	/**
	 * Generate an elasticsearch document object from the given container and stores it in the search index.
	 * 
	 * @param container
	 * @param branchUuid
	 * @param type
	 * @return Single with affected index name
	 */
	public Single<String> storeContainer(HibNodeFieldContainer container, String branchUuid, ContainerType type) {
		ContentDao contentDao = Tx.get().contentDao();
		JsonObject doc = transformer.toDocument(container, branchUuid, type);
		HibProject project = contentDao.getNode(container).getProject();
		HibSchemaVersion version = contentDao.getSchemaContainerVersion(container);
		HibBranch branch = Tx.get().branchDao().findByUuid(project, branchUuid);
		String indexName = ContentDao.composeIndexName(project.getUuid(), branchUuid, version.getUuid(), 
				type, null, version.getMicroschemaVersionHash(branch));
		if (log.isDebugEnabled()) {
			log.debug("Storing node {" + contentDao.getNode(container).getUuid() + "} into index {" + indexName + "}");
		}
		String languageTag = container.getLanguageTag();
		String documentId = ContentDao.composeDocumentId(contentDao.getNode(container).getUuid(), languageTag);
		return searchProvider.storeDocument(indexName, documentId, doc).andThen(Single.just(indexName));
	}

	@Override
	public InternalPermission getReadPermission(InternalActionContext ac) {
		switch (ContainerType.forVersion(ac.getVersioningParameters().getVersion())) {
		case PUBLISHED:
			return InternalPermission.READ_PUBLISHED_PERM;
		default:
			return InternalPermission.READ_PERM;
		}
	}

	/**
	 * Validate the schema by creating an index template.
	 * 
	 * @param schema
	 */
	public Completable validate(SchemaModel schema) {
		return Flowable.defer(() -> {
			schema.validate();
			return Flowable.fromIterable(schema.findOverriddenSearchLanguages()::iterator);
		}).map(language -> createDummyIndexInfo(schema, language))
			.startWith(createDummyIndexInfo(schema, null))
			.flatMapCompletable(ii -> ii.map(searchProvider::validateCreateViaTemplate).orElse(Completable.complete()));
	}

	/**
	 * Construct the full index settings using the provided schema and language as a source.
	 *
	 * @param schema
	 * @return
	 */
	public Optional<JsonObject> createIndexSettings(SchemaModel schema) {
		return createIndexSettings(schema, null);
	}

	/**
	 * Construct the full index settings using the provided schema and language as a source.
	 *
	 * @param schema
	 * @return
	 */
	public Optional<JsonObject> createIndexSettings(SchemaModel schema, String language) {
		return createDummyIndexInfo(schema, language).map(searchProvider::createIndexSettings);
	}

	private Optional<IndexInfo> createDummyIndexInfo(SchemaModel schema, String language) {
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
	public String generateVersion(HibNodeFieldContainer container, String branchUuid, ContainerType type) {
		return getTransformer().generateVersion(container, branchUuid, type);
	}

	@Override
	public Function<String, HibNode> elementLoader() {
		// TODO override in OrientDB implementation to use index for performance, if required
		//return (uuid) -> HibClassConverter.toGraph(db).index().findByUuid(HibNode.class, uuid);
		return uuid -> CommonTx.get().nodeDao().findByUuidGlobal(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, HibNode>>> elementsLoader() {
		return (uuids) -> CommonTx.get().nodeDao().findByUuidGlobal(uuids).stream().map(node -> Pair.of(node.getUuid(), node));
	}	

	@Override
	public Stream<? extends HibNode> loadAllElements() {
		// TODO override in OrientDB implementation to use index for performance, if required
		//return HibClassConverter.toGraph(db).type().findAll(HibNode.class);
		return CommonTx.get().nodeDao().findAllGlobal();
	}

	@Override
	public boolean isDefinitionDataDependent() {
		// We depend on the project.
		return true;
	}
}
