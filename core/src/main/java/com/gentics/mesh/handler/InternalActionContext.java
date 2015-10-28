package com.gentics.mesh.handler;

import java.util.List;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.handler.impl.InternalHttpActionContextImpl;
import com.gentics.mesh.query.impl.PagingParameter;

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
	 * @return
	 */
	String getRolePermisssionParameter();

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
	public <T> Handler<AsyncResult<T>> errorHandler();

}
