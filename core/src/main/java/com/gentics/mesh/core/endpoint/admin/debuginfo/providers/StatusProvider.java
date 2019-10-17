package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Flowable;

@Singleton
public class StatusProvider implements DebugInfoProvider {
	private final Database db;

	@Inject
	public StatusProvider(Database db) {
		this.db = db;
	}

	@Override
	public String name() {
		return "status";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.mergeArray(
			getClusterStatus()
		);
	}

	private Flowable<DebugInfoEntry> getClusterStatus() {
		return db.singleTx(() -> db.clusterManager().getClusterStatus())
			.map(status -> DebugInfoEntry.fromString("clusterStatus.json", status.toJson()))
			.toFlowable();
	}
}
