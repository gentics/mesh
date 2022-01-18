package com.gentics.mesh.core.project.maintenance;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.DateUtils;
import com.google.common.collect.Lists;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see ProjectVersionPurgeHandler
 */
@Singleton
public class ProjectVersionPurgeHandlerImpl implements ProjectVersionPurgeHandler {

	private static final Logger log = LoggerFactory.getLogger(ProjectVersionPurgeHandlerImpl.class);

	private final Database db;

	private final Provider<BulkActionContext> bulkProvider;

	private final MeshOptions meshOptions;

	@Inject
	public ProjectVersionPurgeHandlerImpl(Database db, Provider<BulkActionContext> bulkProvider, MeshOptions meshOptions) {
		this.db = db;
		this.bulkProvider = bulkProvider;
		this.meshOptions = meshOptions;
	}

	@Override
	public Completable purgeVersions(HibProject project, ZonedDateTime maxAge) {
		return Completable.fromAction(() -> {
			db.tx(tx -> {
				BulkActionContext bac = bulkProvider.get();
				for (HibNode node : tx.nodeDao().findAll(project)) {
					purgeNode(tx, node, maxAge, bac);
				}
				bac.process(true);
			});
		});
	}

	private void purgeNode(Tx tx, HibNode node, ZonedDateTime maxAge, BulkActionContext bac) {
		Iterable<? extends HibNodeFieldContainer> initials = tx.contentDao().getFieldContainers(node, ContainerType.INITIAL);
		for (HibNodeFieldContainer initial : initials) {
			Long counter = 0L;
			purgeVersion(tx, counter, bac, initial, initial, false, maxAge);
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
	private void purgeVersion(Tx tx, Long txCounter, BulkActionContext bac, HibNodeFieldContainer lastRemaining, HibNodeFieldContainer version,
		boolean previousRemoved, ZonedDateTime maxAge) {

		ContentDao contentDao = tx.contentDao();
		// We need to load some information first since we may remove the version in this step
		List<HibNodeFieldContainer> nextVersions = Lists.newArrayList(contentDao.getNextVersions(version));
		boolean isNewerThanMaxAge = maxAge != null && !isOlderThanMaxAge(version, maxAge);
		boolean isInTimeFrame = maxAge == null || isOlderThanMaxAge(version, maxAge);

		if (isInTimeFrame && contentDao.isPurgeable(version)) {
			log.info("Purging container " + version.getUuid() + "@" + version.getVersion());
			// Delete this version - This will also take care of removing the version references
			contentDao.delete(version, bac, false);
			previousRemoved = true;
			txCounter++;
		} else {
			// We found a version which is not removable. So link it to the last remaining.
			if (previousRemoved) {
				log.info("Linking {" + lastRemaining.getUuid() + "@" + lastRemaining.getVersion() + " to " + version.getUuid() + "@"
					+ version.getVersion());
				// We only need to link to the previous version if it has been removed in an earlier step
				contentDao.setNextVersion(lastRemaining, version);
				txCounter++;
			}
			if (txCounter % meshOptions.getVersionPurgeMaxBatchSize() == 0 && txCounter != 0) {
				log.info("Committing batch - Elements handled {" + txCounter + "}");
				tx.commit();
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
			for (HibNodeFieldContainer next : nextVersions) {
				purgeVersion(tx, txCounter, bac, lastRemaining, next, previousRemoved, maxAge);
			}
		}
	}

	private boolean isOlderThanMaxAge(HibNodeFieldContainer version, ZonedDateTime maxAge) {
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
