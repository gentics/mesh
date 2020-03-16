package com.gentics.mesh.changelog.changes;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.changelog.AbstractChange;

public class OrientDBHazelcastConfigUpdate extends AbstractChange {

	public static final String ODB_FILENAME = "orientdb-server-config.xml";
	public static final String OLD_PLUGIN_REGEX = "com\\.orientechnologies\\.orient\\.server\\.hazelcast\\.OHazelcastPlugin";
	public static final String NEW_PLUGIN = "com.gentics.mesh.graphdb.cluster.MeshOHazelcastPlugin";

	@Override
	public String getUuid() {
		return "5293468AD4C5408F83A4FB6C3501CD0F";
	}

	@Override
	public String getName() {
		return "OrientDB Hazelcast Config Update";
	}

	@Override
	public String getDescription() {
		return "Changes the hazelcast plugin class in the " + ODB_FILENAME + " config file.";
	}

	@Override
	public void apply() {
		File configFile = new File("config", ODB_FILENAME);

		if (configFile.exists()) {
			try {
				String content = FileUtils.readFileToString(configFile);
				if (log.isDebugEnabled()) {
					log.debug("Old config:\n" + content);
				}
				content = content.replaceAll(OLD_PLUGIN_REGEX, NEW_PLUGIN);
				if (log.isDebugEnabled()) {
					log.debug("New config:\n" + content);
				}
				FileUtils.writeStringToFile(configFile, content);
			} catch (IOException e) {
				throw new RuntimeException("Could not apply changes to {" + configFile + "}", e);
			}
		} else {
			log.warn("Could not find " + ODB_FILENAME + " skipping auto migration.");
		}
	}

}
