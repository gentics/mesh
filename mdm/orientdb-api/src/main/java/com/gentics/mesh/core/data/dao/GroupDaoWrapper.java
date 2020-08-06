package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to GroupDao once migration is done
public interface GroupDaoWrapper extends GroupDao, GroupRoot {

	Group loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm);

	Group create(String name, User user, String uuid);

	TraversalResult<? extends Group> findAll();

	Group findByName(String name);

	Group findByUuid(String uuid);
}
