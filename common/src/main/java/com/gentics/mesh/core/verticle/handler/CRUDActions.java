package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * The CRUD Actions class contains context aware CRUD code. The implementation is utilizing the provided context information to apply CRUD on the specific
 * element.
 * 
 * The tag CRUD action is for example aware of the tagFamilyUuid in the context parameters and uses the project in the context to scope the operation correctly.
 * 
 * @param <T>
 * @param <RM>
 */
public interface CRUDActions<T extends HibElement, RM extends RestModel> {

	T load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	TransformablePage<? extends T> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo);

	boolean update(Tx tx, T element, InternalActionContext ac, EventQueueBatch batch);

	T create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid);

	void delete(Tx tx, T element, BulkActionContext bac);

	RM transformToRestSync(Tx tx, T element, InternalActionContext ac);
}
