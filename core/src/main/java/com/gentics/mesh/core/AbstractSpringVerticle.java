package com.gentics.mesh.core;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AbstractVerticle;

/**
 * {@link AbstractSpringVerticle} is the base base class for all mesh verticles. Verticles that extend this class should also be loaded using the vertx spring
 * verticle factory. Using the factory will automatically add this verticle to the spring context and thus autowiring will be possible.
 */
public abstract class AbstractSpringVerticle extends AbstractVerticle {

	public abstract void start() throws Exception;

	@Autowired
	protected MeshSpringConfiguration springConfiguration;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected Database db;

	public void setSpringConfig(MeshSpringConfiguration config) {
		this.springConfiguration = config;
	}

	public MeshSpringConfiguration getSpringConfiguration() {
		return springConfiguration;
	}

}
