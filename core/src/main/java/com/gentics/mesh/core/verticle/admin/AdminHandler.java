package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static java.util.Comparator.comparing;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import rx.Single;

/**
 * Handler for admin request methods.
 */
public class AdminHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	private Database db;

	public void handleStatus(InternalActionContext ac) {
		ac.send(message(ac, "status_ready"), OK);
	}

	@Inject
	public AdminHandler(Database db) {
		this.db = db;
	}

	/**
	 * Invoke a database backup call to the current graph database provider.
	 * 
	 * @param ac
	 */
	public void handleBackup(InternalActionContext ac) {
		db.operateTx(() -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			db.backupGraph(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());
			return Single.just(message(ac, "backup_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle graph restore action.
	 * 
	 * @param ac
	 */
	public void handleRestore(InternalActionContext ac) {
		db.operateTx(() -> {
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
			return Single.just(message(ac, "restore_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle graph export action.
	 * 
	 * @param ac
	 */
	public void handleExport(InternalActionContext ac) {
		db.operateTx((tx) -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			db.exportGraph(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory());
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

	/**
	 * Handle migration status request.
	 * 
	 * @param ac
	 */
	public void handleMigrationStatus(InternalActionContext ac) {
		if (vertx.isClustered()) {
			vertx.sharedData().getClusterWideMap(MigrationStatusHandler.MIGRATION_DATA_MAP_KEY, rh -> {
				if (rh.failed()) {
					log.error("Could not load status map.", rh.cause());
					ac.fail(rh.cause());
				} else {
					rh.result().get("data", dh -> {
						if (dh.failed()) {
							log.error("Could not load status data from map.", dh.cause());
							ac.fail(dh.cause());
						} else {
							JsonObject obj = (JsonObject) dh.result();
							if (obj != null) {
								System.out.println(obj.encodePrettily());
							}
							//TODO return the correct json
							ac.send(message(ac, "migration_status_idle"), OK);
						}
					});
				}
			});

			// vertx.sharedData().getClusterWideMap("migrationStatus", rh -> {
			// if (rh.failed()) {
			// System.out.println("failed");
			// rh.cause().printStackTrace();
			// } else {
			// rh.result().get("status", vh -> {
			// ac.respond(message(ac, "test"), OK);
			// });
			// }
			// });
		} else {
			LocalMap<String, String> map = vertx.sharedData().getLocalMap("migrationStatus");
			String statusKey = "migration_status_idle";
			String currentStatusKey = map.get("status");
			if (currentStatusKey != null) {
				statusKey = currentStatusKey;
			}
			ac.send(message(ac, statusKey), OK);
		}
	}

}
