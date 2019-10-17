package com.gentics.mesh.core.endpoint.admin.debuginfo;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

@Singleton
public class DebugInfoUtil {
	private final Vertx vertx;
	private static final Logger log = LoggerFactory.getLogger(DebugInfoUtil.class);

	@Inject
	public DebugInfoUtil(Vertx vertx) {
		this.vertx = vertx;
	}

	public Flowable<Buffer> readFileOrEmpty(String path) {
		return vertx.fileSystem().rxReadFile(path)
			.map(io.vertx.reactivex.core.buffer.Buffer::getDelegate)
			.toFlowable()
			.onErrorResumeNext(err -> {
				log.warn(String.format("Could not read file {%s}", path), err);
				return Flowable.empty();
			});
	}

	public Flowable<DebugInfoEntry> readDebugInfoEntryOrEmpty(String path) {
		return readFileOrEmpty(path)
			.map(buf -> DebugInfoEntry.fromBuffer(path, buf));
	}
}
