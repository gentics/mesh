package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_BRANCH;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.BranchRoot;

/**
 * @see BranchRoot
 */
public class BranchRootImpl extends AbstractRootVertex<Branch> implements BranchRoot {

	/**
	 * Initialize the branch type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BranchRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_BRANCH));
		index.createIndex(edgeIndex(HAS_BRANCH).withInOut().withOut());
	}

	@Override
	public Project getProject() {
		return in(HAS_BRANCH_ROOT, ProjectImpl.class).next();
	}

	@Override
	public long globalCount() {
		return db().count(BranchImpl.class);
	}

	@Override
	public Branch getInitialBranch() {
		return out(HAS_INITIAL_BRANCH, BranchImpl.class).nextOrNull();
	}

	@Override
	public void setInitialBranch(Branch branch) {
		setSingleLinkOutTo(branch, HAS_INITIAL_BRANCH);
	}

	@Override
	public Branch getLatestBranch() {
		return out(HAS_LATEST_BRANCH, BranchImpl.class).nextOrNull();
	}

	@Override
	public void setLatestBranch(Branch branch) {
		setSingleLinkOutTo(branch, HAS_LATEST_BRANCH);
	}

	@Override
	public Class<? extends Branch> getPersistanceClass() {
		return BranchImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_BRANCH;
	}

	@Override
	public String getUniqueNameKey(String name) {
		return getUuid() + "-" + name;
	}

	@Override
	public void delete(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting branch root {" + getUuid() + "}");
		}

		// Delete all branches. Should not fire, if called from DAO.
		for (Branch branch : findAll()) {
			log.debug("Deleting branch {" + branch.getUuid() + "}");
			branch.delete(bac);
			bac.process();
		}

		// All branches are gone. Now delete the root.
		getElement().remove();
		bac.process();
	}

	/**
	 * @deprecated Use {@link #findByUuid(Project, String)} instead
	 */
	@Override
	@Deprecated
	public Branch findByUuid(String uuid) {
		return super.findByUuid(uuid);
	}

	@Override
	public Branch findByName(String name) {
		return db().index().checkIndexUniqueness(Branch.UNIQUENAME_INDEX_NAME, BranchImpl.class, getUniqueNameKey(name));
	}

	@Override
	public Branch create() {
		return getGraph().addFramedVertex(BranchImpl.class);
	}
}
