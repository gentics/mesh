package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;

import com.gentics.mesh.Mesh;
import io.vertx.reactivex.core.WorkerExecutor;
import org.apache.commons.io.FileUtils;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract class for graph database implementations.
 */
public abstract class AbstractDatabase implements Database {

	private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

	protected MeshOptions options;
	protected String meshVersion;
	protected String[] basePaths;
	private WorkerExecutor worker;

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
	public void init(MeshOptions options, String meshVersion, String... basePaths) throws Exception {
		this.options = options;
		this.meshVersion = meshVersion;
		this.basePaths = basePaths;
	}

	@Override
	public WorkerExecutor getExecutor() {
		if (worker == null) {
			// TODO Review pool size
			worker = Mesh.rxVertx().createSharedWorkerExecutor("asyncTx", 1);
		}
		return worker;
	}

	/**
	 * Return the graph database storage options.
	 * 
	 * @return
	 */
	public GraphStorageOptions storageOptions() {
		return options.getStorageOptions();
	}

	@Override
	public void reset() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Resetting graph database");
		}
		stop();
		try {
			if (options.getStorageOptions().getDirectory() != null) {
				File storageDirectory = new File(options.getStorageOptions().getDirectory());
				if (storageDirectory.exists()) {
					FileUtils.deleteDirectory(storageDirectory);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		setupConnectionPool();
	}

}
