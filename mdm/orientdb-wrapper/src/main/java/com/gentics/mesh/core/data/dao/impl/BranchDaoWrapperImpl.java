package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.branch.impl.BranchMicroschemaEdgeImpl;
import com.gentics.mesh.core.data.branch.impl.BranchSchemaEdgeImpl;
import com.gentics.mesh.core.data.dao.AbstractRootDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * @see BranchDaoWrapper
 */
@Singleton
public class BranchDaoWrapperImpl extends AbstractRootDaoWrapper<BranchResponse, HibBranch, Branch, HibProject> implements BranchDaoWrapper {

	private final Lazy<GraphDatabase> db;

	@Inject
	public BranchDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<GraphDatabase> db) {
		super(boot);
		this.db = db;
	}

	/**
	 * Load the branch from the project using the provided uuid.
	 * 
	 * @param project
	 * @param uuid
	 * @return Loaded branch or null when the branch can't be found
	 */
	public HibBranch findByUuid(HibProject project, String uuid) {
		Objects.requireNonNull(project);
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findByUuid(uuid);
	}

	@Override
	public HibBranch findByName(HibProject project, String name) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findByName(name);
	}

	@Override
	public Result<? extends HibBranch> findAll(HibProject project) {
		Objects.requireNonNull(project);
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findAll();
	}

	@Override
	public Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<HibBranch> extraFilter) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findAll(ac, pagingInfo, branch -> {
			return extraFilter.test(branch);
		});
	}

	@Override
	public String getAPIPath(HibBranch branch, InternalActionContext ac) {
		Branch graphBranch = toGraph(branch);
		return graphBranch.getAPIPath(ac);
	}

	@Override
	public Page<? extends HibBranch> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().findAll(ac, pagingInfo);
	}

	@Override
	public HibBranch getLatestBranch(HibProject project) {
		Project graphProject = toGraph(project);
		return graphProject.getBranchRoot().getLatestBranch();
	}

// TODO remove if unneeded
//	@Override
//	public long globalCount() {
//		return db.get().count(Branch.class);
//	}

	@Override
	public Stream<? extends HibBranch> findAllStream(HibProject root, InternalActionContext ac,
			InternalPermission permission) {
		return toGraph(root).getBranchRoot().findAllStream(ac, permission);
	}

	@Override
	public Page<? extends HibBranch> findAllNoPerm(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return toGraph(root).getBranchRoot().findAllNoPerm(ac, pagingInfo);
	}

	@Override
	public void addItem(HibProject root, HibBranch item) {
		toGraph(root).getBranchRoot().addItem(toGraph(item));
	}

	@Override
	public void removeItem(HibProject root, HibBranch item) {
		toGraph(root).getBranchRoot().removeItem(toGraph(item));
	}

	@Override
	public String getRootLabel(HibProject root) {
		return toGraph(root).getBranchRoot().getRootLabel();
	}

	@Override
	public Class<? extends HibBranch> getPersistenceClass(HibProject root) {
		return toGraph(root).getBranchRoot().getPersistanceClass();
	}

	@Override
	public long globalCount(HibProject root) {
		return toGraph(root).getBranchRoot().globalCount();
	}

	@Override
	protected RootVertex<Branch> getRoot(HibProject root) {
		return toGraph(root).getBranchRoot();
	}

	@Override
	public HibBranch findConflictingBranch(HibBranch branch, String name) {
		Branch graphBranch = toGraph(branch);
		return db.get().index().checkIndexUniqueness(Branch.UNIQUENAME_INDEX_NAME, graphBranch,
				graphBranch.getRoot().getUniqueNameKey(name));
	}

	/**
	 * Unassigns the latest version of the container from the branch.
	 *
	 * @param container
	 *            Container to handle
	 */
	protected <
				R extends FieldSchemaContainer,
				RM extends FieldSchemaContainerVersion,
				RE extends NameUuidReference<RE>,
				SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
				SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>
	> void unassign(HibBranch branch, HibFieldSchemaElement<R, RM, RE, SC, SCV> container) {
		SCV version = container.getLatestVersion();
		String edgeLabel = null;
		if (version instanceof SchemaVersion) {
			edgeLabel = HAS_SCHEMA_VERSION;
		}
		if (version instanceof MicroschemaVersion) {
			edgeLabel = HAS_MICROSCHEMA_VERSION;
		}

		// Iterate over all versions of the container and unassign it from the
		// branch. We don't know which version was assigned to the branch
		// so we just unassign all versions of the container.
		while (version != null) {
			toGraph(branch).unlinkOut(toGraph(version), edgeLabel);
			version = version.getPreviousVersion();
		}
	}

	@Override
	public HibBranchSchemaVersion connectToSchemaVersion(HibBranch branch, HibSchemaVersion version) {
		return toGraph(branch).addFramedEdgeExplicit(HAS_SCHEMA_VERSION, toGraph(version), BranchSchemaEdgeImpl.class);
	}

	public HibBranchMicroschemaVersion connectToMicroschemaVersion(HibBranch branch, HibMicroschemaVersion version) {
		return toGraph(branch).addFramedEdgeExplicit(HAS_MICROSCHEMA_VERSION, toGraph(version), BranchMicroschemaEdgeImpl.class);
	}

	@Override
	public void onRootDeleted(HibProject root, BulkActionContext bac) {
		// Delete all leaf data
		super.onRootDeleted(root, bac);
		// Delete the root itself
		getRoot(root).delete();
	}
}
