package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import rx.Scheduler;

public abstract class AbstractDatabase implements Database {

	private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

	protected GraphStorageOptions options;
	protected Vertx vertx;

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
	public void init(GraphStorageOptions options, Vertx vertx) throws Exception {
		this.options = options;
		this.vertx = vertx;
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
	public <T> T noTrx(TrxHandler<T> txHandler) {
		try (NoTrx noTx = noTrx()) {
			T result = txHandler.call();
			return result;
		} catch (Exception e) {
			log.error("Error while handling no-transaction.", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> Observable<T> asyncNoTrx(TrxHandler<T> txHandler) {

		Scheduler scheduler = RxHelper.scheduler(Mesh.vertx());
		Observable<T> obs = Observable.create(sub -> {
			try {
				T result = noTrx(txHandler);
				sub.onNext(result);
				sub.onCompleted();
			} catch (Exception e) {
				sub.onError(e);
			}

		});
		return obs.observeOn(scheduler);
	}

	@Override
	public <T> Observable<T> asyncNoTrxExperimental(TrxHandler<Observable<T>> trxHandler) {
		// Create an exception which we can use to enhance error information in case of timeout or other tranaction errors
		final AtomicReference<Exception> reference = new AtomicReference<Exception>(null);
		try {
			throw new Exception("Transaction timeout exception");
		} catch (Exception e1) {
			reference.set(e1);
		}

		ObservableFuture<T> obsFut = RxHelper.observableFuture();
		Mesh.vertx().executeBlocking(bc -> {
			try (NoTrx noTx = noTrx()) {
				Observable<T> result = trxHandler.call();
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
				log.error("Calling transaction stacktrace.", reference.get());
				bc.fail(e);
			}

		}, false, obsFut.toHandler());
		return obsFut;
	}
}
