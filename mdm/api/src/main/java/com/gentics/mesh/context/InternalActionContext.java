package com.gentics.mesh.context;

import java.util.List;

import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.ParameterProviderContext;
import com.gentics.mesh.router.route.SecurityLoggingHandler;
import com.gentics.mesh.shared.SharedKeys;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.spi.logging.LogDelegate;
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
	 * Return the mesh user.
	 * 
	 * @return
	 */
	HibUser getUser();

	/**
	 * Return the mesh auth user.
	 *
	 * @return
	 */
	MeshAuthUser getMeshAuthUser();

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
	 * Return the list of fileuploads that are accessible through the context.
	 * 
	 * @return
	 */
	List<FileUpload> getFileUploads();

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

	/**
	 * Return the requested API version.
	 * 
	 * @return
	 */
	default int getApiVersion() {
		return get(SharedKeys.API_VERSION_CONTEXT_KEY);
	}

	/**
	 * A logger which also logs additional user information. Should be used for logging security related messages.
	 * 
	 * @return
	 */
	default LogDelegate getSecurityLogger() {
		return get(SecurityLoggingHandler.SECURITY_LOGGER_CONTEXT_KEY);
	}

	/**
	 * Check whether the write lock should be skipped.
	 * 
	 * @return
	 */
	boolean isSkipWriteLock();

	/**
	 * Set a flag to skip the write lock.
	 * 
	 * @return
	 */
	InternalActionContext skipWriteLock();

	/**
	 * Check whether the context provides a user which is admin.
	 * 
	 * @return
	 */
	boolean isAdmin();

	/**
	 * Check if the content is requested to be minified.
	 * 
	 * @param config
	 * @return
	 */
	default boolean isMinify(HttpServerConfig config) {
		Boolean localMinify = getDisplayParameters().getMinify();
		return localMinify != null ? localMinify : (config != null && config.isMinifyJson());
	}

	/**
	 * Set the config.
	 * 
	 * @param config
	 */
	void setHttpServerConfig(HttpServerConfig config);
}
