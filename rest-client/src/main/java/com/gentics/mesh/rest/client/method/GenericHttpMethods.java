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

	MeshRequest<JsonObject> get(String path);

	<R> MeshRequest<R> get(String path, Class<R> responseClass);

	/* POST */

	MeshRequest<JsonObject> post(String path);

	MeshRequest<JsonObject> post(String path, JsonObject body);

	<R> MeshRequest<R> post(String path, Class<R> responseClass);

	<R, T extends RestModel> MeshRequest<R> post(String path, T request, Class<R> responseClass);

	/* PUT */

	MeshRequest<JsonObject> put(String path);

	MeshRequest<JsonObject> put(String path, JsonObject body);

	<R> MeshRequest<R> put(String path, Class<R> responseClass);

	<R, T extends RestModel> MeshRequest<R> put(String path, T request, Class<R> responseClass);

	/* DELETE */

	MeshRequest<JsonObject> delete(String path);

	MeshRequest<EmptyResponse> deleteEmpty(String path);

	<R> MeshRequest<R> delete(String path, Class<R> responseClass);

}
