package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import java.util.List;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;
import com.google.common.collect.Lists;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class KoflCleanup extends AbstractConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(KoflCleanup.class);

	@Override
	public String getName() {
		return "kofl-cleanup";
	}

	@Override
	public long getBatchSize() {
		return 10L;
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, NodeImpl.class, (node, result) -> {
			purgeNodeVersions(tx, node, result, attemptRepair);
		}, attemptRepair, tx);
	}

	private void purgeNodeVersions(Tx tx, NodeImpl node, ConsistencyCheckResult result, boolean attemptRepair) {
		Iterable<? extends NodeGraphFieldContainer> initials = node.getAllInitialGraphFieldContainers();
		if (attemptRepair) {
			for (NodeGraphFieldContainer initial : initials) {
				Long counter = 0L;
				purgeVersion(tx, counter, null, initial, initial, false);
			}
		}
	}

	/**
	 * Invoke the purge action on the given container.
	 * 
	 * @param tx
	 * @param txCounter
	 * @param bac
	 *            Action context for the removal operation
	 * @param lastRemaining
	 *            Previous version which remained
	 * @param version
	 *            Current version to be checked for removal
	 * @param previousRemoved
	 *            Flag which indicated whether the previous version has been removed
	 */
	private void purgeVersion(Tx tx, Long txCounter, BulkActionContext bac, NodeGraphFieldContainer lastRemaining, NodeGraphFieldContainer version,
		boolean previousRemoved) {
		List<? extends NodeGraphFieldContainer> nextVersions = Lists.newArrayList(version.getNextVersions());

		if (isPurgeable(version)) {
			log.info("Purging container " + version.getUuid() + "@" + version.getVersion());
			// Delete this version - This will also take care of removing the version references
			// version.delete(bac);
			version.remove();
			previousRemoved = true;
			txCounter++;
		} else {
			// We found a version which is not removable. So link it to the last remaining.
			if (previousRemoved) {
				log.info("Linking {" + lastRemaining.getUuid() + "@" + lastRemaining.getVersion() + " to " + version.getUuid() + "@"
					+ version.getVersion());
				// We only need to link to the previous version if it has been removed in an earlier step
				lastRemaining.setNextVersion(version);
				txCounter++;
			}
			if (txCounter % getBatchSize() == 0 && txCounter != 0) {
				log.info("Comitting batch - Elements handled {" + txCounter + "}");
				tx.getGraph().commit();
			}
			// Update the reference since this version is now the last remaining because it was not removed
			lastRemaining = version;
			previousRemoved = false;
		}

		// Continue with next versions
		for (NodeGraphFieldContainer next : nextVersions) {
			purgeVersion(tx, txCounter, bac, lastRemaining, next, previousRemoved);
		}

	}

	private boolean isPurgeable(NodeGraphFieldContainer version) {
		// The container is purgeable if no edge (publish, draft, initial) exists to its node.
		return !version.inE(HAS_FIELD_CONTAINER).hasNext();
	}

}
