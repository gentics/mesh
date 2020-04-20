package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.AdminIndexHandler;

import io.reactivex.Flowable;

@Singleton
public class StatusProvider implements DebugInfoProvider {
	private final Database db;
	private final AdminIndexHandler adminIndexHandler;
	private final AdminHandler adminHandler;

	@Inject
	public StatusProvider(Database db, AdminIndexHandler adminIndexHandler, AdminHandler adminHandler) {
		this.db = db;
		this.adminIndexHandler = adminIndexHandler;
		this.adminHandler = adminHandler;
	}

	@Override
	public String name() {
		return "status";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.mergeArray(
			getVersions(ac),
			getClusterStatus(),
			getElasticSearchStatus()
		);
	}

	private Flowable<DebugInfoEntry> getClusterStatus() {
		return db.singleTx(() -> db.clusterManager().getClusterStatus())
			.map(status -> DebugInfoBufferEntry.fromString("clusterStatus.json", status.toJson()))
			.toFlowable();
	}

	private Flowable<DebugInfoEntry> getElasticSearchStatus() {
		return adminIndexHandler.getSearchStatus()
			.map(status -> DebugInfoBufferEntry.fromString("searchStatus.json", status.toJson()))
			.toFlowable();
	}

	private Flowable<DebugInfoEntry> getVersions(InternalActionContext ac) {
		return Flowable.just(DebugInfoBufferEntry.fromString("versions.json", adminHandler.getMeshServerInfoModel(ac).toJson()));
	}
}
