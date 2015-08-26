package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.util.VerticleHelper.fail;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static com.gentics.mesh.util.VerticleHelper.*;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;

import io.vertx.ext.web.RoutingContext;

@Component
public class AdminHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

	public void handleStatus(RoutingContext rc) {
		rc.response().setStatusCode(200);
		rc.response().end("OK");
	}

	public void handleBackup(RoutingContext rc) {
		try {
			db.backupGraph(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());
			responde(rc, "backup_finished");
		} catch (IOException e) {
			log.error("Backup failed", e);
			fail(rc, "backup_failed");
		}
	}

	public void handleRestore(RoutingContext rc) {
		try {
			File backupFile = new File(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory(), "");
			db.restoreGraph(backupFile.getAbsolutePath());
			responde(rc, "restore_finished");
		} catch (IOException e) {
			log.error("Restore failed", e);
			fail(rc, "restore_failed");
		}
	}

	public void handleExport(RoutingContext rc) {
		try {
			db.exportGraph(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory());
			responde(rc, "export_finished");
		} catch (IOException e) {
			log.error("Export failed", e);
			fail(rc, "export_failed");
		}
	}

	public void handleImport(RoutingContext rc) {
		try {
			File importFile = new File(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory(), "");
			db.importGraph(importFile.getAbsolutePath());
			responde(rc, "import_finished");
		} catch (IOException e) {
			log.error("Import failed", e);
			fail(rc, "import_failed");
		}
	}

}
