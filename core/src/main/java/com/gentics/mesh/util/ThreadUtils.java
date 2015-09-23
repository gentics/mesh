package com.gentics.mesh.util;

import java.util.function.Consumer;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public final class ThreadUtils {

	public static <T> void executeBlocking(Consumer<Trx> tx,Handler<AsyncResult<T>>  resultHandler) {
		
		Handler<AsyncResult<T>> wrappingHandler = e-> {
			resultHandler.handle(e);
		};
		
		Mesh.vertx().executeBlocking(bh -> {
			MeshSpringConfiguration.getMeshSpringConfiguration().database().trx(tx);
			bh.complete();
		} , false, wrappingHandler);
	}
}
