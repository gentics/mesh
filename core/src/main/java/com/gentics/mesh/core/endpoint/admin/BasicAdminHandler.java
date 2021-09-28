package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_EXPORT_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_EXPORT_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_IMPORT_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_IMPORT_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_START;
import static com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating.INCONSISTENT;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML_UTF8;
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
import javax.naming.InvalidNameException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.generator.RAMLGenerator;
import com.gentics.mesh.parameter.BackupParameters;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for basic admin request methods.
 */
public class BasicAdminHandler extends AbstractHandler implements AdminHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	protected final Database db;

	protected final RouterStorageImpl routerStorage;

	protected final BootstrapInitializer boot;

	protected final MeshOptions options;

	protected final SearchProvider searchProvider;

	protected final HandlerUtilities utils;

	protected final Vertx vertx;

	protected final RouterStorageRegistryImpl routerStorageRegistry;

	protected final WriteLock writeLock;

	protected final ConsistencyCheckHandler consistencyCheckHandler;

	@Inject
	public BasicAdminHandler(Vertx vertx, Database db, RouterStorageImpl routerStorage, BootstrapInitializer boot, SearchProvider searchProvider,
		HandlerUtilities utils,
		MeshOptions options, RouterStorageRegistryImpl routerStorageRegistry, WriteLock writeLock,
		ConsistencyCheckHandler consistencyCheckHandler) {
		this.vertx = vertx;
		this.db = db;
		this.routerStorage = routerStorage;
		this.boot = boot;
		this.searchProvider = searchProvider;
		this.utils = utils;
		this.options = options;
		this.routerStorageRegistry = routerStorageRegistry;
		this.writeLock = writeLock;
		this.consistencyCheckHandler = consistencyCheckHandler;
	}

	/**
	 * Handle the mesh status request.
	 * 
	 * @param ac
	 */
	@Override
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
	@Override
	public void handleBackup(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			BackupParameters params = ac.getBackupParameters();
			if (params.isConsistencyCheck()) {
				log.info("Starting consistency check as requested.");
				ConsistencyCheckResponse result = consistencyCheckHandler.checkConsistency(false).runInExistingTx(tx);
				if (result.getResult() == INCONSISTENT) {
					long count = result.getInconsistencies().size();
					log.error("Backup aborted due to found inconsistencies: " + count);
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
	@Override
	public String backup() {
		if (!(options instanceof OrientDBMeshOptions)) {
			throw error(SERVICE_UNAVAILABLE, "function_not_supported"); // TODO unsupported for non-OrientDB
		}
		Mesh mesh = boot.mesh();
		MeshStatus oldStatus = mesh.getStatus();
		try {
			vertx.eventBus().publish(GRAPH_BACKUP_START.address, null);
			mesh.setStatus(MeshStatus.BACKUP);
			return db.backupDatabase(((OrientDBMeshOptions)options).getStorageOptions().getBackupDirectory());
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
	@Override
	public void handleRestore(InternalActionContext ac) {
		if (!(options instanceof OrientDBMeshOptions)) {
			throw error(SERVICE_UNAVAILABLE, "function_not_supported"); // TODO unsupported for non-OrientDB
		}
		OrientDBMeshOptions config = (OrientDBMeshOptions) options;
		Mesh mesh = boot.mesh();
		String dir = config.getStorageOptions().getDirectory();
		File backupDir = new File(config.getStorageOptions().getBackupDirectory());
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
			if (!ac.getUser().isAdmin()) {
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
			db.restoreDatabase(latestFile.getAbsolutePath());
			// TODO add changelog execution
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
		for (HibProject project : boot.projectDao().findAll()) {
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
	@Override
	public void handleExport(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			if (!(options instanceof OrientDBMeshOptions)) {
				throw error(SERVICE_UNAVAILABLE, "function_not_supported"); // TODO unsupported for non-OrientDB
			}
			String exportDir = ((OrientDBMeshOptions)options).getStorageOptions().getExportDirectory();
			log.debug("Exporting graph to {" + exportDir + "}");
			vertx.eventBus().publish(GRAPH_EXPORT_START.address, null);
			db.exportDatabase(exportDir);
			vertx.eventBus().publish(GRAPH_EXPORT_FINISHED.address, null);
			return message(ac, "export_finished");
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle graph import action.
	 * 
	 * @param ac
	 */
	@Override
	public void handleImport(InternalActionContext ac) {
		db.tx(tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			if (!(options instanceof OrientDBMeshOptions)) {
				throw error(SERVICE_UNAVAILABLE, "function_not_supported"); // TODO unsupported for non-OrientDB
			}
		});
		File importsDir = new File(((OrientDBMeshOptions)options).getStorageOptions().getExportDirectory());

		// Find the file which was last modified
		File latestFile = Arrays.asList(importsDir.listFiles()).stream().filter(file -> file.getName().endsWith(".gz"))
			.sorted(comparing(File::lastModified)).reduce((first, second) -> second).orElseGet(() -> null);
		try {

			vertx.eventBus().publish(GRAPH_IMPORT_START.address, null);
			db.importDatabase(latestFile.getAbsolutePath());
			boot.globalCacheClear();
			// TODO apply changelog after import
			// TODO flush references, clear & init project routers 
			vertx.eventBus().publish(GRAPH_IMPORT_FINISHED.address, null);

			Single.just(message(ac, "import_finished")).subscribe(model -> ac.send(model, OK), ac::fail);
		} catch (IOException e) {
			ac.fail(e);
		}
	}

	/**
	 * Load cluster status information.
	 * 
	 * @param ac
	 */
	@Override
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
	@Override
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
	@Override
	public MeshServerInfoModel getMeshServerInfoModel(InternalActionContext ac) {
		boolean admin = db.tx(() -> ac.isAdmin());
		MeshServerInfoModel info = new MeshServerInfoModel();
		if (options.getHttpServerOptions().isServerTokens() || admin) {
			info.setDatabaseVendor(db.getVendorName());
			info.setSearchVendor(searchProvider.getVendorName());
			info.setDatabaseVersion(db.getVersion());
			info.setSearchVersion(searchProvider.getVersion());
			info.setMeshVersion(Mesh.getPlainVersion());
			info.setVertxVersion(VersionCommand.getVersion());
			info.setDatabaseRevision(db.getDatabaseRevision());
			info.setMeshNodeName(options.getNodeName());
		}
		return info;
	}

	/**
	 * Generate and return the RAML of the server.
	 * 
	 * @param ac
	 */
	@Override
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
}
