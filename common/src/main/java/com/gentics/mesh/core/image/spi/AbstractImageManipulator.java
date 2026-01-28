package com.gentics.mesh.core.image.spi;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.image.CacheFileInfo;
import com.gentics.mesh.core.image.ImageInfo;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.jobs.ImageCacheMigrationProcessor;
import com.gentics.mesh.etc.config.ImageManipulationMode;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.image.ImageManipulation;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

/**
 * Abstract image manipulator implementation.
 */
public abstract class AbstractImageManipulator implements ImageManipulator {
	private static final String IMAGE_CACHE_CLEANER_THREAD_NAME = "mesh-image-cache-cleaner";

	private static final Logger log = LoggerFactory.getLogger(AbstractImageManipulator.class);

	protected ImageManipulatorOptions options;

	protected long imageCacheCleanIntervalInSeconds;

	protected long imageCacheMaxIdleInSeconds;

	protected Vertx vertx;

	/**
	 * Executor service for running the image cache cleaner
	 */
	private ScheduledExecutorService imageCacheCleanerService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, IMAGE_CACHE_CLEANER_THREAD_NAME);
		}
	});

	/**
	 * scheduled image cache cleaner
	 */
	private ScheduledFuture<?> imageCacheCleaner;

	public AbstractImageManipulator(Vertx vertx, ImageManipulatorOptions options) {
		this.vertx = vertx;
		this.options = options;

		imageCacheCleanIntervalInSeconds = Duration.parse(options.getImageCacheCleanInterval()).get(ChronoUnit.SECONDS);
		imageCacheMaxIdleInSeconds = Duration.parse(options.getImageCacheMaxIdle()).get(ChronoUnit.SECONDS);

		startImageCacheCleaner();
	}

	@Override
	public Single<CacheFileInfo> getCacheFilePath(HibBinary binary, ImageManipulation parameters) {
		ImageManipulationMode mode = options.getMode();

		switch (mode) {
		case OFF:
			throw error(BAD_REQUEST, "image_error_turned_off");
		case MANUAL:
		case ON_DEMAND:
			break;
		}

		String sha512 = binary.getSHA512Sum();
		return getCacheFilePathNew(binary, parameters).onErrorResumeNext(e -> {
			if (log.isDebugEnabled()) {
				log.debug("New Image Cache miss", e);
			}
			return getCacheFilePathOld(
					sha512, 
					parameters, 
					Optional.ofNullable(e)
						.filter(CacheFileNotFoundException.class::isInstance)
						.map(CacheFileNotFoundException.class::cast)
						.map(CacheFileNotFoundException::getFilePath));
		});
	}

	@Override
	public void shutdown() {
		stopImageCacheCleaner();
	}

	protected Single<CacheFileInfo> getCacheFilePathNew(HibBinary binary, ImageManipulation parameters) {
		FileSystem fs = vertx.fileSystem();

		String baseFolder = Paths.get(options.getImageCacheDirectory(), ImageCacheMigrationProcessor.getSegmentedPath(binary.getUuid())).toString();
		String baseName = binary.getUuid() + "-" + parameters.getCacheKey();

		return fs.rxMkdirs(baseFolder)
			// Vert.x uses Files.createDirectories internally, which will not fail when the folder already exists.
			// See https://github.com/eclipse-vertx/vert.x/issues/3029
			.andThen(fs.rxReadDir(baseFolder, baseName + "(\\..*)?"))
			.map(foundFiles -> {
				int numFiles = foundFiles.size();
				if (numFiles == 0) {
					String retPath = Paths.get(baseFolder, baseName).toString();
					if (log.isDebugEnabled()) {
						log.debug("No cache file found for base path {" + retPath + "}");
					}
					// TODO uncomment when getCacheFilePathOld() is removed
					//return new CacheFileInfo(retPath, false);
					throw new CacheFileNotFoundException(retPath);
				}
	
				if (numFiles > 1) {
					String indent = System.lineSeparator() + "    - ";
					log.warn(
						"More than one cache file found:"
							+ System.lineSeparator() + "  uuid: " + binary.getUuid()
							+ System.lineSeparator() + "  key: " + parameters.getCacheKey()
							+ System.lineSeparator() + "  files:"
							+ indent
							+ String.join(indent, foundFiles)
							+ System.lineSeparator()
							+ "The cache directory {" + options.getImageCacheDirectory() + "} should be cleared");
				}
	
				if (log.isDebugEnabled()) {
					log.debug("Using cache file {" + foundFiles.size() + "}");
				}
				return new CacheFileInfo(foundFiles.get(0), true);
			});
	}

	@Deprecated
	protected Single<CacheFileInfo> getCacheFilePathOld(String sha512sum, ImageManipulation parameters, Optional<String> maybeNewPath) {
		FileSystem fs = vertx.fileSystem();

		String[] parts = sha512sum.split("(?<=\\G.{8})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}

		String baseFolder = Paths.get(options.getImageCacheDirectory(), buffer.toString()).toString();
		String baseName = "image-" + parameters.getCacheKey();
		String retPath = Paths.get(baseFolder, baseName).toString();

		return fs.rxExists(baseFolder).flatMap(exists -> {
			if (exists) {
				return fs.rxReadDir(baseFolder, baseName + "(\\..*)?").flatMap(foundFiles -> {
					int numFiles = foundFiles.size();
					if (numFiles == 0) {
						if (log.isDebugEnabled()) {
							log.debug("No cache file found for base path {" + retPath + "}");
						}
						return Single.just(new CacheFileInfo(maybeNewPath.orElse(retPath), false));
					}

					if (numFiles > 1) {
						String indent = System.lineSeparator() + "    - ";

						log.warn(
							"More than one cache file found:"
								+ System.lineSeparator() + "  hash: " + sha512sum
								+ System.lineSeparator() + "  key: " + parameters.getCacheKey()
								+ System.lineSeparator() + "  files:"
								+ indent
								+ String.join(indent, foundFiles)
								+ System.lineSeparator()
								+ "The cache directory {" + options.getImageCacheDirectory() + "} should be cleared");
					}

					if (log.isDebugEnabled()) {
						log.debug("Using cache file {" + foundFiles.size() + "}");
					}
					return Single.just(new CacheFileInfo(foundFiles.get(0), true));
				});
			} else {
				return Single.just(new CacheFileInfo(maybeNewPath.orElse(retPath), false));
			}
		});
	}

	@Override
	public Single<ImageInfo> readImageInfo(String path) {
		Maybe<ImageInfo> result = vertx.rxExecuteBlocking(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Reading image information from stream");
			}
			File file = new File(path);
			if (!file.exists()) {
				log.error("The image file {" + file.getAbsolutePath() + "} could not be found.");
				throw error(BAD_REQUEST, "image_error_reading_failed");
			}
			BufferedImage image = readFromFile(file);
			if (image == null) {
				throw error(BAD_REQUEST, "image_error_reading_failed");
			} else {
				return toImageInfo(image);
			}
		}, false);
		return result.toSingle();
	}

	/**
	 * Read the image from the given file into a {@link BufferedImage}
	 * @param imageFile image file
	 * @return buffered image
	 * @throws IOException
	 */
	abstract protected BufferedImage readFromFile(File imageFile) throws IOException;

	/**
	 * Extract the image information from the given buffered image.
	 * 
	 * @param bi
	 * @return
	 */
	private ImageInfo toImageInfo(BufferedImage bi) {
		ImageInfo info = new ImageInfo();
		info.setWidth(bi.getWidth());
		info.setHeight(bi.getHeight());
		int[] rgb = calculateDominantColor(bi);
		// By default we assume white for the images
		String colorHex = "#FFFFFF";
		if (rgb.length >= 3) {
			colorHex = "#" + Integer.toHexString(rgb[0]) + Integer.toHexString(rgb[1]) + Integer.toHexString(rgb[2]);
		}
		info.setDominantColor(colorHex);
		return info;
	}

	/**
	 * Start the image cache cleaner, if configured to do so and not started before
	 */
	private void startImageCacheCleaner() {
		if (imageCacheCleaner == null && options.getImageCacheDirectory() != null
				&& imageCacheCleanIntervalInSeconds > 0) {
			if (log.isDebugEnabled()) {
				log.debug("Starting image cache cleaner");
			}
			imageCacheCleaner = imageCacheCleanerService.scheduleAtFixedRate(
					new ImageCacheCleaner(new File(options.getImageCacheDirectory()), imageCacheMaxIdleInSeconds), 0,
					imageCacheCleanIntervalInSeconds, TimeUnit.SECONDS);
		}
	}

	/**
	 * Stop the image cache cleaner (if started before)
	 */
	private void stopImageCacheCleaner() {
		if (imageCacheCleaner != null) {
			if (log.isDebugEnabled()) {
				log.debug("Stopping image cache cleaner");
			}
			imageCacheCleaner.cancel(true);
			imageCacheCleaner = null;
		}
	}

	@Deprecated
	private static final class CacheFileNotFoundException extends RuntimeException {

		private static final long serialVersionUID = -5565739543146663515L;

		private final String filePath;

		public String getFilePath() {
			return filePath;
}

		public CacheFileNotFoundException(String filePath) {
			this.filePath = filePath;
		}
	}
}
