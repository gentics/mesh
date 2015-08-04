package com.gentics.mesh.core.verticle.handler;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.etc.RouterStorage;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public abstract class AbstractCRUDHandler {

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected FramedThreadedTransactionalGraph fg;

	protected SearchQueue searchQueue;

	protected MeshRoot meshRoot;

	protected Vertx vertx = Mesh.vertx();

	@PostConstruct
	public void setup() {
		searchQueue = boot.meshRoot().getSearchQueue();
		meshRoot = boot.meshRoot();
	}

	protected Project getProject(RoutingContext rc) {
		return boot.projectRoot().findByName(getProjectName(rc));
	}

	protected String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

	abstract public void handleCreate(RoutingContext rc);

	abstract public void handleDelete(RoutingContext rc);

	abstract public void handleUpdate(RoutingContext rc);

	abstract public void handleRead(RoutingContext rc);

	abstract public void handleReadList(RoutingContext rc);

}
