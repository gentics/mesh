package com.gentics.mesh.core.endpoint.admin.consistency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.util.UUIDUtil;
import com.github.jknack.handlebars.internal.lang3.tuple.Pair;

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
			boolean deleteFolderTrace = tx.data().options().getUploadOptions().isDeleteFolderTrace();
			int count = 0;
			do {
				Map<Path, String> deletable = Files.walk(uploadsPath)
					.filter(path -> Files.isRegularFile(path))
					.map(path -> {
						String filename = path.getFileName().toString();
						String binaryUuid = null;
						if (filename.lastIndexOf(".") < 1 
								|| (binaryUuid = filename.substring(0, filename.lastIndexOf("."))).isBlank() 
								|| !UUIDUtil.isUUID(binaryUuid) 
								|| (tx.binaries().findByUuid(binaryUuid).runInExistingTx(Tx.get()) == null && tx.<CommonTx>unwrap().imageVariantDao().findByUuid(binaryUuid) == null)) {
						} else {
							if (log.isDebugEnabled()) {
								log.debug("Binary data valid: " + path);
							}
						}
						return Pair.of(path, binaryUuid);
					}).filter(pair -> pair.getValue() == null)
					.limit(BulkActionContextImpl.DEFAULT_BATCH_SIZE)
					.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
					count = deletable.size();

					deletable.entrySet().stream()
						.forEach(pair -> {
							boolean repaired = false;
							if (attemptRepair) {
								log.info("Binary data is invalid and will be moved to the tmpdir: " + pair.getKey());
		
								String segments = uploadsPath.relativize(pair.getKey().getParent()).toString();
								Path tmpSegmentsPath = tmpPath.resolve(segments);
								try {
									Files.createDirectories(tmpSegmentsPath);
									Files.move(pair.getKey(), tmpSegmentsPath.resolve(pair.getKey().getFileName()));
									Path parent = pair.getKey().getParent().toAbsolutePath();
									while (deleteFolderTrace && parent != null && parent.compareTo(uploadsPath) != 0 && Files.list(parent).count() < 1 && Files.deleteIfExists(parent)) {
										parent = parent.getParent();
									}
									repaired = true;
								} catch (IOException e) {
									log.error("Could not move file " + pair.getKey(), e);
								}
							}
							result.addInconsistency("No binary record found for " + pair.getKey(), pair.getValue(), InconsistencySeverity.LOW, attemptRepair & repaired, RepairAction.DELETE);
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
}
