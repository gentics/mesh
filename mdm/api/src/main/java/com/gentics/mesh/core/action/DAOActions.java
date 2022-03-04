package com.gentics.mesh.core.action;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * The DAO Actions class contains context aware CRUD code. The implementation is utilizing the provided context information to apply CRUD on the specific
 * element.
 * 
 * The TagDAOAction is for example aware of the tagFamilyUuid in the context parameters and uses the project in the context to scope the operation correctly.
 * 
 * @param <T>
 *            Type of the element that this action targets
 * @param <RM>
 *            Response REST Model for the element of this action
 */
public interface DAOActions<T extends HibCoreElement<RM>, RM extends RestModel> extends LoadAllAction<T> {

	/**
	 * Create the entity with the given context and uuid information.
	 * 
	 * @param tx
	 *            Transaction
	 * @param ac
	 *            Action context which hold request information
	 * @param batch
	 *            Batch to be used to store event data
	 * @param uuid
	 *            UUID for the element creation
	 * @return
	 */
	T create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Load the entity with the given uuid.
	 * 
	 * @param ctx
	 *            Context which holds additional information
	 * @param uuid
	 *            UUID of the element
	 * @param perm
	 *            Permission to check for element loading
	 * @param errorIfNotFound
	 *            Throw an error when the element could not be loaded
	 * @return
	 */
	T loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Load the entity with the given name.
	 * 
	 * @param ctx
	 *            Context which holds additional information
	 * @param name
	 *            Name of the entity
	 * @param perm
	 *            Permission to check for element loading
	 * @param errorIfNotFound
	 *            Throw an error when the element could not be loaded
	 * @return
	 */
	T loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Load all elements which this action covers into a page.
	 * 
	 * @param ctx
	 *            Context of the action
	 * @param pagingInfo
	 *            Paging information to be used when loading a page
	 * @param extraFilter
	 *            Additional filters to be used when loading paged elements
	 * @return
	 */
	Page<? extends T> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<T> extraFilter);

	/**
	 * Update the given element.
	 * 
	 * @param tx
	 * @param element
	 *            Element to be updated
	 * @param ac
	 * @param batch
	 * @return True when the element was updated
	 */
	boolean update(Tx tx, T element, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Delete the given element.
	 * 
	 * @param tx
	 * @param element
	 * @param bac
	 */
	void delete(Tx tx, T element, BulkActionContext bac);

	/**
	 * Transform the given element to a REST model.
	 * 
	 * @param tx
	 *            Transaction
	 * @param element
	 *            Element to be transformed
	 * @param ac
	 *            Action Context
	 * @param level
	 *            Current level of transformation
	 * @param languageTags
	 *            Language tags to be used for language fallback handling
	 * @return
	 */
	RM transformToRestSync(Tx tx, T element, InternalActionContext ac, int level, String... languageTags);

	/**
	 * Return the ETag for the element.
	 * 
	 * @param tx
	 *            Transaction
	 * @param ac
	 *            Action context
	 * @param element
	 *            Element to be used
	 * @return
	 */
	String getETag(Tx tx, InternalActionContext ac, T element);

	/**
	 * Return the API path for the given element.
	 * 
	 * @param tx
	 * @param ac
	 * @param element
	 * @return
	 */
	String getAPIPath(Tx tx, InternalActionContext ac, T element);

}
