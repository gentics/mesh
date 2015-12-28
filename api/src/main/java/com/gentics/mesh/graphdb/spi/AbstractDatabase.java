package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
	public <T> Observable<T> asyncTrx(TrxHandler<T> txHandler) {
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		Observable<T> obs = Observable.create(sub -> {
			try {
				T result = trx(txHandler);
				sub.onNext(result);
				sub.onCompleted();
			} catch (Exception e) {
				sub.onError(e);
			}

		});

		return obs.observeOn(scheduler);
		//		ObservableFuture<T> obsFut = RxHelper.observableFuture();
		//		vertx.executeBlocking(bh -> {
		//			try {
		//				T result = trx(txHandler);
		//				bh.complete(result);
		//			} catch (Exception e) {
		//				bh.fail(e);
		//			}
		//		} , false, obsFut.toHandler());
		//		return obsFut;
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

		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
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

		//		ObservableFuture<T> obsFut = RxHelper.observableFuture();
		//		vertx.executeBlocking(bh -> {
		//			try {
		//				T result = noTrx(txHandler);
		//				bh.complete(result);
		//			} catch (Exception e) {
		//				bh.fail(e);
		//			}
		//		} , false, obsFut.toHandler());
		//		return obsFut;
	}

	@Override
	public <T> Observable<T> asyncNoTrx2(TrxHandler<Observable<T>> trxHandler) {
		Scheduler scheduler = RxHelper.blockingScheduler(Mesh.vertx());
		Observable<T> obs = Observable.create(sub -> {

			try (NoTrx noTx = noTrx()) {
				Observable<T> result = trxHandler.call();
				Iterator<T> it = result.toBlocking().getIterator();
				while (it.hasNext()) {
					sub.onNext(it.next());
				}
				sub.onCompleted();
			} catch (Exception e) {
				log.error("Error while handling no-transaction.", e);
				sub.onError(e);
			}

		});
		return obs.observeOn(scheduler);

	}
}
