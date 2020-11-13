package com.gentics.mesh.router;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.db.GlueDatabase;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.handler.VersionHandler;

import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * Central storage for all Vert.x web request routers.
 * 
 * Structure:
 * 
 * <pre>
 * {@code
 * ROOT_ROUTER(:coreRouter) -> customRouters
 *                          -> apiRouters -> apiSubRouter (eg: /users.., /roles..)
 *                          -> projectRouters (eg: /Dummy/nodes)
 * }
 * </pre>
 * 
 * Project routers are automatically bound to all projects. This way only a single node verticle is needed to handle all project requests.
 * 
 */
public interface RouterStorage {

	RootRouter root();

	MeshAuthChain getAuthChain();

	VersionHandler getVersionHandler();

	void registerEventbusHandlers();

	/**
	 * Return the injected request delegator handler.
	 * 
	 * @return
	 */
	RequestDelegator getDelegator();

	BodyHandler getBodyHandler();

	CorsHandler getCorsHandler();

	GlueDatabase getDb();

}
