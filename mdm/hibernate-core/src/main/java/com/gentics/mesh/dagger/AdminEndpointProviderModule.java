package com.gentics.mesh.dagger;

import javax.inject.Singleton;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
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
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.endpoint.admin.HibAdminHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.SearchProvider;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;

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

	@Provides
	@Singleton
	public static AdminHandler provideAdminHandler(Vertx vertx, Database db, RouterStorageImpl routerStorage, BootstrapInitializer boot,
			SearchProvider searchProvider, HandlerUtilities utils, MeshOptions options,
			RouterStorageRegistryImpl routerStorageRegistry, Coordinator coordinator, WriteLock writeLock,
			ConsistencyCheckHandler consistencyCheckHandler, CacheRegistry cacheRegistry, ContentCachedStorage contentCache) {
		return new HibAdminHandler(vertx, db, routerStorage, boot, searchProvider, utils, options, routerStorageRegistry, coordinator, writeLock, consistencyCheckHandler, cacheRegistry, contentCache);
	}
}
