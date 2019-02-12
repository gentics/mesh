package com.gentics.mesh.search.verticle;

import com.gentics.mesh.search.index.group.GroupTransformer;
import com.gentics.mesh.search.index.role.RoleTransformer;
import com.gentics.mesh.search.index.tag.TagTransformer;
import com.gentics.mesh.search.index.user.UserTransformer;

import javax.inject.Singleton;

@Singleton
public class MeshEntities {
	private final UserTransformer userTransformer;
	private final GroupTransformer groupTransformer;
	private final RoleTransformer roleTransformer;
	private final TagTransformer tagTransformer;

	public MeshEntities(UserTransformer userTransformer, GroupTransformer groupTransformer, RoleTransformer roleTransformer, TagTransformer tagTransformer) {
		this.userTransformer = userTransformer;
		this.groupTransformer = groupTransformer;
		this.roleTransformer = roleTransformer;
		this.tagTransformer = tagTransformer;
	}
}
