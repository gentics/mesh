package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import rx.Single;

/**
 * Handler for admin request methods.
 */
public class AdminHandler extends AbstractHandler {

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
	 * @param rc
	 */
	public void handleBackup(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTx(() -> {
			db.backupGraph(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());
			return Single.just(message(ac, "backup_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle graph restore action.
	 * 
	 * @param rc
	 */
	public void handleRestore(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTx(() -> {
			File backupFile = new File(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory(), "");
			db.restoreGraph(backupFile.getAbsolutePath());
			return Single.just(message(ac, "restore_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle graph export action.
	 * 
	 * @param rc
	 */
	public void handleExport(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTx(() -> {
			db.exportGraph(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory());
			return Single.just(message(ac, "export_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle graph import action.
	 * 
	 * @param rc
	 */
	public void handleImport(RoutingContext rc) {

		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTx(() -> {
			File importFile = new File(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory(), "");
			db.importGraph(importFile.getAbsolutePath());
			return Single.just(message(ac, "import_finished"));
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle migration status request.
	 * 
	 * @param ac
	 */
	public void handleMigrationStatus(InternalActionContext ac) {

		if (vertx.isClustered()) {
			// TODO implement this
			throw new NotImplementedException("cluster support for migration status is not yet implemented");
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
