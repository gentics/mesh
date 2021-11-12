package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Spliterator;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
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
import com.gentics.mesh.event.EventQueueBatch;
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
public interface RootVertex<T extends MeshCoreVertex<? extends RestModel>> extends MeshVertex, HasPermissionsRoot {

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
		return new DynamicTransformablePageImpl<>(ac.getUser(), this, pagingInfo, null, null, true);
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

	/**
	 * Resolve the given stack to the vertex.
	 * 
	 * @param stack
	 *            Stack which contains the remaining path elements which should be resolved starting with the current graph element
	 * @return
	 */
	default HibBaseElement resolveToElement(Stack<String> stack) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving for {" + getPersistanceClass().getSimpleName() + "}.");
			if (stack.isEmpty()) {
				log.debug("Stack: is empty");
			} else {
				log.debug("Stack: " + stack.peek());
			}
		}
		if (stack.isEmpty()) {
			return this;
		} else {
			String uuid = stack.pop();
			if (stack.isEmpty()) {
				return findByUuid(uuid);
			} else {
				throw error(BAD_REQUEST, "Can't resolve remaining segments. Next segment would be: " + stack.peek());
			}
		}
	}

	/**
	 * Create a new object which is connected or directly related to this aggregation vertex.
	 * 
	 * @param ac
	 *            Context which is used to load information needed for the object creation
	 * @param batch
	 */
	default T create(InternalActionContext ac, EventQueueBatch batch) {
		return create(ac, batch, null);
	}

	/**
	 * Create a new object which is connected or directly related to this aggregation vertex.
	 * 
	 * @param ac
	 *            Context which is used to load information needed for the object creation
	 * @param batch
	 * @param uuid
	 *            optional uuid to create the object with a given uuid (null to create a random uuid)
	 */
	T create(InternalActionContext ac, EventQueueBatch batch, String uuid);

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

	/**
	 * Delete the element. Additional entries will be added to the batch to keep the search index in sync.
	 *
	 * @param element
	 * @param bac
	 *            Deletion context which keeps track of the deletion process
	 */
	default void delete(T element, BulkActionContext bac) {
		element.delete(bac);
	}

	/**
	 * Update the vertex using the action context information.
	 *
	 * @param ac
	 * @param batch
	 *            Batch to which entries will be added in order to update the search index.
	 * @return true if the element was updated. Otherwise false
	 */
	default boolean update(T element, InternalActionContext ac, EventQueueBatch batch) {
		return element.update(ac, batch);
	}
}
