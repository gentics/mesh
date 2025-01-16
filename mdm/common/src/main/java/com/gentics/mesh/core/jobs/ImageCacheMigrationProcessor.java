package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Migrate the image cache onto a new simplified structure.
 * 
 * @deprecated has to be removed in some future, as the corresponding parts of AbstractImageManipulator.
 */
@Deprecated
public class ImageCacheMigrationProcessor implements SingleJobProcessor {

	private static final Logger log = LoggerFactory.getLogger(ImageCacheMigrationProcessor.class);

	private final MeshOptions options;
	private final BinaryDao dao;
	private final Database db;

	@Inject
	public ImageCacheMigrationProcessor(Database db, MeshOptions options, BinaryDao dao) {
		this.options = options;
		this.dao = dao;
		this.db = db;
	}

	@Override
	public Completable process(HibJob job) {
		return Completable.defer(() -> {
			return db.asyncTx(() -> {
				log.info("Image cache migration started");
				Path imageCachePath = Path.of(options.getImageOptions().getImageCacheDirectory());

				// walk the whole tree
				Files.walkFileTree(imageCachePath, new FileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						// continue from the root directory
						if (dir.equals(imageCachePath)) {
							return FileVisitResult.CONTINUE;
						}
						// continue with directories, which are of the old structure (identifiable by directory names with 8 characters)
						return StringUtils.length(dir.toFile().getName()) == 8 ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						String fileName = file.toFile().getName();
						// files that start with "image-" and are located in the old structure need to be migrated
						if (fileName.startsWith("image-")) {
							String sha512Hash = imageCachePath.relativize(file.getParent()).toString().replace("/", "").replace("\\", "");
							HibBinary binary = null;
							if (sha512Hash.length() == 128 && (binary = dao.findByHash(sha512Hash).runInExistingTx(Tx.get())) != null) {
								String uuid = binary.getUuid();
								String segments = getSegmentedPath(uuid);
								Path segmentsPath = Path.of(options.getImageOptions().getImageCacheDirectory(), segments);
								try {
									Files.createDirectories(segmentsPath);
									Files.move(file, segmentsPath.resolve(uuid + "-" + fileName.replace("image-", "")));
								} catch (IOException e) {
									log.error("Could not copy old cached file " + file, e);
								}
							} else if (sha512Hash.length() == 128) {
								log.info("Binary not found: " + sha512Hash + " deleting file " + file);
								Files.delete(file);
							} else {
								log.info("Not a SHA512: " + sha512Hash);
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
						try {
							Files.delete(dir);
						} catch (DirectoryNotEmptyException e) {
							// fair
						} catch (NoSuchFileException e) {
							// fair
						} catch (IOException e) {
							log.error("Could not delete image cache " + dir, e);
						}
						return FileVisitResult.CONTINUE;
					}
				});

				log.info("Image cache migration finished successfully");
			});
		}).doOnComplete(() -> {
			db.tx(tx -> {
				job.setStopTimestamp();
				job.setStatus(COMPLETED);
				tx.<CommonTx>unwrap().jobDao().mergeIntoPersisted(job);
			});
		}).doOnError(error -> {
			db.tx(tx -> {
				job.setStopTimestamp();
				job.setStatus(FAILED);
				job.setError(error);
				tx.<CommonTx>unwrap().jobDao().mergeIntoPersisted(job);
			});
		});
	}

	/**
	 * Generate the segmented path for the given binary uuid. Unrelated to the implementation of AbstractImageManipulator.
	 * 
	 * @param binaryUuid
	 * @return
	 */
	public static String getSegmentedPath(String binaryUuid) {
		String partA = binaryUuid.substring(0, 2);
		String partB = binaryUuid.substring(2, 4);
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		buffer.append(partA);
		buffer.append(File.separator);
		buffer.append(partB);
		buffer.append(File.separator);
		return buffer.toString();
	}
}
