package com.gentics.mda.entitycollection;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mda.entity.AMeshCoreElement;
import com.gentics.mda.entity.AMeshElement;
import com.gentics.mda.page.ATransformablePage;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface MeshDao<T extends AMeshCoreElement> extends AMeshElement {
	public static final Logger log = LoggerFactory.getLogger(RootVertex.class);

	/**
	 * Return a traversal of all elements. Only use this method if you know that the root->item relation only yields a specific kind of item.
	 *
	 * @return
	 */
	TraversalResult<? extends T> findAll();

	/**
	 * Return an iterator of all elements. Only use this method if you know that the root->item relation only yields a specific kind of item. This also checks
	 * permissions.
	 *
	 * @param ac
	 *            The context of the request
	 * @param permission
	 *            Needed permission
	 */
	Stream<? extends T> findAllStream(InternalActionContext ac, GraphPermission permission);

	/**
	 * Return an traversal result of all elements and use the stored type information to load the items. The {@link #findAll()} will use explicit typing and
	 * thus will be faster. Only use that method if you know that your relation only yields a specific kind of item.
	 *
	 * @return
	 */
	TraversalResult<? extends T> findAllDynamic();

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
	ATransformablePage<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo);

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
	ATransformablePage<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<T> extraFilter);

	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 *
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @return
	 */
	ATransformablePage<? extends T> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Find the element with the given name.
	 *
	 * @param name
	 * @return Found element or null if element with the name could not be found
	 */
	T findByName(String name);

	/**
	 * Load the object by name and check the given permission.
	 *
	 * @param ac
	 *            Context to be used in order to check user permissions
	 * @param name
	 *            Name of the object that should be loaded
	 * @param perm
	 *            Permission that must be granted in order to load the object
	 * @return
	 */
	T findByName(InternalActionContext ac, String name, GraphPermission perm);

	/**
	 * Find the element with the given uuid.
	 *
	 * @param uuid
	 *            Uuid of the element to be located
	 * @return Found element or null if the element could not be located
	 */
	T findByUuid(String uuid);

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
	default T loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return loadObjectByUuid(ac, uuid, perm, true);
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
	 * @param errorIfNotFound
	 *            True if an error should be thrown, when the element could not be found
	 * @return Loaded element. If errorIfNotFound is true, a not found error will be thrown if the element could not be found and the returned value will never
	 *         be null.
	 */
	T loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	/**
	 * Load the object by uuid. No permission check will be performed.
	 *
	 * @param uuid
	 *            Uuid of the object that should be loaded
	 * @param errorIfNotFound
	 *            True if an error should be thrown, when the element could not be found
	 * @return Loaded element. If errorIfNotFound is true, a not found error will be thrown if the element could not be found and the returned value will never
	 *         be null.
	 */
	default T loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		T element = findByUuid(uuid);
		if (element == null) {
			if (errorIfNotFound) {
				throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
			} else {
				return null;
			}
		}
		return element;
	}

	/**
	 * Resolve the given stack to the vertex.
	 *
	 * @param stack
	 *            Stack which contains the remaining path elements which should be resolved starting with the current graph element
	 * @return
	 */
	default AMeshElement resolveToElement(Stack<String> stack) {
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
	 * Add the given item to the this root vertex.
	 *
	 * @param item
	 */
	void addItem(T item);

	/**
	 * Remove the given item from this root vertex.
	 *
	 * @param item
	 */
	void removeItem(T item);

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

	default long computeCount() {
		return findAll().count();
	}

}
