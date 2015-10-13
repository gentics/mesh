package com.gentics.mesh.core.data.root;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.util.InvalidArgumentException;

/**
 * Aggregation vertex for groups.
 */
public interface GroupRoot extends RootVertex<Group> {

	public static final String TYPE = "groups";

	/**
	 * Create a new group and assign it to the group root.
	 * 
	 * @param name
	 *            Name of the group
	 * @param user
	 *            User that is used to set the creator and editor references.
	 * @return Created group
	 */
	Group create(String name, User user);

	/**
	 * Find all groups that are visible to the given user and match the paging parameters.
	 */
	Page<? extends Group> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	/**
	 * Add the group to the aggregation vertex.
	 * 
	 * @param group Group to be added
	 */
	void addGroup(Group group);

	/**
	 * Remove the group from the aggregation vertex.
	 * 
	 * @param group Group to be removed
	 */
	void removeGroup(Group group);
}
