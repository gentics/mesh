package com.gentics.mesh.core.verticle.handler;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractCrudHandler {

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected Database db;

	protected Vertx vertx = Mesh.vertx();

	abstract public void handleCreate(RoutingContext rc);

	abstract public void handleDelete(RoutingContext rc);

	abstract public void handleUpdate(RoutingContext rc);

	abstract public void handleRead(RoutingContext rc);

	abstract public void handleReadList(RoutingContext rc);

}
