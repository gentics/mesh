package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoFileEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * Debug info provider for database dump content. The provider can invoke a dump and add return a {@link DebugInfoEntry} which can be included in the returned
 * zip content.
 */
@Singleton
public class DatabaseDumpProvider implements DebugInfoProvider {
	private final AdminHandler adminHandler;
	private final Database db;
	private final FileSystem fs;
	private final MeshOptions options;

	@Inject
	public DatabaseDumpProvider(AdminHandler adminHandler, Database db, DebugInfoUtil util, Vertx vertx, MeshOptions options) {
		this.adminHandler = adminHandler;
		this.db = db;
		this.fs = vertx.fileSystem();
		this.options = options;
	}

	@Override
	public String name() {
		return "backup";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		if (!(options instanceof OrientDBMeshOptions) || ((OrientDBMeshOptions)options).getStorageOptions().getDirectory() == null) {
			return Flowable.empty();
		}

		return db.singleTx(adminHandler::backup)
			.map(filename -> DebugInfoFileEntry.fromFile(fs, filename, "graphdb.zip", true))
			.toFlowable();
	}
}
