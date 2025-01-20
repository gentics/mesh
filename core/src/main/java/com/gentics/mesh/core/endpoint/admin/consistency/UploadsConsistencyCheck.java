package com.gentics.mesh.core.endpoint.admin.consistency;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.db.CommonTx;
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
			log.info("Uploads consistency " + (attemptRepair ? "repair" : "check") + " started");
			ConsistencyCheckResult result = new ConsistencyCheckResult();
			Path uploadsPath = Path.of(tx.data().options().getUploadOptions().getDirectory());
			Path tmpPath = Path.of(tx.data().options().getUploadOptions().getTempDirectory());
			int count = 0;
			do {
				List<Path> deletable = Files.walk(uploadsPath)
					.filter(path -> Files.isRegularFile(path))
					.filter(path -> {
						String filename = path.getFileName().toString();
						String binaryUuid = null;
						if (filename.lastIndexOf(".") < 1 
								|| (binaryUuid = filename.substring(0, filename.lastIndexOf("."))).isBlank() 
								|| !UUIDUtil.isUUID(binaryUuid) 
								|| (tx.binaries().findByUuid(binaryUuid).runInExistingTx(Tx.get()) == null && tx.<CommonTx>unwrap().imageVariantDao().findByUuid(binaryUuid) == null)) {
							result.addInconsistency("No binary record found for " + path, binaryUuid, InconsistencySeverity.LOW, attemptRepair, RepairAction.DELETE);
							return true;
						} else {
							if (log.isDebugEnabled()) {
								log.debug("Binary data valid: " + path);
							}
							return false;
						}
					}).limit(BulkActionContextImpl.DEFAULT_BATCH_SIZE)
					.collect(Collectors.toList());
					count = deletable.size();

					deletable.stream()
						.forEach(path -> {
							if (attemptRepair) {
								log.info("Binary data is invalid and will be moved to the tmpdir: " + path);
		
								String segments = uploadsPath.relativize(path.getParent()).toString();
								Path tmpSegmentsPath = tmpPath.resolve(segments);
								try {
									Files.createDirectories(tmpSegmentsPath);
									Files.move(path, tmpSegmentsPath.resolve(path.getFileName()));
									Path parent = path.getParent().toAbsolutePath();
									while (parent != null && parent.compareTo(uploadsPath) != 0 && Files.list(parent).count() < 1 && Files.deleteIfExists(parent)) {
										parent = parent.getParent();
									}
								} catch (IOException e) {
									log.error("Could not copy file " + path, e);
								}
							}
						});
			} while (count >= BulkActionContextImpl.DEFAULT_BATCH_SIZE);
			log.info("Uploads consistency " + (attemptRepair ? "repair" : "check") + " finished successfully. Please clean your tmpdir right away, if required.");
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
