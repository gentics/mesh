package com.gentics.mesh.core.image.spi;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.etc.config.ImageManipulatorOptions;

/**
 * The Image Cache Cleaner will check all files in the configured image cache directory.
 * Files older than the configured {@link ImageManipulatorOptions#getImageCacheMaxIdle()} will be deleted.
 * The "age" of a file is either the last access time or last modification time of the file, depending on which is newer.
 */
public class ImageCacheCleaner implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ImageCacheCleaner.class);

	protected File imageCacheDirectory;

	protected long maxIdleInSeconds;

	/**
	 * Create an instance for cleaning the image cache directory
	 * @param imageCacheDirectory image cache directory
	 * @param maxIdleInSeconds max allowed file age in seconds
	 */
	public ImageCacheCleaner(File imageCacheDirectory, long maxIdleInSeconds) {
		this.imageCacheDirectory = imageCacheDirectory;
		this.maxIdleInSeconds = maxIdleInSeconds;
	}

	@Override
	public void run() {
		long start = System.currentTimeMillis();
		AtomicLong checkCounter = new AtomicLong();
		AtomicLong cleanCounter = new AtomicLong();
		FileTime oldestAllowedAccessTime = FileTime.from(System.currentTimeMillis() / 1000L - maxIdleInSeconds, TimeUnit.SECONDS);

		if (log.isDebugEnabled()) {
			log.debug("Start cleaning image cache {} from all files accessed before {}", imageCacheDirectory.getAbsolutePath(), oldestAllowedAccessTime);
		}

		try {
			Files.walkFileTree(imageCacheDirectory.toPath(), new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					checkCounter.incrementAndGet();

					// get the last access time or last modified time, depending on which is later
					// since some filesystems will not set the last access time
					FileTime lastAccessTime = attrs.lastAccessTime();
					FileTime lastModifiedTime = attrs.lastModifiedTime();
					FileTime referenceTime = lastAccessTime.compareTo(lastModifiedTime) > 0 ? lastAccessTime
							: lastModifiedTime;

					if (referenceTime.toMillis() > 0) {
						if (referenceTime.compareTo(oldestAllowedAccessTime) < 0) {
							if (log.isTraceEnabled()) {
								log.trace("{} was last accessed at {} and will be removed", file, referenceTime);
							}
							Files.delete(file);
							cleanCounter.incrementAndGet();
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error("Error while cleaning image cache {}", e, imageCacheDirectory.getAbsolutePath());
		} finally {
			long duration = System.currentTimeMillis() - start;

			if (log.isDebugEnabled()) {
				log.debug("Finished cleaning image cache {}. Duration; {} ms, checked {} files, cleaned {} files.",
						imageCacheDirectory.getAbsolutePath(), duration, checkCounter.get(), cleanCounter.get());
			}
		}
	}
}
