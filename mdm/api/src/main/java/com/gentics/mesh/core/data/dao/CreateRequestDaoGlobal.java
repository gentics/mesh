package com.gentics.mesh.core.data.dao;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.CreateRequest;
import com.gentics.mesh.core.rest.common.ListRequest;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A DAO, that allows batch operations.
 * 
 * @param <C>
 * @param <R>
 * @param <L>
 */
public interface CreateRequestDaoGlobal<C extends CreateRequest, T extends HibBaseElement> extends DaoGlobal<T> {

	/**
	 * Create a leaf object using an explicit request.
	 * 
	 * @param request
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	T create(C request, InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Batch create leaf objects from an action context.
	 */
	default List<T> createBatch(InternalActionContext ac, EventQueueBatch batch) {
		ListRequest<C> request = ac.fromJson(ListRequest.class);
		return request.getData().stream().map(item -> create(item, ac, batch, item.getUuid())).collect(Collectors.toList());
	}
}

