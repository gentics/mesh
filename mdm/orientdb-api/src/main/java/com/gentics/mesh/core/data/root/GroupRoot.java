package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Aggregation vertex for groups.
 */
public interface GroupRoot extends RootVertex<Group>, TransformableElementRoot<Group, GroupResponse> {

	/**
	 * Load a result of all users of the group.
	 * 
	 * @param group
	 * @return
	 */
	Result<? extends HibUser> getUsers(HibGroup group);

	/**
	 * Load a result of all roles of the group.
	 * 
	 * @param group
	 * @return
	 */
	Result<? extends HibRole> getRoles(HibGroup group);

	/**
	 * Return a page of users.
	 * 
	 * @param group
	 *            Group to load users from
	 * @param user
	 *            User to check read permissions
	 * @param pagingInfo
	 *            Paging information
	 * @return Paged result
	 */
	Page<? extends HibUser> getVisibleUsers(Group group, HibUser user, PagingParameters pagingInfo);

	/**
	 * Return a page of roles.
	 * 
	 * @param group
	 *            Group to load roles from
	 * @param user
	 *            User to be used to check read permissions
	 * @param pagingInfo
	 *            Paging settings
	 * @return Paged result
	 */
	Page<? extends Role> getRoles(Group group, HibUser user, PagingParameters pagingInfo);

	/**
	 * Create a new group.
	 * 
	 * @return
	 */
	Group create();

}
