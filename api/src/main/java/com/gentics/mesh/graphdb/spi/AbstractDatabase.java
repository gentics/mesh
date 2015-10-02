package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractDatabase implements Database {

	private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

	protected StorageOptions options;

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}
		try (Trx tx = trx()) {
			tx.getGraph().e().removeAll();
			tx.getGraph().v().removeAll();
			tx.success();
		}
		if (log.isDebugEnabled()) {
			log.debug("Cleared graph");
		}
	}

	@Override
	public void init(StorageOptions options) {
		this.options = options;
		start();
	}

	@Override
	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Resetting graph database");
		}
		stop();
		try {
			if (options.getDirectory() != null) {
				File storageDirectory = new File(options.getDirectory());
				if (storageDirectory.exists()) {
					FileUtils.deleteDirectory(storageDirectory);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		start();
	}

	@Override
	public <T> Database asyncTrx(TrxHandler<Future<T>> txHandler, Handler<AsyncResult<T>> resultHandler) {
		Mesh.vertx().executeBlocking(bh -> {
			trx(txHandler, rh -> {
				if (rh.succeeded()) {
					bh.complete(rh.result());
				} else {
					bh.fail(rh.cause());
				}
			});
		} , false, resultHandler);
		return this;
	}

	@Override
	public <T> Future<T> noTrx(TrxHandler<Future<T>> txHandler) {
		Future<T> future = Future.future();
		try (NoTrx noTx = noTrx()) {
			txHandler.handle(future);
			// TODO maybe we should only retry OConcurrentExceptions?
		} catch (Exception e) {
			log.error("Error while handling no-transaction.", e);
			return Future.failedFuture(e);
		}
		return future;
	}

	@Override
	public <T> Database asyncNoTrx(TrxHandler<Future<T>> txHandler, Handler<AsyncResult<T>> resultHandler) {
		Mesh.vertx().executeBlocking(bh -> {
			Future<T> future = noTrx(txHandler);
			future.setHandler(rh -> {
				if (rh.failed()) {
					bh.fail(rh.cause());
				} else {
					bh.complete(rh.result());
				}
			});
		} , false, resultHandler);
		return this;
	}

}
