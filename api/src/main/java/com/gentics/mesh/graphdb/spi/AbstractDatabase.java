package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract class for graph database implementations.
 */
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
		// Avoid creating a transaction if possible
		if (Database.threadLocalGraph.get() != null) {
			log.error("Bogus noTx() call detected. Avoid creating nested transactions for threads which already use an active transaction.");
			try {
				T result = txHandler.call();
				return result;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			try (NoTx noTx = noTx()) {
				T result = txHandler.call();
				return result;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
