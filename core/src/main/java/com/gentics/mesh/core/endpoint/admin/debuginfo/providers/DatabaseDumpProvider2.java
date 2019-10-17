package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoFileEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Flowable;
import io.vertx.reactivex.core.file.FileSystem;

@Singleton
public class DatabaseDumpProvider2 implements DebugInfoProvider {
	private final AdminHandler adminHandler;
	private final Database db;
	private final DebugInfoUtil util;
	private final FileSystem fs;

	@Inject
	public DatabaseDumpProvider2(AdminHandler adminHandler, Database db, DebugInfoUtil util, FileSystem fs) {
		this.adminHandler = adminHandler;
		this.db = db;
		this.util = util;
		this.fs = fs;
	}

	@Override
	public String name() {
		return "backup";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.HEAVY;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return db.singleTx(adminHandler::backup)
			.map(buffer -> DebugInfoFileEntry.fromFile(fs, "graphdb.zip", buffer))
			.toFlowable();
	}
}
