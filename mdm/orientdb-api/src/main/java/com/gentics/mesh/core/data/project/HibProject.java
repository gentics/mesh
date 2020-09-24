package com.gentics.mesh.core.data.project;

import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.result.Result;

public interface HibProject extends HibCoreElement, HibUserTracking, HibBucketableElement {

	void setUuid(String uuid);

	String getName();

	void setName(String name);

	ProjectReference transformToReference();

	HibBranch findBranchOrLatest(String branchNameOrUuid);

	@Deprecated
	Result<? extends Node> findNodes();

	HibBranch getLatestBranch();

	Node getBaseNode();

	HibBranch getInitialBranch();

	TagFamilyRoot getTagFamilyRoot();

	HibBranch findBranch(String branchNameOrUuid);

	@Deprecated
	NodeRoot getNodeRoot();

	void setBaseNode(Node baseNode);

	@Deprecated
	SchemaRoot getSchemaContainerRoot();

	@Deprecated
	MicroschemaRoot getMicroschemaContainerRoot();

}
