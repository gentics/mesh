package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_JOB;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.FAILED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.QUEUED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.UNKNOWN;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.root.impl.AbstractRootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

/**
 * @see JobRoot
 */
public class JobRootImpl extends AbstractRootVertex<Job> implements JobRoot {

	public static void init(Database database) {
		database.addVertexType(JobRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_JOB, true, false, true);
	}

	@Override
	public Class<? extends Job> getPersistanceClass() {
		return JobImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_JOB;
	}

	@Override
	public Job enqueueSchemaMigration(User creator, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion) {
		Job job = getGraph().addFramedVertex(JobImpl.class);
		job.setType(MigrationType.schema);
		job.setCreated(creator);
		job.setRelease(release);
		job.setStatus(QUEUED);
		job.setFromSchemaVersion(fromVersion);
		job.setToSchemaVersion(toVersion);
		addItem(job);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued schema migration job {" + job.getUuid() + "}");
		}
		return job;
	}

	@Override
	public Job enqueueMicroschemaMigration(User creator, Release release, MicroschemaContainerVersion fromVersion,
			MicroschemaContainerVersion toVersion) {
		Job job = getGraph().addFramedVertex(JobImpl.class);
		job.setType(MigrationType.microschema);
		job.setCreated(creator);
		job.setRelease(release);
		job.setStatus(QUEUED);
		job.setFromMicroschemaVersion(fromVersion);
		job.setToMicroschemaVersion(toVersion);
		addItem(job);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued microschema migration job {" + job.getUuid() + "} - " + toVersion.getSchemaContainer().getName() + " "
					+ fromVersion.getVersion() + " to " + toVersion.getVersion());
		}
		return job;
	}

	@Override
	public Job enqueueReleaseMigration(User creator, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion) {
		Job job = getGraph().addFramedVertex(JobImpl.class);
		job.setCreated(creator);
		job.setType(MigrationType.release);
		job.setRelease(release);
		job.setStatus(QUEUED);
		job.setFromSchemaVersion(fromVersion);
		job.setToSchemaVersion(toVersion);
		addItem(job);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued release migration job {" + job.getUuid() + "} for release {" + release.getUuid() + "}");
		}
		return job;
	}

	@Override
	public Job enqueueReleaseMigration(User creator, Release release) {
		Job job = getGraph().addFramedVertex(JobImpl.class);
		job.setCreated(creator);
		job.setType(MigrationType.release);
		job.setRelease(release);
		addItem(job);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued release migration job {" + job.getUuid() + "} for release {" + release.getUuid() + "}");
		}
		return job;
	}

	@Override
	public MeshVertex resolveToElement(Stack<String> stack) {
		throw error(BAD_REQUEST, "Jobs are not accessible");
	}

	@Override
	public Job create(InternalActionContext ac, SearchQueueBatch batch, String uuid) {
		throw new NotImplementedException("Jobs can be created using REST");
	}

	@Override
	public void process() {
		Iterable<? extends Job> it = findAllIt();
		for (Job job : it) {
			try {
				// Don't execute failed or completed jobs again
				MigrationStatus jobStatus = job.getStatus();
				if (job.hasFailed() || (jobStatus == COMPLETED || jobStatus == FAILED || jobStatus == UNKNOWN)) {
					continue;
				}
				try (Tx tx = db.tx()) {
					job.process();
					tx.success();
				}
			} catch (Exception e) {
				job.markAsFailed(e);
				log.error("Error while processing job {" + job.getUuid() + "}");
			}
		}
	}

	@Override
	public void purgeFailed() {
		log.info("Purging failed jobs..");
		Iterable<? extends JobImpl> it = out(HAS_JOB).hasNot("error", null).frameExplicit(JobImpl.class);
		long count = 0;
		for (Job job : it) {
			job.delete(null);
			count++;
		}
		log.info("Purged {" + count + "} failed jobs.");
	}

}
