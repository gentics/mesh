package com.gentics.mesh.core;

import io.vertx.core.AbstractVerticle;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public abstract class AbstractSpringVerticle extends AbstractVerticle {

	public abstract void start() throws Exception;

	@Autowired
	protected FramedThreadedTransactionalGraph fg;

	@Autowired
	protected MeshSpringConfiguration springConfiguration;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected RoutingContextService rcs;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected I18NService i18n;

	public void setSpringConfig(MeshSpringConfiguration config) {
		this.springConfiguration = config;
	}

	public RoutingContextService getRcs() {
		return rcs;
	}

}
