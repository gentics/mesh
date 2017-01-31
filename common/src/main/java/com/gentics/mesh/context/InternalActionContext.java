package com.gentics.mesh.context;

import java.util.Set;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.ParameterProviderContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;

/**
 * A internal action context exposes various internal method which an API action context would normally not dare to expose.
 */
public interface InternalActionContext extends ActionContext, ParameterProviderContext {

	/**
	 * Set the user to the context.
	 * 
	 * @param user
	 */
	void setUser(MeshAuthUser user);

	/**
	 * Return the project that may be set when this action context is used for a project specific request (e.g.: /api/v1/dummy/nodes..)
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Return the release that may be specified in this action context as query parameter. This method will fail, if no project is set, or if the specified
	 * release does not exist for the project When no release was specified (but a project was set), this will return the latest release of the project
	 * 
	 * @param project
	 *            project for overriding the project set in the action context
	 *
	 * @return release
	 */
	Release getRelease(Project project);

	/**
	 * Return the mesh auth user.
	 * 
	 * @return
	 */
	MeshAuthUser getUser();

	/**
	 * Return an error handler which is able to fail the call chain.
	 * 
	 * @return
	 */
	<T> Handler<AsyncResult<T>> errorHandler();

	/**
	 * Return the currently used database.
	 * 
	 * @return
	 */
	Database getDatabase();

	/**
	 * Transform the rest model to JSON and send the JSON as a respond with the given status code.
	 * 
	 * @param result
	 * @param status
	 */
	void send(RestModel result, HttpResponseStatus status);

	/**
	 * Finish the request and send the provided respond. No body will be send.
	 * 
	 * @param status
	 */
	void send(HttpResponseStatus status);

	/**
	 * Return the set of fileuploads that are accessible through the context.
	 * 
	 * @return
	 */
	Set<FileUpload> getFileUploads();

	/**
	 * Return the request headers.
	 * 
	 * @return
	 */
	MultiMap requestHeaders();

	/**
	 * Adds a cookie to the response.
	 * 
	 * @param cookie
	 */
	void addCookie(Cookie cookie);

	/**
	 * Set the etag which should be returned in the header.
	 * 
	 * @param entityTag
	 * @param isWeak
	 *            which indicates whether the etag is a weak etag
	 */
	void setEtag(String entityTag, boolean isWeak);

	/**
	 * Set the location header value.
	 * 
	 * @param basePath
	 */
	void setLocation(String basePath);

	/**
	 * Checks whether the provided etag matches the etag within the request header.
	 * 
	 * @param entityTag
	 *            Etag to compare to
	 * @param isWeak
	 *            Provided etag is a weak etag
	 * @return
	 */
	boolean matches(String entityTag, boolean isWeak);

}
