package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.job.JobStatus.QUEUED;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
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
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import dagger.Lazy;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class JobDaoImpl extends AbstractHibDaoGlobal<HibJob, JobResponse, HibJobImpl> implements PersistingJobDao {

	public static final String[] SORT_FIELDS = new String[] { "type", "status", "stopDate", "startDate", "nodeName" };
	private static final Logger log = LoggerFactory.getLogger(JobDaoImpl.class);
	
	@Inject
	public JobDaoImpl(DaoHelper<HibJob, HibJobImpl> daoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(daoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public Page<? extends HibJob> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibJob> extraFilter) {
		return PersistingRootDao.shouldSort(pagingInfo) 
				? new DynamicStreamPageImpl<>(
						// Since we do not know yet what the extra filter gives to us, we dare at this moment no paging - it will be applied at the PageImpl
						daoHelper.findAll(ac, Optional.empty(), ((PagingParameters) new PagingParametersImpl().putSort(pagingInfo.getSort())), Optional.empty()).stream(), 
						pagingInfo, extraFilter, false)
				: daoHelper.findAll(ac, pagingInfo, extraFilter, true);
	}

	@Override
	public HibJob enqueueBranchMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion,
			HibSchemaVersion toVersion) {
		HibJob job = createPersisted(null, j -> {
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
	public HibJob enqueueBranchMigration(HibUser creator, HibBranch branch) {
		HibJob job = createPersisted(null, j -> {
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
	public HibJob enqueueMicroschemaMigration(HibUser creator, HibBranch branch, HibMicroschemaVersion fromVersion,
											  HibMicroschemaVersion toVersion) {
		HibJob job = createPersisted(null, j -> {
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
	public HibJob enqueueSchemaMigration(HibUser creator, HibBranch branch, HibSchemaVersion fromVersion,
			HibSchemaVersion toVersion) {
		HibJob job = createPersisted(null, j -> {
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
	public HibJob enqueueVersionPurge(HibUser user, HibProject project, ZonedDateTime before) {
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
	public HibJob enqueueVersionPurge(HibUser user, HibProject project) {
		return enqueueVersionPurge(user, project, null);
	}

	@Override
	public HibJob enqueueConsistencyCheck(HibUser user, boolean repair) {
		HibJob job = createPersisted(null, j -> {
			j.setType(repair ? JobType.consistencyrepair : JobType.consistencycheck);
			j.setCreationTimestamp();
			j.setStatus(QUEUED);
		});
		mergeIntoPersisted(job);
		return job;
	}

	@Override
	public HibJob enqueueImageCacheMigration(HibUser user) {
		HibJob job = createPersisted(null, j -> {
			j.setType(JobType.imagecache);
			j.setCreationTimestamp();
			j.setStatus(QUEUED);
		});
		if (log.isDebugEnabled()) {
			log.debug("Enqueued image cache migration job {" + job.getUuid() + "}");
		}
		mergeIntoPersisted(job);
		return job;
	}

	@Override
	public void delete(HibJob job) {
		em().remove(job);
	}

	/**
	 * Delete the jobs referencing the provided project.
	 * @param project
	 */
	public void deleteByProject(HibProject project) {
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
		for (HibJob job : failedJobs) {
			delete(job);
			count++;
		}
		log.info(count + "} failed jobs purged.");
	}

	@Override
	public void clear() {
		em().createQuery("delete from job").executeUpdate();
	}

	@Override
	public String mapGraphQlSortingFieldName(String gqlName) {
		switch (gqlName) {
		case "type":
			return "jobType";
		case "status":
			return "jobStatus";
		default:
			return super.mapGraphQlSortingFieldName(gqlName);
		}
	}

	@Override
	public String[] getGraphQlSortingFieldNames(boolean noDependencies) {
		return Stream.of(
				Arrays.stream(super.getGraphQlSortingFieldNames(noDependencies)),
				Arrays.stream(SORT_FIELDS)					
			).flatMap(Function.identity()).toArray(String[]::new);
	}
}
