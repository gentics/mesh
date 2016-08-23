package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;

public class AbstractHandler {

	protected RouterStorage routerStorage;

	protected BootstrapInitializer boot;

	protected MeshSpringConfiguration springConfiguration;

	protected Database db;

	protected Vertx vertx = Mesh.vertx();

	@Inject
	public AbstractHandler(Database db, MeshSpringConfiguration springConfiguration, BootstrapInitializer boot, RouterStorage routerStorage) {
		this.db = db;
		this.springConfiguration = springConfiguration;
		this.boot = boot;
		this.routerStorage = routerStorage;
	}

	protected void validateParameter(String value, String name) {
		if (StringUtils.isEmpty(value)) {
			throw error(BAD_REQUEST, "error_request_parameter_missing", name);
		}
	}
}
