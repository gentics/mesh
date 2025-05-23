package com.gentics.mesh.router;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
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

	/**
	 * Return the router.
	 * 
	 * @return
	 */
	RootRouter root();

	/**
	 * Return the authentication handler chain.
	 * 
	 * @return
	 */
	MeshAuthChain getAuthChain();

	/**
	 * Return the version handler.
	 * 
	 * @return
	 */
	VersionHandler getVersionHandler();

	/**
	 * Register on the eventbus in order to update the stored routes. Project routes may need to be removed when a project gets deleted on a different cluster
	 * instance.
	 */
	void registerEventbusHandlers();

	/**
	 * Return the body handler.
	 * 
	 * @return
	 */
	BodyHandler getBodyHandler();

	/**
	 * Return the CORS handler.
	 * 
	 * @return
	 */
	CorsHandler getCorsHandler();

	Database getDb();

	LocalConfigApi getLocalConfigApi();
}
