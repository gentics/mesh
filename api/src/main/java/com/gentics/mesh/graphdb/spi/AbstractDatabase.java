package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

public abstract class AbstractDatabase implements Database {

	private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

	protected GraphStorageOptions options;
	protected Vertx vertx;
	protected String[] basePaths;

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}
		try (Tx tx = tx()) {
			tx.getGraph().e().removeAll();
			tx.getGraph().v().removeAll();
			tx.success();
		}
		if (log.isDebugEnabled()) {
			log.debug("Cleared graph");
		}
	}

	@Override
	public void init(GraphStorageOptions options, Vertx vertx, String... basePaths) throws Exception {
		this.options = options;
		this.vertx = vertx;
		this.basePaths = basePaths;
		start();
	}

	@Override
	public void reset() throws Exception {
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
	public <T> T noTx(TxHandler<T> txHandler) {
		try (NoTx noTx = noTx()) {
			T result = txHandler.call();
			return result;
		} catch (Exception e) {
			log.error("Error while handling no-transaction.", e);
			throw new RuntimeException(e);
		}
	}

//	@Override
//	public <T> Single<T> asyncTx(TxHandler<Single<T>> trxHandler) {
//		return Single.create(sub -> {
//			Mesh.vertx().executeBlocking(bc -> {
//				try (Tx tx = tx()) {
//					Single<T> result = trxHandler.call();
//					if (result == null) {
//						bc.complete();
//					} else {
//						T ele = result.toBlocking().value();
//						bc.complete(ele);
//					}
//				} catch (Exception e) {
//					log.error("Error while handling transaction.", e);
//					bc.fail(e);
//				}
//			}, false, (AsyncResult<T> done) -> {
//				if (done.failed()) {
//					sub.onError(done.cause());
//				} else {
//					sub.onSuccess(done.result());
//				}
//			});
//		});
//
//	}

	@Override
	public <T> Single<T> asyncNoTx(TxHandler<Single<T>> trxHandler) {
		// Create an exception which we can use to enhance error information in case of timeout or other tranaction errors
		final AtomicReference<Exception> reference = new AtomicReference<Exception>(null);
		try {
			throw new Exception("Transaction timeout exception");
		} catch (Exception e1) {
			reference.set(e1);
		}

		return Single.create(sub -> {
			Mesh.vertx().executeBlocking(bc -> {
				try (NoTx noTx = noTx()) {
					Single<T> result = trxHandler.call();
					if (result == null) {
						bc.complete();
					} else {
						try {
							T ele = result.toBlocking().toFuture().get(20, TimeUnit.SECONDS);
							bc.complete(ele);
						} catch (TimeoutException e2) {
							log.error("Timeout while processing result of transaction handler.", e2);
							log.error("Calling transaction stacktrace.", reference.get());
							bc.fail(reference.get());
						}
					}
				} catch (Exception e) {
					log.error("Error while handling no-transaction.", e);
					bc.fail(e);
				}
			}, false, (AsyncResult<T> done) -> {
				if (done.failed()) {
					sub.onError(done.cause());
				} else {
					sub.onSuccess(done.result());
				}
			});
		});
	}

}
