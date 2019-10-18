package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

@Singleton
public class LogProvider implements DebugInfoProvider {
	private final DebugInfoUtil debugInfoUtil;

	@Inject
	public LogProvider(DebugInfoUtil debugInfoUtil) {
		this.debugInfoUtil = debugInfoUtil;
	}

	@Override
	public String name() {
		return "log";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.concatArray(
			debugInfoUtil.readFileOrEmpty("debuginfo/debuginfo.1.log"),
			debugInfoUtil.readFileOrEmpty("debuginfo/debuginfo.log")
		).reduce(this::concat)
		.toFlowable()
		.map(buffer -> DebugInfoBufferEntry.fromBuffer("log.txt", buffer));
	}

	private Buffer concat(Buffer buffer1, Buffer buffer2) {
		return buffer1.appendBuffer(buffer2);
	}
}
