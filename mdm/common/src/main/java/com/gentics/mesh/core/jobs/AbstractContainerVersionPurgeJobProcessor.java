package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.util.Map;
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
import com.gentics.mesh.core.result.Result;
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

				Map<String, Long> contentCounts = containerDao.countVersionEdges();

				Result<? extends SC> result = containerDao.findAll();

				return result.stream()
						.filter(ms -> {
							if (request != null && request.getData() != null) {
								if (request.isExcluded()) {
									return !request.getData().contains(ms.getUuid()) && !request.getData().contains(ms.getName());
								} else {
									return request.getData().contains(ms.getUuid()) || request.getData().contains(ms.getName());
								}
							} else {
								return true;
							}
						})
						.flatMap(ms -> {
							String schemaUuid = ms.getUuid();
							Iterable<? extends SCV> versions = containerDao.findAllVersions(ms);
							return StreamUtil.toStream(versions).map(msv -> msv.getUuid())
									.filter(uuid -> !usedVersionUuids.contains(uuid))
									.filter(uuid -> contentCounts.getOrDefault(uuid, 0L) < 1)
									.map(uuid -> Pair.of(uuid, schemaUuid));
						}).collect(Collectors.toSet());
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
