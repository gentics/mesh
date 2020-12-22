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

	public static final String TYPE = "groups";

	Result<? extends HibUser> getUsers(HibGroup group);

	Result<? extends HibRole> getRoles(HibGroup group);

	Page<? extends HibUser> getVisibleUsers(Group group, HibUser user, PagingParameters pagingInfo);

	Page<? extends Role> getRoles(Group group, HibUser user, PagingParameters pagingInfo);

	Group create();

}
