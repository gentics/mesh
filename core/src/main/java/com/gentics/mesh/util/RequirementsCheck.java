package com.gentics.mesh.util;

import java.nio.file.Paths;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.sun.jna.Platform;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RequirementsCheck {

	private static Logger log = LoggerFactory.getLogger(RequirementsCheck.class);

	public static void init(GraphStorageOptions options) {
		String storageDir = options.getDirectory();

		// Check for directIO support
		if (storageDir != null && System.getProperty("storage.wal.allowDirectIO") == null && Platform.isLinux()) {
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
