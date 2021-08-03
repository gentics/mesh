package com.gentics.mesh.core.data.project;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;

/**
 * Domain model for project.
 */
public interface HibProject extends HibCoreElement<ProjectResponse>, HibUserTracking, HibBucketableElement {

	/**
	 * Set the uuid.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

	/**
	 * Return the project name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the project name.
	 * 
	 * @param name
	 */
	void setName(String name);

	/**
	 * Transform the project to a reference POJO.
	 * 
	 * @return
	 */
	ProjectReference transformToReference();

	/**
	 * Locate the branch with the given name or uuid. Fallback to the latest branch if the given branch could not be found.
	 * 
	 * @param branchNameOrUuid
	 * @return
	 */
	HibBranch findBranchOrLatest(String branchNameOrUuid);

	/**
	 * Return the currently set latest branch of the project.
	 * 
	 * @return
	 */
	HibBranch getLatestBranch();

	/**
	 * Return the initial branch of the project.
	 * 
	 * @return
	 */
	HibBranch getInitialBranch();

	/**
	 * Find the branch with the given name or uuid that exists in the project.
	 * 
	 * @param branchNameOrUuid
	 * @return
	 */
	HibBranch findBranch(String branchNameOrUuid);

	/**
	 * Return the base node of the project.
	 * 
	 * @return
	 */
	HibNode getBaseNode();

	/**
	 * Set the base node of the project.
	 * 
	 * @param baseNode
	 */
	void setBaseNode(HibNode baseNode);

	/**
	 * Return the hib base element which is used to track permissions.
	 * 
	 * @return
	 */
	HibBaseElement getBranchPermissionRoot();

	/**
	 * Return the tag family hib base element which tracks tag family permissions.
	 * 
	 * @return
	 */
	HibBaseElement getTagFamilyPermissionRoot();
}
