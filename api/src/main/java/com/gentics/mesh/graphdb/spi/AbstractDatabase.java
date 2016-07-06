package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
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

	//	@Override
	//	public <T> Observable<T> asyncNoTrxExperimental(TrxHandler<Observable<T>> trxHandler) {
	//
	//		return Observable.<T> create(sub -> {
	//			NoTrx noTx = noTrx();
	//			try {
	//				Observable<T> result = trxHandler.call();
	//				if (result == null) {
	//					sub.onCompleted();
	//				}
	//				result.toList().subscribe(list -> {
	//					// We need to close the transaction right away in order to 
	//					// prevent the transaction to remain open while onNext is
	//					// invoked for following observables. 
	//					noTx.close();
	//					list.forEach(sub::onNext);
	//					sub.onCompleted();
	//				}, error -> {
	//					noTx.close();
	//					log.error("Error while handling noTrx", error);
	//					sub.onError(error);
	//				});
	//			} catch (Exception e) {
	//				log.error("Error while handling no-transaction.", e);
	//				sub.onError(e);
	//			}
	//		}).subscribeOn(RxHelper.blockingScheduler(Mesh.vertx(), false));
	//	}
	//	

	@Override
	public <T> Observable<T> asyncNoTrxExperimental(TrxHandler<Observable<T>> trxHandler) {
		Observable<T> obs = Observable.create(sub -> {
			Mesh.vertx().executeBlocking(bc -> {
				try (NoTrx noTx = noTrx()) {
					Observable<T> result = trxHandler.call();
					if (result == null) {
						bc.complete();
					} else {
						T ele = result.toBlocking().single();
						bc.complete(ele);
					}
				} catch (Exception e) {
					log.error("Error while handling no-transaction.", e);
					bc.fail(e);
				}
			}, false, (AsyncResult<T> done) -> {
				if (done.failed()) {
					sub.onError(done.cause());
				} else {
					sub.onNext(done.result());
					sub.onCompleted();
				}
			});
		});
		return obs;
	}

}
