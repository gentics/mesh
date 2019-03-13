package com.gentics.mesh.core.endpoint.admin;

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

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.search.SearchProvider;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.reactivex.Single;
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

	@Inject
	public AdminHandler(Database db, RouterStorage routerStorage, BootstrapInitializer boot, MeshOptions options, SearchProvider searchProvider) {
		this.db = db;
		this.routerStorage = routerStorage;
		this.boot = boot;
		this.options = options;
		this.searchProvider = searchProvider;
	}

	public void handleMeshStatus(InternalActionContext ac) {
		MeshStatusResponse response = new MeshStatusResponse();
		response.setStatus(Mesh.mesh().getStatus());
		ac.send(response, OK);
	}

	/**
	 * Invoke a database backup call to the current graph database provider.
	 * 
	 * @param ac
	 */
	public void handleBackup(InternalActionContext ac) {
		db.asyncTx(() -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			MeshStatus oldStatus = Mesh.mesh().getStatus();
			Mesh.mesh().setStatus(MeshStatus.BACKUP);
			db.backupGraph(options.getStorageOptions().getBackupDirectory());
			Mesh.mesh().setStatus(oldStatus);
			return Single.just(message(ac, "backup_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle graph restore action.
	 * 
	 * @param ac
	 */
	public void handleRestore(InternalActionContext ac) {
		MeshOptions config = options;
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
		MeshStatus oldStatus = Mesh.mesh().getStatus();
		Completable.fromAction(() -> {
			Mesh.mesh().setStatus(MeshStatus.RESTORE);
			db.stop();
			db.restoreGraph(latestFile.getAbsolutePath());
			db.setupConnectionPool();
			routerStorage.root().apiRouter().projectsRouter().getProjectRouters().clear();
			MeshRootImpl.clearReferences();
			PermissionStore.invalidate(false);
		}).andThen(db.asyncTx(() -> {
			// Update the routes by loading the projects
			boot.initProjects();
			Mesh.mesh().setStatus(oldStatus);
			return Single.just(message(ac, "restore_finished"));
		})).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle graph export action.
	 * 
	 * @param ac
	 */
	public void handleExport(InternalActionContext ac) {
		db.asyncTx((tx) -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			String exportDir = options.getStorageOptions().getExportDirectory();
			log.debug("Exporting graph to {" + exportDir + "}");
			db.exportGraph(exportDir);
			return Single.just(message(ac, "export_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
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
			db.importGraph(latestFile.getAbsolutePath());
			Single.just(message(ac, "import_finished")).subscribe(model -> ac.send(model, OK), ac::fail);
		} catch (IOException e) {
			ac.fail(e);
		}
	}

	public void handleClusterStatus(InternalActionContext ac) {
		db.asyncTx(() -> {
			MeshAuthUser user = ac.getUser();
			if (user != null && !user.hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			if (options.getClusterOptions() != null && options.getClusterOptions().isEnabled()) {
				return Single.just(db.getClusterStatus());
			} else {
				throw error(BAD_REQUEST, "error_cluster_status_only_aviable_in_cluster_mode");
			}
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	public void handleVersions(InternalActionContext ac) {
		MeshServerInfoModel info = new MeshServerInfoModel();
		info.setDatabaseVendor(db.getVendorName());
		info.setDatabaseVersion(db.getVersion());
		info.setSearchVendor(searchProvider.getVendorName());
		info.setSearchVersion(searchProvider.getVersion());
		info.setMeshVersion(Mesh.getPlainVersion());
		info.setMeshNodeName(Mesh.mesh().getOptions().getNodeName());
		info.setVertxVersion(VersionCommand.getVersion());
		info.setDatabaseRevision(db.getDatabaseRevision());
		ac.send(info, OK);
	}

}
