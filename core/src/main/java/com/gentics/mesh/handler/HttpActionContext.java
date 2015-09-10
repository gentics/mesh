package com.gentics.mesh.handler;

import java.util.Set;

import com.gentics.mesh.handler.impl.HttpActionContextImpl;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public interface HttpActionContext extends ActionContext {

	/**
	 * Create a action context using the routing context in order to extract needed parameters.
	 *
	 * @return the body handler
	 */
	static HttpActionContext create(RoutingContext rc) {
		return new HttpActionContextImpl(rc);
	}

	Set<FileUpload> getFileUploads();

	MultiMap requestHeaders();

}
