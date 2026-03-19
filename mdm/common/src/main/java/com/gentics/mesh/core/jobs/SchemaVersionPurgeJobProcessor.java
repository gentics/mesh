package com.gentics.mesh.core.jobs;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.util.StreamUtil;

import io.reactivex.Completable;

public class SchemaVersionPurgeJobProcessor implements SingleJobProcessor {

	public static final Logger log = LoggerFactory.getLogger(SchemaVersionPurgeJobProcessor.class);

	private final Database db;
	private final PersistingJobDao jobDao;
	private final PersistingSchemaDao schemaDao;
	private final PersistingMicroschemaDao microschemaDao;
	private final PersistingProjectDao projectDao;
	private final PersistingBranchDao branchDao;
	private final PersistingContentDao contentDao;

	@Inject
	public SchemaVersionPurgeJobProcessor(Database db, JobDao jobDao, PersistingSchemaDao schemaDao, PersistingMicroschemaDao microschemaDao,
			PersistingProjectDao projectDao, PersistingBranchDao branchDao, PersistingContentDao contentDao) {
		this.db = db;
		this.jobDao = (PersistingJobDao) jobDao;
		this.schemaDao = schemaDao;
		this.microschemaDao = microschemaDao;
		this.projectDao = projectDao;
		this.branchDao = branchDao;
		this.contentDao = contentDao;
	}

	@Override
	public Completable process(HibJob job) {
		return Completable.fromCallable(() -> {
			switch (job.getType()) {
			case microschemaversionpurge:
				Set<String> usedMicroschemaVersionUuids = projectDao.findAll().stream()
					.flatMap(project -> branchDao.findAll(project).stream())
					.flatMap(branch -> microschemaDao.findActiveSchemaVersions(branch).stream())
					.map(version -> version.getUuid())
					.collect(Collectors.toSet());

				microschemaDao.findAll().stream()
					.flatMap(ms -> StreamUtil.toStream(microschemaDao.findAllVersions(ms)))
					.filter(msv -> !usedMicroschemaVersionUuids.contains(msv.getUuid()))
					.filter(msv -> isMicroschemaVersionEmpty(msv))
					.forEach(msv -> microschemaDao.deleteVersion(msv));
				break;
			case schemaversionpurge:
				Set<String> usedSchemaVersionUuids = projectDao.findAll().stream()
					.flatMap(project -> branchDao.findAll(project).stream())
					.flatMap(branch -> schemaDao.findActiveSchemaVersions(branch).stream())
					.map(version -> version.getUuid())
					.collect(Collectors.toSet());

				schemaDao.findAll().stream()
					.flatMap(ms -> StreamUtil.toStream(schemaDao.findAllVersions(ms)))
					.filter(msv -> !usedSchemaVersionUuids.contains(msv.getUuid()))
					.filter(msv -> isSchemaVersionEmpty(msv))
					.forEach(msv -> schemaDao.deleteVersion(msv));
				break;
			default:
				throw new IllegalStateException("Unsupported schema version purge job: " + job.getType() + " / " + job.getUuid());
			}
			return job;
		});
	}

	protected boolean isMicroschemaVersionEmpty(HibMicroschemaVersion msv) {
		return contentDao.getFieldsContainers(msv).findAny().isEmpty();
	}


	protected boolean isSchemaVersionEmpty(HibSchemaVersion msv) {
		return contentDao.getFieldsContainers(msv).findAny().isEmpty();
	}
}
