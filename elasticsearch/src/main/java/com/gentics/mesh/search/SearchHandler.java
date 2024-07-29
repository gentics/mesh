package com.gentics.mesh.search;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
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
public interface SearchHandler<T extends HibCoreElement<?>, RM extends RestModel> {

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
	Page<? extends T> query(InternalActionContext ac, String query, PagingParameters pagingInfo, InternalPermission... permissions)
		throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException;

	/**
	 * Invoke the query and response to the requester via the action context.
	 *
	 * @param ac
	 * @param elementLoader
	 *            Loader function which can return graph elements for uuids
	 * @param classOfRL
	 *            Class of the rest model list that should be used when creating the response
	 * @param indices
	 *            Names of indices which should be searched
	 * @param filterByLanguage
	 *            Whether to add the language term filter (Usually only needed for node queries)
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvalidArgumentException
	 * @throws MeshJsonException
	 * @throws MeshConfigurationException
	 */
	<RL extends ListResponse<RM>> void query(InternalActionContext ac, Function<String, T> elementLoader, Class<RL> classOfRL,
		boolean filterByLanguage)
		throws InstantiationException, IllegalAccessException, InvalidArgumentException, MeshJsonException, MeshConfigurationException;

	/**
	 * Invoke a raw query which will not post process the search result. Instead the result of the search provider will directly be returned.
	 * 
	 * @param ac
	 */
	void rawQuery(InternalActionContext ac);

}
