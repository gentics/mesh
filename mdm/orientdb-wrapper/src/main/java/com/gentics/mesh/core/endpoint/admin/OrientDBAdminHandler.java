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

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Vertx;

/**
 * Handler for admin request methods.
 */
@Singleton
public class OrientDBAdminHandler extends AdminHandler {

	@Inject
	public OrientDBAdminHandler(Vertx vertx, Database db, RouterStorageImpl routerStorage, BootstrapInitializer boot,
			SearchProvider searchProvider, HandlerUtilities utils, MeshOptions options,
			RouterStorageRegistryImpl routerStorageRegistry, Coordinator coordinator, WriteLock writeLock,
			ConsistencyCheckHandler consistencyCheckHandler) {
		super(vertx, db, routerStorage, boot, searchProvider, utils, options, routerStorageRegistry, coordinator, writeLock,
				consistencyCheckHandler);
	}

	@Override
	public String backup() {
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

	@Override
	public void handleRestore(InternalActionContext ac) {
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

	@Override
	public void handleExport(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			String exportDir = ((OrientDBMeshOptions)options).getStorageOptions().getExportDirectory();
			log.debug("Exporting graph to {" + exportDir + "}");
			vertx.eventBus().publish(GRAPH_EXPORT_START.address, null);
			db.exportDatabase(exportDir);
			vertx.eventBus().publish(GRAPH_EXPORT_FINISHED.address, null);
			return message(ac, "export_finished");
		}, model -> ac.send(model, OK));
	}

	@Override
	public void handleImport(InternalActionContext ac) {
		db.tx(tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
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

	@Override
	public boolean isBackupSupported() {
		return ((OrientDBMeshOptions)options).getStorageOptions().getDirectory() != null;
	}
}
