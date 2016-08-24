package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.Mesh;

import io.vertx.core.Vertx;

public class AbstractHandler {

	protected Vertx vertx = Mesh.vertx();

	protected void validateParameter(String value, String name) {
		if (StringUtils.isEmpty(value)) {
			throw error(BAD_REQUEST, "error_request_parameter_missing", name);
		}
	}
}
