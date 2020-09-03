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
public interface DAOActions<T extends HibCoreElement, RM extends RestModel> extends LoadAllAction<T> {

	T create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid);

	T loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound);

	T loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound);

	Page<? extends T> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<T> extraFilter);

	boolean update(Tx tx, T element, InternalActionContext ac, EventQueueBatch batch);

	void delete(Tx tx, T element, BulkActionContext bac);

	RM transformToRestSync(Tx tx, T element, InternalActionContext ac, int level, String... languageTags);

	String getETag(Tx tx, InternalActionContext ac, T element);

	String getAPIPath(Tx tx, InternalActionContext ac, T element);

}
