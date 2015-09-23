package com.gentics.mesh.util;

import java.util.function.Consumer;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public final class ThreadUtil {

	/**
	 * Execute the given consumer in a worker pool thread and call the result handler once a result has been computed.
	 * 
	 * @param tx
	 * @param resultHandler
	 */
	public static <T> void executeBlocking(Consumer<Trx> tx, Handler<AsyncResult<T>> resultHandler) {

		Handler<AsyncResult<T>> wrappingHandler = e -> {
			resultHandler.handle(e);
		};

		Mesh.vertx().executeBlocking(bh -> {
			MeshSpringConfiguration.getMeshSpringConfiguration().database().trx(tx);
			bh.complete();
		} , false, wrappingHandler);
	}
}
