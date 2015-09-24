package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractDatabase implements Database {

	private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

	protected StorageOptions options;

	private int maxRetry = 25;

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
		if (options.getParameters() != null && options.getParameters().get("maxTransactionRetry") != null) {
			this.maxRetry = options.getParameters().get("maxTransactionRetry").getAsInt();
			log.info("Using {" + this.maxRetry + "} transaction retries before failing");
		}
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
	abstract public Trx trx();

	@Override
	abstract public NoTrx noTrx();

	@Override
	public <T> void asyncNoTrx(Consumer<NoTrx> noTrx, Handler<AsyncResult<T>> resultHandler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trx(Consumer<Trx> code) {
		for (int retry = 0; retry < maxRetry; retry++) {
			try (Trx tx = trx()) {
				code.accept(tx);
				break;
			} catch (Exception e) {
				log.error("Error while handling transaction. Retrying " + retry, e);
			}
			if (log.isDebugEnabled()) {
				log.debug("Retrying .. {" + retry + "}");
			}
		}
	}

	@Override
	public <T> void asyncTrx(Consumer<Trx> trx, Handler<AsyncResult<T>> resultHandler) {
		Handler<AsyncResult<T>> wrappingHandler = e -> {
			resultHandler.handle(e);
		};

		Mesh.vertx().executeBlocking(bh -> {
			trx(trx);
			bh.complete();
		} , false, wrappingHandler);
	}
}
