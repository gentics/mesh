package com.gentics.mesh.rest;

import java.util.Arrays;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The mesh response handler will take care of delegating the returned JSON to the correct JSON deserializer.
 * 
 * @param <T>
 */
public class MeshResponseHandler<T> implements Handler<HttpClientResponse> {

	private static final Logger log = LoggerFactory.getLogger(MeshResponseHandler.class);

	private Future<T> future;
	private Class<T> classOfT;
	private Handler<HttpClientResponse> handler;
	private HttpMethod method;
	private SchemaStorage schemaStorage;
	private String uri;

	/**
	 * Create a new response handler.
	 * 
	 * @param classOfT
	 *            Expected response POJO class
	 * @param method
	 *            Method that was used for the request
	 * @param uri
	 *            Uri that was queried
	 * @param schemaStorage
	 *            A filled schema storage
	 */
	public MeshResponseHandler(Class<T> classOfT, HttpMethod method, String uri, SchemaStorage schemaStorage) {
		this.classOfT = classOfT;
		this.future = Future.future();
		this.method = method;
		this.uri = uri;
		this.schemaStorage = schemaStorage;
	}

	@Override
	public void handle(HttpClientResponse response) {
		int code = response.statusCode();
		if (code >= 200 && code < 300) {

			String contentType = response.getHeader("Content-Type");
			//FIXME TODO in theory it would also be possible that a customer uploads JSON into mesh. In those cases we would also need to return it directly (without parsing)
			if (contentType.startsWith(HttpConstants.APPLICATION_JSON)) {
				response.bodyHandler(bh -> {
					String json = bh.toString();
					if (log.isDebugEnabled()) {
						log.debug(json);
					}
					try {
						// Hack to fallback to node responses when dealing with object classes
						if (classOfT.equals(Object.class)) {
							NodeResponse restObj = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
							future.complete((T) restObj);
						} else if (isSchemaClass(classOfT)) {
							T restObj = JsonUtil.readSchema(json, classOfT);
							future.complete(restObj);
						} else if (isNodeClass(classOfT) || isUserListClass(classOfT) || isNodeListClass(classOfT) || isUserClass(classOfT)) {
							T restObj = JsonUtil.readNode(json, classOfT, schemaStorage);
							future.complete(restObj);
						} else {
							T restObj = JsonUtil.readValue(json, classOfT);
							future.complete(restObj);
						}
					} catch (Exception e) {
						log.error("Failed to deserialize json to class {" + classOfT + "}", e);
						future.fail(e);
					}
				});
			} else if (classOfT.equals(String.class)) {
				// if client requested a String, we just return the buffer as string
				response.bodyHandler(buffer -> {
					future.complete((T) buffer.toString());
				});
			} else {
				NodeDownloadResponse downloadResponse = new NodeDownloadResponse();
				downloadResponse.setContentType(contentType);
				String disposition = response.getHeader("content-disposition");
				String filename = disposition.substring(disposition.indexOf("=") + 1);
				downloadResponse.setFilename(filename);

				response.bodyHandler(buffer -> {
					downloadResponse.setBuffer(buffer);
					future.complete((T) downloadResponse);
				});
			}
		} else {
			response.bodyHandler(bh -> {
				String json = bh.toString();
				if (log.isDebugEnabled()) {
					log.debug(json);
				}

				log.error("Request failed with statusCode {" + response.statusCode() + "} statusMessage {" + response.statusMessage() + "} {" + json
						+ "} for method {" + this.method + "} and uri {" + this.uri + "}");

				GenericMessageResponse responseMessage = null;
				try {
					responseMessage = JsonUtil.readValue(json, GenericMessageResponse.class);
				} catch (Exception e) {
					if (log.isDebugEnabled()) {
						log.debug("Could not deserialize response {" + json + "}.", e);
					}
					responseMessage = new GenericMessageResponse(json);
				}
				future.fail(new MeshRestClientHttpException(response.statusCode(), response.statusMessage(), responseMessage));
			});
		}
		if (handler != null) {
			handler.handle(response);
		}

	}

	/**
	 * Check whether the given class is a user rest POJO class.
	 * 
	 * @param clazz
	 * @return
	 */
	private boolean isUserClass(Class<T> clazz) {
		if (clazz.isAssignableFrom(UserResponse.class) || clazz.isAssignableFrom(UserCreateRequest.class)
				|| clazz.isAssignableFrom(UserUpdateRequest.class)) {
			return true;
		}
		return false;
	}

	/**
	 * Check whether the given class is a user rest list POJO class.
	 * 
	 * @param clazz
	 * @return
	 */
	private boolean isUserListClass(Class<?> clazz) {
		if (clazz.isAssignableFrom(UserListResponse.class)) {
			return true;
		}
		return false;
	}

	/**
	 * Check whether the given class is a node rest list POJO class.
	 * 
	 * @param clazz
	 * @return
	 */
	private boolean isNodeListClass(Class<?> clazz) {
		if (clazz.isAssignableFrom(NodeListResponse.class)) {
			return true;
		}
		return false;
	}

	/**
	 * Check whether the given class is a schema rest POJO class.
	 * 
	 * @param clazz
	 * @return
	 */
	private boolean isSchemaClass(Class<?> clazz) {
		return Arrays
				.asList(SchemaResponse.class, SchemaCreateRequest.class, SchemaUpdateRequest.class, SchemaListResponse.class,
						MicroschemaResponse.class, MicroschemaCreateRequest.class, MicroschemaUpdateRequest.class, MicroschemaListResponse.class)
				.stream().anyMatch(c -> clazz.isAssignableFrom(c));
	}

	/**
	 * Check whether the given class is a node rest POJO class.
	 * 
	 * @param clazz
	 * @return
	 */
	private boolean isNodeClass(Class<?> clazz) {
		if (clazz.isAssignableFrom(NodeUpdateRequest.class) || clazz.isAssignableFrom(NodeResponse.class)
				|| clazz.isAssignableFrom(NodeCreateRequest.class) || clazz.isAssignableFrom(NavigationResponse.class)) {
			return true;
		}
		return false;
	}

	public Future<T> getFuture() {
		return future;
	}

	/**
	 * Handle the client response.
	 * 
	 * @param handler
	 */
	public void handle(Handler<HttpClientResponse> handler) {
		this.handler = handler;
	}

}
