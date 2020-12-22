package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.nio.file.Paths;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil;
import com.gentics.mesh.etc.config.AbstractMeshOptions;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * Provider for logging debug information. Please note that this provider will only field data when the debuginfo log is enabled.
 */
@Singleton
public class LogProvider implements DebugInfoProvider {
	private final DebugInfoUtil debugInfoUtil;
	private final AbstractMeshOptions meshOptions;

	@Inject
	public LogProvider(DebugInfoUtil debugInfoUtil, AbstractMeshOptions meshOptions) {
		this.debugInfoUtil = debugInfoUtil;
		this.meshOptions = meshOptions;
	}

	@Override
	public String name() {
		return "log";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		String logFolder = meshOptions.getDebugInfoOptions().getLogFolder();
		return Flowable.concatArray(
			debugInfoUtil.readFileOrEmpty(Paths.get(logFolder, "debuginfo.1.log").toString()),
			debugInfoUtil.readFileOrEmpty(Paths.get(logFolder, "debuginfo.log").toString())).reduce(this::concat)
			.toFlowable()
			.map(buffer -> DebugInfoBufferEntry.fromBuffer("log.txt", buffer));
	}

	private Buffer concat(Buffer buffer1, Buffer buffer2) {
		return buffer1.appendBuffer(buffer2);
	}
}
