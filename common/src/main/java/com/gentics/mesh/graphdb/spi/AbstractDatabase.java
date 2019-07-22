package com.gentics.mesh.graphdb.spi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.ETag;

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
	public String getDatabaseRevision() {
		String overrideRev = System.getProperty("mesh.internal.dbrev");
		if (overrideRev != null) {
			return overrideRev;
		}
		StringBuilder builder = new StringBuilder();
		for (String changeUuid : getChangeUuidList()) {
			builder.append(changeUuid);
		}
		return ETag.hash(builder.toString() + getVersion());
	}

}
