package com.gentics.mesh.core;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AbstractVerticle;

public abstract class AbstractSpringVerticle extends AbstractVerticle {

	public abstract void start() throws Exception;

	@Autowired
	protected MeshSpringConfiguration springConfiguration;

	@Autowired
	protected RouterStorage routerStorage;


	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected Database database;

	public void setSpringConfig(MeshSpringConfiguration config) {
		this.springConfiguration = config;
	}

}
