package com.gentics.mesh.context;

import java.util.Set;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.RestModel;
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
	 * Return the latest branch of the project.
	 * 
	 * @return branch
	 */
	default Branch getBranch() {
		return getBranch(null);
	}

	/**
	 * Return the branch that may be specified in this action context as query parameter. This method will fail, if no project is set, or if the specified
	 * branch does not exist for the project When no branch was specified (but a project was set), this will return the latest branch of the project.
	 * 
	 * @param project
	 *            project for overriding the project set in the action context
	 *
	 * @return branch
	 */
	Branch getBranch(Project project);

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

	/**
	 * Check whether the current context is in fact a migration context. The migration context is only used during node migration.
	 * 
	 * @return
	 */
	boolean isMigrationContext();

	/**
	 * Set the webroot response type.
	 * 
	 * @param type
	 */
	void setWebrootResponseType(String type);

	/**
	 * Set the body model. This will effectively override the body model of the actual request and inject the new one.
	 * 
	 * @param model
	 */
	void setBody(Object model);

	/**
	 * Check whether the context allows version purge operations.
	 * 
	 * @return
	 */
	boolean isPurgeAllowed();
}
