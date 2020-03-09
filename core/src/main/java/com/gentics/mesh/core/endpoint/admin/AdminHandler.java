package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_EXPORT_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_EXPORT_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_IMPORT_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_IMPORT_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static java.util.Comparator.comparing;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.distributed.coordinator.MasterServer;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for admin request methods.
 */
@Singleton
public class AdminHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	private final Database db;

	private final RouterStorage routerStorage;

	private final BootstrapInitializer boot;

	private final MeshOptions options;

	private final SearchProvider searchProvider;

	private final HandlerUtilities utils;

	private final Vertx vertx;

	private final RouterStorageRegistry routerStorageRegistry;

	private final Coordinator coordinator;

	@Inject
	public AdminHandler(Vertx vertx, Database db, RouterStorage routerStorage, BootstrapInitializer boot, SearchProvider searchProvider,
		HandlerUtilities utils,
		MeshOptions options, RouterStorageRegistry routerStorageRegistry, Coordinator coordinator) {
		this.vertx = vertx;
		this.db = db;
		this.routerStorage = routerStorage;
		this.boot = boot;
		this.searchProvider = searchProvider;
		this.utils = utils;
		this.options = options;
		this.routerStorageRegistry = routerStorageRegistry;
		this.coordinator = coordinator;
	}

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
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			backup();
			return message(ac, "backup_finished");
		}, model -> ac.send(model, OK));
	}

	public String backup() {
		Mesh mesh = boot.mesh();
		MeshStatus oldStatus = mesh.getStatus();
		try {
			vertx.eventBus().publish(GRAPH_BACKUP_START.address, null);
			mesh.setStatus(MeshStatus.BACKUP);
			return db.backupGraph(options.getStorageOptions().getBackupDirectory());
		} catch (GenericRestException e) {
			throw e;
		} catch (Throwable e) {
			log.error("Backup process failed", e);
			throw error(INTERNAL_SERVER_ERROR, "backup_failed", e);
		} finally {
			mesh.setStatus(oldStatus);
			vertx.eventBus().publish(GRAPH_BACKUP_FINISHED.address, null);
		}
	}

	/**
	 * Handle graph restore action.
	 * 
	 * @param ac
	 */
	public void handleRestore(InternalActionContext ac) {
		MeshOptions config = options;
		Mesh mesh = boot.mesh();
		String dir = config.getStorageOptions().getDirectory();
		File backupDir = new File(options.getStorageOptions().getBackupDirectory());
		boolean inMemory = dir == null;

		if (config.getClusterOptions() != null && config.getClusterOptions().isEnabled()) {
			error(SERVICE_UNAVAILABLE, "restore_error_in_cluster_mode");
		}
		if (config.getClusterOptions().isEnabled()) {
			throw error(SERVICE_UNAVAILABLE, "restore_error_in_cluster_mode");
		}
		if (config.getStorageOptions().getStartServer()) {
			throw error(SERVICE_UNAVAILABLE, "restore_error_in_server_mode");
		}
		if (inMemory) {
			throw error(SERVICE_UNAVAILABLE, "restore_error_not_supported_in_memory_mode");
		}
		if (!backupDir.exists()) {
			throw error(INTERNAL_SERVER_ERROR, "error_backup", backupDir.getAbsolutePath());
		}

		db.tx((tx) -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
		});

		// Find the file which was last modified
		File latestFile = Arrays.asList(backupDir.listFiles()).stream().filter(file -> file.getName().endsWith(".zip"))
			.sorted(comparing(File::lastModified)).reduce((first, second) -> second).orElseGet(() -> null);
		if (latestFile == null) {
			throw error(INTERNAL_SERVER_ERROR, "error_backup", backupDir.getAbsolutePath());
		}
		MeshStatus oldStatus = mesh.getStatus();
		Completable.fromAction(() -> {
			mesh.setStatus(MeshStatus.RESTORE);
			vertx.eventBus().publish(GRAPH_RESTORE_START.address, null);
			db.stop();
			db.restoreGraph(latestFile.getAbsolutePath());
			db.setupConnectionPool();
			boot.globalCacheClear();
			boot.clearReferences();
			routerStorage.root().apiRouter().projectsRouter().getProjectRouters().clear();
		}).andThen(db.asyncTx(() -> {
			// Update the routes by loading the projects
			initProjects();
			return Single.just(message(ac, "restore_finished"));
		})).doFinally(() -> {
			mesh.setStatus(oldStatus);
			vertx.eventBus().publish(GRAPH_RESTORE_FINISHED.address, null);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * The projects share various subrouters. This method will add the subrouters for all registered projects.
	 *
	 * @throws InvalidNameException
	 */
	private void initProjects() throws InvalidNameException {
		for (Project project : boot.meshRoot().getProjectRoot().findAll()) {
			routerStorageRegistry.addProject(project.getName());
			if (log.isDebugEnabled()) {
				log.debug("Initalized project {" + project.getName() + "}");
			}
		}
	}

	/**
	 * Handle graph export action.
	 * 
	 * @param ac
	 */
	public void handleExport(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			String exportDir = options.getStorageOptions().getExportDirectory();
			log.debug("Exporting graph to {" + exportDir + "}");
			vertx.eventBus().publish(GRAPH_EXPORT_START.address, null);
			db.exportGraph(exportDir);
			vertx.eventBus().publish(GRAPH_EXPORT_FINISHED.address, null);
			return message(ac, "export_finished");
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle graph import action.
	 * 
	 * @param ac
	 */
	public void handleImport(InternalActionContext ac) {
		try (Tx tx = db.tx()) {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
		}
		File importsDir = new File(options.getStorageOptions().getExportDirectory());

		// Find the file which was last modified
		File latestFile = Arrays.asList(importsDir.listFiles()).stream().filter(file -> file.getName().endsWith(".gz"))
			.sorted(comparing(File::lastModified)).reduce((first, second) -> second).orElseGet(() -> null);
		try {

			vertx.eventBus().publish(GRAPH_IMPORT_START.address, null);
			db.importGraph(latestFile.getAbsolutePath());
			boot.globalCacheClear();
			vertx.eventBus().publish(GRAPH_IMPORT_FINISHED.address, null);

			Single.just(message(ac, "import_finished")).subscribe(model -> ac.send(model, OK), ac::fail);
		} catch (IOException e) {
			ac.fail(e);
		}
	}

	public void handleClusterStatus(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			User user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			if (options.getClusterOptions() != null && options.getClusterOptions().isEnabled()) {
				return db.clusterManager().getClusterStatus();
			} else {
				throw error(BAD_REQUEST, "error_cluster_status_only_available_in_cluster_mode");
			}
		}, model -> ac.send(model, OK));
	}

	public void handleVersions(InternalActionContext ac) {
		ac.send(getMeshServerInfoModel(), OK);
	}

	public MeshServerInfoModel getMeshServerInfoModel() {
		MeshServerInfoModel info = new MeshServerInfoModel();
		info.setDatabaseVendor(db.getVendorName());
		info.setDatabaseVersion(db.getVersion());
		info.setSearchVendor(searchProvider.getVendorName());
		info.setSearchVersion(searchProvider.getVersion());
		info.setMeshVersion(Mesh.getPlainVersion());
		info.setMeshNodeName(options.getNodeName());
		info.setVertxVersion(VersionCommand.getVersion());
		info.setDatabaseRevision(db.getDatabaseRevision());
		return info;
	}

	public void handleLoadClusterConfig(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			User user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			return db.loadClusterConfig();
		}, model -> ac.send(model, OK));
	}

	public void handleUpdateClusterConfig(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			User user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			ClusterConfigRequest request = ac.fromJson(ClusterConfigRequest.class);
			db.updateClusterConfig(request);
			return db.loadClusterConfig();
		}, model -> ac.send(model, OK));
	}

	public void handleLoadCoordinationMaster(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			User user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			MasterServer master = coordinator.getMasterMember();
			if (master == null) {
				return message(ac, "error_cluster_coordination_master_not_found");
			}
			return toResponse(master);
		}, model -> ac.send(model, OK));
	}

	public void handleSetCoordinationMaster(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			User user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
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

	private static CoordinatorMasterResponse toResponse(MasterServer server) {
		String name = server.getName();
		String host = server.getHost();
		int port = server.getPort();
		return new CoordinatorMasterResponse(name, port, host);
	}

	public void handleLoadCoordinationConfig(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			User user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			return coordinator.loadConfig();
		}, model -> ac.send(model, OK));
	}

	public void handleUpdateCoordinationConfig(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			User user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			CoordinatorConfig request = ac.fromJson(CoordinatorConfig.class);
			coordinator.updateConfig(request);
			return coordinator.loadConfig();
		}, model -> ac.send(model, OK));
	}
}
