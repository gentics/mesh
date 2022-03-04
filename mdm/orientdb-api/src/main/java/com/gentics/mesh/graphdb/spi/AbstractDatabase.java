package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.CommonDatabase;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.metric.MetricsService;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract class for graph database implementations.
 */
public abstract class AbstractDatabase extends CommonDatabase implements GraphDatabase {

	private static final Logger log = LoggerFactory.getLogger(AbstractDatabase.class);

	protected OrientDBMeshOptions options;
	protected String meshVersion;
	protected String[] basePaths;

	private final Lazy<Vertx> vertx;

	public AbstractDatabase(Lazy<Vertx> vertx, Mesh mesh, MetricsService metrics) {
		super(mesh, metrics);
		this.vertx = vertx;
	}

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}
		tx(tx -> {
			GraphDBTx gtx = HibClassConverter.toGraph(tx);
			gtx.getGraph().e().removeAll();
			gtx.getGraph().v().removeAll();
			tx.success();
		});
		if (log.isDebugEnabled()) {
			log.debug("Cleared graph");
		}
	}

	@Override
	public void init(String meshVersion, String... basePaths) throws Exception {
		this.meshVersion = meshVersion;
		this.basePaths = basePaths;
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

	@Override
	public Vertx vertx() {
		return vertx.get();
	}

}
