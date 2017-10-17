package com.gentics.mesh.search;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * 
 * @param <T>
 *            Underlying type of graph elements which the search would locate.
 */
public interface SearchHandler<T extends MeshCoreVertex<?, T>> {

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

}
