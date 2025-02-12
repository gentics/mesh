package com.gentics.mesh.core.endpoint.admin;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoHandler;
import com.gentics.mesh.core.endpoint.admin.plugin.PluginHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * The default impl of an admin endpoint
 */
public class AdminEndpointImpl extends AdminEndpoint {

	public AdminEndpointImpl() {
		super();
	}

	public AdminEndpointImpl(MeshAuthChain chain, AdminHandler adminHandler, JobHandler jobHandler,
			ConsistencyCheckHandler consistencyHandler, PluginHandler pluginHandler, DebugInfoHandler debugInfoHandler,
			LocalConfigHandler localConfigHandler, ShutdownHandler shutdownHandler, HandlerUtilities handlerUtilities,
			LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super(chain, adminHandler, jobHandler, consistencyHandler, pluginHandler, debugInfoHandler, localConfigHandler,
				shutdownHandler, handlerUtilities, localConfigApi, db, options);
	}	
}
