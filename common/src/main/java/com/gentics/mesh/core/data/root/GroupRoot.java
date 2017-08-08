package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;

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
	default Group create(String name, User user) {
		return create(name, user, null);
	}

	/**
	 * Create a new group and assign it to the group root.
	 * 
	 * @param name
	 *            Name of the group
	 * @param user
	 *            User that is used to set the creator and editor references.
	 * @param uuid
	 *            optional uuid            
	 * @return Created group
	 */
	Group create(String name, User user, String uuid);

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
