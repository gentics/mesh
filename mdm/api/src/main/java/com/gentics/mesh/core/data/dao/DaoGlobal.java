package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Interface for DAO's which provide methods which allow installation wide queries on elements. The provided methods should not be scoped to a project, branch.
 * 
 * @param <T>
 */
public interface DaoGlobal<T extends HibBaseElement> extends Dao<T> {

	/**
	 * Load the element by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @return
	 */
	default T loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		return loadObjectByUuid(ac, uuid, perm, true);
	}

	/**
	 * Load a page of elements.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load a page of elements.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<T> extraFilter);

	/**
	 * Load a page of elements.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo, FilterOperation<?> extraFilter);

	/**
	 * Load the element by name.
	 * 
	 * @param name
	 * @return
	 */
	T findByName(String name);

	/**
	 * Stream the elements with the given names.
	 * 
	 * @param names
	 *            Names of the elements to be loaded
	 * @return a pair of name and the corresponding element or null value if the element could not be located
	 */
	default Stream<Pair<String, T>> findByNames(Collection<String> names) {
		return names.stream().map(name -> Pair.of(name, findByName(name)));
	}

	/**
	 * Delete the element.
	 * 
	 * @param element
	 * @param bac
	 */
	void delete(T element);

	/**
	 * Update the element.
	 * 
	 * @param element
	 * @param ac
	 * @param batch
	 * @return
	 */
	boolean update(T element, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Load the element via uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	default T loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		T element = findByUuid(uuid);
		return checkPerms(element, uuid, ac, perm, errorIfNotFound);
	}

	/**
	 * Find the element globally.
	 * 
	 * @param uuid
	 * @return
	 */
	T findByUuid(String uuid);

	/**
	 * Stream the elements with the given uuids.
	 * 
	 * @param uuids
	 *            Uuids of the elements to be located
	 * @return a pair of uuid and the corresponding element or null value if the element could not be located
	 */
	default Stream<Pair<String, T>> findByUuids(Collection<String> uuids) {
		return uuids.stream().map(uuid -> Pair.of(uuid, findByUuid(uuid)));
	}

	/**
	 * Return total amount of elements which are stored.
	 * 
	 * @return
	 */
	long count();

	/**
	 * Load all elements.
	 * 
	 * @return
	 */
	Result<? extends T> findAll();

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
	 * Check whether the given element is assigned to this root node.
	 * 
	 * @param element
	 * @return
	 */
	default boolean contains(T element) {
		if (findByUuid(element.getUuid()) == null) {
			return false;
		} else {
			return true;
		}
	}
}
