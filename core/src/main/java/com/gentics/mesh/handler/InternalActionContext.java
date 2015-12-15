package com.gentics.mesh.handler;

import java.util.List;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.impl.InternalHttpActionContextImpl;
import com.gentics.mesh.query.impl.ImageManipulationParameter;
import com.gentics.mesh.query.impl.PagingParameter;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * A internal action context exposes various internal method which a api action context would normally not dare to expose.
 */
public interface InternalActionContext extends ActionContext {

	public static InternalActionContext create(RoutingContext rc) {
		return new InternalHttpActionContextImpl(rc);
	}

	/**
	 * Set the user to the context.
	 * 
	 * @param user
	 */
	void setUser(User user);

	/**
	 * Return the project that may be set when this action context is used for a project specific request (eg. /api/v1/dummy/nodes..)
	 * 
	 * @return
	 */
	Project getProject();

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
	 * Send a generic message response with the given message.
	 * 
	 * @param status
	 * @param i18nMessage
	 * @param i18nParameters
	 */
	void sendMessage(HttpResponseStatus status, String i18nMessage, String... i18nParameters);

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
	 * Return the <code>resolveLinks</code> query parameter value.
	 * This will never return null
	 * 
	 * @return
	 */
	public WebRootLinkReplacer.Type getResolveLinksType();

	/**
	 * Return the image request (crop/resize) parameter.
	 * 
	 * @return
	 */
	public ImageManipulationParameter getImageRequestParameter();

}
