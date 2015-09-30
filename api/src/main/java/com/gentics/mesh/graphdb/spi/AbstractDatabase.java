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
	public <T> Database trx(Handler<Future<T>> tcHandler, Handler<AsyncResult<T>> resultHandler) {
		resultHandler.handle(trx(tcHandler));
		return this;
	}

	@Override
	public <T> Database asyncTrx(Handler<Future<T>> tcHandler, Handler<AsyncResult<T>> resultHandler) {
		Mesh.vertx().executeBlocking(bh -> {
			Future<T> future = trx(tcHandler);
			if (future.succeeded()) {
				bh.complete(future.result());
			} else {
				bh.fail(future.cause());
			}
		} , false, resultHandler);
		return this;
	}

	@Override
	public <T> Future<T> noTrx(Handler<Future<T>> tcHandler) {
		Future<T> future = Future.future();
		try (NoTrx noTx = noTrx()) {
			tcHandler.handle(future);
			// TODO maybe we should only retry OConcurrentExceptions?
		} catch (Exception e) {
			log.error("Error while handling no-transaction.", e);
			return Future.failedFuture(e);
		}
		if (!future.isComplete()) {
			future.complete();
		}
		return future;
	}

	// @Override
	// public Database asyncNoTrx(Consumer<NoTrx> transactionCode) {
	// Mesh.vertx().executeBlocking(bh -> {
	// try (NoTrx noTx = noTrx()) {
	// transactionCode.accept(noTx);
	// }
	// } , false, rh -> {
	// if (rh.failed()) {
	// throw rh.cause();
	// }
	// });
	// return this;
	// }

	@Override
	public <T> Database asyncNoTrx(Handler<Future<T>> transactionCodeHandler, Handler<AsyncResult<T>> resultHandler) {
		Mesh.vertx().executeBlocking(bh -> {
			Future<T> future = noTrx(transactionCodeHandler);
			if (future.succeeded()) {
				bh.complete(future.result());
			} else {
				bh.fail(future.cause());
			}
		} , false, resultHandler);
		return this;
	}

}
