package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_CACHES;
import static com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating.INCONSISTENT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML_UTF8;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.distributed.coordinator.MasterServer;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.generator.RAMLGenerator;
import com.gentics.mesh.parameter.BackupParameters;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Vertx;
import io.vertx.core.impl.launcher.commands.VersionCommand;

/**
 * Handler for admin request methods.
 */
public abstract class AdminHandler extends AbstractHandler {

	protected static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	protected final Database db;

	protected final RouterStorageImpl routerStorage;

	protected final BootstrapInitializer boot;

	protected final MeshOptions options;

	protected final SearchProvider searchProvider;

	protected final HandlerUtilities utils;

	protected final Vertx vertx;

	protected final RouterStorageRegistryImpl routerStorageRegistry;

	protected final Coordinator coordinator;

	protected final WriteLock writeLock;

	protected final ConsistencyCheckHandler consistencyCheckHandler;

	protected final CacheRegistry cacheRegistry;

	protected AdminHandler(Vertx vertx, Database db, RouterStorageImpl routerStorage, BootstrapInitializer boot, SearchProvider searchProvider,
		HandlerUtilities utils,
		MeshOptions options, RouterStorageRegistryImpl routerStorageRegistry, Coordinator coordinator, WriteLock writeLock,
		ConsistencyCheckHandler consistencyCheckHandler, CacheRegistry cacheRegistry) {
		this.vertx = vertx;
		this.db = db;
		this.routerStorage = routerStorage;
		this.boot = boot;
		this.searchProvider = searchProvider;
		this.utils = utils;
		this.options = options;
		this.routerStorageRegistry = routerStorageRegistry;
		this.coordinator = coordinator;
		this.writeLock = writeLock;
		this.consistencyCheckHandler = consistencyCheckHandler;
		this.cacheRegistry = cacheRegistry;
	}

	/**
	 * Is backup supported at the current server version?
	 * 
	 * @return
	 */
	public boolean isBackupSupported() {
		return false;
	}

	/**
	 * Handle the mesh status request.
	 * 
	 * @param ac
	 */
	public void handleMeshStatus(InternalActionContext ac) {
		MeshStatusResponse response = new MeshStatusResponse();
		response.setStatus(boot.mesh().getStatus());
		ac.send(response, OK);
	}

	/**
	 * Invoke a database backup call to the current graph database provider.
	 * 
	 * @param ac
	 */
	public void handleBackup(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			BackupParameters params = ac.getBackupParameters();
			if (params.isConsistencyCheck()) {
				log.info("Starting consistency check as requested.");
				ConsistencyCheckResponse result = consistencyCheckHandler.checkConsistency(false, false).runInExistingTx(tx);
				if (result.getResult() == INCONSISTENT) {
					long count = result.getInconsistencies().size();
					throw error(INTERNAL_SERVER_ERROR, "backup_consistency_check_failed", String.valueOf(count));
				}
			}
			backup();
			return message(ac, "backup_finished");
		}, model -> ac.send(model, OK));
	}

	/**
	 * Invoke the graph database backup.
	 * 
	 * @return
	 */
	public String backup() {
		throw error(SERVICE_UNAVAILABLE, "function_not_supported");
	}

	/**
	 * Handle graph restore action.
	 * 
	 * @param ac
	 */
	public void handleRestore(InternalActionContext ac) {
		throw error(SERVICE_UNAVAILABLE, "function_not_supported");
	}

	/**
	 * Handle graph export action.
	 * 
	 * @param ac
	 */
	public void handleExport(InternalActionContext ac) {
		throw error(SERVICE_UNAVAILABLE, "function_not_supported");
	}

	/**
	 * Handle graph import action.
	 * 
	 * @param ac
	 */
	public void handleImport(InternalActionContext ac) {
		throw error(SERVICE_UNAVAILABLE, "function_not_supported");
	}

	/**
	 * Load cluster status information.
	 * 
	 * @param ac
	 */
	public void handleClusterStatus(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			HibUser user = ac.getUser();
			if (user != null && !user.isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			if (options.getClusterOptions() != null && options.getClusterOptions().isEnabled()) {
				return db.clusterManager().getClusterStatus();
			} else {
				throw error(BAD_REQUEST, "error_cluster_status_only_available_in_cluster_mode");
			}
		}, model -> ac.send(model, OK));
	}

	/**
	 * Load the mesh server version information.
	 * 
	 * @param ac
	 */
	public void handleVersions(InternalActionContext ac) {
		MeshServerInfoModel model = getMeshServerInfoModel(ac);
		ac.send(model, OK);
	}

	/**
	 * Populate the mesh server version info.
	 * 
	 * @param ac
	 * @return
	 */
	public MeshServerInfoModel getMeshServerInfoModel(InternalActionContext ac) {
		boolean admin = db.tx(() -> ac.isAdmin());
		MeshServerInfoModel info = new MeshServerInfoModel();
		if (options.getHttpServerOptions().isServerTokens() || admin) {
			db.tx(() -> {
				info.setDatabaseVendor(db.getVendorName());
				info.setDatabaseVersion(db.getVersion());
				info.setDatabaseRevision(db.getDatabaseRevision());
			});
			info.setSearchVendor(searchProvider.getVendorName());
			info.setSearchVersion(searchProvider.getVersion(false));
			info.setMeshVersion(Mesh.getPlainVersion());
			info.setVertxVersion(VersionCommand.getVersion());
			info.setMeshNodeName(options.getNodeName());
			info.setMeshRevision("OSS");
		}
		return info;
	}

	/**
	 * Generate and return the RAML of the server.
	 * 
	 * @param ac
	 */
	public void handleRAML(InternalActionContext ac) {
		boolean admin = db.tx(() -> ac.isAdmin());
		if (admin) {
			RAMLGenerator generator = new RAMLGenerator();
			String raml = generator.generate();
			ac.send(raml, OK, APPLICATION_YAML_UTF8);
		} else {
			throw error(FORBIDDEN, "error_admin_permission_required");
		}
	}

	/**
	 * Load information on the currently elected coordination master.
	 * 
	 * @param ac
	 */
	public void handleLoadCoordinationMaster(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			HibUser user = ac.getUser();
			if (user != null && !user.isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			MasterServer master = coordinator.getMasterMember();
			if (master == null) {
				return message(ac, "error_cluster_coordination_master_not_found");
			}
			return toResponse(master);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Manually set the elected master on the instance which runs this handler.
	 * 
	 * @param ac
	 */
	public void handleSetCoordinationMaster(InternalActionContext ac) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibUser user = ac.getUser();
				if (user != null && !user.isAdmin()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				if (coordinator.isElectable()) {
					coordinator.setMaster();
					return message(ac, "cluster_coordination_master_set");
				} else {
					throw error(BAD_REQUEST, "cluster_coordination_master_set_error_not_electable", options.getNodeName());
				}
			}, model -> ac.send(model, OK));
		}
	}

	private static CoordinatorMasterResponse toResponse(MasterServer server) {
		String name = server.getName();
		String host = server.getHost();
		int port = server.getPort();
		UUID uuid = server.getUuid();
		return new CoordinatorMasterResponse(UUIDUtil.toShortUuid(uuid), name, port, host);
	}

	/**
	 * Clear all internal caches locally and trigger an event to clear the caches cluster wide.
	 * @param ac
	 */
	public void handleCacheClear(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			HibUser user = ac.getUser();
			if (user != null && !user.isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}

			cacheRegistry.clear();
			vertx.eventBus().publish(CLEAR_CACHES.address, null);

			return message(ac, "cache_clear_invoked");
		}, model -> ac.send(model, OK));
	}
}
