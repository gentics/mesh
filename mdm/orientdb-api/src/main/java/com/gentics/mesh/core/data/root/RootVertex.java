package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;

import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A root vertex is an aggregation vertex that is used to aggregate various basic elements such as users, nodes, groups.
 */
public interface RootVertex<T extends MeshCoreVertex<? extends RestModel>> extends MeshVertex, HasPermissionsRoot, ElementResolver<HibBaseElement, T> {

	public static final Logger log = LoggerFactory.getLogger(RootVertex.class);

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
	default Stream<? extends T> findAllStream(InternalActionContext ac, InternalPermission permission) {
		HibUser user = ac.getUser();
		FramedTransactionalGraph graph = GraphDBTx.getGraphTx().getGraph();
		UserDao userDao = GraphDBTx.getGraphTx().userDao();

		String idx = "e." + getRootLabel().toLowerCase() + "_out";
		Spliterator<Edge> itemEdges = graph.getEdges(idx.toLowerCase(), id()).spliterator();
		return StreamSupport.stream(itemEdges, false)
			.map(edge -> edge.getVertex(Direction.IN))
			.filter(vertex -> userDao.hasPermissionForId(user, vertex.getId(), permission))
			.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	/**
	 * Return an traversal result of all elements and use the stored type information to load the items. The {@link #findAll()} will use explicit typing and
	 * thus will be faster. Only use that method if you know that your relation only yields a specific kind of item.
	 * 
	 * @return
	 */
	default Result<? extends T> findAllDynamic() {
		return new TraversalResult<>(out(getRootLabel()).frame(getPersistanceClass()));
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
		GraphDatabase db = HibClassConverter.toGraph(db());
		// Try to load the element using the index. This way no record load will happen.
		T t = db.index().findByUuid(getPersistanceClass(), uuid);
		if (t != null) {
			FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
			// Use the edge index to determine whether the element is part of this root vertex
			Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout", db.index().createComposedIndexKey(t
				.getId(), id()));
			if (edges.iterator().hasNext()) {
				return t;
			}
		}
		return null;
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
		GraphDatabase db = HibClassConverter.toGraph(db());
		FramedGraph graph = getGraph();
		// Check whether the item was already added by checking the index
		Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout", db.index().createComposedIndexKey(item.id(),
			id()));
		if (!edges.iterator().hasNext()) {
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
}
