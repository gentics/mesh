package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.etc.StorageOptions;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;

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
	abstract public Trx trx();

	@Override
	abstract public NoTrx noTrx();

	@Override
	public void trx(Consumer<Trx> code) {
		for (int retry = 0; retry < 100; retry++) {
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
}
