package com.gentics.mesh.core.endpoint.admin.consistency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UploadsConsistencyCheck implements ConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(UploadsConsistencyCheck.class);

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx1, boolean attemptRepair) {
		Transactional<ConsistencyCheckResult> action = db.transactional(tx -> {
			log.info("Uploads cleanup started, repair: " + attemptRepair);
			ConsistencyCheckResult result = new ConsistencyCheckResult();
			Path uploadsPath = Path.of(tx.data().options().getUploadOptions().getDirectory());
			Path tmpPath = Path.of(tx.data().options().getUploadOptions().getTempDirectory());
			Files.walk(uploadsPath)
				.filter(path -> Files.isRegularFile(path))
				.map(path -> {
					String filename = uploadsPath.relativize(path.getParent()).toString();
					String binaryUuid = null;
					if (filename.lastIndexOf(".") < 1 || (binaryUuid = filename.substring(0, filename.lastIndexOf("."))).isBlank() || !UUIDUtil.isUUID(binaryUuid) || tx.binaries().findByUuid(binaryUuid).runInExistingTx(Tx.get()) == null) {
						if (attemptRepair) {
							log.info("Binary data is invalid and will be moved to the tmpdir: " + path);

							String segments = getSegmentedPath(binaryUuid);
							Path tmpSegmentsPath = tmpPath.resolve(segments);
							try {
								Files.createDirectories(tmpSegmentsPath);
								Files.move(path, tmpSegmentsPath.resolve(filename));
								result.addInconsistency("No binary record found for " + path, binaryUuid, InconsistencySeverity.LOW, attemptRepair, RepairAction.DELETE);
							} catch (IOException e) {
								log.error("Could not copy file " + path, e);
							}
						} else {
							result.addInconsistency("No binary record found for " + path, binaryUuid, InconsistencySeverity.LOW);
						}
					} else {
						if (log.isDebugEnabled()) {
							log.debug("Binary data valid: " + path);
						}
					}
					return path;
				}).forEach(path -> {
					Path parent = path.getParent().toAbsolutePath();
					try {
						while (parent != null && parent.compareTo(uploadsPath) != 0 && Files.list(parent).count() < 1 && Files.deleteIfExists(parent)) {
							parent = parent.getParent();
						}
					} catch (IOException e) {
						log.error("Error cleaning up binary dir trace for " + path, e);
					}
				});
			log.info("Uploads cleanup finished successfully. Please clean your tmpdir right away, if required.");
			return result;
		});
		return action.runInExistingTx(tx1);
	}

	@Override
	public String getName() {
		return "uploads";
	}

	@Override
	public boolean asyncOnly() {
		return true;
	}

	/**
	 * Generate the segmented path for the given binary uuid. Keep in sync to the implementation of LocalBinaryStorageImpl!
	 * 
	 * @param binaryUuid
	 * @return
	 */
	private static String getSegmentedPath(String binaryUuid) {
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
