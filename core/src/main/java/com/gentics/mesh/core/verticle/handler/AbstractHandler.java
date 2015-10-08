package com.gentics.mesh.core.verticle.handler;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;

public class AbstractHandler {

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected MeshSpringConfiguration springConfiguration;

	@Autowired
	protected Database db;

	protected Vertx vertx = Mesh.vertx();

}
