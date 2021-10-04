package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
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
public interface DaoGlobal<T extends HibBaseElement> extends Dao<T>, DaoPersistable<T> {

	/**
	 * Load the element by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @return
	 */
	T loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm);

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
	 * Load the element by name.
	 * 
	 * @param name
	 * @return
	 */
	T findByName(String name);

	/**
	 * Delete the element.
	 * 
	 * @param element
	 * @param bac
	 */
	void delete(T element, BulkActionContext bac);

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
	T loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Return the API path.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	String getAPIPath(T element, InternalActionContext ac);

	/**
	 * Find the element globally.
	 * 
	 * @param uuid
	 * @return
	 */
	T findByUuid(String uuid);

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
}
