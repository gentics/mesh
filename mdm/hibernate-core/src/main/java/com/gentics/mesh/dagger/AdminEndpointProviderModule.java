package com.gentics.mesh.dagger;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.AdminEndpoint;
import com.gentics.mesh.core.endpoint.admin.AdminEndpointImpl;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.JobHandler;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.endpoint.admin.LocalConfigHandler;
import com.gentics.mesh.core.endpoint.admin.ShutdownHandler;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoHandler;
import com.gentics.mesh.core.endpoint.admin.plugin.PluginHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;

import dagger.Module;
import dagger.Provides;

@Module
public class AdminEndpointProviderModule {

	@Provides
	public static AdminEndpoint provideAdminEndpoint(MeshAuthChain chain, AdminHandler adminHandler, JobHandler jobHandler,
			ConsistencyCheckHandler consistencyHandler, PluginHandler pluginHandler, DebugInfoHandler debugInfoHandler,
			LocalConfigHandler localConfigHandler, ShutdownHandler shutdownHandler, HandlerUtilities handlerUtilities,
			LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		return new AdminEndpointImpl(chain, adminHandler, jobHandler, consistencyHandler, pluginHandler, debugInfoHandler, localConfigHandler,
				shutdownHandler, handlerUtilities, localConfigApi, db, options);
	}
}
