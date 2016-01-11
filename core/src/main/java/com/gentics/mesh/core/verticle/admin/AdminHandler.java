package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.ext.web.RoutingContext;
import rx.Observable;

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
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrxExperimental(() -> {
			db.backupGraph(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory());
			return Observable.just(message(ac, "backup_finished"));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleRestore(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrxExperimental(() -> {
			File backupFile = new File(Mesh.mesh().getOptions().getStorageOptions().getBackupDirectory(), "");
			db.restoreGraph(backupFile.getAbsolutePath());
			return Observable.just(message(ac, "restore_finished"));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleExport(RoutingContext rc) {
		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrxExperimental(() -> {
			db.exportGraph(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory());
			return Observable.just(message(ac, "export_finished"));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handleImport(RoutingContext rc) {

		InternalActionContext ac = InternalActionContext.create(rc);
		db.asyncNoTrxExperimental(() -> {
			File importFile = new File(Mesh.mesh().getOptions().getStorageOptions().getExportDirectory(), "");
			db.importGraph(importFile.getAbsolutePath());
			return Observable.just(message(ac, "import_finished"));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

}
