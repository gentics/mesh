package com.gentics.mesh.router;

import io.vertx.ext.web.Router;

public interface PluginRouter {

	void removeRouter(String name);

	void addRouter(String name, Router pluginRouter);

}
