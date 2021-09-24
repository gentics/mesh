package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A DAO for the entities, that has an one-to-many connection to other entities,
 * i.e. root-leaves dependencies. This DAO allows operation on leaf entities, when a root entity is given. 
 * 
 * @author plyhun
 *
 * @param <R> root entity type
 * @param <L> leaf entity type
 */
public interface RootDao<R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> {

	public static final Logger log = LoggerFactory.getLogger(RootDao.class);

	/**
	 * Return a of all elements. Only use this method if you know that the root->leaf relation only yields a specific kind of item.
	 * 
	 * @return
	 */
	Result<? extends L> findAll(R root);

	/**
	 * Return an iterator of all elements. Only use this method if you know that the root->leaf relation only yields a specific kind of item. This also checks
	 * permissions.
	 *
	 * @param ac
	 *            The context of the request
	 * @param permission
	 *            Needed permission
	 */
	Stream<? extends L> findAllStream(R root, InternalActionContext ac, InternalPermission permission);

	/**
	 * Return an result of all elements and use the stored type information to load the items. The {@link #findAll()} will use explicit typing and
	 * thus will be faster. Only use that method if you know that your relation only yields a specific kind of item.
	 * 
	 * @return
	 */
	Result<? extends L> findAllDynamic(R root);

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
	Page<? extends L> findAll(R root, InternalActionContext ac, PagingParameters pagingInfo);

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
	Page<? extends L> findAll(R root, InternalActionContext ac, PagingParameters pagingInfo, Predicate<L> extraFilter);

	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options
	 * @return
	 */
	Page<? extends L> findAllNoPerm(R root, InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Find the element with the given name.
	 * 
	 * @param name
	 * @return Found element or null if element with the name could not be found
	 */
	L findByName(R root, String name);

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
	L findByName(R root, InternalActionContext ac, String name, InternalPermission perm);

	/**
	 * Find the element with the given uuid.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located
	 * @return Found element or null if the element could not be located
	 */
	L findByUuid(R root, String uuid);

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
	default L loadObjectByUuid(R root, InternalActionContext ac, String uuid, InternalPermission perm) {
		return loadObjectByUuid(root, ac, uuid, perm, true);
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
	default L loadObjectByUuid(R root, InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		L element = findByUuid(root, uuid);
		return checkPerms(root, element, uuid, ac, perm, errorIfNotFound);
	}

	L checkPerms(R root, L element, String uuid, InternalActionContext ac, InternalPermission perm, boolean errorIfNotFound);
// TODO get this back after Tx is generalized
//	default L checkPerms(R root, L element, String uuid, InternalActionContext ac, InternalPermission perm, boolean errorIfNotFound) {
//		if (element == null) {
//			if (errorIfNotFound) {
//				throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
//			} else {
//				return null;
//			}
//		}
//
//		HibUser requestUser = ac.getUser();
//		String elementUuid = element.getUuid();
//		UserDao userDao = Tx.get().userDao();
//		if (userDao.hasPermission(requestUser, element, perm)) {
//			return element;
//		} else {
//			throw error(FORBIDDEN, "error_missing_perm", elementUuid, perm.getRestPerm().getName());
//		}
//	}

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
	default L loadObjectByUuidNoPerm(R root, String uuid, boolean errorIfNotFound) {
		L element = findByUuid(root, uuid);
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
	 * Resolve the given stack to the element.
	 * 
	 * @param stack
	 *            Stack which contains the remaining path elements which should be resolved starting with the current element
	 * @return
	 */
	default HibBaseElement resolveToElement(R root, Stack<String> stack) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving for {" + getPersistenceClass(root).getSimpleName() + "}.");
			if (stack.isEmpty()) {
				log.debug("Stack: is empty");
			} else {
				log.debug("Stack: " + stack.peek());
			}
		}
		if (stack.isEmpty()) {
			return root;
		} else {
			String uuid = stack.pop();
			if (stack.isEmpty()) {
				return findByUuid(root, uuid);
			} else {
				throw error(BAD_REQUEST, "Can't resolve remaining segments. Next segment would be: " + stack.peek());
			}
		}
	}

	/**
	 * Create a new leaf object which is connected or directly related to this root element.
	 * 
	 * @param ac
	 *            Context which is used to load information needed for the object creation
	 * @param batch
	 */
	default L create(R root, InternalActionContext ac, EventQueueBatch batch) {
		return create(root, ac, batch, null);
	}

	/**
	 * Create a new leaf object which is connected or directly related to this root element.
	 * 
	 * @param ac
	 *            Context which is used to load information needed for the object creation
	 * @param batch
	 * @param uuid
	 *            optional uuid to create the object with a given uuid (null to create a random uuid)
	 */
	L create(R root, InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Add the given item to the this root element (make a leaf).
	 * 
	 * @param item
	 */
	void addItem(R root, L item);

	/**
	 * Remove the given leaf item from this root element.
	 * 
	 * @param item
	 */
	void removeItem(R root, L item);

	/**
	 * Return the label for the root element.
	 * 
	 * @return
	 */
	String getRootLabel(R root);

	/**
	 * Return the persistence class for the items of the root element. (eg. NodeImpl, TagImpl...)
	 * 
	 * @return
	 */
	Class<? extends L> getPersistenceClass(R root);

	/**
	 * Return the total count of all tracked elements.
	 * 
	 * @return
	 */
	default long computeCount(R root) {
		return findAll(root).count();
	}

	/**
	 * Return the global count for all elements of the type that are managed by the root element. The count will include all vertices in the of the
	 * specific type.
	 * 
	 * @return
	 */
	long globalCount(R root);

	/**
	 * Get the permissions of a role for this element.
	 *
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	PermissionInfo getRolePermissions(R root, HibBaseElement element, InternalActionContext ac, String roleUuid);

	/**
	 * Return a result for all roles which grant the permission to the element.
	 *
	 * @param element
	 * @param perm
	 * @return
	 */
	Result<? extends HibRole> getRolesWithPerm(R root, HibBaseElement element, InternalPermission perm);

	/**
	 * Delete the element. Additional entries will be added to the batch to keep the search index in sync.
	 *
	 * @param element
	 * @param bac
	 *            Deletion context which keeps track of the deletion process
	 */
	void delete(R root, L element, BulkActionContext bac);

	/**
	 * Update the element using the action context information.
	 *
	 * @param ac
	 * @param batch
	 *            Batch to which entries will be added in order to update the search index.
	 * @return true if the element was updated. Otherwise false
	 */
	boolean update(R root, L element, InternalActionContext ac, EventQueueBatch batch);
}
