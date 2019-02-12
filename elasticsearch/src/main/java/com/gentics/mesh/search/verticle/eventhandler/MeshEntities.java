package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.index.group.GroupTransformer;
import com.gentics.mesh.search.index.project.ProjectTransformer;
import com.gentics.mesh.search.index.role.RoleTransformer;
import com.gentics.mesh.search.index.tag.TagTransformer;
import com.gentics.mesh.search.index.tagfamily.TagFamilyTransformer;
import com.gentics.mesh.search.index.user.UserTransformer;
import com.gentics.mesh.search.verticle.request.CreateDocumentRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

@Singleton
public class MeshEntities {

	private final MeshHelper helper;

	public final MeshEntity<User> user;
	public final MeshEntity<Group> group;
	public final MeshEntity<Role> role;
	public final MeshEntity<Project> project;
	public final MeshEntity<Tag> tag;
	public final MeshEntity<TagFamily> tagFamily;

	@Inject
	public MeshEntities(MeshHelper helper, BootstrapInitializer boot, UserTransformer userTransformer, RoleTransformer roleTransformer, TagTransformer tagTransformer, ProjectTransformer projectTransformer, GroupTransformer groupTransformer, TagFamilyTransformer tagFamilyTransformer) {
		this.helper = helper;
		user = new MeshEntity<>(userTransformer, boot.userRoot(), USER_CREATED, USER_UPDATED, USER_DELETED);
		group = new MeshEntity<>(groupTransformer, boot.groupRoot(), GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);
		role = new MeshEntity<>(roleTransformer, boot.roleRoot(), ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED);
		project = new MeshEntity<>(projectTransformer, boot.projectRoot(), PROJECT_CREATED, PROJECT_UPDATED, PROJECT_DELETED);
		tag = new MeshEntity<>(tagTransformer, boot.tagRoot(), TAG_CREATED, TAG_UPDATED, TAG_DELETED);
		tagFamily = new MeshEntity<>(tagFamilyTransformer, boot.tagFamilyRoot(), TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED);
	}

	public CreateDocumentRequest createRequest(Group element) {
		return new CreateDocumentRequest(helper.prefixIndexName(Group.composeIndexName()), element.getUuid(), group.transform(element));
	}

	public CreateDocumentRequest createRequest(User element) {
		return new CreateDocumentRequest(helper.prefixIndexName(User.composeIndexName()), element.getUuid(), user.transform(element));
	}
}
