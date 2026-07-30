package com.gentics.mesh.core.endpoint.admin.consistency;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;

public class BinariesConsistencyCheck implements ConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(BinariesConsistencyCheck.class);

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx1, boolean attemptRepair) {
		Transactional<ConsistencyCheckResult> action = db.transactional(tx -> {
			log.info("Binaries check started");
			BinaryStorage bst = tx.data().binaryStorage();
			ConsistencyCheckResult result = new ConsistencyCheckResult();
			tx.binaries().findAll().runInExistingTx(tx)
			.filter(bin -> !new File(bst.getFilePath(bin.getUuid())).exists()).forEach(bin -> result.addInconsistency("No binary data found for the binary", bin.getUuid(), InconsistencySeverity.CRITICAL));
			log.info("Binaries check finished.");
			return result;
		});
		return action.runInExistingTx(tx1);
	}

	@Override
	public String getName() {
		// `binaries` key belongs to `BinaryCheck` :(
		return "binarydata";
	}

	@Override
	public boolean asyncOnly() {
		return true;
	}
}
