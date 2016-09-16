package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.vertx.core.Vertx;

public class AbstractHandler {

	protected Vertx vertx = Mesh.vertx();

	/**
	 * Assert that the parameter has been specified. A {@link GenericRestException} will be thrown if the parameter was not specified.
	 * 
	 * @param value
	 *            Parameter value
	 * @param name
	 *            Parameter name
	 */
	protected void validateParameter(String value, String name) {
		if (StringUtils.isEmpty(value)) {
			throw error(BAD_REQUEST, "error_request_parameter_missing", name);
		}
	}
}
