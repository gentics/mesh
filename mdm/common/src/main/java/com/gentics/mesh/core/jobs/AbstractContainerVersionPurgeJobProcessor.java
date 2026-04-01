package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingContainerDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.NameOrUUIDsRequest;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.job.warning.JobWarning;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.json.JsonUtil;
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
public abstract class AbstractContainerVersionPurgeJobProcessor<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
			M extends FieldSchemaContainer
		> implements SingleJobProcessor {

	protected final Database db;
	protected final PersistingJobDao jobDao;
	protected final PersistingContainerDao<R, RM, RE, SC, SCV, M> containerDao;
	protected final PersistingProjectDao projectDao;
	protected final PersistingBranchDao branchDao;

	public AbstractContainerVersionPurgeJobProcessor(Database db, PersistingJobDao jobDao, PersistingContainerDao<R, RM, RE, SC, SCV, M> containerDao,
			PersistingProjectDao projectDao, PersistingBranchDao branchDao) {
		this.containerDao = containerDao;
		this.projectDao = projectDao;
		this.branchDao = branchDao;
		this.jobDao = jobDao;
		this.db = db;
	}

	@Override
	public Completable process(HibJob job) {
		return Completable.defer(() -> purge(job)).doOnComplete(() -> {
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
	protected Completable purge(HibJob job) {
		return Completable.defer(() -> {
			NameOrUUIDsRequest request = JsonUtil.readValue(job.getQuery(), NameOrUUIDsRequest.class, true);
			Set<Pair<String, String>> affectedVersions = db.tx(tx -> {
				Set<String> usedVersionUuids = containerDao.findActiveSchemaVersions().stream()
						.map(version -> version.getUuid())
						.collect(Collectors.toSet());

				return containerDao.findAll().stream()
						.flatMap(ms -> StreamUtil.toStream(containerDao.findAllVersions(ms)))
						.filter(msv -> !usedVersionUuids.contains(msv.getUuid()))
						.filter(msv -> {
							if (request != null && request.getData() != null) {
								if (request.isExcluded()) {
									return !request.getData().contains(msv.getSchemaContainer().getUuid()) && !request.getData().contains(msv.getName());
								} else {
									return request.getData().contains(msv.getSchemaContainer().getUuid()) || request.getData().contains(msv.getName());
								}
							} else {
								return true;
							}
						})
						.filter(msv -> containerDao.countVersionEdges(msv) < 1)
						.map(msv -> Pair.of(msv.getUuid(), msv.getSchemaContainer().getUuid()))
						.collect(Collectors.toSet());
			});

			return affectedVersions.isEmpty() 
					? Completable.complete()
					: Completable.concat(affectedVersions.stream()
							.map(pair -> db.asyncTx(() -> {
								SC sc = containerDao.findByUuid(pair.getRight());
								SCV scv = containerDao.findVersionByUuid(sc, pair.getLeft());
								containerDao.deleteVersion(scv);
								job.getWarnings().add(new JobWarning().setType("purged").setMessage(scv.getName() + " / " + scv.getVersion() + " / " + scv.getUuid()));
							})).collect(Collectors.toList()));
		}).doOnComplete(() -> {
			db.tx(() -> jobDao.mergeIntoPersisted(job));
		});
	}
}
