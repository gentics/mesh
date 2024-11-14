package com.gentics.mesh.core.data.dao;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.common.CreateRequest;
import com.gentics.mesh.core.rest.common.ListRequest;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A DAO, that allows batch operations.
 * 
 * @param <C>
 * @param <R>
 * @param <L>
 */
public interface CreateRequestRootDao<C extends CreateRequest, R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> extends RootDao<R, L> {

	/**
	 * Create a leaf object using an explicit request.
	 * 
	 * @param request
	 * @param root
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	L create(C request, R root, InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Batch create leaf objects from an action context.
	 */
	default List<L> createBatch(R root, InternalActionContext ac, EventQueueBatch batch) {
		ListRequest<C> request = ac.fromJson(ListRequest.class);
		return request.getData().stream().map(item -> create(item, root, ac, batch, item.getUuid())).collect(Collectors.toList());
	}
}
