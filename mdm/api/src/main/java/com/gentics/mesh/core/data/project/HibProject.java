package com.gentics.mesh.core.data.project;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.project.ProjectReference;

public interface HibProject extends HibCoreElement, HibUserTracking, HibBucketableElement {

	void setUuid(String uuid);

	String getName();

	void setName(String name);

	ProjectReference transformToReference();

	HibBranch findBranchOrLatest(String branchNameOrUuid);

	HibBranch getLatestBranch();

	HibNode getBaseNode();

	HibBranch getInitialBranch();

	HibBranch findBranch(String branchNameOrUuid);

	void setBaseNode(HibNode baseNode);

	HibBaseElement getBranchPermissionRoot();

	HibBaseElement getTagFamilyPermissionRoot();
}
