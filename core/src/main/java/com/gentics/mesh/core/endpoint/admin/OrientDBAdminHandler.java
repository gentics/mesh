package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.distributed.coordinator.MasterServer;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.Vertx;

@Singleton
public class OrientDBAdminHandler extends BasicAdminHandler implements ClusterAdminHandler {

	protected final Coordinator coordinator;
	
	@Inject
	public OrientDBAdminHandler(Vertx vertx, Database db, RouterStorageImpl routerStorage, BootstrapInitializer boot,
			SearchProvider searchProvider, HandlerUtilities utils, MeshOptions options,
			RouterStorageRegistryImpl routerStorageRegistry, Coordinator coordinator, WriteLock writeLock,
			ConsistencyCheckHandler consistencyCheckHandler) {
		super(vertx, db, routerStorage, boot, searchProvider, utils, options, routerStorageRegistry, writeLock,
				consistencyCheckHandler);
		this.coordinator = coordinator;
	}

	/**
	 * Load information on the currently elected coordination master.
	 * 
	 * @param ac
	 */
	@Override
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
	@Override
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
		return new CoordinatorMasterResponse(name, port, host);
	}

	/**
	 * Return the currently set coordinator config.
	 * 
	 * @param ac
	 */
	@Override
	public void handleLoadCoordinationConfig(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			HibUser user = ac.getUser();
			if (user != null && !user.isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			return coordinator.loadConfig();
		}, model -> ac.send(model, OK));
	}

	/**
	 * Update the coordination configuration.
	 * 
	 * @param ac
	 */
	@Override
	public void handleUpdateCoordinationConfig(InternalActionContext ac) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibUser user = ac.getUser();
				if (user != null && !user.isAdmin()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				CoordinatorConfig request = ac.fromJson(CoordinatorConfig.class);
				coordinator.updateConfig(request);
				return coordinator.loadConfig();
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Load the currently active cluster configuration.
	 * 
	 * @param ac
	 */
	@Override
	public void handleLoadClusterConfig(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			HibUser user = ac.getUser();
			if (user != null && !user.isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			return db.loadClusterConfig();
		}, model -> ac.send(model, OK));
	}

	/**
	 * Update the OrientDB cluster configuration.
	 * 
	 * @param ac
	 */
	@Override
	public void handleUpdateClusterConfig(InternalActionContext ac) {
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				HibUser user = ac.getUser();
				if (user != null && !user.isAdmin()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				ClusterConfigRequest request = ac.fromJson(ClusterConfigRequest.class);
				db.updateClusterConfig(request);
				return db.loadClusterConfig();
			}, model -> ac.send(model, OK));
		}
	}
}
