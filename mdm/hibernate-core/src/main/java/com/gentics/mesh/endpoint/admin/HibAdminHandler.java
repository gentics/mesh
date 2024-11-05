package com.gentics.mesh.endpoint.admin;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_CACHES;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.Vertx;

/**
 * An implementation of administration routes handler. The primary place for the admin functionality extension.
 * 
 * @author plyhun
 *
 */
public class HibAdminHandler extends AdminHandler {

	private final ContentCachedStorage contentCache;

	public HibAdminHandler(Vertx vertx, Database db, RouterStorageImpl routerStorage, BootstrapInitializer boot,
			SearchProvider searchProvider, HandlerUtilities utils, MeshOptions options,
			RouterStorageRegistryImpl routerStorageRegistry, Coordinator coordinator, WriteLock writeLock,
			ConsistencyCheckHandler consistencyCheckHandler, CacheRegistry cacheRegistry, ContentCachedStorage contentCache) {
		super(vertx, db, routerStorage, boot, searchProvider, utils, options, routerStorageRegistry, coordinator, writeLock, consistencyCheckHandler, cacheRegistry);
		this.contentCache = contentCache;
	}

	@Override
	public void handleCacheClear(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			HibUser user = ac.getUser();
			if (user != null && !user.isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}

			cacheRegistry.clear();
			contentCache.evictAll();
			vertx.eventBus().publish(CLEAR_CACHES.address, null);

			return message(ac, "cache_clear_invoked");
		}, model -> ac.send(model, OK));
	}
}
