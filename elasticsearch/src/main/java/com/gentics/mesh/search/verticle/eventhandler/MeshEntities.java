package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.event.ProjectEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.index.group.GroupTransformer;
import com.gentics.mesh.search.index.project.ProjectTransformer;
import com.gentics.mesh.search.index.role.RoleTransformer;
import com.gentics.mesh.search.index.tag.TagTransformer;
import com.gentics.mesh.search.index.tagfamily.TagFamilyTransformer;
import com.gentics.mesh.search.index.user.UserTransformer;
import com.gentics.mesh.search.verticle.request.CreateDocumentRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Optional;

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
	private static final Logger log = LoggerFactory.getLogger(MeshEntities.class);

	private final MeshHelper helper;
	private final BootstrapInitializer boot;

	public final MeshEntity<User> user;
	public final MeshEntity<Group> group;
	public final MeshEntity<Role> role;
	public final MeshEntity<Project> project;
//	public final MeshEntity<Tag> tag;
	public final MeshEntity<TagFamily> tagFamily;

	@Inject
	public MeshEntities(MeshHelper helper, BootstrapInitializer boot, UserTransformer userTransformer, RoleTransformer roleTransformer, TagTransformer tagTransformer, ProjectTransformer projectTransformer, GroupTransformer groupTransformer, TagFamilyTransformer tagFamilyTransformer) {
		this.helper = helper;
		this.boot = boot;
		user = new MeshEntity<>(userTransformer, USER_CREATED, USER_UPDATED, USER_DELETED, byUuid(boot.userRoot()));
		group = new MeshEntity<>(groupTransformer, GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED, byUuid(boot.groupRoot()));
		role = new MeshEntity<>(roleTransformer, ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED, byUuid(boot.roleRoot()));
		project = new MeshEntity<>(projectTransformer, PROJECT_CREATED, PROJECT_UPDATED, PROJECT_DELETED, byUuid(boot.projectRoot()));
		tagFamily = new MeshEntity<>(tagFamilyTransformer, TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED, this::toTagFamily);
//		tag = new MeshEntity<>(tagTransformer, TAG_CREATED, TAG_UPDATED, TAG_DELETED, this::toTag);
	}

	private Optional<TagFamily> toTagFamily(MeshEventModel eventModel) {
		ProjectEvent event = Util.requireType(ProjectEvent.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getTagFamilyRoot(), eventModel.getUuid()));
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> Optional<T> findElementByUuid(RootVertex<T> rootVertex, String uuid) {
		Optional<T> val = Optional.ofNullable(rootVertex.findByUuid(uuid));
		if (!val.isPresent()) {
			log.warn(String.format("Could not find element with uuid {%s} in class {%s}", uuid, rootVertex.getClass().getSimpleName()));
		}
		return val;
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> EventVertexMapper<T> byUuid(RootVertex<T> rootVertex) {
		return event -> findElementByUuid(rootVertex, event.getUuid());
	}

	public CreateDocumentRequest createRequest(Group element) {
		return new CreateDocumentRequest(helper.prefixIndexName(Group.composeIndexName()), element.getUuid(), group.transform(element));
	}

	public CreateDocumentRequest createRequest(User element) {
		return new CreateDocumentRequest(helper.prefixIndexName(User.composeIndexName()), element.getUuid(), user.transform(element));
	}
}
