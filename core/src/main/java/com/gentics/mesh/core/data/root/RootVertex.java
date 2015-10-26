package com.gentics.mesh.core.data.root;

import java.util.List;
import java.util.Stack;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A root vertex is an aggregation vertex that is used to aggregate various basic elements such as users, nodes, groups.
 */
public interface RootVertex<T extends GenericVertex<? extends RestModel>> extends MeshVertex {

	/**
	 * Return a list of all elements.
	 * 
	 * @return
	 */
	List<? extends T> findAll();

	/**
	 * Find the element with the given name.
	 * 
	 * @param name
	 * @return
	 */
	T findByName(String name);

	/**
	 * Find the element with the given uuid and call the result handler.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located.
	 * @param resultHandler
	 *            Handler that is being invoked when search finished.
	 * @return this root vertex - fluent API
	 */
	RootVertex<T> findByUuid(String uuid, Handler<AsyncResult<T>> resultHandler);

	/**
	 * Find the visible elements and return a paged result.
	 * 
	 * @param requestUser
	 *            User that is used to check read permissions against.
	 * @param pagingInfo
	 *            Paging information object that contains page options.
	 * @return
	 * @throws InvalidArgumentException
	 *             if the paging options are malformed.
	 */
	Page<? extends T> findAll(MeshAuthUser requestUser, PagingParameter pagingInfo) throws InvalidArgumentException;

	void resolveToElement(Stack<String> stack, Handler<AsyncResult<? extends MeshVertex>> resultHandler);

	/**
	 * Create a new object within this aggregation vertex.
	 * 
	 * @param ac
	 * @param handler
	 */
	void create(InternalActionContext ac, Handler<AsyncResult<T>> handler);

	/**
	 * Find the object by uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	T findByUuidBlocking(String uuid);

}
