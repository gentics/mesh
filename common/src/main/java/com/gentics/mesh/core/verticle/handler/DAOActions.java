package com.gentics.mesh.core.verticle.handler;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
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
 * @param <RM>
 */
public interface DAOActions<T extends HibCoreElement, RM extends RestModel> {

	T load(Tx tx, InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	TransformablePage<? extends T> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo);

	TransformablePage<? extends T> loadAll(Tx tx, InternalActionContext ac, PagingParameters pagingInfo, Predicate<T> extraFilter);

	boolean update(Tx tx, T element, InternalActionContext ac, EventQueueBatch batch);

	T create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid);

	void delete(Tx tx, T element, BulkActionContext bac);

	RM transformToRestSync(Tx tx, T element, InternalActionContext ac, int level, String... languageTags);
}
