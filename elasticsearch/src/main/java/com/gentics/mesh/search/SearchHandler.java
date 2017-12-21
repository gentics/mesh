package com.gentics.mesh.search;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * 
 * @param <T>
 *            Underlying type of graph elements which the search would locate.
 */
public interface SearchHandler<T extends MeshCoreVertex<RM, T>, RM extends RestModel> {

	/**
	 * Invoke an elastic search query on the database and return a page which lists the found elements.
	 * 
	 * @param ac
	 * @param query
	 *            Elasticsearch query
	 * @param pagingInfo
	 *            Paging settings
	 * @param permissions
	 *            Permissions to check against
	 * @return
	 * @throws MeshConfigurationException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	Page<? extends T> query(InternalActionContext ac, String query, PagingParameters pagingInfo, GraphPermission... permissions)
			throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException;

	/**
	 * Invoke the query and response to the requester via the action context.
	 *
	 * @param ac
	 * @param rootVertex
	 *            Root Vertex of the elements that should be searched
	 * @param classOfRL
	 *            Class of the rest model list that should be used when creating the response
	 * @param indices
	 *            Names of indices which should be searched
	 * @param permission
	 *            required permission
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvalidArgumentException
	 * @throws MeshJsonException
	 * @throws MeshConfigurationException
	 */
	<RL extends ListResponse<RM>> void query(InternalActionContext ac, Supplier<RootVertex<T>> rootVertex, Class<RL> classOfRL)
			throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException, MeshConfigurationException;

	/**
	 * Invoke a raw query which will not post process the search result. Instead the result of the search provider will directly be returned.
	 * 
	 * @param ac
	 */
	void rawQuery(InternalActionContext ac);

}
