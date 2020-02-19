package com.gentics.mesh.util;

import java.io.File;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.common.jna.ONative;
import com.sun.jna.Platform;

public final class FilesystemUtil {

	public static final Logger log = LoggerFactory.getLogger(FilesystemUtil.class);

	private FilesystemUtil() {

	}

	/**
	 * Check whether provided filesystem location supports directIO.
	 * 
	 * @param path
	 * @return
	 */
	public static boolean supportsDirectIO(Path path) {
		if (Platform.isLinux()) {
			try {
				path.toFile().mkdirs();
				Path testPath = path.resolve(".check_fs_support");
				File testFile = testPath.toFile();
				if (testFile.exists()) {
					testFile.delete();
				}
				final int fd = ONative.instance().open(testPath.toAbsolutePath().toString(),
					ONative.O_WRONLY | ONative.O_CREAT | ONative.O_EXCL | ONative.O_APPEND | ONative.O_DIRECT);
				ONative.instance().close(fd);
				return testFile.delete();
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Got an error while testing for directIO support. This is to be expected when directIO is not supported.", e);
				}
				return false;
			}
		}
		// DirectIO is a linux feature
		return false;
	}

}
