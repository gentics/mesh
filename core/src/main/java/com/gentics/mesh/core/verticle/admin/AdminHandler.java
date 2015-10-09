package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.util.VerticleHelper.responde;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.HttpActionContext;

import io.vertx.ext.web.RoutingContext;

@Component
public class AdminHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	public void handleStatus(RoutingContext rc) {
		//TODO refactor and send a json response
		rc.response().setStatusCode(200);
		rc.response().end("OK");
	}

	/**
	 * Invoke a database backup call to the current graph database provider.
	 * 
	 * @param rc
	 */
	public void handleBackup(RoutingContext rc) {
		ActionContext ac = HttpActionContext.create(rc);
		try {
			db.backupGraph(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());
			responde(ac, "backup_finished");
		} catch (IOException e) {
			log.error("Backup failed", e);
			ac.fail(INTERNAL_SERVER_ERROR, "backup_failed");
		}
	}

	public void handleRestore(RoutingContext rc) {
		ActionContext ac = HttpActionContext.create(rc);
		try {
			File backupFile = new File(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory(), "");
			db.restoreGraph(backupFile.getAbsolutePath());
			responde(ac, "restore_finished");
		} catch (IOException e) {
			log.error("Restore failed", e);
			ac.fail(INTERNAL_SERVER_ERROR, "restore_failed");
		}
	}

	public void handleExport(RoutingContext rc) {
		ActionContext ac = HttpActionContext.create(rc);
		try {
			db.exportGraph(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory());
			responde(ac, "export_finished");
		} catch (IOException e) {
			log.error("Export failed", e);
			ac.fail(INTERNAL_SERVER_ERROR, "export_failed");
		}
	}

	public void handleImport(RoutingContext rc) {
		ActionContext ac = HttpActionContext.create(rc);
		try {
			File importFile = new File(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory(), "");
			db.importGraph(importFile.getAbsolutePath());
			responde(ac, "import_finished");
		} catch (IOException e) {
			log.error("Import failed", e);
			ac.fail(INTERNAL_SERVER_ERROR, "import_failed");
		}
	}

}
