package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ElementIdComparator;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A root vertex is an aggregation vertex that is used to aggregate various basic elements such as users, nodes, groups.
 */
public interface RootVertex<T extends MeshCoreVertex<? extends RestModel, T>> extends MeshVertex {

	public static final Logger log = LoggerFactory.getLogger(RootVertex.class);

	Database database();

	/**
	 * Return a list of all elements.
	 * 
	 * @return
	 */
	default public List<? extends T> findAll() {
		return out(getRootLabel()).toListExplicit(getPersistanceClass());
	}

	/**
	 * Find the visible elements and return a paged result.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options.
	 * 
	 * @return
	 * @throws InvalidArgumentException
	 *             if the paging options are malformed.
	 */
	default public Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo)
			throws InvalidArgumentException {

		int page = pagingInfo.getPage();
		int perPage = pagingInfo.getPerPage();

		if (page < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(page));
		}
		if (perPage < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(perPage));
		}

		// Internally we start with page 0 instead of 1 to keep page range calculations simple.
		// External (for the enduser) all pages start with 1.
		page = page - 1;

		int low = page * perPage;

		if (perPage == 0) {
			low = 0;
		}

		MeshAuthUser requestUser = ac.getUser();

		// Iterate over all vertices that are managed by this root vertex
		FramedGraph graph = Database.getThreadLocalGraph();
		Iterable<Edge> itemEdges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_out", this.getId());

		AtomicLong counter = new AtomicLong();
		List<T> elementsOfPage = StreamSupport.stream(itemEdges.spliterator(), false)

				// Get the vertex from the edge
				.map(itemEdge -> itemEdge.getVertex(Direction.IN))

				// Only handle elements which are visible to the user
				.filter(item -> requestUser.hasPermissionForId(item.getId(), READ_PERM))

				// We need to get a total count of all visible elements
				.map(item -> {
					counter.incrementAndGet();
					return item;
				})

				// Sort the elements by element id
				.sorted(new ElementIdComparator())

				// Apply paging - skip to lower bounds
				.skip(low)

				// Apply paging - only include a specific amout of elements
				.limit(perPage)

				// Frame the remaining elements so that we can access their ferma methods
				.map(item -> graph.frameElementExplicit(item, getPersistanceClass()))

				// Create a list of all found elements
				.collect(Collectors.toList());

		// The totalPages of the list response must be zero if the perPage parameter is also zero.
		int totalPages = 0;
		if (perPage != 0) {
			totalPages = (int) Math.ceil(counter.get() / (double) (perPage));
		}

		return new PageImpl<T>(elementsOfPage, counter.get(), ++page, totalPages, elementsOfPage.size(), perPage);
	}

	/**
	 * Find the element with the given name.
	 * 
	 * @param name
	 * @return
	 */
	default public T findByName(String name) {
		return out(getRootLabel()).has("name", name).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	/**
	 * Load the object by name and check the given permission.
	 * 
	 * @param ac
	 *            Context to be used in order to check user permissions
	 * @param name
	 *            Name of the object that should be loaded
	 * @param perm
	 *            Permission that must be granted in order to load the object
	 * 
	 * @return
	 */
	default public T findByName(InternalActionContext ac, String name, GraphPermission perm) {
		Database db = database();
		reload();
		T element = findByName(name);
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_name", name);
		}

		T result = db.noTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			String elementUuid = element.getUuid();
			if (requestUser.hasPermission(element, perm)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", elementUuid);
			}
		});

		return result;
	}

	/**
	 * Find the element with the given uuid.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located
	 * @return Found element or null if the element could not be located
	 */
	default public T findByUuid(String uuid) {
		FramedGraph graph = Database.getThreadLocalGraph();
		// 1. Find the element with given uuid within the whole graph
		Iterator<Vertex> it = database().getVertices(getPersistanceClass(), new String[] { "uuid" },
				new String[] { uuid });
		if (it.hasNext()) {
			Vertex potentialElement = it.next();
			// 2. Use the edge index to determine whether the element is part of this root vertex
			Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout",
					database().createComposedIndexKey(potentialElement.getId(), getId()));
			if (edges.iterator().hasNext()) {
				return graph.frameElementExplicit(potentialElement, getPersistanceClass());
			}
		}
		return null;
	}

	/**
	 * Load the object by uuid and check the given permission.
	 * 
	 * @param ac
	 *            Context to be used in order to check user permissions
	 * @param uuid
	 *            Uuid of the object that should be loaded
	 * @param perm
	 *            Permission that must be granted in order to load the object
	 * @return Loaded element. A not found error will be thrown if the element could not be found. Returned value will never be null.
	 */
	default public T loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		reload();
		T element = findByUuid(uuid);
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		MeshAuthUser requestUser = ac.getUser();
		String elementUuid = element.getUuid();
		if (requestUser.hasPermission(element, perm)) {
			return element;
		} else {
			throw error(FORBIDDEN, "error_missing_perm", elementUuid);
		}
	}

	/**
	 * Resolve the given stack to the vertex.
	 * 
	 * @param stack
	 *            Stack which contains the remaining path elements which should be resolved starting with the current graph element
	 * @return
	 */
	default public MeshVertex resolveToElement(Stack<String> stack) {
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
	T create(InternalActionContext ac, SearchQueueBatch batch);

	/**
	 * Add the given item to the this root vertex.
	 * 
	 * @param item
	 */
	default public void addItem(T item) {
		setUniqueLinkOutTo(item, getRootLabel());
	}

	/**
	 * Remove the given item from this root vertex.
	 * 
	 * @param item
	 */
	default public void removeItem(T item) {
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

}
