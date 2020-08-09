package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Aggregation vertex for groups.
 */
public interface GroupRoot extends RootVertex<Group>, TransformableElementRoot<Group, GroupResponse> {

	public static final String TYPE = "groups";

	TraversalResult<? extends User> getUsers(Group group);

	TraversalResult<? extends Role> getRoles(Group group);

	TransformablePage<? extends User> getVisibleUsers(Group group, MeshAuthUser user, PagingParameters pagingInfo);

	TransformablePage<? extends Role> getRoles(Group group, User user, PagingParameters pagingInfo);

	Group create();

}
