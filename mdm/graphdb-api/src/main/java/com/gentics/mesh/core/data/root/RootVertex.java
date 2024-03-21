package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.dao.ElementResolver;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicNonTransformablePageImpl;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.query.MeshGraphEdgeQuery;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * A root vertex is an aggregation vertex that is used to aggregate various basic elements such as users, nodes, groups.
 */
public interface RootVertex<T extends MeshCoreVertex<? extends RestModel>> extends MeshVertex, HasPermissionsRoot, ElementResolver<HibBaseElement, T> {

	@Override
	default boolean checkReadPermissionBeforeApplyingPermissions() {
		// We don't check for read permission on root elements, since the mesh-ui doesn't allow currently to add
		// read permissions for root elements.
		return false;
	}

	/**
	 * Return a traversal of all elements. Only use this method if you know that the root->item relation only yields a specific kind of item.
	 * 
	 * @return
	 */
	default Result<? extends T> findAll() {
		return out(getRootLabel(), getPersistanceClass());
	}

	/**
	 * Return an iterator of all elements. Only use this method if you know that the root->item relation only yields a specific kind of item. This also checks
	 * permissions.
	 *
	 * @param ac
	 *            The context of the request
	 * @param permission
	 *            Needed permission
	 */
	default Stream<? extends T> findAllStream(InternalActionContext ac, InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		HibUser user = ac.getUser();
		DelegatingFramedMadlGraph<? extends Graph> graph = GraphDBTx.getGraphTx().getGraph();
		UserDao userDao = GraphDBTx.getGraphTx().userDao();

		Spliterator<Edge> itemEdges;
		MeshGraphEdgeQuery query = GraphDBTx.getGraphTx().edgeQuery(getPersistanceClass(), getRootLabel().toUpperCase());

		List<String> sortParams = paging.getSort().entrySet().stream().map(e -> e.getKey() + " " + e.getValue().getValue()).collect(Collectors.toUnmodifiableList());
		query.setOrderPropsAndDirs(sortParams.toArray(new String[sortParams.size()]));
		query.inComesFrom(id());
		query.filter(maybeFilter.map(filter -> parseFilter(filter, ContainerType.PUBLISHED, user, permission, Optional.of("inV()"))));
		if (paging.getPerPage() != null) {
			query.skip((int) (paging.getActualPage() * paging.getPerPage()));
			query.limit(paging.getPerPage().intValue());
		}
		Optional<? extends Collection<? extends Class<?>>> maybeVariations = getPersistenceClassVariations();
		itemEdges = query.fetch(maybeVariations).spliterator();

		return StreamSupport.stream(itemEdges, false)
			.map(Edge::inVertex)
			.filter(vertex -> userDao.hasPermissionForId(user, vertex.id(), permission))
			.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	/**
	 * Return an traversal result of all elements and use the stored type information to load the items. The {@link #findAll()} will use explicit typing and
	 * thus will be faster. Only use that method if you know that your relation only yields a specific kind of item.
	 * 
	 * @return
	 */
	default Result<? extends T> findAllDynamic() {
		return new TraversalResult<>(out(getRootLabel(), getPersistanceClass()));
	}

	/**
	 * Find the visible elements and return a paged result.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * 
	 * @return
	 */
	default Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return new DynamicTransformablePageImpl<>(ac.getUser(), this, pagingInfo, READ_PERM, null, true);
	}

	/**
	 * Find the visible elements and return a paged result.
	 *
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @param extraFilter
	 *            Additional filter to be applied
	 * @return
	 */
	default Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<T> extraFilter) {
		return new DynamicNonTransformablePageImpl<>(ac.getUser(), this, pagingInfo, READ_PERM, extraFilter, true);
	}

	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @return
	 */
	default Page<? extends T> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return findAllNoPerm(ac, pagingInfo, null);
	}

	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @param extraFilter
	 *            Additional filter to be applied
	 * @return
	 */
	default Page<? extends T> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo, Predicate<T> extraFilter) {
		return new DynamicTransformablePageImpl<>(ac.getUser(), this, pagingInfo, null, extraFilter, true);
	}
	/**
	 * Find the element with the given name.
	 * 
	 * @param name
	 * @return Found element or null if element with the name could not be found
	 */
	default T findByName(String name) {
		return out(getRootLabel()).has("name", name).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	/**
	 * Find the element with the given uuid.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located
	 * @return Found element or null if the element could not be located
	 */
	default T findByUuid(String uuid) {
		return out(getRootLabel()).has(UUID_KEY, uuid).has(getPersistanceClass()).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	@Override
	default BiFunction<HibBaseElement, String, T> getFinder() {
		return (unused, uuid) -> findByUuid(uuid);
	}

	/**
	 * Create an uninitialized persisted object.
	 * 
	 * @return
	 */
	T create();

	/**
	 * Add the given item to the this root vertex.
	 * 
	 * @param item
	 */
	default void addItem(T item) {
		// Check whether the item was already added
		boolean hasEdge = out(getRootLabel()).rawTraversal().hasId(item.id()).hasNext();
		hasEdge = hasEdge && getGraph().getFramedVertexExplicitOrNull(item.getClass(), item.id()) != null;
		if (!hasEdge) {
			linkOut(item, getRootLabel());
		}
	}

	/**
	 * Remove the given item from this root vertex.
	 * 
	 * @param item
	 */
	default void removeItem(T item) {
		unlinkOut(item, getRootLabel());
	}

	/**
	 * Return the label for the item edges.
	 * 
	 * @return
	 */
	String getRootLabel();

	/**
	 * Return the ferma graph persistance class for the items of the root vertex. (eg. NodeImpl, TagImpl...)
	 * 
	 * @return
	 */
	Class<? extends T> getPersistanceClass();

	/**
	 * Return the total count of all tracked elements.
	 * 
	 * @return
	 */
	default long computeCount() {
		return findAll().count();
	}

	/**
	 * Return the global count for all elements of the type that are managed by the root vertex. The count will include all vertices in the graph of the
	 * specific type.
	 * 
	 * @return
	 */
	long globalCount();

	/**
	 * Get the permissions of a role for this element.
	 *
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid);

	/**
	 * Return a traversal result for all roles which grant the permission to the element.
	 *
	 * @param vertex
	 * @param perm
	 * @return
	 */
	Result<? extends HibRole> getRolesWithPerm(HibBaseElement vertex, InternalPermission perm);

	/**
	 * Get extensions/variations of the ferma graph persistence class.
	 * 
	 * @return
	 */
	default Optional<? extends Collection<Class<? extends T>>> getPersistenceClassVariations() {
		return Optional.empty();
	}

	/**
	 * Find all entities with an optional native filter.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param maybeExtraFilter
	 * @return
	 */
	Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeExtraFilter);
}
