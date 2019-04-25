package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_JOB;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.QUEUED;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.root.impl.AbstractRootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.reactivex.Completable;

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

	/**
	 * Find the element with the given uuid.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located
	 * @return Found element or null if the element could not be located
	 */
	public Job findByUuid(String uuid) {
		FramedGraph graph = Tx.getActive().getGraph();
		// 1. Find the element with given uuid within the whole graph
		Iterator<Vertex> it = database().getVertices(MeshVertexImpl.class, new String[] { "uuid" }, new String[] { uuid });
		if (it.hasNext()) {
			Vertex potentialElement = it.next();
			// 2. Use the edge index to determine whether the element is part of this root vertex
			Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout",
				database().createComposedIndexKey(potentialElement.getId(), id()));
			if (edges.iterator().hasNext()) {
				// Don't frame explicitly since multiple types can be returned
				return graph.frameElement(potentialElement, getPersistanceClass());
			}
		}
		return null;
	}

	@Override
	public TraversalResult<? extends Job> findAll() {
		// We need to enforce the usage of dynamic loading since the root->item yields different types of vertices.
		return super.findAllDynamic();
	}

	@Override
	public Job enqueueSchemaMigration(User creator, Branch branch, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion) {
		NodeMigrationJobImpl job = getGraph().addFramedVertex(NodeMigrationJobImpl.class);
		job.setType(JobType.schema);
		//job.setCreated(creator);
		job.setBranch(branch);
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
	public Job enqueueMicroschemaMigration(User creator, Branch branch, MicroschemaContainerVersion fromVersion,
		MicroschemaContainerVersion toVersion) {
		MicronodeMigrationJobImpl job = getGraph().addFramedVertex(MicronodeMigrationJobImpl.class);
		job.setType(JobType.microschema);
		//job.setCreated(creator);
		job.setBranch(branch);
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
	public Job enqueueBranchMigration(User creator, Branch branch, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion) {
		Job job = getGraph().addFramedVertex(BranchMigrationJobImpl.class);
		//job.setCreated(creator);
		job.setType(JobType.branch);
		job.setBranch(branch);
		job.setStatus(QUEUED);
		job.setFromSchemaVersion(fromVersion);
		job.setToSchemaVersion(toVersion);
		addItem(job);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued branch migration job {" + job.getUuid() + "} for branch {" + branch.getUuid() + "}");
		}
		return job;
	}

	@Override
	public Job enqueueBranchMigration(User creator, Branch branch) {
		Job job = getGraph().addFramedVertex(BranchMigrationJobImpl.class);
		//job.setCreated(creator);
		job.setType(JobType.branch);
		job.setStatus(QUEUED);
		job.setBranch(branch);
		addItem(job);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued branch migration job {" + job.getUuid() + "} for branch {" + branch.getUuid() + "}");
		}
		return job;
	}
	
	@Override
	public Job enqueueVersionPurge(User user, Project project) {
		VersionPurgeJobImpl job = getGraph().addFramedVertex(VersionPurgeJobImpl.class);
		//TODO why is user omitted?
		//job.setCreated(user);
		job.setType(JobType.versionpurge);
		job.setStatus(QUEUED);
		job.setProject(project);
		addItem(job);
		if (log.isDebugEnabled()) {
			log.debug("Enqueued project version purge job {" + job.getUuid() + "} for project {" + project.getName() + "}");
		}
		return job;
	}

	@Override
	public MeshVertex resolveToElement(Stack<String> stack) {
		throw error(BAD_REQUEST, "Jobs are not accessible");
	}

	@Override
	public Job create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new NotImplementedException("Jobs can be created using REST");
	}

	/**
	 * Find the visible elements and return a paged result.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options.
	 * 
	 * @return
	 */
	public TransformablePage<? extends Job> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return new DynamicTransformablePageImpl<>(ac.getUser(), this, pagingInfo, READ_PERM, null, false);
	}

	/**
	 * Find all elements and return a paged result. No permission check will be performed.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options.
	 * 
	 * @return
	 */
	public TransformablePage<? extends Job> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return new DynamicTransformablePageImpl<>(ac.getUser(), this, pagingInfo, null, null, false);
	}

	@Override
	public Completable process() {
		List<Completable> actions = new ArrayList<>();
		Iterable<? extends Job> it = findAll();
		for (Job job : it) {
			try {
				// Don't execute failed or completed jobs again
				JobStatus jobStatus = job.getStatus();
				if (job.hasFailed() || (jobStatus == COMPLETED || jobStatus == FAILED || jobStatus == UNKNOWN)) {
					continue;
				}
				actions.add(job.process());
			} catch (Exception e) {
				job.markAsFailed(e);
				log.error("Error while processing job {" + job.getUuid() + "}");
			}
		}
		return Completable.concat(actions);
	}

	@Override
	public void purgeFailed() {
		log.info("Purging failed jobs..");
		Iterable<? extends JobImpl> it = out(HAS_JOB).hasNot("error", null).frameExplicit(JobImpl.class);
		long count = 0;
		for (Job job : it) {
			job.delete();
			count++;
		}
		log.info("Purged {" + count + "} failed jobs.");
	}

	@Override
	public void clear() {
		out(HAS_JOB).removeAll();
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The job root can't be deleted");
	}

}
