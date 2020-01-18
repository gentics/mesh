package com.gentics.mesh.util;

import java.nio.file.Paths;

import com.gentics.mesh.etc.config.GraphStorageOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RequirementsCheck {

	private static Logger log = LoggerFactory.getLogger(RequirementsCheck.class);

	public static void init(GraphStorageOptions options) {
		String storageDir = options.getDirectory();
		if (storageDir != null) {
			log.info("Checking for directIO support");
			if (FilesystemUtil.supportsDirectIO(Paths.get(storageDir))) {
				log.info("DirectIO support verified");
			} else {
				log.info("DirectIO not supported.");
				System.setProperty("storage.wal.allowDirectIO", "false");
			}
		}
	}

}
