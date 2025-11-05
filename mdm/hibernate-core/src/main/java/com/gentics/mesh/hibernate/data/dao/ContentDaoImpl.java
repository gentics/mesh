package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl.QUERY_DELETE_ALL_BY_KEYS;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Hibernate;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.cache.ListableFieldCache;
import com.gentics.mesh.cache.TotalsCache;
import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentKey;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibDeletableField;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibDisplayField;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.HibQueryFieldMapper;
import com.gentics.mesh.hibernate.data.domain.AbstractFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.AbstractHibFieldSchemaVersion;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.BucketTracking;
import com.gentics.mesh.hibernate.data.domain.HibBinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibBooleanListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibDateListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibFieldEdge;
import com.gentics.mesh.hibernate.data.domain.HibHtmlListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibImageVariantImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerVersionsEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNumberListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibStringListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.hibernate.data.loader.DataLoaders;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractDeletableHibField;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractHibField;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractHibListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.AbstractReferenceHibField;
import com.gentics.mesh.hibernate.data.node.field.impl.HibBooleanListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibDateListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibHtmlListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibMicronodeFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibMicronodeListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNumberListFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibStringListFieldImpl;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.hibernate.util.TriFunction;
import com.gentics.mesh.hibernate.util.UuidGenerator;
import com.gentics.mesh.util.CollectionUtil;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.Completable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

/**
 * Content DAO implementation for Gentics Mesh. Please pay attention, that Content entity is not JPA-backed.
 *
 * @author plyhun
 *
 */
@Singleton
public class ContentDaoImpl implements PersistingContentDao, HibQueryFieldMapper {

	private static final Logger log = LoggerFactory.getLogger(ContentDaoImpl.class);

	protected final UuidGenerator uuidGenerator;
	protected final CurrentTransaction currentTransaction;
	protected final TotalsCache totalsCache;
	private final ContentStorage contentStorage;
	private final DatabaseConnector databaseConnector;

	@Inject
	public ContentDaoImpl(UuidGenerator uuidGenerator, CurrentTransaction currentTransaction, ContentStorage contentStorage, DatabaseConnector databaseConnector, TotalsCache totalsCache) {
		this.uuidGenerator = uuidGenerator;
		this.currentTransaction = currentTransaction;
		this.contentStorage = contentStorage;
		this.totalsCache = totalsCache;
		this.databaseConnector = databaseConnector;
	}

	@Override
	public HibNodeFieldContainer getLatestDraftFieldContainer(HibNode node, String languageTag) {
		return getFieldContainer(node, languageTag, node.getProject().getLatestBranch(), DRAFT);
	}

	@Override
	public HibNodeFieldContainerImpl getFieldContainerOfEdge(HibNodeFieldContainerEdge edge) {
		HibNodeFieldContainerEdgeImpl edgeImpl = (HibNodeFieldContainerEdgeImpl) edge;
		return getFieldContainer(edgeImpl.getVersion(), edgeImpl.getContentUuid());
	}

	@Override
	public void removeEdge(HibNodeFieldContainerEdge edge) {
		((HibNodeImpl)edge.getNode()).removeEdge((HibNodeFieldContainerEdgeImpl) edge);
		currentTransaction.getEntityManager().remove(edge);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(HibNode node, ContainerType type) {
		List<HibNodeFieldContainerEdgeImpl> edges;
		if (contentEdgesInitialized(node)) {
			HibNodeImpl impl = (HibNodeImpl) node;
			edges = impl.getContentEdges()
					.stream().filter(edge -> type.equals(edge.getType()))
					.collect(Collectors.toList());
		} else {
			edges = currentTransaction.getEntityManager()
					.createNamedQuery("contentEdge.findByNodeAndType", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("node", node)
					.setParameter("type", type)
					.getResultList();
		}

		List<HibNodeFieldContainerImpl> content = contentStorage.findMany(edges);
		return new TraversalResult<>(content);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(HibNode node, String branchUuid, ContainerType type) {
		if (maybeTypeLoader().isPresent()) {
			return new TraversalResult<>(maybeTypeLoader().get().apply(node, type));
		}

		List<HibNodeFieldContainerEdgeImpl> edges;
		if (contentEdgesInitialized(node)) {
			HibNodeImpl impl = (HibNodeImpl) node;
			edges = impl.getContentEdges()
					.stream().filter(edge -> type.equals(edge.getType()) && branchUuid.equals(edge.getBranchUuid()))
					.collect(Collectors.toList());
		} else {
			edges = currentTransaction.getEntityManager()
					.createNamedQuery("contentEdge.findByNodeTypeAndBranch", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("node", node)
					.setParameter("type", type)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.getResultList();
		}

		List<HibNodeFieldContainerImpl> content = contentStorage.findMany(edges);
		return new TraversalResult<>(content);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<HibNode, List<HibNodeFieldContainer>> getFieldsContainers(Set<HibNode> nodes, String branchUuid, ContainerType type) {
		if (nodes.isEmpty()) {
			return Collections.emptyMap();
		}

		UUID branchId = UUIDUtil.toJavaUuid(branchUuid);
		List<UUID> nodesUuids = nodes.stream().map(HibNode::getId).map(UUID.class::cast).collect(Collectors.toList());
		return SplittingUtils.splitAndMergeInMapOfLists(nodesUuids, HibernateUtil.inQueriesLimitForSplitting(2), (uuids) -> {
			List<HibNodeFieldContainerEdgeImpl> edges = currentTransaction.getEntityManager()
					.createNamedQuery("contentEdge.findByNodesTypeAndBranch", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("nodeUuids", uuids)
					.setParameter("type", type)
					.setParameter("branchUuid", branchId)
					.setHint(AvailableHints.HINT_CACHEABLE, true)
					.getResultList();

			return contentStorage.findMany(edges).stream()
					.collect(Collectors.groupingBy(HibNodeFieldContainerImpl::getNodeId))
					.entrySet().stream()
					.map(uuidContent -> Pair.of(nodes.stream().filter(node -> node.getId().equals(uuidContent.getKey())).findAny().orElse(null), (List<HibNodeFieldContainer>) (List<?>) uuidContent.getValue()))
					.filter(pair -> pair.getKey() != null)
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		});
	}

	/**
	 * Find containers of a given {@link ContainerType} for the nodes.
	 *
	 * @param nodes
	 * @param type
	 * @return
	 */
	public List<HibNodeFieldContainerImpl> getFieldsContainers(Set<HibNode> nodes, ContainerType type) {
		if (nodes.isEmpty()) {
			return Collections.emptyList();
		}

		List<HibNodeFieldContainerEdgeImpl> edges = SplittingUtils.splitAndMergeInList(nodes.stream().map(HibNode::getId).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(1), slice -> currentTransaction.getEntityManager()
				.createNamedQuery("contentEdge.findByNodesAndType", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("nodeUuids", slice)
				.setParameter("type", type)
				.getResultList());

		return contentStorage.findMany(edges);
	}

	@Override
	public Map<HibNode, List<HibNodeFieldContainer>> getFieldsContainers(Set<HibNode> nodes, String branchUuid, VersionNumber versionNumber) {
		if (nodes.isEmpty()) {
			return Collections.emptyMap();
		}

		// At the moment the only way to get a specific version is to first get the draft field containers for the branch
		// for each container we recursively find the previous one until the matched version is found.
		// Each of this recursion step will trigger a query, and is therefore very inefficient.
		// The following code should be refactored in the future to minimize these queries
		// Alternatives:
		// 1. Fetch all containers with the correct version directly from the content tables. Once we have those, we
		//    need to identify to which branch they belong, which requires also to traverse the version chain until
		//    we find an edge
		// 2. Store the version number in the version table. From there we traverse the version chain until an edge is found,
		// 	  using a recursive query (for those database that support it). If an edge is found, we can check the branch,
		// 	  and then query the content table only for those containers that match the version and the branch
		// 	  In pseudo SQL:
		// 	  select uuid, schemaversion from version_table version where exists
		// 	  (select recursive parent from version_table parent join edge on edge.uuid = parent.id where edge.branch = :branch)
		// 	  where version.versionNumber = versionNumber
		// 3. create cache for content versions
		List<HibNodeFieldContainerEdgeImpl> edges = SplittingUtils.splitAndMergeInList(nodes.stream().map(HibNode::getId).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(3), slice -> currentTransaction.getEntityManager()
					.createNamedQuery("contentEdge.findByNodesTypeAndBranch", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("nodeUuids", slice)
					.setParameter("type", ContainerType.DRAFT)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setHint(AvailableHints.HINT_CACHEABLE, true)
					.getResultList());

		return contentStorage.findMany(edges).stream()
				.map(container -> {
					HibNodeFieldContainer current = container;
					while (current != null && !current.getVersion().equals(versionNumber)) {
						current = current.getPreviousVersion();
					}

					return current;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(HibNodeFieldContainer::getNode));
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, HibBranch branch, ContainerType type) {
		return getFieldContainer(node, languageTag, branch.getUuid(), type);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag) {
		String branch = node.getProject().getLatestBranch().getUuid();
		return getFieldContainer(node, languageTag, branch, DRAFT);
	}

	private List<HibNodeFieldContainerEdgeImpl> getEdges(Collection<HibNodeImpl> nodes, List<String> languageTags, String branchUuid, ContainerType type) {
		List<HibNodeFieldContainerEdgeImpl> resultList = SplittingUtils.splitAndMergeInList(nodes, HibernateUtil.inQueriesLimitForSplitting(3 + languageTags.size()), slice -> currentTransaction.getEntityManager()
				.createNamedQuery("contentEdge.findByNodeTypeBranchAndLanguageForAdmin", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("nodes", slice)
				.setParameter("type", type)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("languageTags", languageTags)
				.getResultList());

		return new ArrayList<>(resultList.stream()
				.collect(Collectors.toMap(HibNodeFieldContainerEdgeImpl::getNode, Function.identity(), (a, b) -> {
					if (languageTags.indexOf(a.getLanguageTag()) < languageTags.indexOf(b.getLanguageTag())) {
						return a;
					}
					return b;
				})).values());
	}

	@Override
	public HibNodeFieldContainerEdge getEdge(HibNode node, String languageTag, String branchUuid, ContainerType type) {
		HibNodeImpl impl = (HibNodeImpl) node;
		if (contentEdgesInitialized(node)) {
			return impl.getContentEdges().stream()
				.filter(e -> e.getLanguageTag().equals(languageTag) && e.getBranchUuid().equals(branchUuid) && e.getType().equals(type))
				.findAny()
				.orElse(null);
		}

		List<HibNodeFieldContainerEdgeImpl> edges = getEdges(Collections.singletonList(impl), Collections.singletonList(languageTag), branchUuid, type);
		return edges.size() > 0 ? edges.get(0) : null;
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, String branchUuid, ContainerType type) {
		HibNodeFieldContainerEdge edge = getEdge(node, languageTag, branchUuid, type);
		if (edge == null) {
			return null;
		}

		return getFieldContainerOfEdge(edge);
	}

	public long getFieldContainerCount(HibNode node) {
		EntityManager entityManager = currentTransaction.getEntityManager();
		return ((Number) entityManager.createNamedQuery("contentEdge.countContentByNode")
					.setParameter("node", node)
					.getSingleResult())
				.longValue();
	}

	@Override
	public Stream<HibNodeField> getInboundReferences(HibNode node, boolean lookupInFields, boolean lookupInLists) {
		EntityManager em = currentTransaction.getEntityManager();
		Stream<HibNodeField> listItems = lookupInLists ? em.createNamedQuery("nodelistitem.findByNodeUuid", HibNodeListFieldEdgeImpl.class)
				.setParameter("nodeUuid", node.getId())
				.getResultStream()
				.map(HibNodeField.class::cast) : Stream.empty();
		Stream<HibNodeField> edges = lookupInFields ? currentTransaction.getEntityManager()
				.createNamedQuery("nodefieldref.findEdgeByNodeUuid", HibNodeFieldEdgeImpl.class)
				.setParameter("uuid", node.getId())
				.getResultStream()
				.map(HibNodeField.class::cast) : Stream.empty();
		return Stream.concat(listItems, edges);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<Pair<HibNodeField, HibNode>> getInboundReferences(Collection<HibNode> nodes) {
		EntityManager em = currentTransaction.getEntityManager();
		Collection<Object> nodeUuids = nodes.stream().map(HibNode::getId).collect(Collectors.toSet());
		Set<Object[]> refs = SplittingUtils.splitAndMergeInSet(nodeUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			Stream<Object[]> listItems = em.createNamedQuery("nodelistitem.findByNodeUuids")
					.setParameter("nodeUuids", slice)
					.getResultStream();
			Stream<Object[]> edges = currentTransaction.getEntityManager()
					.createNamedQuery("nodefieldref.findEdgeByNodeUuids")
					.setParameter("uuids", slice)
					.getResultStream();
			return Stream.concat(listItems, edges).collect(Collectors.toSet());
		});
		return refs.stream().map((tuple) -> {
			return Pair.of((HibNodeField) tuple[0], (HibNode) tuple[1]);
		});
	}

	private <T extends HibField> Optional<T> castField(HibField field, Class<T> cls) {
		if (cls.isInstance(field)) {
			return Optional.ofNullable(cls.cast(field));
		}
		return Optional.empty();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Stream<Pair<HibNodeField, Collection<HibNodeFieldContainer>>> getReferencingContents(Collection<HibNodeField> fields) {
		Map<ReferenceType, Set<HibNodeField>> classFields = (Map) fields.stream()
				.collect(Collectors.groupingBy(impl -> castField(impl, AbstractFieldEdgeImpl.class).map(AbstractFieldEdgeImpl::getContainerType)
						.or(() -> castField(impl, AbstractHibField.class).map(impl1 -> impl1.getContainer().getReferenceType()))
						.orElseThrow(() -> new IllegalStateException("Unknown HibField implementation: " + impl)), 
					Collectors.mapping(Function.identity(), Collectors.toSet())));

		// Nodes referenced by nodes (already loaded)
		Stream<Pair<HibNodeField, Collection<HibNodeFieldContainer>>> nodesAlreadyLoaded = (Stream) Optional.ofNullable(
					classFields.get(ReferenceType.FIELD))
						.map(fields1 -> fields1.stream()
								.filter(AbstractHibField.class::isInstance)
								.map(field -> Pair.of(field, Collections.singleton(HibNodeFieldContainer.class.cast(AbstractHibField.class.cast(field).getContainer()))))
				).orElseGet(() -> Stream.empty());

		// Nodes referenced by nodes (edges only)
		Map<ContentKey, Collection<AbstractFieldEdgeImpl>> nodesToLoad = (Map) Optional.ofNullable(
					classFields.get(ReferenceType.FIELD))
						.map(fields1 -> fields1.stream()
								.filter(AbstractFieldEdgeImpl.class::isInstance)
								.map(field -> {
									AbstractFieldEdgeImpl<UUID> impl = AbstractFieldEdgeImpl.class.cast(field);
									return Pair.of(ContentKey.fromEdge(impl), impl);
								})
				).orElseGet(() -> Stream.empty())
				.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toSet())));
		Stream<Pair<HibNodeField, Collection<HibNodeFieldContainer>>> nodesJustLoaded = (Stream) contentStorage.findMany(nodesToLoad.keySet()).stream()
			.flatMap(content -> nodesToLoad.get(ContentKey.fromContent(content)).stream().map(field -> Pair.of(field, content)))
			.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toSet())))
			.entrySet().stream()
			.map(entry -> Pair.of(entry.getKey(), entry.getValue()));

		// origin content UUID / set of its referencing fields
		Map<UUID, Collection<HibNodeField>> micronodeFields = (Map) Optional.ofNullable(
				classFields.get(ReferenceType.MICRONODE))
					.map(fields1 -> fields1.stream().map(impl -> Pair.of(
							impl, 
							castField(impl, AbstractFieldEdgeImpl.class).map(AbstractFieldEdgeImpl::getContainerUuid)
								.or(() -> castField(impl, AbstractHibField.class).map(impl1 -> impl1.getContainer().getDbUuid()))
								.orElseThrow(() -> new IllegalStateException("Unknown HibField implementation: " + impl))))
			).orElseGet(() -> Stream.empty()).collect(Collectors.groupingBy(Pair::getValue, Collectors.mapping(Pair::getKey, Collectors.toSet())));

		// Early return if no micronodes
		if (micronodeFields.size() < 1) {
			return Stream.of(nodesAlreadyLoaded, nodesJustLoaded).flatMap(Function.identity());
		}

		EntityManager em = currentTransaction.getEntityManager();
		// origin content UUID / referencing Content map, split by reftype
		Map<ReferenceType, Map<UUID, ContentKey>> micronodeEdges = SplittingUtils.splitAndMergeInSet(micronodeFields.keySet(), HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
				return Stream.concat(
					em.createNamedQuery("micronodefieldref.findByMicrocontainerUuids", HibMicronodeFieldEdgeImpl.class)
						.setParameter("micronodeUuids", slice)
						.getResultStream(),
					em.createNamedQuery("micronodelistitem.findByMicronodeUuids", HibMicronodeListFieldEdgeImpl.class)
						.setParameter("micronodeUuids", slice)
						.getResultStream()
				).map(edge -> Pair.of(ContentKey.fromEdge(edge), edge.getValueOrUuid())).collect(Collectors.toSet());
			}).stream().collect(Collectors.groupingBy(pair -> pair.getLeft().getType(), Collectors.mapping(Function.identity(), Collectors.toMap(pair -> pair.getRight(), pair -> pair.getLeft()))));

		// Nodes referenced by micronode (s|lists). Note the remove() call.
		Stream<Pair<HibNodeField, Collection<HibNodeFieldContainer>>> nodeMicroEdges = (Stream) Optional.ofNullable(micronodeEdges.remove(ReferenceType.FIELD))
				.map(nodeFieldEdges -> {
					// referencing ContentKey / origin 
					Map<ContentKey, Set<UUID>> contentKeyUuids = nodeFieldEdges.entrySet().stream().collect(Collectors.groupingBy(Entry::getValue, Collectors.mapping(Entry::getKey, Collectors.toSet())));
					// content referencing micronodes of target fields
					List<HibNodeFieldContainerImpl> nodesReferencingMicronodes = contentStorage.findMany(nodeFieldEdges.values().stream().collect(Collectors.toSet()));
					return nodesReferencingMicronodes.stream().flatMap(nodeMicroContent -> Optional.ofNullable(contentKeyUuids.get(ContentKey.fromContent(nodeMicroContent)))
									.map(Set::stream).orElseGet(Stream::empty)
								.flatMap(origContentUuid -> Optional.ofNullable(micronodeFields.get(origContentUuid))
											.map(Collection::stream).orElseGet(Stream::empty)
										.map(field -> Pair.of(field, nodeMicroContent)))
								.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toSet())))
								.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())));
				}).orElseGet(() -> Stream.empty());

		return Stream.of(
					nodesAlreadyLoaded,
					nodesJustLoaded,
					nodeMicroEdges,
					// all other cases, too complex to batch
					micronodeEdges.values().stream()
						.flatMap(origContentUuidRefKeys -> origContentUuidRefKeys.keySet().stream().map(origContentUuid -> Optional.ofNullable(micronodeFields.get(origContentUuid))))
						.flatMap(maybeFields1 -> maybeFields1.map(fields1 -> PersistingContentDao.super.getReferencingContents(fields1)).orElseGet(Stream::empty))
				).flatMap(Function.identity());
	}

	/**
	 * Delete all containers (and associated entities) for the given version and project.
	 * @param version
	 * @param project
	 */
	public void delete(HibSchemaVersion version, HibProject project) {
		long deletedCount = contentStorage.delete(version, project);
		if (deletedCount > 0) {
			deleteUnreferencedForVersion(version);
		}
	}

	/**
	 * Delete all micronodes (and associated entities) for the given versions which are not referenced by any field
	 * @param version
	 */
	public void deleteUnreferencedMicronodes(HibMicroschemaVersion version) {
		long deletedCount = contentStorage.deleteUnreferencedMicronodes(version);
		if (deletedCount > 0) {
			Set<Pair<FieldTypes, FieldTypes>> fieldTypes = getFieldTypePairs(version.getSchema().getFields());
			for (Pair<FieldTypes, FieldTypes> typePair : fieldTypes) {
				FieldTypes type = typePair.getLeft();
				switch (type) {
					case STRING:
					case HTML:
					case NUMBER:
					case DATE:
					case BOOLEAN:
						// nothing to do
						break;
					case NODE:
						deleteUnreferencedFieldRows(HibNodeFieldEdgeImpl.class, version);
						break;
					case LIST:
						deleteUnreferencedFieldRows(getListClass(typePair.getRight()), version);
						break;
					default:
						throw new IllegalArgumentException("Don't know how to delete field of type " + type + " for a micronode");
				}
			}
		}
	}

	public Set<Pair<FieldTypes, FieldTypes>> getFieldTypePairs(List<FieldSchema> fields) {
		return fields.stream()
				.map(field -> Pair.of(field.getType(), field instanceof ListFieldSchema ? ((ListFieldSchema) field).getListType() : null))
				.map(pair -> Pair.of(FieldTypes.valueByName(pair.getLeft()), pair.getRight() != null ? FieldTypes.valueByName(pair.getRight()) : null))
				.collect(Collectors.toSet());
	}

	/**
	 * Delete all the containers for the given versions which are related to one of the provided nodes, plus
	 * delete all rows of all tables that are referencing deleted containers.
	 * @param versions
	 * @param nodes
	 */
	public void delete(Set<HibSchemaVersion> versions, Set<HibNodeImpl> nodes) {
		Set<ContentKey> contentKeys = new HashSet<>();
		versions.forEach(version -> {
			contentKeys.addAll(contentStorage.findByNodes(version, nodes));
		});
		delete(contentKeys);
	}

	/**
	 * Delete the node field containers referenced by the provided keys, and all of the rows of the table referencing those keys.
	 * @param contentKeys
	 */
	public void delete(Set<ContentKey> contentKeys) {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();

		// 1. Delete versions.
		List<UUID> contentUuids = contentKeys.stream().map(ContentKey::getContentUuid).collect(Collectors.toList());
		SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(2), slice -> em.createNamedQuery("containerversions.bulkDeleteThisContents")
				.setParameter("contentUuids", slice)
				.executeUpdate());
		SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(2), slice -> em.createNamedQuery("containerversions.bulkDeleteNextContents")
				.setParameter("contentUuids", slice)
				.executeUpdate());

		// 2. Create a set of pairs, where the left of the pair is the type of the field, and the right is the optional
		// extra type (available for list fields). We can then iterate through the type list to delete the edges
		// corresponding to the field type.
		Set<HibSchemaVersion> schemaVersions = contentKeys.stream()
				.map(ContentKey::getSchemaVersionUuid)
				.distinct()
				.map(id -> em.find(HibSchemaVersionImpl.class, id))
				.collect(Collectors.toSet());

		Set<Pair<String, String>> fieldTypes = schemaVersions.stream()
				.flatMap(schema -> schema.getSchema().getFields().stream())
				.map(field -> Pair.of(field.getType(), field instanceof ListFieldSchema ? ((ListFieldSchema) field).getListType() : null))
				.collect(Collectors.toSet());

		for (Pair<String, String> typePair : fieldTypes) {
			FieldTypes fieldType = FieldTypes.valueByName(typePair.getLeft());
			switch (fieldType) {
				case STRING:
				case HTML:
				case NUMBER:
				case DATE:
				case BOOLEAN:
					// These are stored in the content table, so they will be deleted later on.
					break;
				case NODE:
					SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("nodefieldref.removeByContainerUuids")
							.setParameter("containerUuids", slice)
							.executeUpdate());
					break;
				case LIST:
					FieldTypes listType = FieldTypes.valueByName(typePair.getRight());
					if (FieldTypes.MICRONODE.equals(listType)) {
						// micronode list get a special treatment since we need to delete the unreferenced micronodes afterwards.
						deleteMicronodesList(contentUuids);
					} else {
						Class<?> listClass = getListClass(listType);
						String tableName = databaseConnector.getSessionMetadataIntegrator().getTableName(listClass);
						String entityName = tableName.substring(MeshTablePrefixStrategy.TABLE_NAME_PREFIX.length());
						SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> tx.entityManager().createQuery(String.format(QUERY_DELETE_ALL_BY_KEYS, entityName))
								.setParameter("containerUuids", slice)
								.executeUpdate());
					}
					break;
				case BINARY:
					HibernateTx.get().binaryDao().removeField(contentUuids);
					break;
				case S3BINARY:
					HibernateTx.get().s3binaryDao().removeField(contentUuids);
					break;
				case MICRONODE:
					deleteMicronodes(contentUuids);
					break;
			}
		}

		// 3. Delete the containers themselves
		contentStorage.delete(contentKeys);
	}

	private void deleteUnreferencedForVersion(HibSchemaVersion version) {
		deleteUnreferencedContentVersions(version);

		Set<Pair<FieldTypes, FieldTypes>> fieldTypes = getFieldTypePairs(version.getSchema().getFields());
		for (Pair<FieldTypes, FieldTypes> typePair : fieldTypes) {
			FieldTypes type = typePair.getLeft();
			switch (type) {
				case STRING:
				case HTML:
				case NUMBER:
				case DATE:
				case BOOLEAN:
					// nothing to do
					break;
				case NODE:
					deleteUnreferencedFieldRows(HibNodeFieldEdgeImpl.class, version);
					break;
				case LIST:
					deleteUnreferencedFieldRows(getListClass(typePair.getRight()), version);
					break;
				case BINARY:
					deleteUnreferencedFieldRows(HibBinaryFieldEdgeImpl.class, version);
					deleteUnreferencedImageVariantEdges();
					deleteUnreferencedBinary();
					deleteUnreferencedImageVariants();
					break;
				case S3BINARY:
					deleteUnreferencedFieldRows(HibS3BinaryFieldEdgeImpl.class, version);
					deleteUnreferencedS3Binary();
					break;
				case MICRONODE:
					deleteUnreferencedFieldRows(HibMicronodeFieldEdgeImpl.class, version);
					break;
				default:
					throw new IllegalArgumentException("Don't know how to delete field of type " + type + " for a field container");
			}
		}
	}

	private void deleteUnreferencedBinary() {
		List<UUID> uuids = currentTransaction.getEntityManager().createNamedQuery("binary.findUnreferencedBinaryUuids", UUID.class).getResultList();
		Completable deleteAction = Completable.merge(StreamUtil.toIterable(uuids.stream().map(uuid -> currentTransaction.getTx().data().binaryStorage().delete(UUIDUtil.toShortUuid(uuid)))));
		HibernateTx.get().data().maybeGetBulkActionContext().ifPresentOrElse(bac -> bac.add(deleteAction), () -> HibernateTx.get().batch().add(() -> deleteAction.blockingAwait()));
		SplittingUtils.splitAndConsume(uuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
		currentTransaction.getEntityManager().createQuery("delete from binary where dbUuid in :uuids")
				.setParameter("uuids", slice)
				.executeUpdate();
		});
	}

	private void deleteUnreferencedS3Binary() {
		currentTransaction.getEntityManager().createNamedQuery("s3Binary.deleteUnreferenced")
				.executeUpdate();
	}

	private void deleteUnreferencedContentVersions(HibSchemaVersion version) {
		String contentTableName = databaseConnector.getPhysicalTableName(version);
		String query = String.format("delete from mesh_nodefieldcontainer_versions_edge " +
				" where thisversion_dbuuid = :versionUuid and not exists (select 1 from %1$s content where content.%2$s = thiscontentuuid) " +
				" or nextversion_dbuuid = :versionUuid and not exists (select 1 from %1$s content where content.%2$s = nextcontentuuid) ", contentTableName, databaseConnector.renderColumn(CommonContentColumn.DB_UUID));

		currentTransaction.getEntityManager().createNativeQuery(query)
				.setParameter("versionUuid", version.getId())
				.unwrap(NativeQuery.class)
				.addSynchronizedEntityClass(HibNodeFieldContainerVersionsEdgeImpl.class)
				.executeUpdate();
	}

	/**
	 * Delete all image variant edges, that are not being referenced by a single field.
	 */
	private void deleteUnreferencedImageVariantEdges() {
		String query = "delete from " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "binary_field_variant where " + databaseConnector.renderNonContentColumn("fields_dbUuid") + " not in ("
				+ " select bfv." + databaseConnector.renderNonContentColumn("fields_dbUuid") + " from " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "binary_field_variant bfv "
				+ " inner join " + databaseConnector.getSessionMetadataIntegrator().getTableName(HibBinaryFieldEdgeImpl.class) + " br on br." + databaseConnector.renderNonContentColumn("dbUuid") + " = bfv." + databaseConnector.renderNonContentColumn("fields_dbUuid") 
			+ ")";
		currentTransaction.getEntityManager().createNativeQuery(query)
				.unwrap(NativeQuery.class)
				.executeUpdate();
	}

	/**
	 * Delete all image variant edges, that are not being referenced by a single field.
	 */
	private void deleteUnreferencedImageVariants() {
		String query = "delete from " + databaseConnector.getSessionMetadataIntegrator().getTableName(HibImageVariantImpl.class) + " where " + databaseConnector.renderNonContentColumn("dbUuid") + " not in ("
				+ " select bfv." + databaseConnector.renderNonContentColumn("variants_dbUuid") + " from " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "binary_field_variant bfv "
				+ " inner join " + databaseConnector.getSessionMetadataIntegrator().getTableName(HibBinaryFieldEdgeImpl.class) + " br on br." + databaseConnector.renderNonContentColumn("dbUuid") + " = bfv." + databaseConnector.renderNonContentColumn("fields_dbUuid") 
				+ " union all select iv." + databaseConnector.renderNonContentColumn("dbUuid") + " from " + databaseConnector.getSessionMetadataIntegrator().getTableName(HibImageVariantImpl.class) + " iv inner join " + databaseConnector.getSessionMetadataIntegrator().getTableName(HibBinaryImpl.class) + " bin "
				+ " on bin." + databaseConnector.renderNonContentColumn("dbUuid") + " = iv." + databaseConnector.renderNonContentColumn("binary_dbuuid")
			+ ")";
		currentTransaction.getEntityManager().createNativeQuery(query)
				.unwrap(NativeQuery.class)
				.executeUpdate();
	}

	/**
	 * Delete all the rows of the field table that are referencing container uuids which have been deleted
	 * @param clazz the class of the field
	 * @param version
	 */
	private void deleteUnreferencedFieldRows(Class<?> clazz, HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		String fieldTableName = databaseConnector.getSessionMetadataIntegrator().getTableName(clazz);
		String contentTableName = databaseConnector.getPhysicalTableName(version);
		String query = String.format("delete from %s " +
				" where not exists (select 1 from %s content where content.%s = containeruuid) " +
				" and containerversionuuid = :versionUuid", fieldTableName, contentTableName, databaseConnector.renderColumn(CommonContentColumn.DB_UUID));

		currentTransaction.getEntityManager().createNativeQuery(query)
				.setParameter("versionUuid", version.getId())
				.unwrap(NativeQuery.class)
				.addSynchronizedEntityClass(clazz)
				.executeUpdate();
	}

	@Override
	public void delete(HibNodeFieldContainer content) {
		delete(content, true);
	}

	/**
	 * Delete the provided field containers making sure to repair the container version chain.
	 * @param containers
	 */
	@SuppressWarnings("unchecked")
	public void purge(List<? extends HibNodeFieldContainer> containers) {
		if (containers.isEmpty()) {
			return;
		}
		log.info("Containers to purge: {}", containers.size());

		// 1. Connect the previous versions with the next versions.
		EntityManager em = currentTransaction.getEntityManager();

		Set<UUID> containersUuids = containers.stream().map(container -> (UUID) container.getId()).collect(Collectors.toSet());

		SplittingUtils.splitAndConsumeProgress(containersUuids, 100, (uuids, progress) -> {
			List<Object[]> previousAndNext = em.createNamedQuery("containerversions.findPreviousAndNext")
					.setParameter("contentUuids", uuids)
					.getResultList();

			Set<Pair<HibNodeFieldContainerVersionsEdgeImpl, HibNodeFieldContainerVersionsEdgeImpl>> previousAndNextPairs = previousAndNext.stream()
					.map((Object[] tuple) -> {
						return Pair.of((HibNodeFieldContainerVersionsEdgeImpl) tuple[0], (HibNodeFieldContainerVersionsEdgeImpl) tuple[1]);
					}).collect(Collectors.toSet());

			Map<UUID, HibNodeFieldContainerVersionsEdgeImpl> ancestorLookup = previousAndNextPairs.stream()
					.map(Pair::getLeft)
					.distinct()
					.collect(Collectors.toMap(HibNodeFieldContainerVersionsEdgeImpl::getNextContentUuid, Function.identity()));

			for (Pair<HibNodeFieldContainerVersionsEdgeImpl, HibNodeFieldContainerVersionsEdgeImpl> versionPair : previousAndNextPairs) {
				HibNodeFieldContainerVersionsEdgeImpl previous = versionPair.getLeft();
				HibNodeFieldContainerVersionsEdgeImpl next = versionPair.getRight();

				HibNodeFieldContainerVersionsEdgeImpl ancestor = findAncestor(previous, ancestorLookup, containersUuids);

				if (ancestor == null || next == null) {
					continue;
				}

				HibNodeFieldContainerVersionsEdgeImpl toPersist = new HibNodeFieldContainerVersionsEdgeImpl();
				toPersist.setThisContentUuid(ancestor.getThisContentUuid());
				toPersist.setThisVersion(ancestor.getThisVersion());
				toPersist.setNextContentUuid(next.getNextContentUuid());
				toPersist.setNextVersion(next.getNextVersion());

				em.persist(toPersist);
			}
			log.info("Containers purged: {} of {}", progress, containersUuids.size());
		});
		// 2. Now we can delete the containers (and all their related edges, version edges, field edges) safely.
		delete(containers);
	}

	private HibNodeFieldContainerVersionsEdgeImpl findAncestor(HibNodeFieldContainerVersionsEdgeImpl edge, Map<UUID, HibNodeFieldContainerVersionsEdgeImpl> ancestorLookup, Set<UUID> blackList) {
		if (edge == null) {
			return null;
		}

		HibNodeFieldContainerVersionsEdgeImpl ancestor = ancestorLookup.get(edge.getNextContentUuid());
		if (ancestor == null) {
			// this could happen when the blacklist contains all the keys of the lookup map
			return null;
		}

		if (!blackList.contains(ancestor.getThisContentUuid())) {
			return ancestor;
		}

		return findAncestor(ancestorLookup.get(ancestor.getThisContentUuid()), ancestorLookup, blackList);
	}

	/**
	 * Delete the containers, notifying the bulk context.
	 *
	 * @param containers
	 * @param bac
	 */
	@SuppressWarnings("unchecked")
	public void delete(List<? extends HibNodeFieldContainer> containers) {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();

		// 1. Delete versions.
		List<UUID> contentUuids = containers.stream().map(HibNodeFieldContainerImpl.class::cast).map(HibNodeFieldContainerImpl::getDbUuid)
				.collect(Collectors.toList());
		SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(2), slice -> em.createNamedQuery("containerversions.bulkDeleteThisContents")
				.setParameter("contentUuids", slice)
				.executeUpdate());
		SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(2), slice -> em.createNamedQuery("containerversions.bulkDeleteNextContents")
				.setParameter("contentUuids", slice)
				.executeUpdate());

		// 2. Create a set of pairs, where the left of the pair is the type of the field, and the right is the optional
		// extra type (available for list fields). We can then iterate through the type list to delete the edges
		// corresponding to the field type.
		Set<HibSchemaVersion> schemaVersions = containers.stream().map(HibNodeFieldContainer::getSchemaContainerVersion)
				.collect(Collectors.toSet());

		Set<Pair<String, String>> fieldTypes = schemaVersions.stream()
				.flatMap(schema -> schema.getSchema().getFields().stream())
				.map(field -> Pair.of(field.getType(), field instanceof ListFieldSchema ? ((ListFieldSchema) field).getListType() : null))
				.collect(Collectors.toSet());

		for (Pair<String, String> typePair : fieldTypes) {
			FieldTypes fieldType = FieldTypes.valueByName(typePair.getLeft());
			switch (fieldType) {
				case STRING:
				case HTML:
				case NUMBER:
				case DATE:
				case BOOLEAN:
					// These are stored in the content table, so they will be deleted later on.
					break;
				case NODE:
					SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("nodefieldref.removeByContainerUuids")
							.setParameter("containerUuids", slice)
							.executeUpdate());
					break;
				case LIST:
					FieldTypes listType = FieldTypes.valueByName(typePair.getRight());
					if (FieldTypes.MICRONODE.equals(listType)) {
						// micronode list get a special treatment since we need to delete the unreferenced micronodes afterwards.
						deleteMicronodesList(contentUuids);
					} else {
						Class<?> listClass = getListClass(listType);
						String tableName = databaseConnector.getSessionMetadataIntegrator().getTableName(listClass);
						String entityName = tableName.substring(MeshTablePrefixStrategy.TABLE_NAME_PREFIX.length());
						SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> tx.entityManager().createQuery(String.format(QUERY_DELETE_ALL_BY_KEYS, entityName))
								.setParameter("containerUuids", slice)
								.executeUpdate());
					}
					break;
				case BINARY:
					HibernateTx.get().binaryDao().removeField(contentUuids);
					break;
				case S3BINARY:
					HibernateTx.get().s3binaryDao().removeField(contentUuids);
					break;
				case MICRONODE:
					deleteMicronodes(contentUuids);
					break;
			}
		}

		// 3. Add events to the action context.
		List<HibNodeFieldContainerEdgeImpl> edges = new ArrayList<>();
		for (HibNodeFieldContainer container : containers) {
			List<? extends HibNodeFieldContainerEdge> containerEdges = getContainerEdges(container).collect(Collectors.toList());
			containerEdges.forEach(edge -> {
				switch (edge.getType()) {
					case DRAFT:
					case PUBLISHED:
						String branchUuid = ((HibNodeFieldContainerEdgeImpl) edge).getBranch().getUuid();
						tx.batch().add(onDeleted(container, branchUuid, edge.getType()));
						break;
					default:
						break;
				}
			});

			edges.addAll((Collection<? extends HibNodeFieldContainerEdgeImpl>) containerEdges);
		}

		// 4. Delete the edges.
		if (!edges.isEmpty()) {
			SplittingUtils.splitAndConsume(edges.stream().map(HibNodeFieldContainerEdgeImpl::getElement).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("contentEdge.deleteByUuids")
					.setParameter("uuids", slice)
					.executeUpdate());

			// We also need to remove them from the associated nodes if they are loaded and detach them from the hibernate context.
			// This is needed because the node linked to the edge might be removed later on during this transaction.
			// Since its deletion triggers the deletion of his edges a second time, we have to add this workaround.
			edges.forEach(edge -> {
				if (Hibernate.isInitialized(edge.getNode()) && Hibernate.isInitialized(((HibNodeImpl) edge.getNode()).getContentEdges())) {
					((HibNodeImpl) edge.getNode()).getContentEdges().remove(edge);
				}
				em.detach(edge);
			});
		}

		// 5. Delete the containers themselves
		contentStorage.delete((Collection<? extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>>) containers);

		// 6. Update bac count.
		containers.forEach(ignored -> tx.data().maybeGetBulkActionContext().ifPresent(BulkActionContext::inc));
	}

	private Class<?> getListClass(FieldTypes listType) {
		switch (listType) {
			case STRING:
				return HibStringListFieldEdgeImpl.class;
			case HTML:
				return HibHtmlListFieldEdgeImpl.class;
			case NUMBER:
				return HibNumberListFieldEdgeImpl.class;
			case DATE:
				return HibDateListFieldEdgeImpl.class;
			case BOOLEAN:
				return HibBooleanListFieldEdgeImpl.class;
			case NODE:
				return HibNodeListFieldEdgeImpl.class;
			case MICRONODE:
				return HibMicronodeListFieldEdgeImpl.class;
			default:
				throw new RuntimeException("Don't know class for list type " + listType);
		}
	}

	@Override
	public void delete(HibNodeFieldContainer content, boolean deleteNext) {
		if (deleteNext) {
			// Recursively delete all versions of the container
			for (HibNodeFieldContainer next : getNextVersions(content)) {
				// no need to delete next since we will do that
				delete(next);
			}
		}
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();

		// delete versioning edges
		em.createNamedQuery("containerversions.deleteThisContent")
				.setParameter("contentUuid", content.getId())
				.executeUpdate();
		em.createNamedQuery("containerversions.deleteNextContent")
				.setParameter("contentUuid", content.getId())
				.executeUpdate();

		// Delete the fields
		content.getFields().forEach(field -> ((AbstractHibField) field).onFieldDeleted(tx));

		List<? extends HibNodeFieldContainerEdge> edges = getContainerEdges(content).collect(Collectors.toList());

		edges.forEach(edge -> {
			switch (edge.getType()) {
			case DRAFT:
			case PUBLISHED:
				String branchUuid = ((HibNodeFieldContainerEdgeImpl) edge).getBranch().getUuid();
				tx.batch().add(onDeleted(content, branchUuid, edge.getType()));
				break;
			default:
				break;
			}
			removeEdge(edge);
		});

		// Delete the container itself
		contentStorage.delete((UUID) content.getId(), getSchemaContainerVersion(content));

		tx.data().maybeGetBulkActionContext().ifPresent(BulkActionContext::inc);
	}

	@Override
	public Stream<? extends HibNodeFieldContainerEdge> getContainerEdges(HibNodeFieldContainer container) {
		EntityManager em = currentTransaction.getEntityManager();
		if (contentEdgesInitialized(container.getNode())) {
			HibNodeImpl impl = (HibNodeImpl) container.getNode();
			return impl.getContentEdges().stream().filter(edge -> edge.getContentUuid().equals(container.getId()));
		}
		return em.createNamedQuery("contentEdge.findByContent", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("contentUuid", container.getId())
				.getResultStream();
	}

	@Override
	public String getDisplayFieldValue(HibNodeFieldContainer content) {
		SchemaModel schema = getSchemaContainerVersion(content).getSchema();
		String displayFieldName = schema.getDisplayField();
		FieldSchema fieldSchema = schema.getField(displayFieldName);

		if (fieldSchema != null) {
			HibField field = content.getField(fieldSchema);
			if (field instanceof HibDisplayField) {
				return ((HibDisplayField) field).getDisplayName();
			}
		}
		return null;
	}

	@Override
	public HibNodeFieldContainer findVersion(HibNode node, List<String> languageTags, String branchUuid, String version) {
		if (maybeVersionLoader().isPresent()) {
			return maybeVersionLoader().get().apply(node, languageTags, version);
		}

		return PersistingContentDao.super.findVersion(node, languageTags, branchUuid, version);
	}

	@Override
	public HibNode getNode(HibNodeFieldContainer content) {
		HibNodeFieldContainerImpl impl = (HibNodeFieldContainerImpl) content;
		// TODO Should this getter make the permissions check?
		return impl.get(CommonContentColumn.NODE, () -> null,
				uuid -> HibernateTx.get().load(uuid, HibNodeImpl.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Stream<Pair<HibNodeFieldContainer, HibNode>> getNodes(Collection<HibNodeFieldContainer> contents) {
		Map<UUID, Collection<HibNodeFieldContainer>> nodeUuids = (Map) contents.stream()
				.map(HibNodeFieldContainerImpl.class::cast)
				.map(impl -> Pair.of(impl.get(CommonContentColumn.NODE, () -> null), impl))
				.collect(Collectors.groupingBy(pair -> (UUID) pair.getKey(), Collectors.mapping(Pair::getValue, Collectors.toSet())));
		return currentTransaction.getTx().nodeDao().loadNodesWithEdges(nodeUuids.keySet()).stream().flatMap(node -> nodeUuids.get(node.getId()).stream().map(content ->  Pair.of(content, node)));
	}

	@Override
	public HibNodeFieldContainer getNodeFieldContainer(HibField field) {
		if (field instanceof AbstractHibField) {
			return (HibNodeFieldContainer) ((AbstractHibField) field).getContainer();
		}

		return null;
	}

	@Override
	public VersionNumber getVersion(HibNodeFieldContainer content) {
		HibNodeFieldContainerImpl impl = (HibNodeFieldContainerImpl) content;
		return new VersionNumber(impl.<String>get(CommonContentColumn.CURRENT_VERSION_NUMBER,
				() -> contentStorage.findColumn(getSchemaContainerVersion(content), impl.getDbUuid(),
						CommonContentColumn.CURRENT_VERSION_NUMBER)));
	}

	@Override
	public void setVersion(HibNodeFieldContainer content, VersionNumber version) {
		HibNodeFieldContainerImpl impl = (HibNodeFieldContainerImpl) content;
		impl.put(CommonContentColumn.CURRENT_VERSION_NUMBER, version.getFullVersion());
	}

	@Override
	public boolean hasNextVersion(HibNodeFieldContainer content) {
		return HibernateUtil.isNotEmpty(currentTransaction.getEntityManager().createNamedQuery("containerversions.findNextEdgeByVersion", HibNodeFieldContainerVersionsEdgeImpl.class)
				.setParameter("contentUuid", content.getId())
				.setParameter("version", getSchemaContainerVersion(content)));
	}

	@Override
	public Iterable<HibNodeFieldContainer> getNextVersions(HibNodeFieldContainer content) {
		return StreamUtil.toIterable(currentTransaction.getEntityManager().createNamedQuery("containerversions.findNextEdgeByVersion", HibNodeFieldContainerVersionsEdgeImpl.class)
				.setParameter("contentUuid", content.getId())
				.setParameter("version", getSchemaContainerVersion(content))
				.getResultStream()
				.map(edge -> getFieldContainer(edge.getNextVersion(), edge.getNextContentUuid())));
	}

	@Override
	public void setNextVersion(HibNodeFieldContainer current, HibNodeFieldContainer next) {
		EntityManager em = currentTransaction.getEntityManager();
		HibNodeFieldContainerVersionsEdgeImpl edge = new HibNodeFieldContainerVersionsEdgeImpl();
		edge.setThisContentUuid((UUID) current.getId());
		edge.setThisVersion(em.find(HibSchemaVersionImpl.class, current.getSchemaContainerVersion().getId()));
		edge.setNextContentUuid((UUID) next.getId());
		edge.setNextVersion(em.find(HibSchemaVersionImpl.class, next.getSchemaContainerVersion().getId()));
		currentTransaction.getEntityManager().persist(edge);
	}

	@Override
	public boolean hasPreviousVersion(HibNodeFieldContainer content) {
		return HibernateUtil.isNotEmpty(currentTransaction.getEntityManager().createNamedQuery("containerversions.findPreviousEdgeByVersion", HibNodeFieldContainerVersionsEdgeImpl.class)
				.setParameter("contentUuid", content.getId())
				.setParameter("version", getSchemaContainerVersion(content)));
	}

	@Override
	public HibNodeFieldContainer getPreviousVersion(HibNodeFieldContainer content) {
		HibNodeFieldContainerVersionsEdgeImpl edge = HibernateUtil.firstOrNull(currentTransaction.getEntityManager().createNamedQuery("containerversions.findPreviousEdgeByVersion", HibNodeFieldContainerVersionsEdgeImpl.class)
				.setParameter("contentUuid", content.getId())
				.setParameter("version", getSchemaContainerVersion(content)));
		if (edge == null) {
			return null;
		}
		return getFieldContainer(edge.getThisVersion(), edge.getThisContentUuid());
	}

	@Override
	public void clone(HibNodeFieldContainer dest, HibNodeFieldContainer src) {
		List<HibField> otherFields = src.getFields();

		for (HibField graphField : otherFields) {
			graphField.cloneTo(dest);
		}
	}

	@Override
	public boolean isType(HibNodeFieldContainer content, ContainerType type) {
		EntityManager em = currentTransaction.getEntityManager();
		if (contentEdgesInitialized(content.getNode())) {
			HibNodeImpl impl = (HibNodeImpl) content.getNode();
			return impl.getContentEdges().stream().anyMatch(edge -> edge.getContentUuid().equals(content.getId()) && edge.getType().equals(type));
		}

		return HibernateUtil.isNotEmpty(em.createNamedQuery("contentEdge.findByContentAndTypes", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("contentUuid", content.getId())
				.setParameter("types", Collections.singletonList(type))
				.setHint(AvailableHints.HINT_CACHEABLE, true));
	}

	@Override
	public boolean isType(HibNodeFieldContainer content, ContainerType type, String branchUuid) {
		EntityManager em = currentTransaction.getEntityManager();
		if (contentEdgesInitialized(content.getNode())) {
			HibNodeImpl impl = (HibNodeImpl) content.getNode();
			return impl.getContentEdges().stream().anyMatch(edge -> edge.getContentUuid().equals(content.getId()) && edge.getType().equals(type) && edge.getBranchUuid().equals(branchUuid));
		}

		return HibernateUtil.isNotEmpty(em.createNamedQuery("contentEdge.findByContentTypeAndBranch", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("contentUuid", content.getId())
				.setParameter("type", type)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setHint(AvailableHints.HINT_CACHEABLE, true));
	}

	@Override
	public Set<String> getBranches(HibNodeFieldContainer content, ContainerType type) {
		EntityManager em = currentTransaction.getEntityManager();

		return em.createNamedQuery("contentEdge.findByContentAndTypes", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("contentUuid", content.getId())
				.setParameter("types", Collections.singletonList(type))
				.getResultStream()
				.map(HibNodeFieldContainerEdgeImpl::getBranch)
				.map(HibBranch::getUuid)
				.collect(Collectors.toSet());
	}

	@Override
	public HibSchemaVersion getSchemaContainerVersion(HibNodeFieldContainer content) {
		return content.getSchemaContainerVersion();
	}

	@Override
	public List<HibMicronodeField> getMicronodeFields(HibNodeFieldContainer content) {
		return currentTransaction.getEntityManager()
				.createNamedQuery("micronodefieldref.findEdgeByContentUuidAndType", HibMicronodeFieldEdgeImpl.class)
				.setParameter("uuid", content.getId())
				.setParameter("type", ReferenceType.FIELD)
				.getResultStream()
				.map(edge -> new HibMicronodeFieldImpl((HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) content, edge))
				.collect(Collectors.toList());
	}

	@Override
	public Result<HibMicronodeFieldList> getMicronodeListFields(HibNodeFieldContainer content) {
		return new TraversalResult<>(currentTransaction.getEntityManager()
				.createNamedQuery("micronodelistitem.findUniqueFieldKeysByContentUuidTypeAndVersion", Tuple.class)
				.setParameter("containerUuid", content.getId())
				.setParameter("containerType", content.getReferenceType())
				.getResultStream()
				.map(tuple -> new HibMicronodeListFieldImpl(tuple.get(0, String.class), (HibUnmanagedFieldContainer<?, ?, ?, ?, ?>) content, tuple.get(1, UUID.class))));
	}

	@Override
	public void setDisplayFieldValue(HibNodeFieldContainer container, String value) {
		// nothing to do
	}

	@Override
	public boolean isPurgeable(HibNodeFieldContainer content) {
		return getContainerEdges(content).findAny().isEmpty();
	}

	@Override
	public String getLanguageTag(HibNodeFieldContainer content) {
		return content.getLanguageTag();
	}

	@Override
	public void setLanguageTag(HibNodeFieldContainer content, String languageTag) {
		content.setLanguageTag(languageTag);
	}

	@Override
	public Iterator<? extends HibNodeFieldContainerEdge> getContainerEdges(HibNodeFieldContainer container, ContainerType type, String branchUuid) {
		EntityManager em = currentTransaction.getEntityManager();
		if (contentEdgesInitialized(container.getNode())) {
			HibNodeImpl impl = (HibNodeImpl) container.getNode();
			return impl.getContentEdges().stream()
					.filter(edge -> edge.getContentUuid().equals(container.getId()) && edge.getType().equals(type) && edge.getBranchUuid().equals(branchUuid))
					.iterator();
		}
		return em.createNamedQuery("contentEdge.findByContentTypeAndBranch", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("contentUuid", container.getId())
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("type", type)
				.getResultList()
				.iterator();
	}

	@Override
	public HibNodeFieldContainerEdge getConflictingEdgeOfWebrootPath(HibNodeFieldContainer content, String segmentInfo, String branchUuid, ContainerType type, HibNodeFieldContainerEdge edge) {
		//TODO do we need to check contentUuid?
		EntityManager em = currentTransaction.getEntityManager();
		return HibernateUtil.firstOrNull(em.createNamedQuery("contentEdge.findByBranchTypeAndWebroot", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("webrootPath", segmentInfo)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("type", type)
				.setParameter("edgeUuid", ((HibNodeFieldContainerEdgeImpl ) edge).getElement()));
	}

	@Override
	public HibNodeFieldContainerEdge getConflictingEdgeOfWebrootField(HibNodeFieldContainer content, HibNodeFieldContainerEdge edge, String urlFieldValue, String branchUuid, ContainerType type) {
		//TODO do we need to check contentUuid?
		EntityManager em = currentTransaction.getEntityManager();
		return HibernateUtil.firstOrNull(em.createNamedQuery("contentEdge.findByBranchTypeAndUrlField", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("type", type)
				.setParameter("field", urlFieldValue)
				.setParameter("edgeUuid", ((HibNodeFieldContainerEdgeImpl ) edge).getElement()));
	}

	@Override
	public HibNodeFieldContainerEdge getConflictingEdgeOfWebrootField(HibNodeFieldContainer content, HibNodeFieldContainerEdge edge, Set<String> urlFieldValues, String branchUuid, ContainerType type) {
		//TODO do we need to check contentUuid?

		EntityManager em = currentTransaction.getEntityManager();
		AtomicReference<HibNodeFieldContainerEdge> foundConflictingEdge = new AtomicReference<>();
		UUID edgeUuid = ((HibNodeFieldContainerEdgeImpl ) edge).getElement();

		// split the set of field values into slices, and - as long as we did not find a conflicting edge - keep searching for each slice
		SplittingUtils.splitAndConsume(urlFieldValues, HibernateUtil.inQueriesLimitForSplitting(3), slice -> {
			if (foundConflictingEdge.get() == null) {
				foundConflictingEdge.set(HibernateUtil.firstOrNull(em.createNamedQuery("contentEdge.findByBranchTypeAndUrlFieldValues", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("type", type)
					.setParameter("field", slice)
					.setParameter("edgeUuid", edgeUuid)));
			}
		});
		return foundConflictingEdge.get();
	}

	@Override
	public Result<? extends HibNodeFieldContainerEdge> getFieldEdges(HibNode node, String branchUuid, ContainerType type) {
		EntityManager em = currentTransaction.getEntityManager();
		return new TraversalResult<>(em.createNamedQuery("contentEdge.findByNodeTypeAndBranch", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("node", node)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("type", type)
				.getResultList());
	}

	@Override
	public long globalCount() {
		return contentStorage.getGlobalCount();
	}

	@Override
	public Stream<? extends HibMicronodeContainerImpl> findAllMicronodes() {
		return HibernateTx.get().loadAll(HibMicronodeFieldEdgeImpl.class)
				.map(HibMicronodeFieldEdgeImpl::getMicronode);
	}

	@Override
	public HibNodeFieldContainer createPersisted(String nodeUUID, HibSchemaVersion schemaVersion, String uuid,
			String languageTag, VersionNumber versionNumber, HibUser editor) {
		long dbVersion = 1L;
		Object schemaVersionDbUuid = schemaVersion.getId();
		Object schemaDbUuid = schemaVersion.getSchemaContainer().getId();
		UUID nodeUuid = UUIDUtil.toJavaUuid(nodeUUID);

		HibNodeFieldContainerImpl container = new HibNodeFieldContainerImpl();
		container.setDbUuid(uuidGenerator.toJavaUuidOrGenerate(uuid));
		container.setSchemaContainerVersion(schemaVersion);
		container.put(CommonContentColumn.DB_VERSION, dbVersion);
		container.put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, schemaVersionDbUuid);
		container.put(CommonContentColumn.SCHEMA_DB_UUID, schemaDbUuid);
		container.put(CommonContentColumn.NODE, nodeUuid);
		container.put(CommonContentColumn.BUCKET_ID, BucketTracking.generateBucketId());
		container.put(CommonContentColumn.LANGUAGE_TAG, languageTag);
		container.put(CommonContentColumn.EDITED, new Timestamp(System.currentTimeMillis()));
		container.put(CommonContentColumn.EDITOR_DB_UUID, editor != null ? editor.getId() : null);
		container.put(CommonContentColumn.CURRENT_VERSION_NUMBER, versionNumber.getFullVersion());

		ContentInterceptor contentInterceptor = HibernateTx.get().getContentInterceptor();
		contentInterceptor.persist(container);

		return container;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<HibNode, HibNodeFieldContainer> getFieldsContainers(Collection<? extends HibNode> nodes,
			String languageTag, HibBranch branch, ContainerType type) {
		return contentStorage.findMany(getEdges((Collection<HibNodeImpl>) nodes, List.of(languageTag), branch.getUuid(), type))
				.stream().collect(Collectors.toMap(HibNodeFieldContainerImpl::getNodeId, Function.identity()))
				.entrySet().stream()
				.map(entry -> Pair.of(nodes.stream().filter(node -> node.getId().equals(entry.getKey())).findAny().orElse(null), entry.getValue()))
				.filter(pair -> pair.getKey() != null)
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	/**
	 * Create the persistent database table for the given {@link HibSchemaVersion}.
	 *
	 * @param version
	 */
	public void createContentTable(HibSchemaVersion version) {
		contentStorage.createTable(version);
		contentStorage.createIndex(version, CommonContentColumn.NODE, false);
	}

	/**
	 * Create the persistent database table for the given {@link HibMicroschemaVersion}.
	 *
	 * @param version
	 */
	public void createContentTable(HibMicroschemaVersion version) {
		contentStorage.createMicronodeTable(version);
	}

	/**
	 * Delete the persisted table of a {@link HibSchemaVersion}.
	 *
	 * @param version
	 */
	public void deleteContentTable(HibSchemaVersion version) {
		contentStorage.dropTable(version);
	}

	/**
	 * Delete the persisted table of a {@link HibMicroschemaVersion}.
	 *
	 * @param version
	 */
	public void deleteContentTable(HibMicroschemaVersion version) {
		contentStorage.dropTable(version);
	}

	@Override
	public HibNodeFieldContainerEdgeImpl createContainerEdge(HibNode node, HibNodeFieldContainer container, HibBranch branch, String languageTag, ContainerType type) {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();
		HibNodeImpl graphNode = (HibNodeImpl) node;
		HibNodeFieldContainerEdgeImpl edge = new HibNodeFieldContainerEdgeImpl();
		edge.setElement(uuidGenerator.generateType1UUID());
		edge.setBranch(branch);
		edge.setContentUuid((UUID) container.getId());
		edge.setLanguageTag(languageTag);
		edge.setNode(node);
		edge.setType(type);
		edge.setVersion(getSchemaContainerVersion(container));
		edge.setLastEditedTimestamp(container.getLastEditedTimestamp() != null ? container.getLastEditedTimestamp() : System.currentTimeMillis());
		edge.setEditor(container.getEditor());
		em.persist(edge);
		graphNode.addEdge(edge);
		// Using Tx here, as we do not know yet, if the node is already persisted.
		tx.persist(graphNode);
		return edge;
	}
	@Override
	public void connectFieldContainer(HibNode node, HibNodeFieldContainer container, HibBranch branch,
									  String languageTag, boolean handleDraftEdge) {
		HibNodeImpl graphNode = (HibNodeImpl) node;
		if (handleDraftEdge) {
			HibNodeFieldContainerEdgeImpl oldDraftEdge = getDraftEdgeForLanguageAndBranch(graphNode.getContentEdges(), languageTag, branch);
			String segmentInfo = null;
			if (oldDraftEdge != null) {
				segmentInfo = oldDraftEdge.getSegmentInfo();
			}

			// remove existing draft edge
			if (oldDraftEdge != null) {
				removeEdge(oldDraftEdge);
			}

			// create a new draft edge
			HibNodeFieldContainerEdgeImpl newDraftEdge = createContainerEdge(node, container, branch, languageTag, DRAFT);
			if (!StringUtils.isEmpty(segmentInfo)) {
				newDraftEdge.setSegmentInfo(segmentInfo);
			}
			updateWebrootPathInfo(container, branch.getUuid(), "node_conflicting_segmentfield_update", false);
		}

		// if there is no initial edge, create one
		if (graphNode.getContentEdges().stream()
				.noneMatch(edge -> edge.getBranch().equals(branch)
						&& edge.getLanguageTag().equals(languageTag)
						&& edge.getType().equals(INITIAL))) {
			createContainerEdge(node, container, branch, languageTag, INITIAL);
		}
	}

	public HibNodeFieldContainerEdgeImpl getDraftEdgeForLanguageAndBranch(Set<HibNodeFieldContainerEdgeImpl> edges, String languageTag, HibBranch branch) {
		return edges.stream()
				.filter(edge -> edge.getBranch().equals(branch)
						&& edge.getLanguageTag().equals(languageTag)
						&& edge.getType().equals(DRAFT))
				.findAny()
				.orElse(null);
	}

	@Override
	public HibNode getParentNode(HibNodeFieldContainer container, String branchUuid) {
		return container.getNode();
	}

	public void delete(HibMicronode micronode) {
		HibernateTx tx = HibernateTx.get();
		// Delete the fields
		micronode.getFields().forEach(field -> ((AbstractHibField) field).onFieldDeleted(tx));
		// Delete the explicitly owned micronode data
		contentStorage.delete((UUID) micronode.getId(), micronode.getSchemaContainerVersion());
	}

	/**
	 * Delete the micronode lists (including the micronodes themselves), which are referenced from the given containers
	 * @param containerUuids container UUIDs
	 * @param bac bulk action context
	 */
	private void deleteMicronodesList(List<UUID> containerUuids) {
		EntityManager em = HibernateTx.get().entityManager();

		// 1. get all micro lists to remove
		List<HibMicronodeListFieldEdgeImpl> listFieldEdges = SplittingUtils.splitAndMergeInList(containerUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("micronodelistitem.findByContainerUuids", HibMicronodeListFieldEdgeImpl.class)
				.setParameter("containerUuids", slice)
				.getResultList());

		Set<ContentKey> microKeys = listFieldEdges.stream()
				.map(edge -> new ContentKey(edge.getValueOrUuid(), (UUID) edge.getMicroschemaVersion().getId(), ReferenceType.MICRONODE))
				.collect(Collectors.toSet());

		// 2. get all containers
		Map<UUID, HibMicronodeContainerImpl> microFieldContainers = contentStorage.findManyMicronodes(microKeys)
				.stream()
				.collect(Collectors.toMap(HibMicronodeContainerImpl::getDbUuid, Function.identity()));

		// 3. remove the micro lists
		SplittingUtils.splitAndConsume(containerUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("micronodelistitem.removeByContainerUuids")
				.setParameter("containerUuids", slice)
				.executeUpdate());

		// 4. identify the referenced micro field containers
		Set<UUID> referencedMicronodesUuids = getReferencedMicroFieldContainers(microFieldContainers.keySet());

		// 5. remove those referenced micro fields from the micro field map
		// we are left with unreferenced micro fields
		microFieldContainers.keySet().removeAll(referencedMicronodesUuids);

		// 6. remove the associated fields of the micro fields
		Set<Pair<String, String>> fieldTypes = microFieldContainers.values().stream()
				.flatMap(micro -> micro.getSchemaContainerVersion().getSchema().getFields().stream())
				.map(field -> Pair.of(field.getType(), field instanceof ListFieldSchema ? ((ListFieldSchema) field).getListType() : null))
				.collect(Collectors.toSet());

		List<UUID> contentUuids = new ArrayList<>(microFieldContainers.keySet());

		for (Pair<String, String> typePair : fieldTypes) {
			FieldTypes fieldType = FieldTypes.valueByName(typePair.getLeft());
			switch (fieldType) {
				case STRING:
				case HTML:
				case NUMBER:
				case DATE:
				case BOOLEAN:
					// These are stored in the content table, so they will be deleted later on.
					break;
				case NODE:
					SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("nodefieldref.removeByContainerUuids")
							.setParameter("containerUuids", slice)
							.executeUpdate());
					break;
				case LIST:
					FieldTypes listType = FieldTypes.valueByName(typePair.getRight());
					if (FieldTypes.MICRONODE.equals(listType)) {
						// this is not possible (micronodes cannot be nested)
					} else {
						Class<?> listClass = getListClass(listType);
						String tableName = databaseConnector.getSessionMetadataIntegrator().getTableName(listClass);
						String entityName = tableName.substring(MeshTablePrefixStrategy.TABLE_NAME_PREFIX.length());
						SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createQuery(String.format(QUERY_DELETE_ALL_BY_KEYS, entityName))
								.setParameter("containerUuids", slice)
								.executeUpdate());
					}
					break;
				case BINARY:
					HibernateTx.get().binaryDao().removeField(contentUuids);
					break;
				case S3BINARY:
					HibernateTx.get().s3binaryDao().removeField(contentUuids);
					break;
				case MICRONODE:
					// this is not possible (micronodes cannot be nested)
					break;
			}
		}

		// 7. finally remove the containers from the content storage
		contentStorage.delete(microFieldContainers.values());
	}

	/**
	 * Delete the micronodes, which are referenced by the given containers
	 * @param containerUuids container UUIDs
	 * @param bac bulk action context
	 */
	private void deleteMicronodes(List<UUID> containerUuids) {
		EntityManager em = HibernateTx.get().entityManager();

		// 1. get all edges to remove
		List<HibMicronodeFieldEdgeImpl> edges = SplittingUtils.splitAndMergeInList(containerUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("micronodefieldref.findByContainerUuids", HibMicronodeFieldEdgeImpl.class)
				.setParameter("containerUuids", slice)
				.getResultList());

		Set<ContentKey> microKeys = edges.stream()
				.map(edge -> new ContentKey(edge.getValueOrUuid(), (UUID) edge.getMicroschemaVersion().getId(), ReferenceType.MICRONODE))
				.collect(Collectors.toSet());

		// 2. get all containers
		Map<UUID, HibMicronodeContainerImpl> microFieldContainers = contentStorage.findManyMicronodes(microKeys)
				.stream()
				.collect(Collectors.toMap(HibMicronodeContainerImpl::getDbUuid, Function.identity()));

		// 3. remove the edges
		SplittingUtils.splitAndConsume(containerUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("micronodefieldref.removeByContainerUuids")
				.setParameter("containerUuids", slice)
				.executeUpdate());

		// 4. identify referenced micro field containers uuids
		Set<UUID> referencedMicronodesUuids = getReferencedMicroFieldContainers(microFieldContainers.keySet());

		// 5. remove those referenced micro fields from the micro field map
		// we are left with unreferenced micro fields
		microFieldContainers.keySet().removeAll(referencedMicronodesUuids);

		// 6. remove the associated fields of the micro fields
		Set<Pair<String, String>> fieldTypes = microFieldContainers.values().stream()
				.flatMap(micro -> micro.getSchemaContainerVersion().getSchema().getFields().stream())
				.map(field -> Pair.of(field.getType(), field instanceof ListFieldSchema ? ((ListFieldSchema) field).getListType() : null))
				.collect(Collectors.toSet());

		List<UUID> contentUuids = new ArrayList<>(microFieldContainers.keySet());

		for (Pair<String, String> typePair : fieldTypes) {
			FieldTypes fieldType = FieldTypes.valueByName(typePair.getLeft());
			switch (fieldType) {
				case STRING:
				case HTML:
				case NUMBER:
				case DATE:
				case BOOLEAN:
					// These are stored in the content table, so they will be deleted later on.
					break;
				case NODE:
					SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createNamedQuery("nodefieldref.removeByContainerUuids")
							.setParameter("containerUuids", slice)
							.executeUpdate());
					break;
				case LIST:
					FieldTypes listType = FieldTypes.valueByName(typePair.getRight());
					if (FieldTypes.MICRONODE.equals(listType)) {
						// this is not possible (micronodes cannot be nested)
					} else {
						Class<?> listClass = getListClass(listType);
						String tableName = databaseConnector.getSessionMetadataIntegrator().getTableName(listClass);
						String entityName = tableName.substring(MeshTablePrefixStrategy.TABLE_NAME_PREFIX.length());
						SplittingUtils.splitAndConsume(contentUuids, HibernateUtil.inQueriesLimitForSplitting(1), slice -> em.createQuery(String.format(QUERY_DELETE_ALL_BY_KEYS, entityName))
								.setParameter("containerUuids", slice)
								.executeUpdate());
					}
					break;
				case BINARY:
					HibernateTx.get().binaryDao().removeField(contentUuids);
					break;
				case S3BINARY:
					HibernateTx.get().s3binaryDao().removeField(contentUuids);
					break;
				case MICRONODE:
					// this is not possible (micronodes cannot be nested)
					break;
			}
		}

		// 7. finally remove them from the content table
		contentStorage.delete(microFieldContainers.values());
	}

	/**
	 * Return which of the provided uuids is referenced by either micronode edges or micronode list edges.
	 * @param micronodeUuids
	 * @return
	 */
	public Set<UUID> getReferencedMicroFieldContainers(Collection<UUID> micronodeUuids) {
		EntityManager em = HibernateTx.get().entityManager();

		return SplittingUtils.splitAndMergeInSet(micronodeUuids, HibernateUtil.inQueriesLimitForSplitting(2), slice -> {
			List<UUID> referencedByEdges = em.createNamedQuery("micronodefieldref.findByMicrocontainerUuids", HibMicronodeFieldEdgeImpl.class)
						.setParameter("micronodeUuids", slice)
					.getResultStream()
					.map(HibMicronodeFieldEdgeImpl::getValueOrUuid)
					.collect(Collectors.toList());
	
			List<UUID> referencedInLists = em.createNamedQuery("micronodelistitem.findByMicronodeUuids", HibMicronodeListFieldEdgeImpl.class)
						.setParameter("micronodeUuids", slice)
					.getResultStream()
					.map(HibMicronodeListFieldEdgeImpl::getValueOrUuid)
					.collect(Collectors.toList());

			return Stream.of(referencedByEdges, referencedInLists).flatMap(Collection::stream).collect(Collectors.toSet());
		});
	}

	public void tryDelete(HibMicronode micronode, AbstractFieldEdgeImpl<UUID> owner) {
		if (micronode == null) {
			return;
		}
		UUID edgeContainerUuid = owner.getContainerUuid();
		ReferenceType edgeContainerType = owner.getContainerType();
		micronode.getContents()
			.map(HibUnmanagedFieldContainer.class::cast)
			.filter(container -> // Filter out the container we currently belong to
					!container.getDbUuid().equals(edgeContainerUuid) || !container.getReferenceType().equals(edgeContainerType))
			.findAny()
			.ifPresentOrElse(
					otherOwner -> {
						// The micronode is occupied by someone else -> no deletion allowed.
						HibMicronodeField.log.debug("Micronode { " + micronode.getUuid() + " } is occupied by " + otherOwner.getReferenceType() + "{ " + owner.getUuid() + " }, no deletion possible");
					}, () -> {
						// Free to delete
						delete(micronode);
					}
			);
	}

	public HibNodeFieldContainerImpl getFieldContainer(HibSchemaVersion version, UUID containerUUID) {
		return contentStorage.findOne(version, containerUUID);
	}

	public HibMicronodeContainerImpl getFieldContainer(HibMicroschemaVersion version, UUID containerUUID) {
		return contentStorage.findOneMicronode(version, containerUUID);
	}

	public Stream<HibNodeFieldContainerImpl> getFieldsContainers(HibSchemaVersion version) {
		return contentStorage.findMany(version).stream();
	}

	public Stream<HibMicronodeContainerImpl> getFieldsContainers(HibMicroschemaVersion version) {
		return contentStorage.findManyMicronodes(version).stream();
	}

	public Stream<HibNodeFieldContainerEdgeImpl> streamEdgesOfWebrootField(String urlFieldValue, String branchUuid, ContainerType type) {
		EntityManager em = currentTransaction.getEntityManager();
		return em.createNamedQuery("contentEdge.findAllByBranchTypeAndUrlField", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("type", type)
				.setParameter("field", urlFieldValue)
				.getResultStream();
	}

	public Stream<HibNodeFieldContainerEdgeImpl> streamEdgesOfWebrootPath(String segmentInfo, String branchUuid, ContainerType type) {
		EntityManager em = currentTransaction.getEntityManager();
		return em.createNamedQuery("contentEdge.findAllByBranchTypeAndWebroot", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("webrootPath", segmentInfo)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("type", type)
				.getResultStream();
	}

	@Override
	public void deleteField(HibDeletableField field) {
		AbstractDeletableHibField<?> impl = (AbstractDeletableHibField<?>) field;
		HibFieldEdge referenced = impl.getReferencedEdge();
		if (referenced != null) {
			currentTransaction.getEntityManager().remove(referenced);
		}
	}

	@Override
	public HibField detachField(HibField field) {
		if (AbstractReferenceHibField.class.isInstance(field)) {
			return AbstractReferenceHibField.class.cast(field).getReferencedEdge();
		}
		return field;
	}

	private Optional<BiFunction<HibNode, ContainerType, List<HibNodeFieldContainer>>> maybeTypeLoader() {
		HibernateTx tx = HibernateTx.get();
		ContentInterceptor contentInterceptor = tx.getContentInterceptor();
		return contentInterceptor.getDataLoaders().flatMap(DataLoaders::getTypeLoader);
	}

	private Optional<TriFunction<HibNode, List<String>, String, HibNodeFieldContainer>> maybeVersionLoader() {
		HibernateTx tx = HibernateTx.get();
		ContentInterceptor contentInterceptor = tx.getContentInterceptor();
		return contentInterceptor.getDataLoaders().flatMap(DataLoaders::getVersionLoader);
	}

	/**
	 * Returns whether the content edges are initialized for the node
	 * @param node
	 * @return
	 */
	private boolean contentEdgesInitialized(HibNode node) {
		EntityManager em = currentTransaction.getEntityManager();
		return em.contains(node);
	}

	/**
	 * Given the provided container keys, return a map where the key is the provided container uuid, and the value is
	 * their previous version. If there is no previous version, the value is null.
	 * @param containersUuids
	 * @return
	 */
	public Map<ContentKey, ContentKey> getPreviousContainersUuidsMap(Set<ContentKey> containersUuids) {
		EntityManager em = currentTransaction.getEntityManager();

		Map<ContentKey, ContentKey> nextContentMap = SplittingUtils.splitAndMergeInMap(
				containersUuids, 
				HibernateUtil.inQueriesLimitForSplitting(1), 
				slice -> em.createNamedQuery("containerversions.findPreviousByIds", HibNodeFieldContainerVersionsEdgeImpl.class)
					.setParameter("contentUuids", slice.stream().map(ContentKey::getContentUuid).collect(Collectors.toList()))
				.getResultStream()
				.collect(Collectors.toMap(e -> ContentKey.fromContentUUIDAndVersion(e.getNextContentUuid(), e.getNextVersion()),
							e -> ContentKey.fromContentUUIDAndVersion(e.getThisContentUuid(), e.getThisVersion())))
			);

		return CollectionUtil.addFallbackValueForMissingKeys(nextContentMap, containersUuids, null);
	}

	/**
	 * Preload values of list fields in the given containers
	 * @param containers containers
	 */
	public void loadListFields(List<? extends HibNodeFieldContainer> containers) {
		getBooleanListFieldValues(getListFieldListUuids(containers, HibBooleanListFieldImpl.class));
		getDateListFieldValues(getListFieldListUuids(containers, HibDateListFieldImpl.class));
		getNumberListFieldValues(getListFieldListUuids(containers, HibNumberListFieldImpl.class));
		getHtmlListFieldValues(getListFieldListUuids(containers, HibHtmlListFieldImpl.class));
		getStringListFieldValues(getListFieldListUuids(containers, HibStringListFieldImpl.class));
		getMicronodeListFieldValues(getListFieldListUuids(containers, HibMicronodeListFieldImpl.class));
	}

	/**
	 * Generic method to get all list UUIDs of list fields of given class in the given containers
	 * @param <U> type of the list field implementation class
	 * @param containers containers
	 * @param classOfU class of the list field implementation in question
	 * @return list of list UUIDs
	 */
	protected <U extends AbstractHibListFieldImpl<?, ?, ?, ?, ?>> List<String> getListFieldListUuids(List<? extends HibNodeFieldContainer> containers, Class<U> classOfU) {
		return containers.stream()
				.flatMap(container -> container.getFields().stream())
				.filter(field -> classOfU.isAssignableFrom(field.getClass()))
				.map(field -> (classOfU.cast(field)).valueOrNull())
				.filter(value -> value != null)
				.map(UUID::toString)
				.collect(Collectors.toList());
	}

	@Override
	public boolean supportsPrefetchingListFieldValues() {
		return true;
	}

	@Override
	public Map<String, List<Boolean>> getBooleanListFieldValues(List<String> listUuids) {
		return getListValues(listUuids, HibBooleanListFieldEdgeImpl::getBoolean, HibBooleanListFieldEdgeImpl.class);
	}

	@Override
	public Map<String, List<Long>> getDateListFieldValues(List<String> listUuids) {
		return getListValues(listUuids, HibDateListFieldEdgeImpl::getDate, HibDateListFieldEdgeImpl.class);
	}

	@Override
	public Map<String, List<Number>> getNumberListFieldValues(List<String> listUuids) {
		return getListValues(listUuids, HibNumberListFieldEdgeImpl::getNumber, HibNumberListFieldEdgeImpl.class);
	}

	@Override
	public Map<String, List<String>> getHtmlListFieldValues(List<String> listUuids) {
		return getListValues(listUuids, HibHtmlListFieldEdgeImpl::getHTML, HibHtmlListFieldEdgeImpl.class);
	}

	@Override
	public Map<String, List<String>> getStringListFieldValues(List<String> listUuids) {
		return getListValues(listUuids, HibStringListFieldEdgeImpl::getString, HibStringListFieldEdgeImpl.class);
	}

	@Override
	public Map<String, List<HibMicronode>> getMicronodeListFieldValues(List<String> listUuids) {
		Map<String, List<ContentKey>> keyLists = getListValues(listUuids, edge -> new ContentKey(edge.getValueOrUuid(),
				(UUID) edge.getMicroschemaVersion().getId(), ReferenceType.MICRONODE),
				HibMicronodeListFieldEdgeImpl.class);

		Set<ContentKey> keys = new HashSet<>();
		keyLists.values().forEach(keys::addAll);
		List<HibMicronodeContainerImpl> micronodes = contentStorage.findManyMicronodes(keys);

		Map<ContentKey, HibMicronode> micronodesPerKey = new HashMap<>();
		micronodes.forEach(micronode -> {
			ContentKey key = ContentKey.fromContent(micronode);
			micronodesPerKey.put(key, micronode);
		});

		Map<String, List<HibMicronode>> resultMap = new HashMap<>();
		keyLists.entrySet().forEach(entry -> {
			List<HibMicronode> micronodeList = new ArrayList<>();
			entry.getValue().forEach(key -> {
				HibMicronode micronode = micronodesPerKey.get(key);
				if (micronode != null) {
					micronodeList.add(micronode);
				}
			});
			resultMap.put(entry.getKey(), micronodeList);
		});

		return resultMap;
	}

	/**
	 * Generic method to get list field values for given list UUIDs. The implementation will first get the items from the {@link ListableFieldCache}.
	 * Everything not found in the cache will be loaded (with a single query) from the database and put into the cache.
	 * @param <U> type of the field values
	 * @param <I> type of the list field implementation
	 * @param listUuids list UUIDs
	 * @param valueExtractor function that will extract the field value out of the implementation
	 * @param classOfI class of the field implementation
	 * @return map of list UUIDs to lists of string field values
	 */
	protected <U, I extends AbstractHibListFieldEdgeImpl<?>> Map<String, List<U>> getListValues(List<String> listUuids,
			Function<I, U> valueExtractor, Class<I> classOfI) {
		if (CollectionUtils.isEmpty(listUuids)) {
			return Collections.emptyMap();
		}

		ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> listableFieldCache = HibernateTx.get().data().getListableFieldCache();

		Map<String, List<U>> valueMap = new HashMap<>();
		Collection<UUID> toLoad = new ArrayList<>();
		listUuids.stream().forEach(listUuid -> {
			UUID uuid = UUID.fromString(listUuid);
			List<? extends AbstractHibListFieldEdgeImpl<?>> cached = listableFieldCache.get(uuid);
			if (cached != null) {
				// get values from cache
				valueMap.put(listUuid,
						cached.stream().map(item -> classOfI.cast(item)).map(valueExtractor).collect(Collectors.toList()));
			} else {
				toLoad.add(uuid);
			}
		});

		// load missing
		if (!toLoad.isEmpty()) {
			Map<UUID, List<I>> itemMap = AbstractHibListFieldEdgeImpl.getItems(HibernateTx.get(), classOfI, toLoad);
			for (Entry<UUID, List<I>> entry : itemMap.entrySet()) {
				// put into cache
				listableFieldCache.put(entry.getKey(), entry.getValue());
				// add the value map
				valueMap.put(entry.getKey().toString(), entry.getValue().stream().map(valueExtractor).collect(Collectors.toList()));
			}
		}

		return valueMap;
	}

	@Override
	public String[] getHibernateEntityName(Object... args) {
		List<String> versionTables = Arrays.stream(args).filter(AbstractHibFieldSchemaVersion.class::isInstance).map(AbstractHibFieldSchemaVersion.class::cast).map(version -> MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX + UUIDUtil.toShortUuid(version.getUuid())).collect(Collectors.toList());
		if (versionTables.size() > 0) {
			return versionTables.toArray(new String[versionTables.size()]);
		} else {
			return new String[] {databaseConnector.maybeGetDatabaseEntityName(HibNodeFieldContainerEdgeImpl.class).get()};
		}
	}

	@Override
	public boolean isContent() {
		return true;
	}

	@Override
	public Map<HibMicronodeField, HibMicronode> getMicronodes(Collection<HibMicronodeField> micronodeFields) {
		EntityManager em = currentTransaction.getEntityManager();

		Map<UUID, List<HibMicronodeField>> micronodeUuids = new HashMap<>();
		for (HibMicronodeField field : micronodeFields) {
			UUID uuid = null;

			if (field instanceof HibMicronodeFieldImpl) {
				uuid = HibMicronodeFieldImpl.class.cast(field).valueOrNull();
			}
			if (uuid != null) {
				micronodeUuids.computeIfAbsent(uuid, key -> new ArrayList<>()).add(field);
			}
		}

		Map<ContentKey, Set<UUID>> keyMap = SplittingUtils.splitAndMergeInMapOfSets(micronodeUuids.keySet(),
				HibernateUtil.inQueriesLimitForSplitting(0),
				slice -> {
					List<Tuple> resultList = em.createNamedQuery("micronodefieldref.findContentKeysByUuids", Tuple.class)
						.setParameter("uuids", slice).getResultList();
					Map<ContentKey, Set<UUID>> tempMap = new HashMap<>();
					for (Tuple tuple : resultList) {
						ContentKey contentKey = new ContentKey(tuple.get("key_id", UUID.class), tuple.get("version_id", UUID.class),
								ReferenceType.MICRONODE);
						UUID uuid = tuple.get("edge_id", UUID.class);
						tempMap.computeIfAbsent(contentKey, key -> new HashSet<>()).add(uuid);
					}
					return tempMap;
				});

		List<HibMicronodeContainerImpl> micronodes = contentStorage.findManyMicronodes(keyMap.keySet());

		Map<HibMicronodeField, HibMicronode> resultMap = new HashMap<>();
		micronodes.stream().forEach(micronode -> {
			ContentKey key = ContentKey.fromContent(micronode);

			for (UUID uuid : keyMap.getOrDefault(key, Collections.emptySet())) {
				for (HibMicronodeField field : micronodeUuids.getOrDefault(uuid, Collections.emptyList())) {
					resultMap.put(field, micronode);
				}
			}
		});

		return resultMap;
	}

	public TotalsCache getTotalsCache() {
		return totalsCache;
	}
}
