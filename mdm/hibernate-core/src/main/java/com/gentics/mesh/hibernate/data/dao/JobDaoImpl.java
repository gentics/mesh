package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.job.JobStatus.QUEUED;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibJobImpl;
import com.gentics.mesh.hibernate.data.domain.HibVersionPurgeJobImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class JobDaoImpl extends AbstractHibDaoGlobal<Job, JobResponse, HibJobImpl> implements PersistingJobDao {
	private static final Logger log = LoggerFactory.getLogger(JobDaoImpl.class);
	
	@Inject
	public JobDaoImpl(DaoHelper<Job, HibJobImpl> daoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public Page<? extends Job> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Job> extraFilter) {
		return daoHelper.findAll(ac, pagingInfo, extraFilter, true);
	}

	@Override
	public Job enqueueBranchMigration(User creator, Branch branch, SchemaVersion fromVersion,
			SchemaVersion toVersion) {
		Job job = createPersisted(null, j -> {
			j.setType(JobType.branch);
			j.setCreationTimestamp();
			j.setBranch(branch);
			j.setStatus(QUEUED);
			j.setFromSchemaVersion(fromVersion);
			j.setToSchemaVersion(toVersion);
		});
		if (log.isDebugEnabled()) {
			log.debug("Enqueued branch migration job {" + job.getUuid() + "} for branch {" + branch.getUuid() + "}");
		}

		return job;
	}

	@Override
	public Job enqueueBranchMigration(User creator, Branch branch) {
		Job job = createPersisted(null, j -> {
			j.setType(JobType.branch);
			j.setCreationTimestamp();
			j.setStatus(QUEUED);
			j.setBranch(branch);	
		});
		if (log.isDebugEnabled()) {
			log.debug("Enqueued branch migration job {" + job.getUuid() + "} for branch {" + branch.getUuid() + "}");
		}
		mergeIntoPersisted(job);
		return job;
	}

	@Override
	public Job enqueueMicroschemaMigration(User creator, Branch branch, MicroschemaVersion fromVersion,
											  MicroschemaVersion toVersion) {
		Job job = createPersisted(null, j -> {
			j.setType(JobType.microschema);
			j.setCreationTimestamp();
			j.setBranch(branch);
			j.setStatus(QUEUED);
			j.setFromMicroschemaVersion(fromVersion);
			j.setToMicroschemaVersion(toVersion);	
		});
		if (log.isDebugEnabled()) {
			log.debug("Enqueued schema migration job {" + job.getUuid() + "}");
		}
		mergeIntoPersisted(job);
		return job;
	}

	@Override
	public Job enqueueSchemaMigration(User creator, Branch branch, SchemaVersion fromVersion,
			SchemaVersion toVersion) {
		Job job = createPersisted(null, j -> {
			j.setType(JobType.schema);
			j.setCreationTimestamp();
			j.setBranch(branch);
			j.setStatus(QUEUED);
			j.setFromSchemaVersion(fromVersion);
			j.setToSchemaVersion(toVersion);
		});
		if (log.isDebugEnabled()) {
			log.debug("Enqueued schema migration job {" + job.getUuid() + "}");
		}
		mergeIntoPersisted(job);
		return job;
	}

	@Override
	public Job enqueueVersionPurge(User user, Project project, ZonedDateTime before) {
		HibVersionPurgeJobImpl job = HibernateTx.get().create(HibVersionPurgeJobImpl.class);
		job.setCreationTimestamp();
		job.setType(JobType.versionpurge);
		job.setStatus(QUEUED);
		job.setProject(project);
		job.setMaxAge(before);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued project version purge job {" + job.getUuid() + "} for project {" + project.getName() + "}");
		}
		mergeIntoPersisted(job);
		return job;
	}

	@Override
	public Job enqueueVersionPurge(User user, Project project) {
		return enqueueVersionPurge(user, project, null);
	}

	@Override
	public Job enqueueConsistencyCheck(User user, boolean repair) {
		Job job = createPersisted(null, j -> {
			j.setType(repair ? JobType.consistencyrepair : JobType.consistencycheck);
			j.setCreationTimestamp();
			j.setStatus(QUEUED);
		});
		mergeIntoPersisted(job);
		return job;
	}

	@Override
	public void delete(Job job, BulkActionContext bac) {
		em().remove(job);
	}

	/**
	 * Delete the jobs referencing the provided project.
	 * @param project
	 */
	public void deleteByProject(Project project) {
		em().createQuery("delete from job where project = :project")
				.setParameter("project", project)
				.executeUpdate();
	}

	@Override
	public void purgeFailed() {
		log.info("Purging failed jobs...");
		List<HibJobImpl> failedJobs = em().createQuery("from job where errorMessage is not null", HibJobImpl.class)
				.getResultList();
		long count = 0;
		for (Job job : failedJobs) {
			delete(job, null);
			count++;
		}
		log.info(count + "} failed jobs purged.");
	}

	@Override
	public void clear() {
		em().createQuery("delete from job").executeUpdate();
	}
}
