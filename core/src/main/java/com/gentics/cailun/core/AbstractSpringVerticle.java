package com.gentics.cailun.core;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.service.I18NService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

import io.vertx.core.AbstractVerticle;

public abstract class AbstractSpringVerticle extends AbstractVerticle {

	public abstract void start() throws Exception;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;
	
	@Autowired
	protected I18NService i18n;

	public void setSpringConfig(CaiLunSpringConfiguration config) {
		this.springConfig = config;
	}

}
