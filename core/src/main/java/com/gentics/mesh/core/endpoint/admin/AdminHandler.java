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
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.RouterStorage;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for admin request methods.
 */
@Singleton
public class AdminHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	private Database db;

	private RouterStorage routerStorage;

	private BootstrapInitializer boot;

	private HandlerUtilities utils;

	@Inject
	public AdminHandler(Database db, RouterStorage routerStorage, BootstrapInitializer boot, HandlerUtilities utils) {
		this.db = db;
		this.routerStorage = routerStorage;
		this.boot = boot;
		this.utils = utils;
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
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			MeshStatus oldStatus = Mesh.mesh().getStatus();
			Mesh.mesh().setStatus(MeshStatus.BACKUP);
			db.backupGraph(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());
			Mesh.mesh().setStatus(oldStatus);
			return message(ac, "backup_finished");
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle graph restore action.
	 * 
	 * @param ac
	 */
	public void handleRestore(InternalActionContext ac) {
		MeshOptions config = Mesh.mesh().getOptions();
		if (config.getClusterOptions() != null && config.getClusterOptions().isEnabled()) {
			ac.fail(error(SERVICE_UNAVAILABLE, "restore_error_in_cluster_mode"));
			return;
		}

		utils.syncTx(ac, tx -> {
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

			// FIXME: Fix for PrjHub #10569 - ClassCastException
			db.reindex();
			// Now clear the cached references and cached permissions
			MeshRootImpl.clearReferences();
			PermissionStore.invalidate(false);
			routerStorage.root().apiRouter().projectsRouter().getProjectRouters().clear();
			boot.initProjects();

			return message(ac, "restore_finished");
		}, model -> ac.send(model, OK));
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
			String exportDir = Mesh.mesh().getOptions().getStorageOptions().getExportDirectory();
			log.debug("Exporting graph to {" + exportDir + "}");
			db.exportGraph(exportDir);
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
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}

			MeshOptions options = Mesh.mesh().getOptions();
			if (options.getClusterOptions() != null && options.getClusterOptions().isEnabled()) {
				return db.getClusterStatus();
			} else {
				throw error(BAD_REQUEST, "error_cluster_status_only_aviable_in_cluster_mode");
			}
		}, model -> ac.send(model, OK));
	}

}
