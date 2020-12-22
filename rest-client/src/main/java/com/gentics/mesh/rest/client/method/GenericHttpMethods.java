package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.vertx.core.json.JsonObject;

/**
 * Set of route agnostic methods which can for example be used to handle plugin requests.
 */
public interface GenericHttpMethods {

	/* GET */

	/**
	 * Create a GET request for the path which returns {@link JsonObject}.
	 * 
	 * @param path
	 * @return
	 */
	MeshRequest<JsonObject> get(String path);

	/**
	 * Create a GET request which returns a model for the provided response class.
	 * 
	 * @param <R>
	 * @param path
	 * @param responseClass
	 * @return
	 */
	<R> MeshRequest<R> get(String path, Class<R> responseClass);

	/* POST */

	/**
	 * Create a post request for the given path.
	 * 
	 * @param path
	 * @return
	 */
	MeshRequest<JsonObject> post(String path);

	/**
	 * Create a POST request for the given path.
	 * 
	 * @param path
	 * @param body
	 *            Payload to be posted
	 * @return
	 */
	MeshRequest<JsonObject> post(String path, JsonObject body);

	/**
	 * Create a POST request for the given path.
	 * 
	 * @param <R>
	 * @param path
	 * @param responseClass
	 * @return
	 */
	<R> MeshRequest<R> post(String path, Class<R> responseClass);

	/**
	 * Create a POST request for the given path.
	 * 
	 * @param <R>
	 * @param <T>
	 * @param path
	 * @param request
	 * @param responseClass
	 * @return
	 */
	<R, T extends RestModel> MeshRequest<R> post(String path, T request, Class<R> responseClass);

	/* PUT */

	/**
	 * Create a PUT request for the given path.
	 * 
	 * @param path
	 * @return
	 */
	MeshRequest<JsonObject> put(String path);

	/**
	 * Create a PUT request for the given path.
	 * 
	 * @param path
	 * @param body
	 * @return
	 */
	MeshRequest<JsonObject> put(String path, JsonObject body);

	/**
	 * Create a PUT request for the given path.
	 * 
	 * @param <R>
	 * @param path
	 * @param responseClass
	 * @return
	 */
	<R> MeshRequest<R> put(String path, Class<R> responseClass);

	/**
	 * Create a PUT request for the given path.
	 * 
	 * @param <R>
	 *            Response type
	 * @param <T>
	 *            Request payload type
	 * @param path
	 *            Path of the request
	 * @param request
	 *            Request payload
	 * @param responseClass
	 *            Response model class
	 * @return
	 */
	<R, T extends RestModel> MeshRequest<R> put(String path, T request, Class<R> responseClass);

	/* DELETE */

	/**
	 * Create a delete request for the path which returns a json.
	 * 
	 * @param path
	 * @return
	 */
	MeshRequest<JsonObject> delete(String path);

	/**
	 * Create a delete request which does not return a body.
	 * 
	 * @param path
	 * @return
	 */
	MeshRequest<EmptyResponse> deleteEmpty(String path);

	/**
	 * Create a delete request which expects a specific response model.
	 * 
	 * @param <R>
	 * @param path
	 * @param responseClass
	 * @return
	 */
	<R> MeshRequest<R> delete(String path, Class<R> responseClass);

}
