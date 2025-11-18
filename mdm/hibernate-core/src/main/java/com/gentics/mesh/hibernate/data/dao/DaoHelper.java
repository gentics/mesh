package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;
import static com.gentics.mesh.hibernate.util.HibernateUtil.makeAlias;
import static com.gentics.mesh.hibernate.util.HibernateUtil.makeParamName;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.jpa.SpecHints;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;

import com.gentics.graphqlfilter.filter.operation.FieldOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.Join;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.cache.TotalsCache;
import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.HibernateTxImpl;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.graphql.filter.ListFilter;
import com.gentics.mesh.graphql.filter.NodeReferenceFilter;
import com.gentics.mesh.graphql.filter.operation.EntityReferenceOperationOperand;
import com.gentics.mesh.graphql.filter.operation.ListItemOperationOperand;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.HibQueryFieldMapper;
import com.gentics.mesh.hibernate.data.dao.helpers.RootJoin;
import com.gentics.mesh.hibernate.data.domain.AbstractBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.AbstractFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibBooleanListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibDatabaseElement;
import com.gentics.mesh.hibernate.data.domain.HibDateListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibGroupImpl;
import com.gentics.mesh.hibernate.data.domain.HibHtmlListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibImageVariantImpl;
import com.gentics.mesh.hibernate.data.domain.HibJobImpl;
import com.gentics.mesh.hibernate.data.domain.HibLanguageImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNumberListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibPermissionImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.domain.HibRoleImpl;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibStringListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagFamilyImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.hibernate.data.domain.HibUserImpl;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateFilter;
import com.gentics.mesh.hibernate.util.JpaUtil;
import com.gentics.mesh.hibernate.util.TypeInfoUtil;
import com.gentics.mesh.hibernate.util.UuidGenerator;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.RolePermissionParameters;
import com.gentics.mesh.parameter.SortingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.query.NativeFilterJoin;
import com.gentics.mesh.query.NativeJoin;
import com.gentics.mesh.query.ReferencedNodesFilterJoin;
import com.gentics.mesh.unhibernate.Select;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.UUIDUtil;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;

import dagger.Lazy;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Common methods for DAOs.
 * Instances are provided by {@link com.gentics.mesh.dagger.DaoHelperModule}
 *
 * @param <T> The interface class (e.g. HibUser)
 * @param <D> The entity class (e.g. HibUserImpl)
 */
@AutoFactory
public class DaoHelper<T extends HibBaseElement, D extends T> {

	private static final String INNER_JOIN_MESH_CONTENT = "inner join " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX;

	private static final Logger log = getLogger(DaoHelper.class);

	private final CurrentTransaction currentTransaction;
	private final EventFactory eventFactory;
	private final Lazy<HibDaoCollectionImpl> daoCollection;
	private final TotalsCache totalsCache;
	private final DatabaseConnector databaseConnector;
	
	private final Class<D> domainClass;
	private final ElementType elementType;

	public DaoHelper(
		@Provided CurrentTransaction currentTransaction,
		@Provided EventFactory eventFactory,
		@Provided Lazy<HibDaoCollectionImpl> daoCollection,
		@Provided TotalsCache totalsCache,
		@Provided DatabaseConnector databaseConnector,
		Class<D> domainClass) {
		this.currentTransaction = currentTransaction;
		this.eventFactory = eventFactory;
		this.daoCollection = daoCollection;
		this.totalsCache = totalsCache;
		this.databaseConnector = databaseConnector;
		this.domainClass = domainClass;
		this.elementType = getElementType(domainClass);
	}

	private ElementType getElementType(Class<D> domainClass) {
		return TypeInfoUtil.getType(domainClass);
	}

	public TraversalResult<T> findAll() {
		CriteriaQuery<D> query = cb().createQuery(domainClass);
		query.from(domainClass);

		return new TraversalResult<>(setEntityGraph(em().createQuery(query), getEntityGraph("load", false)).getResultStream());
	}

	public TraversalResult<T> findAll(Bucket bucket) {
		CriteriaQuery<D> query = cb().createQuery(domainClass);
		Root<D> root = query.from(domainClass);

		query.where(cb().greaterThanOrEqualTo(root.get("bucketTracking").get("bucketId"), bucket.start()), cb().lessThanOrEqualTo(root.get("bucketTracking").get("bucketId"), bucket.end()));
		return new TraversalResult<>(setEntityGraph(em().createQuery(query), getEntityGraph("load", false)).getResultStream());
	}

	public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return findAll(ac, pagingInfo, (java.util.function.Predicate<T>) null, true);
	}

	public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, java.util.function.Predicate<T> extraFilter, boolean readPerm) {
		CriteriaQuery<D> query = cb().createQuery(domainClass).distinct(true);
		Root<D> dRoot = query.from(domainClass);
		query.select(dRoot);

		if (readPerm) {
			addPermissionRestriction(query, dRoot, ac.getUser(), InternalPermission.READ_PERM);
		}
		return getResultPage(ac, query, pagingInfo, getEntityGraph("rest", false), extraFilter);
	}

	// sql pagination can be performed only if we have per page info and no extra filter
	// if we had an extra filter and performed sql pagination, then we would lose some values by paginating
	// before filtering
	@Deprecated
	private <R> boolean paginateWithSql(PagingParameters pagingInfo, java.util.function.Predicate<R> extraFilter) {
		return pagingInfo != null && pagingInfo.getPerPage() != null && extraFilter == null;
	}

	@Deprecated
	private <R> Page<? extends R> getResultPageWithSqlPagination(InternalActionContext ac, CriteriaQuery<? extends R> query, EntityGraph<?> entityGraph, PagingParameters pagingInfo) {
		Long perPage = pagingInfo.getPerPage();
		int perPageInt = perPage > Integer.MAX_VALUE ? Integer.MAX_VALUE : perPage.intValue();
		long totalCount = em().createQuery(JpaUtil.countCriteria(em(), query)).getSingleResult();

		List<? extends R> list;
		if (perPageInt > 0 && totalCount > 0) {
			list = setEntityGraph(em().createQuery(query), entityGraph)
					.setFirstResult(pagingInfo.getActualPage() * perPageInt)
					.setMaxResults(perPageInt)
					.getResultList();
		} else {
			list = Collections.emptyList();
		}

		return new PageImpl<>(list, pagingInfo, totalCount);
	}

	public <E extends HibDatabaseElement> Stream<E> getLimitedStream(TypedQuery<E> query, PagingParameters pagingInfo) {
		Long perPage = pagingInfo.getPerPage();
		if (perPage == null) {
			return query.getResultStream();
		} else if (perPage == 0) {
			return Stream.empty();
		} else {
			return query.setFirstResult(pagingInfo.getActualPage() * perPage.intValue())
				.setMaxResults(perPage.intValue())
				// Scrolling is not supported with query containing collection fetches
				.getResultList()
				.stream();
		}
	}

	@SuppressWarnings("unchecked")
	EntityGraph<D> getEntityGraph(String suffix, boolean inRoot) {
		return (EntityGraph<D>) em().getEntityGraphs(domainClass).stream()
			.filter(graph -> graph.getName().endsWith(suffix))
			.filter(graph -> databaseConnector.allows(graph, inRoot))
			.findAny()
			.orElse(null);
	}

	/**
	 * Extract schema version from an existing join.
	 * 
	 * @param join join to inspect
	 * @param bypass should the extraction pass by?
	 * @return
	 */
	public Optional<HibSchemaVersion> extractVersion(NativeJoin join, boolean bypass) {
		String contentAlias = makeAlias("CONTENT");
		return Optional.of(!bypass && join.getJoins().containsKey(contentAlias)).filter(hasContent -> hasContent).flatMap(yes -> {
			String fragmentString = join.getJoins().get(contentAlias).getValue().toSqlJoinString();
			return Optional.of(fragmentString.contains(INNER_JOIN_MESH_CONTENT) && fragmentString.contains(" content_ ")).filter(hasVersion -> hasVersion)
					.map(hasVersion -> fragmentString.substring(fragmentString.indexOf(INNER_JOIN_MESH_CONTENT) + INNER_JOIN_MESH_CONTENT.length(), fragmentString.indexOf(" content_ ")))
					.map(uuid -> HibernateTx.get().load(UUIDUtil.toJavaUuid(uuid), HibSchemaVersionImpl.class));
		});
	}

	/**
	 * Adds a restriction to the query so that the result only contains nodes to which the user has access to. 
	 * By default a READ_PUBLISHED permission is checked along with the PUBLISHED node content status.
	 *
	 * @param query The query to be modified
	 * @param root The selected node root
	 * @param user The user to check permissions of
	 * @param permission The permission to be checked
	 * @param <E>
	 * @return
	 */
	public <E> AbstractQuery<E> addPermissionRestriction(AbstractQuery<E> query, Root<HibNodeImpl> root, HibUser user, InternalPermission permission) {
		return addPermissionRestriction(query, root, root.join("content"), user, permission);
	}

	public <E> AbstractQuery<E> addPermissionRestriction(AbstractQuery<E> query, Path<HibNodeImpl> root, Path<HibNodeFieldContainerEdgeImpl> jContent, HibUser user, InternalPermission permission) {
		if (!user.isAdmin()) {
			Root<HibPermissionImpl> rPerm = query.from(HibPermissionImpl.class);
			if (permission == null || InternalPermission.READ_PUBLISHED_PERM == permission) {
				addRestrictions(query, cb().or(
						cb().isTrue(rPerm.get(HibPermissionImpl.getColumnName(InternalPermission.READ_PERM))), 
						cb().and(
								cb().isTrue(rPerm.get(HibPermissionImpl.getColumnName(InternalPermission.READ_PUBLISHED_PERM))), 
								cb().equal(jContent.get("type"), ContainerType.PUBLISHED)
							)
					));			
			} else {
				addRestrictions(query, cb().isTrue(rPerm.get(HibPermissionImpl.getColumnName(permission))));
			}
		}
		return query;
	}

	/**
	 * Adds a restriction to the query so that the result only contains elements to which the user has access to.
	 *
	 * @param query The query to be modified
	 * @param root The selected root
	 * @param user The user to check permissions of
	 * @param permission The permission to be checked
	 * @param <E>
	 * @return
	 */
	public <E> CriteriaQuery<E> addPermissionRestriction(CriteriaQuery<E> query, Path<?> root, HibUser user, InternalPermission permission) {
		return addPermissionRestriction(query, root, user, Collections.singletonList(permission));
	}

	/**
	 * Adds a restriction to the query so that the result only contains elements to which the user has at least one of the
	 * provided permissions
	 * @param query The query to be modified
	 * @param root The selected root
	 * @param user The user to check permission of
	 * @param permissions The permissions to be checked. User must have access to at least on of them, otherwise we consider the user to not have permissions for the element
	 * @param <E>
	 * @return
	 */
	public <E> CriteriaQuery<E> addPermissionRestriction(CriteriaQuery<E> query, Path<?> root, HibUser user, List<InternalPermission> permissions) {
		if (!user.isAdmin()) {
			Subquery<UUID> permissionQuery = query.subquery(UUID.class);
			Root<HibPermissionImpl> permissionRoot = permissionQuery.from(HibPermissionImpl.class);
			jakarta.persistence.criteria.Join<Object, Object> userRoot = permissionRoot.join("role").join("groups").join("users");

			permissionQuery.select(permissionRoot.get("element"));

			Predicate[] permissionPredicates = permissions.stream()
					.map(perm -> cb().isTrue(permissionRoot.get(HibPermissionImpl.getColumnName(perm))))
					.toArray(Predicate[]::new);

			permissionQuery.where(
					cb().equal(userRoot, user),
					cb().or(permissionPredicates)
			);

			addRestrictions(
					query,
					cb().in(root.get("dbUuid")).value(permissionQuery)
			);
		}
		return query;
	}

	/**
	 * Adds restrictions to a query without overriding any existing ones.
	 *
	 * @param query
	 * @param predicates
	 * @param <E>
	 * @return
	 */
	public <E> AbstractQuery<E> addRestrictions(AbstractQuery<E> query, Predicate... predicates) {
		Predicate previousPredicate = query.getRestriction();
		if (previousPredicate == null) {
			query.where(predicates);
		} else {
			query.where(cb().and(previousPredicate, cb().and(predicates)));
		}
		return query;
	}

	/**
	 * Gets a page from a query according to the paging parameters.
	 *
	 * @param query
	 * @param pagingInfo
	 * @return
	 */
	public <E> Page<? extends E> getResultPage(CriteriaQuery<? extends E> query, PagingParameters pagingInfo) {
		return getResultPage(null, query, pagingInfo, null, null);
	}

	/**
	 * Gets a page from a query according to the paging parameters.
	 * 
	 * @param <R>
	 * @param <F>
	 * @param ac
	 * @param query
	 * @param pagingInfo
	 * @param entityGraph
	 * @param fromTypeClass
	 * @param extraFilter
	 * @return
	 */
	public <R, F> Page<? extends R> getResultPage(InternalActionContext ac, CriteriaQuery<? extends R> query, PagingParameters pagingInfo,
												  EntityGraph<?> entityGraph, java.util.function.Predicate<R> extraFilter) {
		if (paginateWithSql(pagingInfo, extraFilter)) {
			return getResultPageWithSqlPagination(ac, query, entityGraph, pagingInfo);
		}
		Stream<? extends R> resultStream = getResultStream(ac, query, entityGraph);

		if (extraFilter != null) {
			return new DynamicStreamPageImpl<R>(resultStream, pagingInfo, extraFilter, false);
		} else {
			return new DynamicStreamPageImpl<>(resultStream, pagingInfo);
		}	
	}

	public Page<? extends T> findAll(InternalActionContext ac, InternalPermission perm, PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeExtraFilter) {
		return findAll(ac, Optional.ofNullable(perm), pagingInfo, maybeExtraFilter, Optional.empty());
	}

	public Page<? extends T> findAll(InternalActionContext ac, Optional<InternalPermission> maybePerm, PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeExtraFilter) {
		return findAll(ac, maybePerm, pagingInfo, maybeExtraFilter, Optional.empty());
	}

	@SuppressWarnings("rawtypes")
	private <R extends HibBaseElement> Pair<Stream<? extends T>, Long> query(InternalActionContext ac, Optional<InternalPermission> maybePerm, PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeExtraFilter, Optional<Pair<RootJoin<D, R>, R>> maybeRoot, boolean countOnly) {
		HibernateTxImpl tx = currentTransaction.getTx();

		HibUser user = ac.getUser();
		HibProject project = tx.getProject(ac);
		String branchUuid = project != null ? tx.getBranch(ac, project).getUuid() : null;

		String myAlias = makeAlias(databaseConnector.maybeGetDatabaseEntityName(domainClass).get());

		// Have a filter to apply?
		Optional<HibernateFilter> maybeFilterJoin = maybeExtraFilter.map(extraFilter -> appendFilter(StringUtils.EMPTY, extraFilter, Collections.emptySet(), Optional.ofNullable(UUIDUtil.toJavaUuid(branchUuid)), Optional.empty(), Optional.empty(), new ArrayDeque<>()));

		// Make permission table joins
		Optional<NativeJoin> maybePermJoins = maybePerm.map(perm -> makePermissionJoins(StringUtils.EMPTY, user, perm, Optional.empty(), Optional.ofNullable(UUIDUtil.toJavaUuid(branchUuid)), maybeFilterJoin.map(j -> j.getRawSqlJoins()).orElse(Collections.emptySet())));

		// Make sort joins
		Pair<NativeJoin, Map<String, SortOrder>> sortJoins = mapSorting(pagingInfo, StringUtils.EMPTY, Optional.ofNullable(UUIDUtil.toJavaUuid(branchUuid)), Optional.empty(), Stream.of(
				maybePermJoins.stream(),
				maybeFilterJoin.map(filterJoin -> filterJoin.getRawSqlJoins().stream()).orElseGet(() -> Stream.empty())
			).flatMap(Function.identity()).collect(Collectors.toSet()));

		// Make root joins, if requested
		Optional<NativeJoin> maybeRootJoin = maybeRoot.map(joinEntity -> joinEntity.getLeft().makeJoin(myAlias, joinEntity.getRight()));

		List<String> domainColumns = getDomainColumns().stream().map(column -> myAlias + "." + column).collect(Collectors.toList());

		Select select = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
		select.setTableName(databaseConnector.maybeGetPhysicalTableName(domainClass).get() + " " + myAlias);
		List<String> columns = new ArrayList<>();
		columns.addAll(domainColumns);
		if (!countOnly && !sortJoins.getValue().isEmpty()) {
			sortJoins.getValue().keySet().stream()
				.filter(col -> columns.stream().noneMatch(acol -> col.equalsIgnoreCase(acol)))
				.forEach(columns::add);
		}
		select.addColumn(" DISTINCT " + columns.stream().collect(Collectors.joining(", ")));

		// Merge joins alltogether
		Set<NativeJoin> allJoins = Stream.of(
				maybePermJoins.stream(),
				maybeRootJoin.map(filterJoin -> Stream.of(filterJoin)).orElse(Stream.empty()),
				maybeFilterJoin.map(filterJoin -> filterJoin.getRawSqlJoins().stream()).orElseGet(() -> Stream.empty()),
				Stream.of(sortJoins.getKey())
			).flatMap(Function.identity())
				.filter(j -> !Objects.isNull(j)).collect(Collectors.toSet());

		// Install joins
		select.setJoin(mergeJoins(allJoins.stream()));

		// Collect wheres
		String wheres = allJoins.stream().flatMap(join -> join.getWhereClauses().stream()).collect(Collectors.joining(" AND "));
		select.addRestriction(wheres + maybeFilterJoin.map(filterJoin -> (StringUtils.isBlank(wheres) ? StringUtils.EMPTY : " AND ") + filterJoin.getSqlFilter()).orElse(StringUtils.EMPTY));

		if (!countOnly && !sortJoins.getValue().isEmpty()) {
			select.setOrderBy(" order by " + sortJoins.getValue().entrySet().stream().map(entry -> {
				return databaseConnector.findSortAlias(entry.getKey()) + " " + entry.getValue().getValue();
			}).collect(Collectors.joining(", ")));
		}

		String sqlQuery = select.toStatementString();

		if (countOnly) {
			sqlQuery = databaseConnector.installCount(sqlQuery);
		} else if (PersistingRootDao.shouldPage(pagingInfo)) {
			sqlQuery = databaseConnector.installPaging(sqlQuery, myAlias, pagingInfo);
		}

		NativeQuery query = countOnly ? em().createNativeQuery(sqlQuery).unwrap(NativeQuery.class) : em().createNativeQuery(sqlQuery, domainClass).unwrap(NativeQuery.class);

		// Fill join params
		allJoins.stream().forEach(join -> {
			join.getParameters().entrySet().stream().forEach(e -> query.setParameter(e.getKey(), e.getValue()));
		});

		// Fill filter params, if applicable
		maybeFilterJoin.ifPresent(filterJoin -> {
			filterJoin.getParameters().entrySet().stream().forEach(e -> {
				query.setParameter(e.getKey(), e.getValue());
			});
		});

		if (!countOnly && PersistingRootDao.shouldPage(pagingInfo)) {
			databaseConnector.installPagingArguments(sqlQuery, query, pagingInfo);
		}
		try {
			if (countOnly) {
				return Pair.of(Stream.empty(), getOrFetchTotal(query));
			} else {
				return Pair.of(query.getResultStream(), 0L);
			}
		} catch (Throwable e) {
			log.error("Failure at query: " + sqlQuery);
			throw e;
		}
	}

	/**
	 * Count results natively.
	 * 
	 * @param <R>
	 * @param ac
	 * @param perm
	 * @param pagingInfo
	 * @param maybeExtraFilter
	 * @param maybeRoot
	 * @return
	 */
	public <R extends HibBaseElement> long countAll(InternalActionContext ac, Optional<InternalPermission> maybePerm, PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeExtraFilter, Optional<Pair<RootJoin<D, R>, R>> maybeRoot) {
		return query(ac, maybePerm, pagingInfo, maybeExtraFilter, maybeRoot, true).getRight();
	}

	/**
	 * Fetch results considering filtering/sorting/paging natively.
	 * 
	 * @param <R>
	 * @param ac
	 * @param maybePerm
	 * @param pagingInfo
	 * @param maybeExtraFilter
	 * @param maybeRoot
	 * @return
	 */
	public <R extends HibBaseElement> Page<? extends T> findAll(InternalActionContext ac, Optional<InternalPermission> maybePerm, PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeExtraFilter, Optional<Pair<RootJoin<D, R>, R>> maybeRoot) {
		Stream<? extends T> resultStream = query(ac, maybePerm, pagingInfo, maybeExtraFilter, maybeRoot, false).getLeft();
		if (PersistingRootDao.shouldPage(pagingInfo)) {
			return new PageImpl<T>(
					resultStream.collect(Collectors.toList()), 
					pagingInfo, 
					countAll(ac, maybePerm, pagingInfo, maybeExtraFilter, maybeRoot));
		} else {
			return new DynamicStreamPageImpl<T>(resultStream, pagingInfo, null, pagingInfo.getPerPage() != null);
		}
	}

	/**
	 * Map default sorting argument to the one acceptable by SQL, plus extract its arguments into a join.
	 * 
	 * @param <S>
	 * @param sorting
	 * @return
	 */
	<S extends SortingParameters> Pair<NativeJoin, Map<String, SortOrder>> mapSorting(S sorting, String ownerAlias, Optional<UUID> maybeBranch, Optional<ContainerType> maybeContainerType, Set<NativeJoin> otherJoins) {
		NativeJoin ansiJoin = new NativeJoin();
		if (!PersistingRootDao.shouldSort(sorting)) {
			return Pair.of(ansiJoin, Collections.emptyMap());
		}
		HibQueryFieldMapper mapper = daoCollection.get().maybeFindFieldMapper(domainClass).get();
		Map<String, SortOrder> items = sorting.getSort();
		items = items.entrySet().stream().map(entry -> {
			String key = mapper.mapGraphQlFilterFieldName(entry.getKey());
			String[] keyParts = key.split("\\.");
			// ALIAS: <owner>
			String alias = ownerAlias;
			Pair<String, String> baseJoin = Pair.of(makeAlias(mapper.getHibernateEntityName()[0]), null);
			for (int i = 0; i < keyParts.length; i++) {
				if ("fields".equals(keyParts[i]) && i < (keyParts.length+2)) {
					// We expect format of 'fields.<schema_name>.<field_name>'
					HibFieldSchemaElement<?, ?, ?, ?, ?> schema = Tx.get().schemaDao().findByName(keyParts[i+1]);
					if (schema != null) {
						// ALIAS: <owner_>content_
						makeContentJoin(alias, ansiJoin, schema.getLatestVersion(), maybeContainerType, maybeBranch, Optional.empty());
						alias += makeAlias("CONTENT");
					} else if ((schema = Tx.get().microschemaDao().findByName(keyParts[i+1])) != null) {
						String entityAlias = alias + "MICROCONTENT";
						String entityLocalAlias = makeAlias(entityAlias);
						ansiJoin.addJoin(databaseConnector.getPhysicalTableName(schema.getLatestVersion()), entityLocalAlias, 
								new String[] {alias + "." + databaseConnector.renderNonContentColumn("valueOrUuid")}, new String[] {databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, 
								JoinType.LEFT, alias);
						// ALIAS: <owner_>content_<field>_microcontent_
						alias = entityLocalAlias;
					}
					if (schema == null) {
						throw new IllegalArgumentException("No (micro)schema found for sorting request: " + key);
					}
					FieldSchema field = schema.getLatestVersion().getSchema().getFieldsAsMap().get(keyParts[i+2]);
					if (field == null) {
						// Not a field sort, but something other (schema/microschema etc). Skip upto the sorting target.
						i += 2;
						continue;
					}
					String columnName = databaseConnector.renderNonContentColumn(field.getName() + "-" + field.getType().toLowerCase());
					switch (FieldTypes.valueOf(field.getType().toUpperCase())) {
					case MICRONODE:
						String localName = alias + field.getName();
						String localAlias = makeAlias(localName);
						ansiJoin.addJoin(databaseConnector.maybeGetPhysicalTableName(
								HibMicronodeFieldEdgeImpl.class).get(), localAlias, 
								new String[] {alias + "." + columnName, alias + "." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, new String[] {databaseConnector.renderNonContentColumn("dbUuid"), databaseConnector.renderNonContentColumn("containerUuid")}, 
								JoinType.LEFT, alias);
						// ALIAS: <owner_>content_<field>_
						alias = localAlias;
						baseJoin = Pair.of(alias, "microschemaVersion_dbUuid");
						if (i < keyParts.length+3 && "micronode".equals(keyParts[i+3])) {
							i += 1;
						}
						break;
					case BINARY:
						localName = alias + field.getName();
						localAlias = makeAlias(localName);
						ansiJoin.addJoin(databaseConnector.maybeGetPhysicalTableName(
								HibBinaryFieldEdgeImpl.class).get(), localAlias, 
								new String[] {alias + "." + columnName, alias + "." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, new String[] {databaseConnector.renderNonContentColumn("dbUuid"), databaseConnector.renderNonContentColumn("containerUuid")}, 
								JoinType.LEFT, alias);
						// ALIAS: <owner_>content_<field>_
						alias = localAlias;
						baseJoin = Pair.of(null, null);
						break;
					case S3BINARY:
						localName = alias + field.getName();
						localAlias = makeAlias(localName);
						ansiJoin.addJoin(databaseConnector.maybeGetPhysicalTableName(
								HibS3BinaryFieldEdgeImpl.class).get(), localAlias, 
								new String[] {alias + "." + columnName, alias + "." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, new String[] {databaseConnector.renderNonContentColumn("dbUuid"), databaseConnector.renderNonContentColumn("containerUuid")}, 
								JoinType.LEFT, alias);
						// ALIAS: <owner_>content_<field>_
						alias = localAlias;
						baseJoin = Pair.of(null, null);
						break;
					case NODE:
						localName = alias + field.getName();
						localAlias = makeAlias(localName);
						makeNodeSchemaVersionJoin(Optional.of(localAlias), localAlias, "NODE", ansiJoin);
						if (i < keyParts.length+3 && "node".equals(keyParts[i+3])) {
							i += 1;
						}
						alias = localAlias += makeAlias("NODE");
						break;
					default:
						key = databaseConnector.installStringContentColumn(alias + "." + columnName, true, true);
						break;
					}
					// We've done with the whole 'fields.<schema>.<field>' chain, skip it.
					i += 2;
				} else {
					String[] localKeyParts = keyParts;
					if (i > 0) {
						localKeyParts = Arrays.copyOfRange(localKeyParts, i, localKeyParts.length); // Collections.singletonList(IntStream.range(i, keyParts.length).mapToObj(start -> keyParts[start]).collect(Collectors.joining("."))).toArray(new String[keyParts.length-i]);
					}
					boolean noAlias = StringUtils.isBlank(alias);
					Pair<String, String> pair = makeSortJoins(alias, localKeyParts, mapper, ansiJoin, maybeContainerType, maybeBranch, baseJoin, otherJoins);
					key = alias + Optional.ofNullable(pair.getLeft()).map(table -> table + ".").orElseGet(() -> noAlias ? StringUtils.EMPTY : ".") + databaseConnector.renderNonContentColumn(pair.getRight());
					break;
				}	
			}
			return Pair.of(key, entry.getValue());
		}).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
		return Pair.of(ansiJoin, items);
	}

	/**
	 * Make joins for the sorting request, based on an arbitrary entity.
	 * 
	 * @param ownerAlias
	 * @param columns
	 * @param mapper
	 * @param ansiJoin
	 * @param maybeCtype
	 * @param maybeBranch
	 * @param baseJoin
	 * @param otherJoins
	 * @return
	 */
	private Pair<String, String> makeSortJoins(String ownerAlias, String[] columns, HibQueryFieldMapper mapper, NativeJoin ansiJoin, Optional<ContainerType> maybeCtype, Optional<UUID> maybeBranch, Pair<String, String> baseJoin, Set<NativeJoin> otherJoins) {
		for (String column: columns) {
			if (StringUtils.isBlank(column)) {
				continue;
			}
			String mapped = mapper.mapGraphQlSortingFieldName(column);
			boolean mappedContainsDot = mapped.contains(".");
			if (mappedContainsDot) {
				while (mappedContainsDot) {
					String[] parts = mapped.split("\\.");
					if ("CONTENT".equals(parts[0])) {
						throw new IllegalStateException("CONTENT is unsupported at this point. Use `fields.<schema_name>.<field_name>` format.");
					} else if ("CONTAINER".equals(parts[0])) {
						if (otherJoins.stream().noneMatch(join -> join.isAlreadyJoined("container"))) {
							makeContentEdgeJoin(ansiJoin, ownerAlias, maybeCtype, maybeBranch, Optional.empty());
						}
						baseJoin = Pair.of(makeAlias(parts[0]), parts[1]);
					} else {
						if (otherJoins.stream().noneMatch(join -> join.isAlreadyJoined(parts[0]))) {
							if (StringUtils.isBlank(baseJoin.getRight())) {
								baseJoin = Pair.of(baseJoin.getLeft(), parts[1]);
							}
							mapper = ElementType.parse(parts[0]).flatMap(etype -> daoCollection.get().maybeFindDao(etype)).map(HibQueryFieldMapper.class::cast).orElse(mapper);
							for (String tableName: mapper.getHibernateEntityName()) {
								ansiJoin.addJoin(
										getPhysicalTableNameIdentifier(tableName), makeAlias(parts[0]), 
										new String[] {baseJoin.getLeft() + "." + databaseConnector.renderNonContentColumn(baseJoin.getRight())}, 
										new String[] {databaseConnector.renderNonContentColumn(mapper.mapGraphQlFilterFieldName("uuid"))}, 
										JoinType.LEFT, baseJoin.getLeft());
							}
						}
						baseJoin = Pair.of(makeAlias(parts[0]), null);
					}
					String mapped1 = mapped;
					mapped = mapper.mapGraphQlSortingFieldName(mapped);
					if (mapped1.equals(mapped)) {
						break;
					} else {
						mappedContainsDot = mapped.contains(".");
					}
				}
			} else {
				baseJoin = Pair.of(baseJoin.getLeft(), mapped);
			}
		}
		return baseJoin;
	}

	/**
	 * Create joins for permissions check
	 * 
	 * @param user caller user
	 * @param perm the perm to check upon
	 * @param maybeBranchUuid branch UUID for the cases of content query
	 * @param otherJoins other joins to check the already existing required joins, before they are added. 
	 * @return
	 */
	NativeJoin makePermissionJoins(String ownerAlias, HibUser user, InternalPermission perm, Optional<ContainerType> maybeContainerType, Optional<UUID> maybeBranchUuid, Iterable<NativeJoin> otherJoins) {
		NativeJoin permJoin = new NativeJoin();
		if (user.isAdmin()) {
			// Early return on admin users.
			return permJoin;
		}
		String domainAlias = makeAlias(databaseConnector.maybeGetDatabaseEntityName(domainClass).get());
		String permAlias = ownerAlias + makeAlias(databaseConnector.maybeGetDatabaseEntityName(HibPermissionImpl.class).get());
		String permUserAlias = ownerAlias + makeAlias("perm_user");
		String permGroupUserAlias = ownerAlias + makeAlias("perm_group_user");
		String permGroupRoleAlias = ownerAlias + makeAlias("perm_group_role");
		{
			String permUserParam = makeParamName(user);
			permJoin.appendWhereClause(String.format(" (%s.%s = :%s) ", permUserAlias, databaseConnector.renderNonContentColumn("dbUuid"), permUserParam));
			permJoin.getParameters().put(permUserParam, user.getId());
		}
		{
			String isPermittedParam = makeParamName(Boolean.TRUE);
			String edgeJoin = StringUtils.EMPTY;
			if ((perm == null || perm == READ_PUBLISHED_PERM) && HibNodeImpl.class.isAssignableFrom(domainClass)) {
				if (StreamUtil.toStream(otherJoins).noneMatch(join -> join.isAlreadyJoined("container"))) {
					makeContentEdgeJoin(permJoin, ownerAlias, maybeContainerType, maybeBranchUuid, Optional.empty());
				}
				String publishedParam = makeParamName(PUBLISHED);
				permJoin.getParameters().put(publishedParam, PUBLISHED.name());
				edgeJoin = String.format(" OR (%s.%s = :%s AND %s.%s = :%s) ", ownerAlias + makeAlias("CONTAINER"), databaseConnector.renderNonContentColumn("type"), publishedParam, permAlias, databaseConnector.renderNonContentColumn(HibPermissionImpl.getColumnName(READ_PERM)), isPermittedParam);
			}
			permJoin.appendWhereClause(String.format(" ( %s.%s = :%s %s ) ", permAlias, databaseConnector.renderNonContentColumn(HibPermissionImpl.getColumnName(perm)), isPermittedParam, edgeJoin));
			permJoin.getParameters().put(isPermittedParam, Boolean.TRUE);
		}
		permJoin.addJoin(databaseConnector.maybeGetPhysicalTableName(HibPermissionImpl.class).get(), permAlias, 
				new String[] { domainAlias + "." + databaseConnector.renderNonContentColumn("dbUuid")}, 
				new String[] { databaseConnector.renderNonContentColumn("element")}, 
				JoinType.INNER,
				domainAlias);		
		permJoin.addJoin(getPhysicalTableNameIdentifier("group_role"), permGroupRoleAlias, 
				new String[] { permAlias + "." + databaseConnector.renderNonContentColumn("role_dbUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("roles_dbUuid")}, 
				JoinType.INNER, permAlias);
		permJoin.addJoin(getPhysicalTableNameIdentifier("group_user"), permGroupUserAlias, 
				new String[] { permGroupRoleAlias + "." + databaseConnector.renderNonContentColumn("groups_dbUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("groups_dbUuid")}, 
				JoinType.INNER, permGroupRoleAlias);
		permJoin.addJoin(databaseConnector.maybeGetPhysicalTableName(HibUserImpl.class).get(), permUserAlias, 
				new String[] {permGroupUserAlias + "." + databaseConnector.renderNonContentColumn("users_dbUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("dbUuid")},
				JoinType.INNER, permGroupUserAlias);

		return permJoin;
	}

	/**
	 * Parse a native filter of an arbitrary depth.
	 * 
	 * @param ownerAlias
	 * @param filter
	 * @param otherJoins
	 * @param maybeBranch
	 * @param maybeTopLevelFilter
	 * @param maybeContainerType
	 * @param callStack
	 * @return
	 */
	HibernateFilter appendFilter(String ownerAlias, FilterOperation<?> filter, Set<Join> otherJoins, Optional<UUID> maybeBranch, Optional<HibernateFilter> maybeTopLevelFilter, 
			Optional<ContainerType> maybeContainerType, Deque<FilterOperation<?>> callStack) {
		Set<Join> myJoins = filter.getJoins(otherJoins);
		ElementType myEtype = TypeInfoUtil.getType(domainClass);
		BinaryOperator<String> sqlMerger = (aa, bb) -> {
			if ((StringUtils.isNotBlank(aa) || filter.isUnary()) && StringUtils.isNotBlank(bb)) {
				return "(" + aa + " " + filter.getOperator() + " " + bb + ")";
			} else if (StringUtils.isNotBlank(aa)) {
				return aa;
			} else if (StringUtils.isNotBlank(bb)) {
				return bb;
			} else {
				return StringUtils.EMPTY;
			}
		};
		BinaryOperator<HibernateFilter> merger = (a, b) -> a.merge(b, sqlMerger);

		NativeJoin myJoin = new NativeJoin();
		Set<NativeJoin> myMeaningfulJoins = Collections.singleton(myJoin);
		HibernateFilter hf = new HibernateFilter(null, myMeaningfulJoins, new HashMap<>());
		myJoins.forEach(join -> makeSqlJoin(ownerAlias, join, myJoin, myEtype, maybeBranch, maybeContainerType, callStack));
		return filter.maybeCombination()
			// If combination, parse each distinctly
			.map(filters -> filters.stream().map(f -> { 
					callStack.push(filter);
					HibernateFilter ret = appendFilter(ownerAlias, f, myJoins, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack);
					callStack.pop();
					return ret;
				}).reduce(hf, merger))
			// If comparison, process it
			.orElseGet(() -> filter.maybeComparison().map(operands -> {
				FilterOperand<?> left = operands.first;
				FilterOperand<?> right = operands.second;

				HibernateFilter leftSql = makeSqlComparisonOperand(left, ownerAlias, true, myJoin, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack, Optional.empty());
				HibernateFilter rightSql = makeSqlComparisonOperand(right, ownerAlias, true, myJoin, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack, leftSql.getMaybeFieldType());
				String filter1;
				if (right.isLiteral() && !StringUtils.equals(filter.getOperator(), "IS") && !StringUtils.equals(filter.getOperator(), "IS NOT") && !StringUtils.equals(filter.getOperator(), "<>")) {
				    // when we compare a value in the DB with a literal (not with IS or IS NOT), then we should add the "AND [column] IS NOT NULL" clause.
				    // this will change the behavior, if the whole clause is negated with "NOT". In that cases, records having a null value in the [column] will be returned (which is the expected behavior)
				    filter1 = String.format(" (%s %s %s AND %s IS NOT NULL) ",
				            leftSql.getSqlFilter(), filter.getOperator(), rightSql.getSqlFilter(), leftSql.getSqlFilter());
				} else if (right.isLiteral() && StringUtils.equals(filter.getOperator(), "<>")) {
				    // when we compare with <> (different), we also check whether the value is null
				    filter1 = String.format(" (%s %s %s OR %s IS NULL) ",
				            leftSql.getSqlFilter(), filter.getOperator(), rightSql.getSqlFilter(), leftSql.getSqlFilter());
				} else {
				    filter1 = String.format(filter.shouldBracket() ? " (%s %s %s) " : " %s %s %s ",
				            leftSql.getSqlFilter(), filter.getOperator(), rightSql.getSqlFilter());
				}
				HibernateFilter nf = new HibernateFilter(
						filter1, 
						Stream.of(leftSql.getRawSqlJoins().stream(), rightSql.getRawSqlJoins().stream(), myMeaningfulJoins.stream())
								.flatMap(Function.identity())
								.collect(Collectors.toSet()),
						Stream.of(leftSql, rightSql)
								.map(HibernateFilter::getParameters)
								.flatMap(m -> m.entrySet().stream())
								.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (a,b) -> a))
					);
				return maybeTopLevelFilter.map(oldNf -> oldNf.merge(nf, sqlMerger)).orElse(nf);
			}).orElseGet(() -> new HibernateFilter(filter.toSql(), Collections.emptySet(), Collections.emptyMap())));
	}

	/**
	 * General case of making a JOIN from two parts.
	 * 
	 * @param ownerAlias
	 * @param join
	 * @param ansiJoin
	 * @param etype
	 * @param maybeBranch
	 * @param maybeContainerType
	 * @param callStack
	 * @return
	 */
	private boolean makeSqlJoin(String ownerAlias, Join join, NativeJoin ansiJoin, ElementType etype, Optional<UUID> maybeBranch, Optional<ContainerType> maybeContainerType, Deque<FilterOperation<?>> callStack) {
		JoinPart left = join.getLeft();
		JoinPart right = join.getRight();
		
		Pair<Class<?>, String> src = joinIntoPair(left);
		Pair<Class<?>, String> dst = joinIntoPair(right);
		Optional<HibQueryFieldMapper> srcMapper = daoCollection.get().maybeFindFieldMapper(src.getKey());
		Optional<HibQueryFieldMapper> dstMapper = daoCollection.get().maybeFindFieldMapper(dst.getKey());

		// If the entity requesting join is the left one, then the entity joining is the right one.
		boolean useDstJoin = etype.name().equals(join.getLeft().getTable());

		// Sometimes the mappers appear to be the same. This happens when we come here from item reference iteration, and here we should not join - this is a responsibility of reference iteration method itself.
		Optional<HibQueryFieldMapper> targetMapper = Optional.of(!Objects.equals(src.getKey(), dst.getKey())).filter(mappersDontMatch -> mappersDontMatch).flatMap(yes -> (useDstJoin ? dstMapper : srcMapper));

		return targetMapper
				.filter(mapper -> !mapper.isContent()) 
				.map(mapper -> Arrays.stream(mapper.getHibernateEntityName()))
				.map(names -> {
					names
					// TODO FIXME: it tries connecting schemas for every non-root entity - this works, but not efficient and imprecise.
					.filter(name -> StringUtils.isBlank(ownerAlias) || !"schema".equalsIgnoreCase(name))
					.forEach(entityName -> {
						String tableName = getPhysicalTableNameIdentifier(entityName);
						// Entity case
						String pkValueAlias = makeAlias(useDstJoin ? left.getField() : right.getTable());
						String fkValueAlias = makeAlias(useDstJoin ? left.getTable() : right.getField());
						String pkValueField = useDstJoin ? dstMapper.map(dao -> dao.mapGraphQlFilterFieldName(dst.getRight())).orElse(dst.getRight()) : srcMapper.map(dao -> dao.mapGraphQlFilterFieldName(left.getField())).orElse(left.getField());
						String fkValueField = useDstJoin ? srcMapper.map(dao -> dao.mapGraphQlFilterFieldName(src.getRight())).orElse(src.getRight()) : dstMapper.map(dao -> dao.mapGraphQlFilterFieldName(right.getField())).orElse(right.getField()) ;
						if (fkValueField.contains(".")) {
							String[] parts = fkValueField.split("\\.");
							fkValueAlias = makeAlias(parts[0]);
							fkValueField = parts[1];
							if ("CONTAINER".equals(parts[0])) {
								makeContentEdgeJoin(ansiJoin, ownerAlias, maybeContainerType, maybeBranch, Optional.empty());
							}
						}
						if (pkValueField.contains(".")) {
							String[] parts = pkValueField.split("\\.");
							pkValueAlias = makeAlias(parts[0]);
							pkValueField = parts[1];
							if ("CONTAINER".equals(parts[0])) {
								makeContentEdgeJoin(ansiJoin, ownerAlias, maybeContainerType, maybeBranch, Optional.empty());
							}
						}
						if (!ownerAlias.endsWith(pkValueAlias)) {
							pkValueAlias = ownerAlias + pkValueAlias;
						} else {
							pkValueAlias = ownerAlias;
						}
						if (!ownerAlias.endsWith(fkValueAlias)) {
							fkValueAlias = ownerAlias + fkValueAlias;
						} else {
							fkValueAlias = ownerAlias;
						}
						ansiJoin.addJoin(tableName, pkValueAlias, 
								new String[] {fkValueAlias + "." + databaseConnector.renderNonContentColumn(fkValueField)}, 
								new String[] {databaseConnector.renderNonContentColumn(pkValueField)}, StringUtils.isNotBlank(ownerAlias) ? JoinType.LEFT : ContentUtils.getJoinType(entityName), 
							fkValueAlias);
					});
					return true;
				}).orElseGet(() -> maybeMakeContentJoin(ownerAlias, ansiJoin, join, maybeContainerType, maybeBranch, callStack));
	}

	public String mergeJoins(Stream<NativeJoin> joins, String... knownAliases) {
		StringBuffer sb = new StringBuffer();
		Map<String, Pair<String, NativeFilterJoin>> orphans = new HashMap<>();
		Set<String> joined = new HashSet<>();
		joined.add(makeAlias(databaseConnector.maybeGetDatabaseEntityName(domainClass).get()));
		Arrays.stream(knownAliases).forEach(joined::add);

		joins.forEach(join -> {
			join.getJoins().entrySet().stream().forEach(entry -> {
				if (!joined.contains(entry.getKey())) {
					Pair<String, NativeFilterJoin> pair = entry.getValue();
					if (joined.contains(pair.getKey()) || StringUtils.isBlank(pair.getKey())) {
						sb.append(pair.getValue().toSqlJoinString());
						sb.append(" ");
						joined.add(entry.getKey());
					} else {
						orphans.put(entry.getKey(), pair);
					}
				}
			});
		});
		for (int i = orphans.size(); i >= 0; i--) {
			Set<String> keys = new HashSet<>(orphans.keySet());
			keys.stream().forEach(dep -> {
				Pair<String, NativeFilterJoin> pair = orphans.get(dep);
				if (joined.contains(pair.getKey()) || StringUtils.isBlank(pair.getKey())) {
					if (!joined.contains(dep)) {
						sb.append(pair.getValue().toSqlJoinString());
						sb.append(" ");
						joined.add(dep);
					}
					orphans.remove(dep);
				} 
			});
			if (orphans.size() < 1) {
				break;
			}
		}
		orphans.entrySet().stream().forEach(e -> {
			log.error("Orphan JOIN part `{}` with missing dependencies: {} / {}", e.getKey(), e.getValue().getKey(), e.getValue().getValue().toSqlJoinString());
			// will crash later during the query call
		});
		return sb.toString();
	}

	NativeJoin makeContentEdgeJoin(NativeJoin ansiJoin, String ownerAlias, Optional<ContainerType> maybeContainerType, Optional<UUID> maybeBranch, Optional<List<String>> maybeLanguages) {
		if (ansiJoin.isAlreadyJoined("container")) {
			return ansiJoin;
		}
		String containerAlias = ownerAlias + makeAlias("CONTAINER");
		JoinType joinType = JoinType.LEFT;

		if (StringUtils.isBlank(ownerAlias)) {
			ownerAlias = makeAlias(ElementType.NODE);
			joinType = JoinType.INNER;
		}
		maybeBranch.ifPresent(branchUuid -> {
			String branchUuidParam = makeParamName(branchUuid);
			ansiJoin.appendWhereClause(String.format(" ( %s.%s = %s ) ", containerAlias, databaseConnector.renderNonContentColumn("branch_dbUuid"), ":" + branchUuidParam));
			ansiJoin.getParameters().put(branchUuidParam, branchUuid);
		});
		maybeLanguages.filter(langs -> !langs.isEmpty()).ifPresent(langs -> {
			String langsParam = makeParamName(maybeLanguages);
			ansiJoin.appendWhereClause(String.format(" ( %s.%s IN :%s ) ", containerAlias, databaseConnector.renderNonContentColumn("languageTag"), langsParam));
			ansiJoin.getParameters().put(langsParam, langs);
		});
		maybeContainerType.ifPresent(containerType -> {
			String containerTypeParam = makeParamName(containerType);
			ansiJoin.appendWhereClause(String.format(" ( %s.%s = %s ) ", containerAlias, databaseConnector.renderNonContentColumn("type"), ":" + containerTypeParam));
			ansiJoin.getParameters().put(containerTypeParam, containerType.name());
		});
		ansiJoin.addJoin(
				databaseConnector.maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get(), containerAlias, 
				new String[] {ownerAlias + "." + databaseConnector.renderNonContentColumn("dbUuid")}, new String[] {databaseConnector.renderNonContentColumn("node_dbUuid")},
				joinType,
				ownerAlias);
		return ansiJoin;
	}

	private boolean maybeMakeContentJoin(String ownerAlias, NativeJoin ansiJoin, Join join, Optional<ContainerType> maybeContainerType, Optional<UUID> maybeBranch, Deque<FilterOperation<?>> callStack) {
		return !callStack.isEmpty() 
				&& callStack.peek().getInitiatingFilterName().equals(join.getLeft().getField()) 
				&& "CONTENT".equals(join.getLeft().getTable()) 
				&& joinIntoSchemaRef(join.getLeft()).map(ref -> makeContentJoin(ownerAlias, ansiJoin, ref.getKey().getLatestVersion(), maybeContainerType, maybeBranch, Optional.empty())).isPresent();
	}

	NativeJoin makeContentJoin(String ownerAlias, NativeJoin ansiJoin, HibFieldSchemaVersionElement<?,?,?,?,?> version, Optional<ContainerType> maybeContainerType, Optional<UUID> maybeBranch, Optional<List<String>> maybeLanguage) {
		makeContentEdgeJoin(ansiJoin, ownerAlias, maybeContainerType, maybeBranch, maybeLanguage);
		String contentAlias = ownerAlias + makeAlias("CONTENT");
		String containerAlias = ownerAlias + makeAlias("CONTAINER");
		ansiJoin.addJoin(
				databaseConnector.getPhysicalTableName(version), contentAlias, 
				new String[] {containerAlias + "." + databaseConnector.renderNonContentColumn("contentUuid")}, new String[] {databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, 
				JoinType.LEFT, containerAlias).appendVersion(version);
		return ansiJoin;
	}

	private Optional<String> makeNodeSchemaVersionJoin(Optional<String> maybeOwner, String dependency, String entityType, NativeJoin localJoin) {
		localJoin.addJoin(
				databaseConnector.maybeGetPhysicalTableName(HibNodeImpl.class).get(), maybeOwner.get() + makeAlias(entityType), 
				new String[] {dependency + "." + databaseConnector.renderNonContentColumn("valueOrUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
				JoinType.LEFT, dependency);
		maybeOwner = maybeOwner.map(o -> o + makeAlias(entityType));
		localJoin.addJoin(
				databaseConnector.maybeGetPhysicalTableName(HibSchemaImpl.class).get(), maybeOwner.get() + makeAlias("SCHEMA"), 
				new String[] {maybeOwner.get() + "." + databaseConnector.renderNonContentColumn("schemaContainer_dbUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
				JoinType.LEFT, maybeOwner.get());
		return maybeOwner;
	}

	private void makeMicronodeSchemaVersionJoin(String owner, String dependency, NativeJoin localJoin) {
		String versionAlias = owner + makeAlias(ElementType.MICROSCHEMAVERSION);
		localJoin.addJoin(
				databaseConnector.maybeGetPhysicalTableName(HibMicroschemaVersionImpl.class).get(), versionAlias, 
				new String[] {dependency + "." + databaseConnector.renderNonContentColumn("microschemaVersion_dbUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
				JoinType.LEFT, dependency);
		localJoin.addJoin(
				databaseConnector.maybeGetPhysicalTableName(HibMicroschemaImpl.class).get(), owner + makeAlias(ElementType.MICROSCHEMA), 
				new String[] {versionAlias + "." + databaseConnector.renderNonContentColumn("microschema_dbUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
				JoinType.LEFT, versionAlias);
	}

	private Optional<HibernateFilter> maybeMakeLiteralOperand(FilterOperand<?> operand, String ownerAlias, Optional<String> maybeOperandType) {
		return Optional.ofNullable(operand).filter(FilterOperand::isLiteral).map(op -> {
			HibernateFilter nf;
			Object value = op.getValue();
			if (value == null || "null".equalsIgnoreCase(op.toSql())) {
				nf = new HibernateFilter("NULL", Collections.emptySet(), Collections.emptyMap());
			} else {
				String paramName = makeParamName(value);
				if (value != null) {
					if (UUIDUtil.isUUID(value.toString())) {
						if (maybeOperandType.filter(otype -> "string".equalsIgnoreCase(otype) || "html".equalsIgnoreCase(otype)).isEmpty()) {
							value = UUIDUtil.toJavaUuid(value.toString());
						}
					} else if (value.getClass().isEnum()) {
						value = Enum.class.cast(value).name();
					}
				}
				nf = new HibernateFilter(":" + paramName, Collections.emptySet(), Collections.singletonMap(paramName, value));
			}
			return nf;
		});		
	}

	@SuppressWarnings("rawtypes")
	private String buildListFieldItemOperand(ListItemOperationOperand listItemOperationOperand, Optional<String> maybeOwner, String ownerAlias, Map<String, Object> paramsMap, String localFieldName, 
				Optional<UUID> maybeBranch, Optional<HibernateFilter> maybeTopLevelFilter, Optional<ContainerType> maybeContainerType, Deque<FilterOperation<?>> callStack) {
		FilterOperation<?> listItemOperation = listItemOperationOperand.getValue();
		Set<Join> joins = listItemOperationOperand.getJoins();

		// Item type / Field name pair
		Optional<Pair<String, String>> maybeListData = 
			// Reference list case
			maybeOwner.filter(o -> o.endsWith("REFERENCELIST"))
				.map(listItemName -> Pair.of(listItemName, listItemName))
			// Field list case
			.or(() -> joins.stream()
				.filter(j -> doOwnerAndJoinMatchContentTrait(maybeOwner.get(), j.getLeft().getTable()) && j.getRight().getTable().equals(j.getLeft().getField() + "." + localFieldName))
				.map(j -> joins.stream().filter(jj -> jj.getLeft().equals(j.getRight())).findAny().get())
				.findAny()
				.map(j -> {
					String field = j.getLeft().getTable().split("\\.")[1];
					String itemType = j.getRight().getField();
					return Pair.of(itemType, field);
				}))
			// Image variants list case
			.or(() -> joins.stream()
				.filter(j -> "variants".equals(j.getLeft().getField()) && "IMAGEVARIANTLIST".equals(j.getRight().getTable()))
				.findAny()
				.map(j -> Pair.of(j.getLeft().getTable(), j.getLeft().getField())));
		return maybeListData.map(listData -> {
			String contentAlias = ownerAlias + makeAlias("CONTENT");
			String listAlias = contentAlias + makeAlias(listData.getRight());
			if (listItemOperationOperand.isReferenceItem()) {
				if (maybeOwner.filter(o -> "MICRONODELIST".equals(o)).isPresent()) {
					listAlias += makeAlias("MICROCONTENT");
				} else if (maybeOwner.filter(o -> "NODELIST".equals(o)).isPresent()) {
					// New iteration of appendFilter() call will deal with adding `node_` by itself, no need to set it here
					//listAlias += makeAlias(ElementType.NODE);
				}
			}

			NativeJoin localJoin = new NativeJoin();

			// This select counts the direct matches of list items
			Select selectDirect = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
			StringBuilder sbDirect = new StringBuilder();
			selectDirect.addColumn("count(1)");

			// This select counts the opposite matches of list items
			Select selectOpposite = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
			StringBuilder sbOpposite = new StringBuilder();
			selectOpposite.addColumn("count(1)");

			if (maybeOwner.filter(o -> o.endsWith("REFERENCELIST")).isEmpty()) {
				if (maybeOwner.filter(o -> "IMAGEVARIANTLIST".equals(o)).isPresent()) {
					selectDirect.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(cls)).get() + " " + listAlias);
					selectOpposite.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(cls)).get() + " " + listAlias);
					String variantEdgeAlias = listAlias + makeAlias("binary_field_variant");
					String binaryFieldAlias = listAlias + makeAlias("binaryfieldref");

					// The topmost callstack item is `variants`, which we already know.
					FilterOperation<?> topmost = callStack.pop();
					String fieldKey = callStack.peek().getInitiatingFilterName();
					callStack.push(topmost);
					String fieldKeyParam = makeParamName(fieldKey);

					localJoin.addJoin(
							getPhysicalTableNameIdentifier("binary_field_variant"), variantEdgeAlias, 
							new String[] {listAlias + "." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, 
							new String[] {databaseConnector.renderNonContentColumn("variants_dbUuid")}, 
							JoinType.INNER, listAlias);
					localJoin.addJoin(
							databaseConnector.maybeGetPhysicalTableName(HibBinaryFieldEdgeImpl.class).get(), binaryFieldAlias, 
							new String[] {variantEdgeAlias + "." + databaseConnector.renderNonContentColumn("fields_dbUuid")}, 
							new String[] {databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, 
							JoinType.INNER, variantEdgeAlias);

					sbDirect.append(binaryFieldAlias).append(".").append(databaseConnector.renderNonContentColumn("containerUuid")).append(" = ").append(contentAlias).append(".").append(databaseConnector.renderColumn(CommonContentColumn.DB_UUID));
					sbDirect.append(" AND ").append(binaryFieldAlias).append(".").append(databaseConnector.renderNonContentColumn("fieldKey")).append(" = ").append(":").append(fieldKeyParam);
					sbOpposite.append(binaryFieldAlias).append(".").append(databaseConnector.renderNonContentColumn("containerUuid")).append(" = ").append(contentAlias).append(".").append(databaseConnector.renderColumn(CommonContentColumn.DB_UUID));
					sbOpposite.append(" AND ").append(binaryFieldAlias).append(".").append(databaseConnector.renderNonContentColumn("fieldKey")).append(" = ").append(":").append(fieldKeyParam);
					paramsMap.put(fieldKeyParam, fieldKey);
				} else {
					selectDirect.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(cls)).get() + " " + listAlias);

					sbDirect.append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("containerUuid")).append(" = ").append(contentAlias).append(".").append(databaseConnector.renderColumn(CommonContentColumn.DB_UUID));
					sbDirect.append(" AND ").append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("listUuid")).append(" = ").append(contentAlias).append(".").append(databaseConnector.renderColumnUnsafe(listData.getRight() + "-list." + listData.getLeft(), false));

					selectOpposite.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(cls)).get() + " " + listAlias);

					sbOpposite.append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("containerUuid")).append(" = ").append(contentAlias).append(".").append(databaseConnector.renderColumn(CommonContentColumn.DB_UUID));
					sbOpposite.append(" AND ").append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("listUuid")).append(" = ").append(contentAlias).append(".").append(databaseConnector.renderColumnUnsafe(listData.getRight() + "-list." + listData.getLeft(), false));
				}
			} else {
				byte lookupFeatures = maybeOwner.map(o -> o.split("REFERENCELIST")).filter(o -> o.length > 0).map(o -> o[0]).filter(StringUtils::isNotBlank).map(Byte::valueOf).orElse(NodeReferenceFilter.createLookupChange(true, true, true, true));
				boolean lookupInContent = NodeReferenceFilter.isLookupInContent(lookupFeatures);
				boolean lookupInMicronode = NodeReferenceFilter.isLookupInMicrocontent(lookupFeatures);
				boolean lookupInFields = NodeReferenceFilter.isLookupInFields(lookupFeatures);
				boolean lookupInLists = NodeReferenceFilter.isLookupInLists(lookupFeatures);
				String refEdgesAlias = listAlias;
				String referencedNodeAlias = StringUtils.isNoneBlank(ownerAlias) ? ownerAlias : makeAlias(ElementType.NODE);
				listAlias = contentAlias + makeAlias(maybeOwner.get()) + makeAlias(ElementType.NODE);
				ReferencedNodesFilterJoin rfJoin = new ReferencedNodesFilterJoin(refEdgesAlias, referencedNodeAlias + "." + databaseConnector.renderNonContentColumn("dbUuid"), JoinType.INNER, 
						lookupInFields, lookupInLists, lookupInContent, lookupInMicronode);
				localJoin.addCustomJoin(rfJoin, refEdgesAlias, listAlias);
				localJoin.addJoin(
						databaseConnector.maybeGetPhysicalTableName(HibSchemaImpl.class).get(), listAlias + makeAlias("SCHEMA"), 
						new String[] {listAlias + "." + databaseConnector.renderNonContentColumn("schemaContainer_dbUuid")}, 
						new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
						JoinType.INNER, listAlias);

				selectDirect.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(HibNodeImpl.class)).get() + " " + listAlias);
				sbDirect.append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("dbUuid")).append(" = ").append(refEdgesAlias).append(".").append(ReferencedNodesFilterJoin.ALIAS_NODE_UUID);

				selectOpposite.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(HibNodeImpl.class)).get() + " " + listAlias);
				sbOpposite.append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("dbUuid")).append(" = ").append(refEdgesAlias).append(".").append(ReferencedNodesFilterJoin.ALIAS_NODE_UUID);
			}

			Function<? super FilterOperand, String> listOperationMapper = lop -> {
				Object literalValue = lop.getValue();
				String paramName1;
				if (literalValue != null) {
					paramName1 = makeParamName(literalValue);
					if (UUIDUtil.isUUID(literalValue.toString())) {
						literalValue = UUIDUtil.toJavaUuid(literalValue.toString());
					} else if (Instant.class.isInstance(literalValue)) {
						// Date list items want Long, not Instant...
						literalValue = Instant.class.cast(literalValue).toEpochMilli();
					} else if (literalValue.getClass().isEnum()) {
						literalValue = Enum.class.cast(literalValue).name();
					}
				} else {
					paramName1 = makeParamName(lop);
				}
				paramsMap.put(paramName1, literalValue);
				return paramName1;
			};

			String whereClause;
			if (listItemOperationOperand.isReferenceItem()) {
				if (maybeOwner.filter(o -> "IMAGEVARIANTLIST".equals(o)).isPresent()) {
					callStack.push(listItemOperation);
					HibernateFilter hf = appendFilter(listAlias, listItemOperation, joins, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack);
					callStack.pop();
					selectDirect.setJoin(mergeJoins(Stream.of(localJoin), contentAlias, listAlias));
					selectOpposite.setJoin(mergeJoins(Stream.of(localJoin), contentAlias, listAlias));
					Stream<String> wheresJoin = Stream.of(Stream.of(hf.getSqlFilter()), localJoin.getWhereClauses().stream()).flatMap(Function.identity()).filter(StringUtils::isNotBlank);
					whereClause = Optional.of(wheresJoin.collect(Collectors.joining(" AND "))).filter(StringUtils::isNotBlank).orElse(StringUtils.EMPTY);
					localJoin.getParameters().entrySet().stream().forEach(e -> {
						paramsMap.put(e.getKey(), e.getValue());
					});
					hf.getParameters().entrySet().stream().forEach(e -> {
						paramsMap.put(e.getKey(), e.getValue());
					});
					hf.getRawSqlJoins().stream().flatMap(j -> j.getParameters().entrySet().stream()).forEach(e -> {
						paramsMap.put(e.getKey(), e.getValue());
					});
				} else {
					String itemAlias = listAlias;
					if (maybeOwner.filter(o -> "MICRONODELIST".equals(o)).isPresent()) {
						String versionAlias = listAlias+ makeAlias(ElementType.MICROSCHEMAVERSION);
						localJoin.addJoin(
								databaseConnector.maybeGetPhysicalTableName(HibMicroschemaVersionImpl.class).get(), versionAlias, 
								new String[] {listAlias + "." + databaseConnector.renderNonContentColumn("microschemaVersion_dbUuid")}, 
								new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
								JoinType.LEFT, listAlias);
						localJoin.addJoin(
								databaseConnector.maybeGetPhysicalTableName(HibMicroschemaImpl.class).get(), listAlias + makeAlias(ElementType.MICROSCHEMA), 
								new String[] {versionAlias + "." + databaseConnector.renderNonContentColumn("microschema_dbUuid")}, 
								new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
								JoinType.LEFT, versionAlias);
					} else if (maybeOwner.filter(o -> "NODELIST".equals(o)).isPresent()) {
						itemAlias = listAlias+ makeAlias(ElementType.NODE);
						localJoin.addJoin(
								databaseConnector.maybeGetPhysicalTableName(HibNodeImpl.class).get(), itemAlias, 
								new String[] {listAlias + "." + databaseConnector.renderNonContentColumn("valueOrUuid")}, 
								new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
								JoinType.LEFT, listAlias);
						localJoin.addJoin(
								databaseConnector.maybeGetPhysicalTableName(HibSchemaImpl.class).get(), itemAlias + makeAlias("SCHEMA"), 
								new String[] {itemAlias + "." + databaseConnector.renderNonContentColumn("schemaContainer_dbUuid")}, 
								new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
								JoinType.LEFT, itemAlias);
					}
					callStack.push(listItemOperation);
					HibernateFilter hf = appendFilter(itemAlias, listItemOperation, joins, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack);
					callStack.pop();
					Set<NativeJoin> joinsSet = Stream.of(Stream.of(localJoin), hf.getRawSqlJoins().stream()).flatMap(Function.identity()).collect(Collectors.toSet());
					selectDirect.setJoin(mergeJoins(joinsSet.stream(), contentAlias, listAlias));
					selectOpposite.setJoin(mergeJoins(joinsSet.stream(), contentAlias, listAlias));
					Stream<String> wheresJoin = Stream.of(Stream.of(hf.getSqlFilter()), localJoin.getWhereClauses().stream()).flatMap(Function.identity()).filter(StringUtils::isNotBlank);
					whereClause = Optional.of(wheresJoin.collect(Collectors.joining(" AND "))).filter(StringUtils::isNotBlank).orElse(StringUtils.EMPTY);
					localJoin.getParameters().entrySet().stream().forEach(e -> {
						paramsMap.put(e.getKey(), e.getValue());
					});
					hf.getParameters().entrySet().stream().forEach(e -> {
						paramsMap.put(e.getKey(), e.getValue());
					});
					hf.getRawSqlJoins().stream().flatMap(j -> j.getParameters().entrySet().stream()).forEach(e -> {
						paramsMap.put(e.getKey(), e.getValue());
					});
				}
			} else {
				String paramName = listItemOperation.maybeCombination().map(lco -> lco.stream().map(lop -> lop.getOperands().stream()
							// Nullable object case
							.map(FilterOperand.class::cast)
							.filter(FilterOperand::isLiteral)
							.findAny().get()).findAny()
							.map(listOperationMapper).orElseThrow(() -> new IllegalStateException("No param name found"))
						).orElseGet(() -> {
							// Boolean case
							graphql.util.Pair<FilterOperand<?>, FilterOperand<?>> lco = listItemOperation.maybeComparison().get();
							return Stream.of(lco.first, lco.second)
									.map(FilterOperand.class::cast)
									.filter(FilterOperand::isLiteral)
									.findAny()
									.map(listOperationMapper).orElseThrow(() -> new IllegalStateException("No param name found"));
						});
				String operator = listItemOperation.maybeCombination().map(lco -> lco.stream()
							// Nullable object case
							.filter(FilterOperation.class::isInstance)
							.map(FilterOperation.class::cast)
							.findAny()
							.map(FilterOperation::getOperator).orElseGet(() -> listItemOperation.getOperator())
						).orElseGet(() -> {
							// Boolean case
							graphql.util.Pair<FilterOperand<?>, FilterOperand<?>> lco = listItemOperation.maybeComparison().get();
							return Stream.of(lco.first, lco.second)
								.filter(FilterOperation.class::isInstance)
								.map(FilterOperation.class::cast)
								.findAny()
								.map(FilterOperation::getOperator).orElseGet(() -> listItemOperation.getOperator());
						});

				whereClause = currentTransaction.getTx().data().getDatabaseConnector().installListContentColumn(localFieldName, listAlias, operator, paramName);
			}
			sbDirect.append(" AND ").append(listItemOperationOperand.isNegate() ? "NOT(" : StringUtils.EMPTY)
				.append(whereClause).append(listItemOperationOperand.isNegate() ? ")" : StringUtils.EMPTY);

			selectDirect.addRestriction(sbDirect.toString());
			String selectionSubquery = " (" + selectDirect.toStatementString() + ") ";

			if (listItemOperationOperand.isCheckIfExists()) {
				sbOpposite.append(" AND ").append(whereClause);
				selectOpposite.addRestriction(sbOpposite.toString());
				selectionSubquery = selectionSubquery + " AND 0 NOT IN (" + selectOpposite.toStatementString() + ") ";
			}
			
			return selectionSubquery;
		}).orElse(localFieldName);
	}

	private String buildListFieldItemCountOperand(FilterOperand<?> op, Optional<String> maybeOwner, String ownerAlias, Deque<FilterOperation<?>> callStack, Map<String, Object> paramsMap) {
		if (maybeOwner.filter(o -> o.endsWith("REFERENCELIST")).isPresent()) {
			byte lookupFeatures = maybeOwner.map(o -> o.split("REFERENCELIST")).filter(o -> o.length > 0).map(o -> o[0]).filter(StringUtils::isNotBlank).map(Byte::valueOf).orElse(NodeReferenceFilter.createLookupChange(true, true, true, true));
			boolean lookupInContent = NodeReferenceFilter.isLookupInContent(lookupFeatures);
			boolean lookupInMicronode = NodeReferenceFilter.isLookupInMicrocontent(lookupFeatures);
			boolean lookupInFields = NodeReferenceFilter.isLookupInFields(lookupFeatures);
			boolean lookupInLists = NodeReferenceFilter.isLookupInLists(lookupFeatures);
			String referencedNodeAlias = StringUtils.isNoneBlank(ownerAlias) ? ownerAlias : makeAlias(ElementType.NODE);
			String refNodeAlias = makeAlias("REFERENCELIST") + makeAlias(ElementType.NODE);
			String refEdgesAlias = makeAlias("REFERENCELIST") + makeAlias("REFS");
			Select select = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
			select.addColumn("count(1)");
			select.setTableName(databaseConnector.maybeGetPhysicalTableName(HibNodeImpl.class).get() + " " + refNodeAlias);
			ReferencedNodesFilterJoin rfJoin = new ReferencedNodesFilterJoin(refEdgesAlias, referencedNodeAlias + "." + databaseConnector.renderNonContentColumn("dbUuid"), JoinType.INNER,
					lookupInFields, lookupInLists, lookupInContent, lookupInMicronode);
			select.setJoin(rfJoin.toSqlJoinString());
			select.addRestriction(refNodeAlias + "." + databaseConnector.renderNonContentColumn("dbUuid") + " = " + refEdgesAlias + "." + ReferencedNodesFilterJoin.ALIAS_NODE_UUID);
			return " (" + select.toStatementString() + ") ";
		} else {
			String itemType = maybeOwner.get().substring(0, maybeOwner.get().indexOf("LIST"));
			return op.getJoins().stream()
					.filter(j -> "list".equals(j.getLeft().getField()) && "list".equals(j.getRight().getTable()) && itemType.equalsIgnoreCase(j.getRight().getField()) && j.getLeft().getTable().indexOf('.') > 0)
					.map(j -> j.getLeft().getTable().split("\\.")[1])
					.findAny()
					.map(localFieldName -> {
						String listAlias = ownerAlias + makeAlias("CONTENT" + "_" + localFieldName);
						Select select = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
						select.addColumn("count(1)");
						select.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(cls)).get() + " " + listAlias);
						
						StringBuilder sb = new StringBuilder();
						sb.append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("containerUuid")).append(" = ").append(ownerAlias).append(makeAlias("CONTENT"))
								.append(".").append(databaseConnector.renderColumn(CommonContentColumn.DB_UUID));
						sb.append(" AND ").append(listAlias).append(".").append(databaseConnector.renderNonContentColumn("listUuid")).append(" = ").append(ownerAlias).append(makeAlias("CONTENT"))
								.append(".").append(databaseConnector.renderColumnUnsafe(localFieldName + "-list." + itemType, false));
						select.addRestriction(sb.toString());
						return " (" + select.toStatementString() + ") ";
					}).or(() -> Optional.ofNullable(itemType)
						.filter(it -> "IMAGEVARIANT".equals(it))
						.map(it -> {
							NativeJoin localJoin = new NativeJoin();
							String listData = op.getJoins().stream()
									.filter(j -> "variants".equals(j.getLeft().getField()) && "IMAGEVARIANTLIST".equals(j.getRight().getTable()))
									.findAny()
									.map(j -> j.getLeft().getField()).orElseThrow();
							String contentAlias = ownerAlias + makeAlias("CONTENT");
							String listAlias = contentAlias + makeAlias(listData);
							String variantEdgeAlias = listAlias + makeAlias("binary_field_variant");
							String binaryFieldAlias = listAlias + makeAlias("binaryfieldref");

							Select select = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
							select.addColumn("count(1)");
							select.setTableName(tableNameIntoClass(maybeOwner.get()).flatMap(cls -> databaseConnector.maybeGetPhysicalTableName(cls)).get() + " " + listAlias);
							
							// The topmost callstack item is `count`, which we already know.
							FilterOperation<?> topmost = callStack.pop();
							// The second topmost callstack item is `variants`, which we we don't need.
							FilterOperation<?> secondTopmost = callStack.pop();
							String fieldKey = callStack.peek().getInitiatingFilterName();
							callStack.push(secondTopmost);
							callStack.push(topmost);

							String fieldKeyParam = makeParamName(fieldKey);

							localJoin.addJoin(
									getPhysicalTableNameIdentifier("binary_field_variant"), variantEdgeAlias, 
									new String[] {listAlias + "." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, 
									new String[] {databaseConnector.renderNonContentColumn("variants_dbUuid")}, 
									JoinType.INNER, listAlias);
							localJoin.addJoin(
									databaseConnector.maybeGetPhysicalTableName(HibBinaryFieldEdgeImpl.class).get(), binaryFieldAlias, 
									new String[] {variantEdgeAlias + "." + databaseConnector.renderNonContentColumn("fields_dbUuid")}, 
									new String[] {databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, 
									JoinType.INNER, variantEdgeAlias);

							StringBuilder sb = new StringBuilder();
							sb.append(binaryFieldAlias).append(".").append(databaseConnector.renderNonContentColumn("containerUuid")).append(" = ").append(contentAlias).append(".").append(databaseConnector.renderColumn(CommonContentColumn.DB_UUID));
							sb.append(" AND ").append(binaryFieldAlias).append(".").append(databaseConnector.renderNonContentColumn("fieldKey")).append(" = ").append(":").append(fieldKeyParam);
							select.setJoin(mergeJoins(Stream.of(localJoin), listAlias, contentAlias));
							paramsMap.put(fieldKeyParam, fieldKey);
							select.addRestriction(sb.toString());
							return " (" + select.toStatementString() + ") ";
						})).orElseThrow(() -> new IllegalStateException("Unable to find a field name for filter: " + op));
		}
	}

	private Optional<String> maybeBuildListFieldName(FilterOperand<?> op) {
		String itemType = op.maybeGetOwner().get().substring(0, op.maybeGetOwner().get().indexOf("LIST"));
		return op.getJoins().stream()
				.filter(j -> "list".equals(j.getLeft().getField()) && "list".equals(j.getRight().getTable()) && itemType.equalsIgnoreCase(j.getRight().getField()) && j.getLeft().getTable().indexOf('.') > 0)
				.map(j -> j.getLeft().getTable().split("\\.")[1] + "-list." + itemType.toLowerCase())
				.findAny();
	}

	private void makeBinaryJoin(Class<? extends AbstractBinaryImpl> cls, String dependency, String owner, NativeJoin join) {
		join.addJoin(
				databaseConnector.maybeGetPhysicalTableName(cls).get(), dependency + makeAlias(owner), 
				new String[] {dependency + "." + databaseConnector.renderNonContentColumn("valueOrUuid")}, 
				new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
				JoinType.LEFT, dependency);
	}

	private HibernateFilter makeParameterOperand(
			FilterOperand<?> op, String ownerAlias, boolean isNative, NativeJoin join, 
			Optional<UUID> maybeBranch, Optional<HibernateFilter> maybeTopLevelFilter, Optional<ContainerType> maybeContainerType, Deque<FilterOperation<?>> callStack) {

		Map<String, Object> paramsMap = new HashMap<>(1);
		Set<NativeJoin> localJoins = new HashSet<>(1); 
		Optional<String> maybeOwner = op.maybeGetOwner();
		String value = (op.getValue() instanceof FilterOperation<?>) ? StringUtils.EMPTY : String.valueOf(op.getValue());
		Set<Join> joins = op.getJoins();

		// Map the GraphQL field onto the actual entity field. It may be a reference, see below.
		String actualFieldName = maybeOwner.map(o -> joinIntoPair(new JoinPart(o, value)))
				.flatMap(pair -> daoCollection.get().maybeFindFieldMapper(pair.getKey()))
				.map(dao -> dao.mapGraphQlFilterFieldName(value))
				.orElse(value);

		// If the entity field is a reference (e.g. <entity>.<field_name>), dig it in
		String[] parts = actualFieldName.split("\\.");
		actualFieldName = (
					parts.length > 1 
					? Optional.ofNullable(joinIntoPair(new JoinPart(parts[0], parts[1]))) 
					: maybeOwner.map(o -> joinIntoPair(new JoinPart(o, value)))
				).flatMap(j -> daoCollection.get()
						.maybeFindFieldMapper(j.getKey())
						.map(dao -> dao.mapGraphQlFilterFieldName(j.getValue())))
				.orElse(actualFieldName);

		// If we got another reference, dig it through.
		if (actualFieldName.startsWith("CONTENT.")) {
			actualFieldName = parts[1];
			maybeOwner = Optional.of("CONTENT");
		} else if (actualFieldName.startsWith("CONTAINER.")) {
			actualFieldName = parts[1];
			maybeOwner = Optional.of("CONTAINER");
		} else if (actualFieldName.startsWith("USER.")) {
			actualFieldName = parts[1];
			maybeOwner = Optional.of("USER");
		} else if (maybeOwner.filter(o -> "USER".equals(o)).isPresent()) {
			maybeOwner = joins.stream().map(j -> {
				if ("USER".equals(j.getLeft().getTable())) {
					return j.getRight().getField();
				} else if ("USER".equals(j.getRight().getTable())) {
					return j.getLeft().getField();
				} else {
					return null;
				}
			}).filter(s -> StringUtils.isNotBlank(s)).findAny().or(() -> op.maybeGetOwner());
		} else if (maybeOwner.filter(o -> o.endsWith("LIST")).isPresent()) {
			if (op instanceof ListItemOperationOperand) {
				actualFieldName = buildListFieldItemOperand((ListItemOperationOperand) op, maybeOwner, ownerAlias, paramsMap, callStack.peek().getInitiatingFilterName(), maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack);
				maybeOwner = Optional.empty();
				isNative = true;
			} else if (op instanceof FieldOperand && ListFilter.OP_COUNT.equals(((FieldOperand<?>)op).getValue())) {
				actualFieldName = buildListFieldItemCountOperand(op, maybeOwner, ownerAlias, callStack, paramsMap);
				maybeOwner = Optional.empty();
				isNative = true;
			} else {
				maybeOwner = Optional.of("CONTENT");
				actualFieldName = maybeBuildListFieldName(op).orElseThrow(() -> new IllegalStateException("This list filter is not supported natively: " + op + ". Please turn off native filtering for its usage."));
			}
		} else if (maybeOwner.filter(o -> o.endsWith("FIELD")).isPresent()) {
			if (op instanceof EntityReferenceOperationOperand) {
				return buildReferenceFieldOperand(op, maybeOwner, ownerAlias, paramsMap, joins, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack);
			} else if (op instanceof FieldOperand && "isNull".equals(((FieldOperand<?>)op).getValue())) {
				actualFieldName = "valueOrUuid";
				isNative = true;
			}
		} else if (maybeOwner.filter(o -> "MICROCONTENT".equals(o)).isPresent()) {
			String microschemaVersionUuid = StreamUtil.toStream(callStack.iterator())
					.filter(frame -> frame.maybeGetFilterId().isPresent())
					.map(frame -> Tx.get().microschemaDao().findByName(frame.getInitiatingFilterName()))
					.filter(microschema -> microschema != null)
					.map(microschema -> microschema.getLatestVersion().getUuid())
					.findAny().orElseThrow(() -> new IllegalStateException("Could not find a UUID for " + callStack.peek().getInitiatingFilterName() + " in the query call stack"));
			join.addJoin(
					databaseConnector.getPhysicalTableName(UUIDUtil.toJavaUuid(microschemaVersionUuid)), ownerAlias + makeAlias(maybeOwner.get()), 
					new String[] {ownerAlias + "." + databaseConnector.renderNonContentColumn("valueOrUuid")}, 
					new String[] {databaseConnector.renderColumn(CommonContentColumn.DB_UUID)}, 
					JoinType.LEFT, ownerAlias);
		} else if (maybeOwner.filter(o -> "BINARY".equals(o)).isPresent()) {
			makeBinaryJoin(HibBinaryImpl.class, ownerAlias, maybeOwner.get(), join);
		} else if (maybeOwner.filter(o -> "S3BINARY".equals(o)).isPresent()) {
			makeBinaryJoin(HibS3BinaryImpl.class, ownerAlias, maybeOwner.get(), join);
		} else if (maybeOwner.filter(o -> "NODE".equals(o)).isPresent() 
				&& (ReferencedNodesFilterJoin.ALIAS_CONTENTFIELDKEY.equals(actualFieldName) || ReferencedNodesFilterJoin.ALIAS_MICROCONTENTFIELDKEY.equals(actualFieldName))) {
			ownerAlias = ownerAlias.substring(0, ownerAlias.lastIndexOf(makeAlias(ElementType.NODE)));
		} else if (maybeOwner.filter(o -> "SCHEMA".equals(o)).isPresent() && "isContainer".equals(actualFieldName)) {
			// TODO this has to be a part of mesh-graphql API.
			String oldOwner = maybeOwner.get();
			String owner = "LATESTVERSION";
			maybeOwner = Optional.empty();
			String versionAlias = ownerAlias + makeAlias(owner);
			String schemaAlias = ownerAlias + makeAlias(oldOwner);
			join.addJoin(
					databaseConnector.maybeGetPhysicalTableName(HibSchemaVersionImpl.class).get(), versionAlias, 
					new String[] {schemaAlias + "." + databaseConnector.renderNonContentColumn("latestVersion_dbUuid")}, 
					new String[] {databaseConnector.renderNonContentColumn("dbUuid")}, 
					JoinType.INNER, schemaAlias);
			actualFieldName = String.format("(%s.%s LIKE '%s')%s", 
					versionAlias, databaseConnector.renderNonContentColumn("schemaJson"), "%\"container\" : true%", databaseConnector.getDummyComparison(paramsMap, true));
		}
		// At last, in the case of content, get the right owner table and field name, if applicable
		String schemaKeySuffix = "." + actualFieldName;
		Optional<String> maybeFieldType = maybeOwner.flatMap(o -> joins.stream()
				.filter(j -> j.getLeft().getTable().equals(o) && j.getRight().getTable().equals(j.getLeft().getField() + schemaKeySuffix))
				.map(j -> j.getRight().getField()).findAny());
		String fieldSuffix = maybeFieldType.map(v -> "-" + v).orElse(StringUtils.EMPTY);
		// Wrap the field name according the dialect, if it exists, and the field name is not an expression or a placeholder.
		Optional<String> maybeFieldName = Optional.ofNullable(actualFieldName).map(fieldName -> (StringUtils.isBlank(fieldName) || fieldName.contains(" ")) ? fieldName : databaseConnector.renderNonContentColumn(fieldName + fieldSuffix));
		// Check both table alias / field name for existence 
		StringBuilder sb = new StringBuilder();
		if (!actualFieldName.contains("(")) {
			sb.append(ownerAlias);
		}
		if (maybeOwner.filter(owner -> !owner.endsWith("FIELD")).isPresent()) {
			String declaredOwnerAlias = makeAlias(maybeOwner.get());
			// If we came from the reference field builder and have already joined the (micro)content alias, the typename alias is present in an owner twice in the end.
			// TODO FIXME this is error-prone at the bigger recursion levels.
			if (!ownerAlias.endsWith(declaredOwnerAlias + declaredOwnerAlias) && !ReferencedNodesFilterJoin.ALIAS_CONTENTFIELDKEY.equals(actualFieldName)) {
				sb.append(declaredOwnerAlias);
			}
		}
		maybeFieldName.ifPresent(fn -> {
			if (sb.length() > 0) {
				sb.append(".");
			}			
		});
		if (maybeOwner.filter(owner -> owner.endsWith("CONTENT")).isPresent()) {
			sb.append(databaseConnector.renderColumnUnsafe(actualFieldName + fieldSuffix, !isNative));
		} else {
			sb.append(maybeFieldName.get());
		}
		return new HibernateFilter(HibernateTx.get().data().getDatabaseConnector().installStringContentColumn(sb.toString(), false, false), localJoins, paramsMap, maybeFieldType);
	}

	private HibernateFilter buildReferenceFieldOperand(FilterOperand<?> op, Optional<String> maybeOwner, String ownerAlias, Map<String, Object> paramsMap, 
			Set<Join> joins, Optional<UUID> maybeBranch, Optional<HibernateFilter> maybeTopLevelFilter, Optional<ContainerType> maybeContainerType, Deque<FilterOperation<?>> callStack) {
		EntityReferenceOperationOperand erop = (EntityReferenceOperationOperand) op;
		FilterOperation<?> wrappedOp = erop.getValue();
		String entityType = erop.getReferenceType();
		String localFieldName = erop.getFieldName();
		Class<?> edgeClass = maybeOwner.flatMap(this::tableNameIntoClass).orElseThrow(() -> new IllegalStateException("Unknown field edge entity name + " + op.maybeGetOwner().orElse("[EMPTY]")));
		String dependency = ownerAlias + makeAlias("CONTENT");
		NativeJoin localJoin = new NativeJoin();
		
		// Join the field edge
		{
			String fieldKey = callStack.peek().getInitiatingFilterName();
			maybeOwner = Optional.of(dependency + makeAlias(fieldKey));
			localJoin.addJoin(
					databaseConnector.maybeGetPhysicalTableName(edgeClass).get(), maybeOwner.get(), 
					new String[] {dependency + "." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID), dependency + "." + databaseConnector.renderColumnUnsafe(fieldKey + "-" + entityType, false)}, 
					new String[] {databaseConnector.renderNonContentColumn("containerUuid"), databaseConnector.renderNonContentColumn("dbUuid")}, 
					JoinType.LEFT, dependency);
			dependency = maybeOwner.get();
		}
		// We reference an actual entity, thus have to join it as well
		if (entityType.equals(localFieldName)) {
			if (HibMicronodeFieldEdgeImpl.class.equals(edgeClass)) {
				makeMicronodeSchemaVersionJoin(maybeOwner.get(), dependency, localJoin);
				maybeOwner = maybeOwner.map(o -> o + makeAlias("MICROCONTENT"));		
			} else if (HibNodeFieldEdgeImpl.class.equals(edgeClass)) {
				dependency = makeNodeSchemaVersionJoin(maybeOwner, dependency, entityType, localJoin).get();
			} else {
				maybeOwner = maybeOwner.map(o -> o + makeAlias(entityType));
			}
		}
		callStack.push(wrappedOp);
		HibernateFilter hf = appendFilter(dependency, wrappedOp, joins, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack);
		callStack.pop();
		hf.getRawSqlJoins().add(localJoin);
		return hf;
	}

	private HibernateFilter makeSqlComparisonOperand(FilterOperand<?> op, String ownerAlias, boolean isNative, NativeJoin join, 
			Optional<UUID> maybeBranch, Optional<HibernateFilter> maybeTopLevelFilter, Optional<ContainerType> maybeContainerType, Deque<FilterOperation<?>> callStack, Optional<String> maybeOperandType) {
		return maybeMakeLiteralOperand(op, ownerAlias, maybeOperandType).orElseGet(() -> makeParameterOperand(op, ownerAlias, isNative, join, maybeBranch, maybeTopLevelFilter, maybeContainerType, callStack));
	}

	private boolean doOwnerAndJoinMatchContentTrait(String joinOwner, String currentJoin) {
		return (joinOwner.endsWith("LIST") || "CONTENT".equals(joinOwner) || "MICROCONTENT".equals(joinOwner)) && ("CONTENT".equals(currentJoin) || "MICROCONTENT".equals(currentJoin));
	}

	private Optional<Pair<HibFieldSchemaElement<?,?,?,?,?>, String>> joinIntoSchemaRef(JoinPart join) {
		Class<?> tableClass = joinIntoPair(join).getKey();
		if (tableClass == null || !HibUnmanagedFieldContainer.class.isAssignableFrom(tableClass)) {
			// Already processed - entity case
			return Optional.empty();
		}
		HibFieldSchemaElement<?,?,?,?,?> schema = null;
		if ((schema = Tx.get().schemaDao().findByName(join.getField())) != null) {
			return Optional.of(Pair.of(schema, join.getField()));
		} else if ((schema = Tx.get().microschemaDao().findByName(join.getField())) != null) {
			return Optional.of(Pair.of(schema, join.getField()));
		} else {
			//throw new IllegalArgumentException("Unsupported element type: " + join.getTable());
			return Optional.empty();
		}
	}

	private Pair<Class<?>, String> joinIntoPair(JoinPart join) {
		return Pair.of(tableNameIntoClass(join.getTable()).orElse(null), join.getField());
	}

	@SuppressWarnings("rawtypes")
	private Optional<Class<?>> tableNameIntoClass(String tableName) {
		Class<?> cls;
		cls = ElementType.parse(tableName).map(etype -> {
			Class clss;
			switch (etype) {
			case BRANCH:
				clss = HibBranchImpl.class;
				break;
			case GROUP:
				clss = HibGroupImpl.class;
				break;
			case JOB:
				clss = HibJobImpl.class;
				break;
			case LANGUAGE:
				clss = HibLanguageImpl.class;
				break;
			case MICROSCHEMA:
				clss = HibMicroschemaImpl.class;
				break;
			case MICROSCHEMAVERSION:
				clss = HibMicroschemaVersionImpl.class;
				break;
			case NODE:
				clss = HibNodeImpl.class;
				break;
			case PROJECT:
				clss = HibProjectImpl.class;
				break;
			case ROLE:
				clss = HibRoleImpl.class;
				break;
			case SCHEMA:
				clss = HibSchemaImpl.class;
				break;
			case SCHEMAVERSION:
				clss = HibSchemaVersionImpl.class;
				break;
			case TAG:
				clss = HibTagImpl.class;
				break;
			case TAGFAMILY:
				clss = HibTagFamilyImpl.class;
				break;
			case USER:
				clss = HibUserImpl.class;
				break;
			default:
				throw new IllegalStateException("FIXME: unexpected element type, that should be supported " + etype);
			}
			return clss;
		}).orElseGet(() -> {
			Class<?> clss;
			switch (tableName) {
			case "CONTAINER":
				clss = HibNodeFieldContainerEdgeImpl.class;
				break;
			case "CONTENT":
				clss = HibNodeFieldContainerImpl.class;
				break;
			case "MICRONODE":
				clss = HibMicronodeFieldEdgeImpl.class;
				break;
			case "MICROCONTENT":
				clss = HibMicronodeContainerImpl.class;
				break;
			case "LIST":
				clss = AbstractHibListFieldEdgeImpl.class;
				break;
			case "BINARY":
				clss = HibBinaryImpl.class;
				break;
			case "S3BINARY":
				clss = HibS3BinaryImpl.class;
				break;
			case "STRINGLIST":
				clss = HibStringListFieldEdgeImpl.class;
				break;
			case "NUMBERLIST":
				clss = HibNumberListFieldEdgeImpl.class;
				break;
			case "HTMLLIST":
				clss = HibHtmlListFieldEdgeImpl.class;
				break;
			case "BOOLEANLIST":
				clss = HibBooleanListFieldEdgeImpl.class;
				break;
			case "DATELIST":
				clss = HibDateListFieldEdgeImpl.class;
				break;
			case "NODELIST":
				clss = HibNodeListFieldEdgeImpl.class;
				break;
			case "MICRONODELIST":
				clss = HibMicronodeListFieldEdgeImpl.class;
				break;
			case "NODEFIELD":
				clss = HibNodeFieldEdgeImpl.class;
				break;
			case "MICRONODEFIELD":
				clss = HibMicronodeFieldEdgeImpl.class;
				break;
			case "BINARYFIELD":
				clss = HibBinaryFieldEdgeImpl.class;
				break;
			case "S3BINARYFIELD":
				clss = HibS3BinaryFieldEdgeImpl.class;
				break;
			case "IMAGEVARIANTLIST":
				clss = HibImageVariantImpl.class;
				break;
//			case "BINARYLIST":
//				clss = HibBinaryListFieldEdgeImpl.class;
//				break;
//			case "S3BINARYLIST":
//				clss = HibS3BinaryListFieldEdgeImpl.class;
//				break;
			default:
				if (tableName.endsWith("REFERENCELIST")) {
					clss = AbstractFieldEdgeImpl.class;
				} else {
					clss = null;
				}
				break;
			}
			return clss;
		});
		return Optional.ofNullable(cls);
	}

	/**
	 * Gets a query results stream according to the paging parameters.
	 *
	 * @param ac
	 * @param query
	 * @param entityGraph
	 * @return
	 */
	public <R, F> Stream<? extends R> getResultStream(InternalActionContext ac, CriteriaQuery<? extends R> query, EntityGraph<?> entityGraph) {
		return setEntityGraph(em().createQuery(query), entityGraph).getResultList().stream();
	}

	/**
	 * Gets a page from a query according to the paging parameters.
	 *
	 * @param query
	 * @param pagingInfo
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <E> Page<E> getResultPageTuple(CriteriaQuery<?> query, PagingParameters pagingInfo, EntityGraph<?> entityGraph) {
		List<? extends E> results;
		long totalElements;
		Long perPage = pagingInfo.getPerPage();
		if (perPage == null) {
			results = setEntityGraph(em().createQuery(query), entityGraph)
				.getResultStream()
				.map(element -> {
					if (element instanceof Object[]) {
						Object[] arr = (Object[]) element;
						return (E) arr[0];
					} else {
						return (E) element;
					}
				})
				.collect(Collectors.toList());
			totalElements = results.size();
		} else if (perPage == 0) {
			results = Collections.emptyList();
			totalElements = JpaUtil.count(em(), query);
		} else {
			results = setEntityGraph(em().createQuery(query), entityGraph)
				.setFirstResult(pagingInfo.getActualPage() * perPage.intValue())
				.setMaxResults(perPage.intValue())
				// Hibernate does not always support scrolling
				.getResultList()
				.stream()
				.map(element -> {
					if (element instanceof Object[]) {
						Object[] arr = (Object[]) element;
						return (E) arr[0];
					} else {
						return (E) element;
					}
				})
				.collect(Collectors.toList());
			totalElements = JpaUtil.count(em(), query);
		}

		return new PageImpl<>(results, pagingInfo, totalElements);
	}

	private <E> TypedQuery<E> setEntityGraph(TypedQuery<E> query, EntityGraph<?> entityGraph) {
		if (entityGraph != null) {
			query.setHint(SpecHints.HINT_SPEC_FETCH_GRAPH, entityGraph);
		}
		return query;
	}

	/**
	 * Creates a domain object with a random uuid that will be generated using {@link UuidGenerator#generateType1UUID()}.
	 *
	 * @return
	 */
	public D create() {
		return create(null, d -> {});
	}

	/**
	 * Creates a domain object with the given uuid.
	 * If the uuid is null, a random uuid will be generated using {@link UuidGenerator#generateType1UUID()}.
	 *
	 * @param uuid
	 * @param inflater
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public D create(String uuid, Consumer<T> inflater) {
		return (D) currentTransaction.getTx().create(uuid, domainClass, inflater);
	}

	/**
	 * Load the object by uuid and check the given permission.
	 *
	 * @param ac
	 *			Context to be used in order to check user permissions
	 * @param uuid
	 *			Uuid of the object that should be loaded
	 * @param perm
	 *			Permission that must be granted in order to load the object
	 * @return Loaded element. A not found error will be thrown if the element could not be found. Returned value will never be null.
	 */
	public T loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		return loadObjectByUuid(ac, uuid, perm, true);
	}

	/**
	 * Load the object by uuid and check the given permission.
	 *
	 * @param ac
	 *			Context to be used in order to check user permissions
	 * @param uuid
	 *			Uuid of the object that should be loaded
	 * @param perm
	 *			Permission that must be granted in order to load the object
	 * @param errorIfNotFound
	 *			True if an error should be thrown, when the element could not be found
	 * @return Loaded element. If errorIfNotFound is true, a not found error will be thrown if the element could not be found and the returned value will never
	 *		 be null.
	 */
	public T loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		T element = findByUuid(uuid);
		return checkPerms(element, uuid, ac, perm, errorIfNotFound);
	}

	private T checkPerms(T element, String uuid, InternalActionContext ac, InternalPermission perm, boolean errorIfNotFound) {
		if (element == null) {
			if (errorIfNotFound) {
				throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
			} else {
				return null;
			}
		}
		HibUser requestUser = ac.getUser();
		String elementUuid = element.getUuid();
		if (daoCollection.get().userDao().hasPermission(requestUser, element, perm)) {
			return element;
		} else {
			throw error(FORBIDDEN, "error_missing_perm", elementUuid, perm.getRestPerm().getName());
		}
	}

	/**
	 * Find the element with the given String uuid.
	 *
	 * @param uuid
	 *			Uuid of the element to be located
	 * @return Found element or null if the element could not be located
	 */
	public T findByUuid(String uuid) {
		if (!UUIDUtil.isUUID(uuid)) {
			return null;
		}
		return findByUuid(UUIDUtil.toJavaUuid(uuid));
	}

	/**
	 * Find the element with the given uuid.
	 *
	 * @param uuid
	 *			Uuid of the element to be located
	 * @return Found element or null if the element could not be located
	 */
	public T findByUuid(UUID uuid) {
		return em().find(domainClass, uuid);
	}

	/**
	 * Find the element with the given name.
	 *
	 * @param name
	 * @return Found element or null if element with the name could not be found
	 */
	public T findByName(String name) {
		CriteriaQuery<D> query = cb().createQuery(domainClass);
		Root<D> root = query.from(domainClass);
		query.where(cb().equal(root.get("name"), name));
		return firstOrNull(em().createQuery(query).setHint(AvailableHints.HINT_CACHEABLE, true).setMaxResults(1));
	}

	/**
	 * Add common fields to the given rest model object. The method will add common files like creator, editor, uuid, permissions, edited, created.
	 *
	 * @param model
	 * @param fields
	 *			Set of fields which should be included. All fields will be included if no selective fields have been specified.
	 * @param ac
	 */
	@SuppressWarnings("unchecked")
	public void fillCommonRestFields(T element, InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		if (fields.has("uuid")) {
			model.setUuid(element.getUuid());
		}

		if (element instanceof HibEditorTracking) {
			HibEditorTracking edited = (HibEditorTracking) element;

			if (fields.has("editor")) {
				HibUser editor = edited.getEditor();
				if (editor != null) {
					model.setEditor(editor.transformToReference());
				} else {
					log.warn("The object {" + getClass().getSimpleName() + "} with uuid {" + element.getUuid() + "} has no editor. Omitting editor field");
				}
			}

			if (fields.has("edited")) {
				String date = edited.getLastEditedDate();
				model.setEdited(date);
			}
		}

		if (element instanceof HibCreatorTracking) {
			HibCreatorTracking created = (HibCreatorTracking) element;
			if (fields.has("creator")) {
				HibUser creator = created.getCreator();
				if (creator != null) {
					model.setCreator(creator.transformToReference());
				}
			}
			if (fields.has("created")) {
				String date = created.getCreationDate();
				model.setCreated(date);
			}
		}

		if (fields.has("perms")) {
			// When this is a node migration, do not set user permissions
			if (!(ac instanceof NodeMigrationActionContextImpl)) {
				PermissionInfo permissionInfo = getPermissionForUser(ac.getUser())
					.map(hibPermission -> {
						if (elementType.equals(ElementType.NODE)) {
							return hibPermission.getPermissionInfo();
						} else {
							return hibPermission.getPermissionInfoWithoutPublish();
						}
					})
					.orElseGet(PermissionInfo::noPermissions);
				model.setPermissions(permissionInfo);
			}
		}

		RolePermissionParameters rolePermissionParameters = ac.getRolePermissionParameters();
		if (rolePermissionParameters != null && element instanceof HibCoreElement) {
			model.setRolePerms(daoCollection.get().roleDao().getRolePermissions(((HibCoreElement<? extends RestModel>) element), ac, rolePermissionParameters.getRoleUuid()));
		}
	}

	/**
	 * Gets the permission entity for a user.
	 * This might be empty if there is no entry in the database for the user-element pair.
	 * This case is equivalent to the user having no permissions to the element.
	 *
	 * @param user
	 * @return
	 */
	public Optional<HibPermissionImpl> getPermissionForUser(HibUser user) {
		return em().createQuery("select p" +
				" from permission p" +
				" join p.role r" +
				" join r.groups g" +
				" join g.users u" +
				" where u = :user",
			HibPermissionImpl.class
		).setParameter("user", user)
			.getResultStream()
			.findAny();
	}

	EntityManager em() {
		return currentTransaction.getEntityManager();
	}

	CriteriaBuilder cb() {
		return em().getCriteriaBuilder();
	}

	@SuppressWarnings("unchecked")
	public void delete(T element) {
		currentTransaction.getTx().delete(element);
		if (element instanceof HibCoreElement) {
			currentTransaction.getTx().batch().add(eventFactory.onDeleted(((HibCoreElement<? extends RestModel>) element)));
		} else {
			log.warn("Couldn't emit event. Class of element: " + element.getClass());
		}
	}
	
	/**
	 * Count the instances of the domain class.
	 * 
	 * @return
	 */
	public long count() {
		CriteriaQuery<D> query = cb().createQuery(domainClass);
		query.from(domainClass);
		return JpaUtil.count(em(), query);
	}

	/**
	 * Count the instances that satisfy permission restrictions.
	 * 
	 * @param ac
	 * @param permission
	 * @return
	 */
	public long count(InternalActionContext ac, InternalPermission permission) {
		CriteriaQuery<D> query = cb().createQuery(domainClass);
		addPermissionRestriction(query, query.from(domainClass), ac.getUser(), permission);
		return JpaUtil.count(em(), query);
	}

	public String getAPIPath(String domainEndpoint, HibCoreElement<? extends RestModel> element, InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + domainEndpoint + "/" + element.getUuid();
	}	
	
	public String getSubETag(HibEditorTracking element, InternalActionContext ac) {
		return String.valueOf(element.getLastEditedTimestamp());
	}
	
	public Class<D> getDomainClass() {
		return domainClass;
	}

	/**
	 * If the entity is not null, load it by its UUID.
	 * This will make sure that the entity is either loaded from the database or taken from the hibernate cache
	 * @param entity entity to reload
	 * @return entity
	 */
	public T nullSafeFindByUuid(T entity) {
		if (entity != null) {
			return findByUuid(entity.getUuid());
		} else {
			return entity;
		}
	}

	/**
	 * Get the cached total count value for the given native query, or fetch its value by execution. Please pay attention to the query's return type.
	 * 
	 * @param query
	 * @param fetcher
	 * @return
	 */
	public long getOrFetchTotal(NativeQuery<?> query) {
		String key = query.getQueryString() + query.getParameterMetadata().getNamedParameterNames().stream().collect(Collectors.joining());
		return totalsCache.get(key, unused -> ((Number) query.getSingleResult()).longValue());
	}

	/**
	 * Get a list of database table columns for this domain entity, through cache.
	 * 
	 * @return
	 */
	public Set<String> getDomainColumns() {
		return HibernateTx.get().data().getTableColumnsCache().get(domainClass, cls -> databaseConnector.getDatabaseColumnNames(cls).orElse(null));
	}

	/**
	 * Get the String representation of the table using current dialect.
	 * 
	 * @return
	 */
	protected String getPhysicalTableNameIdentifier(String tableName) {
		return getPhysicalTableNameIdentifier(tableName, databaseConnector.getPhysicalNamingStrategy(), databaseConnector.getSessionMetadataIntegrator().getJdbcEnvironment()).render(databaseConnector.getHibernateDialect());
	}

	/**
	 * Get the Hibernate identifier of the table as per SQL DB engine dialect.
	 * 
	 * @return
	 */
	protected Identifier getPhysicalTableNameIdentifier(String tableName, PhysicalNamingStrategy physicalNamingStrategy, JdbcEnvironment jdbcEnvironment) {
		return physicalNamingStrategy.toPhysicalTableName(
				jdbcEnvironment.getIdentifierHelper().toIdentifier(tableName),
				jdbcEnvironment
		);
	}
}
