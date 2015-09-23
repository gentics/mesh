package com.gentics.mesh.handler;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.handler.impl.InternalHttpActionContextImpl;

import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

public interface InternalActionContext extends ActionContext {

	public static InternalActionContext create(RoutingContext rc) {
		return new InternalHttpActionContextImpl(rc);
	}

	void setUser(User user);

	Project getProject();

	MeshAuthUser getUser();

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @return List of languages. List can be empty.
	 */
	List<String> getSelectedLanguageTags();

	/**
	 * Extract the paging information from the request parameters. The paging information contains information about the number of the page that is currently
	 * requested and the amount of items that should be included in a single page.
	 * 
	 * @return Paging information
	 */
	PagingInfo getPagingInfo();

}
