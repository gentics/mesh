package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;

import io.reactivex.Flowable;

/**
 * Debug info provider for consistency check information.
 */
@Singleton
public class ConsistencyCheckProvider implements DebugInfoProvider {

	private final ConsistencyCheckHandler consistencyCheckHandler;
	private final Database db;

	@Inject
	public ConsistencyCheckProvider(ConsistencyCheckHandler consistencyCheckHandler, Database db) {
		this.consistencyCheckHandler = consistencyCheckHandler;
		this.db = db;
	}

	@Override
	public String name() {
		return "consistencyCheck";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return db.singleTx(tx -> consistencyCheckHandler.checkConsistency(false)
			.runInExistingTx(tx))
			.map(response -> DebugInfoBufferEntry.fromString("consistencyCheck.json", response.toJson(false)))
			.toFlowable();
	}
}
