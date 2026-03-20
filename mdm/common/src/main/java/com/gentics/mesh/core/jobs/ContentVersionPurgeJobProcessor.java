package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingContainerDao;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.util.StreamUtil;

import io.reactivex.Completable;

/**
 * Abstract container schema version processor
 * 
 * @param <R>
 * @param <RM>
 * @param <RE>
 * @param <SC>
 * @param <SCV>
 * @param <M>
 */
public abstract class ContentVersionPurgeJobProcessor<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
			M extends FieldSchemaContainer
		> implements SingleJobProcessor {

	private static final Logger log = LoggerFactory.getLogger(ContentVersionPurgeJobProcessor.class);

	protected final Database db;
	protected final PersistingContainerDao<R, RM, RE, SC, SCV, M> containerDao;
	protected final PersistingProjectDao projectDao;
	protected final PersistingBranchDao branchDao;

	public ContentVersionPurgeJobProcessor(Database db, PersistingContainerDao<R, RM, RE, SC, SCV, M> containerDao,
			PersistingProjectDao projectDao, PersistingBranchDao branchDao) {
		this.containerDao = containerDao;
		this.projectDao = projectDao;
		this.branchDao = branchDao;
		this.db = db;
	}

	@Override
	public Completable process(HibJob job) {
		return Completable.defer(() -> purge()).doOnComplete(() -> {
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
	 * Create an async purge completable.
	 * 
	 * @return
	 */
	protected Completable purge() {
		return Completable.defer(() -> db.asyncTx(() -> {
			Set<String> usedVersionUuids = containerDao.findActiveSchemaVersions().stream()
					.map(version -> version.getUuid())
					.collect(Collectors.toSet());

			containerDao.findAll().stream()
					.flatMap(ms -> StreamUtil.toStream(containerDao.findAllVersions(ms)))
					.filter(msv -> !usedVersionUuids.contains(msv.getUuid()))
					.filter(msv -> containerDao.countVersionEdges(msv) < 1)
					.forEach(msv -> containerDao.deleteVersion(msv));
		}));
	}
}
