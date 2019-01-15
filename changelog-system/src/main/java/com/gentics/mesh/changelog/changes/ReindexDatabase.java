package com.gentics.mesh.changelog.changes;

import java.io.File;
import java.io.IOException;

import com.gentics.mesh.changelog.AbstractChange;

public class ReindexDatabase extends AbstractChange {
	@Override
	public void actualApply() {
		// Move old config files away which will no longer work with orientdb 3.0.x
		moveOldConfig("default-distributed-db-config.json");
		moveOldConfig("hazelcast.xml");
		moveOldConfig("orientdb-server-config.xml");
		try {
			getDb().initConfigurationFiles();
		} catch (IOException e) {
			throw new RuntimeException("Error while generating new configuration files", e);
		}
		getDb().reindex();
	}

	private void moveOldConfig(String confName) {
		File oldConf = new File("config", confName);
		if (oldConf.exists()) {
			log.info("Moving old OrientDB server configuration file away.");
			File newConf = new File("config", confName + ".org.bak");
			if (!oldConf.renameTo(newConf)) {
				throw new RuntimeException("Failed to move {" + oldConf + "} to {" + newConf + "}");
			}
		}
	}

	@Override
	protected boolean applyInTx() {
		return false;
	}

	@Override
	public String getDescription() {
		return "Repopulates the database indices. This is necessary when upgrading to OrientDB 3." +
			" This change is executed before all other changes to ensure that the changes" +
			" can access the database correctly.";
	}

	@Override
	public String getUuid() {
		return "DCF025827B8F44BBB025827B8FA4BB94";
	}

	@Override
	public String getName() {
		return "Invoke database reindex";
	}
}
