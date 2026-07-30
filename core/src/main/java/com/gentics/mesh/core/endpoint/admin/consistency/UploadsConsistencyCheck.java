package com.gentics.mesh.core.endpoint.admin.consistency;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.util.UUIDUtil;

public class UploadsConsistencyCheck implements ConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(UploadsConsistencyCheck.class);

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx1, boolean attemptRepair) {
		Transactional<ConsistencyCheckResult> action = db.transactional(tx -> {
			log.info("Uploads consistency " + (attemptRepair ? "repair" : "check") + " started");
			ConsistencyCheckResult result = new ConsistencyCheckResult();
			Path uploadsPath = Path.of(tx.data().options().getUploadOptions().getDirectory());
			Path tmpPath = Path.of(tx.data().options().getUploadOptions().getTempDirectory());
			
			Files.walkFileTree(uploadsPath, new FileVisitor<>() {

				@Override
				public FileVisitResult postVisitDirectory(Path arg0, IOException arg1) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path arg0, BasicFileAttributes arg1) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes arg1) throws IOException {
					String filename = path.getFileName().toString();
					String binaryUuid = null;
					if (filename.lastIndexOf(".") < 1 
							|| (binaryUuid = filename.substring(0, filename.lastIndexOf("."))).isBlank() 
							|| !UUIDUtil.isUUID(binaryUuid) 
							|| (tx.binaries().findByUuid(binaryUuid).runInExistingTx(Tx.get()) == null && tx.<CommonTx>unwrap().imageVariantDao().findByUuid(binaryUuid) == null)) {
						boolean repaired = false;
						if (attemptRepair) {
							log.info("Binary data is invalid and will be moved to the tmpdir: " + path);
	
							String segments = uploadsPath.relativize(path.getParent()).toString();
							Path tmpSegmentsPath = tmpPath.resolve(segments);
							try {
								Files.createDirectories(tmpSegmentsPath);
								Files.move(path, tmpSegmentsPath.resolve(path.getFileName()));
								repaired = true;
							} catch (IOException e) {
								log.error("Could not move file " + path, e);
							}
						}
						result.addInconsistency("No binary record found for " + path, binaryUuid, InconsistencySeverity.LOW, attemptRepair & repaired, RepairAction.DELETE);
					} else {
						if (log.isDebugEnabled()) {
							log.debug("Binary data valid: " + path);
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path arg0, IOException arg1) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
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
}
