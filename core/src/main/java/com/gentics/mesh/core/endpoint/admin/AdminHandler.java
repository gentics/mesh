package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
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
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.madl.tx.Tx;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for admin request methods.
 */
@Singleton
public class AdminHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	private LegacyDatabase db;

	private RouterStorage routerStorage;

	private BootstrapInitializer boot;

	@Inject
	public AdminHandler(LegacyDatabase db, RouterStorage routerStorage, BootstrapInitializer boot) {
		this.db = db;
		this.routerStorage = routerStorage;
		this.boot = boot;
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
			db.backupGraph(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());
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
		db.asyncTx(() -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			File backupDir = new File(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());

			// Find the file which was last modified
			File latestFile = Arrays.asList(backupDir.listFiles()).stream().filter(file -> file.getName().endsWith(".zip"))
				.sorted(comparing(File::lastModified)).reduce((first, second) -> second).orElseGet(() -> null);

			if (latestFile == null) {
				throw error(INTERNAL_SERVER_ERROR, "error_backup", backupDir.getAbsolutePath());
			}
			db.restoreGraph(latestFile.getAbsolutePath());

			// Now clear the cached references and cached permissions
			MeshRootImpl.clearReferences();
			PermissionStore.invalidate(false);
			routerStorage.root().apiRouter().projectsRouter().getProjectRouters().clear();
			boot.initProjects();

			return Single.just(message(ac, "restore_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
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
			String exportDir = Mesh.mesh().getOptions().getStorageOptions().getExportDirectory();
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
		File importsDir = new File(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory());

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
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}

			MeshOptions options = Mesh.mesh().getOptions();
			if (options.getClusterOptions() != null && options.getClusterOptions().isEnabled()) {
				return Single.just(db.getClusterStatus());
			} else {
				throw error(BAD_REQUEST, "error_cluster_status_only_aviable_in_cluster_mode");
			}
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

}
