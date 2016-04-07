package com.gentics.mesh.context;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.query.impl.ImageManipulationParameter;
import com.gentics.mesh.query.impl.NavigationRequestParameter;
import com.gentics.mesh.query.impl.PagingParameter;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

/**
 * A internal action context exposes various internal method which a api action context would normally not dare to expose.
 */
public interface InternalActionContext extends ActionContext {

	public static InternalActionContext create(RoutingContext rc) {
		return new InternalRoutingActionContextImpl(rc);
	}

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
	 * Return the release that may be specified in this action context as query parameter.
	 * This method will fail, if no project is set, or if the specified release does not exist for the project
	 * When no release was specified (but a project was set), this will return the latest release of the project
	 * @param project project for overriding the project set in the action context
	 *
	 * @return release
	 */
	Release getRelease(Project project);

	/**
	 * Return the requested version. If the requested version is neither "draft" nor "published" nor a valid version number, an
	 * error will be raised.
	 * If no specific version number was requested, this will return "published"
	 * @return requested version number or "published"
	 */
	String getVersion();

	/**
	 * Set the requested version, overriding the version requested in the wrapped RoutingContext
	 * @param version requested version
	 * @return fluent API
	 */
	InternalActionContext setVersion(String version);

	/**
	 * Return the mesh auth user.
	 * 
	 * @return
	 */
	MeshAuthUser getUser();

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @return List of languages. List can be empty.
	 */
	List<String> getSelectedLanguageTags();

	/**
	 * Return the role parameter value.
	 * 
	 * @return parameter value or null when no parameter was set
	 */
	String getRolePermissionParameter();

	/**
	 * Extract the paging parameter from the request parameters. The paging information contains information about the number of the page that is currently
	 * requested and the amount of items that should be included in a single page.
	 * 
	 * @return Paging parameter
	 */
	PagingParameter getPagingParameter();

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
	 * Return the <code>expandAll</code> query parameter flag value.
	 * 
	 * @return
	 */
	boolean getExpandAllFlag();

	/**
	 * Return the <code>resolveLinks</code> query parameter value. This will never return null
	 * 
	 * @return
	 */
	WebRootLinkReplacer.Type getResolveLinksType();

	/**
	 * Return the image request (crop/resize) parameters.
	 * 
	 * @return
	 */
	ImageManipulationParameter getImageRequestParameter();

	/**
	 * Return the navigation specific request parameters.
	 * 
	 * @return
	 */
	NavigationRequestParameter getNavigationRequestParameter();

	/**
	 * Transform the rest model to JSON and send the JSON as a respond with the given status code.
	 * 
	 * @param result
	 * @param status
	 */
	void respond(RestModel result, HttpResponseStatus status);

	/**
	 * Return a list of field name that should be expanded.
	 * 
	 * @return List of fields that should be expanded
	 */
	List<String> getExpandedFieldnames();

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

}
