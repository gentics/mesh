package com.gentics.mesh.graphdb.check;

import java.io.File;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.etc.config.DiskQuotaOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * {@link Runnable} which checks the disk quota
 */
public class DiskQuotaChecker implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(DiskQuotaChecker.class);

	private final File storageDirectory;

	private final DiskQuotaOptions options;

	private final Consumer<Boolean> resultConsumer;

	/**
	 * Create an instance
	 * @param storageDirectory storage directory
	 * @param options disk quota options
	 * @param resultConsumer result consumer
	 */
	public DiskQuotaChecker(File storageDirectory, DiskQuotaOptions options, Consumer<Boolean> resultConsumer) {
		this.storageDirectory = storageDirectory;
		this.options = options;
		this.resultConsumer = resultConsumer;
	}

	@Override
	public void run() {
		try {
			long absoluteReadOnlyThreshold = options.getAbsoluteReadOnlyThreshold(storageDirectory);
			long absoluteWarnThreshold = options.getAbsoluteWarnThreshold(storageDirectory);
			long usableSpace = storageDirectory.getUsableSpace();
			long totalSpace = storageDirectory.getTotalSpace();
			long quota = totalSpace > 0 ? usableSpace * 100 / totalSpace : 0;

			if (log.isDebugEnabled()) {
				log.debug(String.format("Warn below %s, read-only below %s",
						FileUtils.byteCountToDisplaySize(absoluteWarnThreshold),
						FileUtils.byteCountToDisplaySize(absoluteReadOnlyThreshold)));
			}

			if (absoluteReadOnlyThreshold > 0 && usableSpace < absoluteReadOnlyThreshold) {
				resultConsumer.accept(true);
				log.error(String.format("Total space: %s, usable: %s (%d%%)",
						FileUtils.byteCountToDisplaySize(totalSpace), FileUtils.byteCountToDisplaySize(usableSpace),
						quota));
			} else if (absoluteWarnThreshold > 0 && usableSpace < absoluteWarnThreshold) {
				resultConsumer.accept(false);
				// warn
				log.warn(String.format("Total space: %s, usable: %s (%d%%)",
						FileUtils.byteCountToDisplaySize(totalSpace), FileUtils.byteCountToDisplaySize(usableSpace),
						quota));
			} else {
				resultConsumer.accept(false);
				log.info(String.format("Total space: %s, usable: %s (%d%%)",
						FileUtils.byteCountToDisplaySize(totalSpace), FileUtils.byteCountToDisplaySize(usableSpace),
						quota));
			}
		} catch (Throwable e) {
			log.error("Error while checking disk quota", e);
		}
	}
}
