package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.AdminIndexHandler;

import io.reactivex.Flowable;

@Singleton
public class StatusProvider implements DebugInfoProvider {
	private final Database db;
	private final AdminIndexHandler adminIndexHandler;

	@Inject
	public StatusProvider(Database db, AdminIndexHandler adminIndexHandler) {
		this.db = db;
		this.adminIndexHandler = adminIndexHandler;
	}

	@Override
	public String name() {
		return "status";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.LIGHT;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.mergeArray(
			getClusterStatus(),
			getElasticSearchStatus()
		);
	}

	private Flowable<DebugInfoEntry> getClusterStatus() {
		return db.singleTx(() -> db.clusterManager().getClusterStatus())
			.map(status -> DebugInfoEntry.fromString("clusterStatus.json", status.toJson()))
			.toFlowable();
	}

	private Flowable<DebugInfoEntry> getElasticSearchStatus() {
		return adminIndexHandler.getSearchStatus()
			.map(status -> DebugInfoEntry.fromString("searchStatus.json", status.toJson()))
			.toFlowable();
	}
}
