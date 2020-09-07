package com.gentics.mesh.core.data.project;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.result.Result;

public interface HibProject extends HibCoreElement, HibUserTracking {

	void setUuid(String uuid);

	String getName();

	void setName(String name);

	ProjectReference transformToReference();

	HibBranch findBranchOrLatest(String branchNameOrUuid);

	@Deprecated
	Result<? extends HibNode> findNodes();

	HibBranch getLatestBranch();

	HibNode getBaseNode();

	HibBranch getInitialBranch();

	HibBranch findBranch(String branchNameOrUuid);

	@Deprecated
	NodeRoot getNodeRoot();

	void setBaseNode(HibNode baseNode);

	@Deprecated
	SchemaRoot getSchemaContainerRoot();

	@Deprecated
	MicroschemaRoot getMicroschemaContainerRoot();

	HibBaseElement getBranchPermissionRoot();
}
