package com.gentics.cailun.core;

import io.vertx.core.AbstractVerticle;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.service.I18NService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.RouterStorage;

public abstract class AbstractSpringVerticle extends AbstractVerticle {

	public abstract void start() throws Exception;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected GraphDatabaseService graphDb;

	public void setSpringConfig(CaiLunSpringConfiguration config) {
		this.springConfig = config;
	}

}
