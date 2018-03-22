package com.gentics.mesh.plugin.rest;

import io.vertx.ext.web.Router;

public abstract class AbstractRestExtension implements RestExtension {

	private RestExtensionScope scope;

	private String name;

	private Router router;

	public AbstractRestExtension(RestExtensionScope scope, String name) {
		this.scope = scope;
		this.name = name;
	}

	@Override
	public Router router() {
		return router;
	}

	@Override
	public RestExtensionScope scope() {
		return scope;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public void init(Router router) {
		this.router = router;
		start();
	}

	@Override
	abstract public void start();

}
