package com.gentics.mesh.core.data.project;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface HibProject extends HibCoreElement {

	String getName();

	void setName(String name);

	ProjectReference transformToReference();

	Branch findBranchOrLatest(String branchNameOrUuid);

	TraversalResult<? extends Node> findNodes();

	Branch getLatestBranch();

	BranchRoot getBranchRoot();

	Node getBaseNode();

	Branch getInitialBranch();

	TagFamilyRoot getTagFamilyRoot();

	Branch findBranch(String branchNameOrUuid);

	NodeRoot getNodeRoot();

	SchemaRoot getSchemaContainerRoot();

	MicroschemaRoot getMicroschemaContainerRoot();

	/**
	 * Convert this back to the non-mdm project
	 * 
	 * @return
	 * @deprecated This method should only be used when there is really no other way
	 */
	@Deprecated
	default Project toProject() {
		return (Project) this;
	}

}
