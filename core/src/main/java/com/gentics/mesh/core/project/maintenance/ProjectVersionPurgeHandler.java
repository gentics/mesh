package com.gentics.mesh.core.project.maintenance;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.DateUtils;
import com.google.common.collect.Lists;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class ProjectVersionPurgeHandler {

	private static final Logger log = LoggerFactory.getLogger(ProjectVersionPurgeHandler.class);

	private final Database db;

	private final Provider<BulkActionContext> bulkProvider;

	@Inject
	public ProjectVersionPurgeHandler(Database db, Provider<BulkActionContext> bulkProvider) {
		this.db = db;
		this.bulkProvider = bulkProvider;
	}

	public long getBatchSize() {
		return 10L;
	}

	/**
	 * Purge the versions of all nodes in the project.
	 * 
	 * @param project
	 * @param maxAge
	 *            Limit the purge operation to versions which exceed the max age.
	 * @return
	 */
	public Completable purgeVersions(Project project, ZonedDateTime maxAge) {
		return Completable.fromAction(() -> {
			db.tx(tx -> {
				NodeRoot nodeRoot = project.getNodeRoot();
				Iterator<? extends Node> it = nodeRoot.findAll().iterator();
				while (it.hasNext()) {
					Node node = it.next();
					purgeNode(tx, node, maxAge);
				}
				return null;
			});
		});
	}

	private void purgeNode(Tx tx, Node node, ZonedDateTime maxAge) {
		Iterable<? extends NodeGraphFieldContainer> initials = node.getGraphFieldContainers(ContainerType.INITIAL);
		for (NodeGraphFieldContainer initial : initials) {
			Long counter = 0L;
			purgeVersion(tx, counter, bulkProvider.get(), initial, initial, false, maxAge);
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
	 * @param maxAge
	 */
	private void purgeVersion(Tx tx, Long txCounter, BulkActionContext bac, NodeGraphFieldContainer lastRemaining, NodeGraphFieldContainer version,
		boolean previousRemoved, ZonedDateTime maxAge) {

		// We need to load some information first since we may remove the version in this step
		List<? extends NodeGraphFieldContainer> nextVersions = Lists.newArrayList(version.getNextVersions());
		boolean isNewerThanMaxAge = maxAge != null && !isOlderThanMaxAge(version, maxAge);
		boolean isInTimeFrame = maxAge == null || isOlderThanMaxAge(version, maxAge);

		if (isInTimeFrame && version.isPurgeable()) {
			log.info("Purging container " + version.getUuid() + "@" + version.getVersion());
			// Delete this version - This will also take care of removing the version references
			version.delete(bac, false);
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
				log.info("Committing batch - Elements handled {" + txCounter + "}");
				tx.getGraph().commit();
			}
			// Update the reference since this version is now the last remaining because it was not removed
			lastRemaining = version;
			previousRemoved = false;
		}

		// Check if a maxage is set and whether the version is newer than the maxage
		if (isNewerThanMaxAge) {
			// We can stop traversing the tree at this point.
			return;
		} else {
			// Continue with next versions
			for (NodeGraphFieldContainer next : nextVersions) {
				purgeVersion(tx, txCounter, bac, lastRemaining, next, previousRemoved, maxAge);
			}
		}
	}

	private boolean isOlderThanMaxAge(NodeGraphFieldContainer version, ZonedDateTime maxAge) {
		Long editTs = version.getLastEditedTimestamp();
		ZonedDateTime editDate = DateUtils.toZonedDateTime(editTs);
		ZonedDateTime maxDate = maxAge;
		if (editDate.isAfter(maxDate)) {
			log.info("Version {" + version.getUuid() + "}@{" + version.getVersion() + "} is not purgable since it was edited {" + editDate
				+ "} which is newer than {" + maxDate + "}");
			return false;
		}
		return true;
	}

}
