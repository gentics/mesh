package com.gentics.mesh.search.verticle.entity;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.ProjectEvent;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.search.index.group.GroupTransformer;
import com.gentics.mesh.search.index.microschema.MicroschemaTransformer;
import com.gentics.mesh.search.index.node.NodeContainerTransformer;
import com.gentics.mesh.search.index.project.ProjectTransformer;
import com.gentics.mesh.search.index.role.RoleTransformer;
import com.gentics.mesh.search.index.schema.SchemaTransformer;
import com.gentics.mesh.search.index.tag.TagTransformer;
import com.gentics.mesh.search.index.tagfamily.TagFamilyTransformer;
import com.gentics.mesh.search.index.user.UserTransformer;
import com.gentics.mesh.search.verticle.eventhandler.EventVertexMapper;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import com.gentics.mesh.search.verticle.eventhandler.Util;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.warningOptional;

@Singleton
public class MeshEntities {
	private static final Logger log = LoggerFactory.getLogger(MeshEntities.class);

	private final MeshHelper helper;
	private final BootstrapInitializer boot;

	public final MeshEntity<User> user;
	public final MeshEntity<Group> group;
	public final MeshEntity<Role> role;
	public final MeshEntity<Project> project;
	public final MeshEntity<Tag> tag;
	public final MeshEntity<TagFamily> tagFamily;
	public final MeshEntity<SchemaContainer> schema;
	public final MeshEntity<MicroschemaContainer> microschema;
	public final MeshEntity<NodeGraphFieldContainer> node;

	@Inject
	public MeshEntities(MeshHelper helper, BootstrapInitializer boot, UserTransformer userTransformer, RoleTransformer roleTransformer, TagTransformer tagTransformer, ProjectTransformer projectTransformer, GroupTransformer groupTransformer, TagFamilyTransformer tagFamilyTransformer, SchemaTransformer schemaTransformer, MicroschemaTransformer microschemaTransformer, NodeContainerTransformer nodeTransformer) {
		this.helper = helper;
		this.boot = boot;
		schema = new SimpleMeshEntity<>(schemaTransformer, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED, byUuid(boot.schemaContainerRoot()));
		microschema = new SimpleMeshEntity<>(microschemaTransformer, MICROSCHEMA_CREATED, MICROSCHEMA_UPDATED, MICROSCHEMA_DELETED, byUuid(boot.microschemaContainerRoot()));
		user = new SimpleMeshEntity<>(userTransformer, USER_CREATED, USER_UPDATED, USER_DELETED, byUuid(boot.userRoot()));
		group = new SimpleMeshEntity<>(groupTransformer, GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED, byUuid(boot.groupRoot()));
		role = new SimpleMeshEntity<>(roleTransformer, ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED, byUuid(boot.roleRoot()));
		project = new SimpleMeshEntity<>(projectTransformer, PROJECT_CREATED, PROJECT_UPDATED, PROJECT_DELETED, byUuid(boot.projectRoot()));
		tagFamily = new SimpleMeshEntity<>(tagFamilyTransformer, TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED, this::toTagFamily);
		tag = new SimpleMeshEntity<>(tagTransformer, TAG_CREATED, TAG_UPDATED, TAG_DELETED, this::toTag);
		node = new NodeMeshEntity(nodeTransformer, NODE_CREATED, NODE_UPDATED, NODE_DELETED, this::toNode);
	}

	public MeshEntity<User> getUser() {
		return user;
	}

	public MeshEntity<Group> getGroup() {
		return group;
	}

	public MeshEntity<Role> getRole() {
		return role;
	}

	public MeshEntity<Project> getProject() {
		return project;
	}

	public MeshEntity<Tag> getTag() {
		return tag;
	}

	public MeshEntity<TagFamily> getTagFamily() {
		return tagFamily;
	}

	public MeshEntity<SchemaContainer> getSchema() {
		return schema;
	}

	public MeshEntity<MicroschemaContainer> getMicroschema() {
		return microschema;
	}

	public MeshEntity<NodeGraphFieldContainer> getNode() {
		return node;
	}

	private Optional<TagFamily> toTagFamily(MeshElementEventModel eventModel) {
		ProjectEvent event = Util.requireType(ProjectEvent.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getTagFamilyRoot(), eventModel.getUuid()));
	}

	private Optional<Tag> toTag(MeshElementEventModel eventModel) {
		return toTagFamily(eventModel)
			.flatMap(family -> findElementByUuid(family, eventModel.getUuid()));
	}

	private Optional<NodeGraphFieldContainer> toNode(MeshElementEventModel eventModel) {
		NodeMeshEventModel event = Util.requireType(NodeMeshEventModel.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getNodeRoot(), eventModel.getUuid()))
			.flatMap(node -> warningOptional(
				"Could not find NodeGraphFieldContainer for event " + eventModel.toJson(),
				node.getGraphFieldContainer(event.getLanguageTag(), event.getBranchUuid(), ContainerType.forVersion(event.getType()))
			));
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> Optional<T> findElementByUuid(RootVertex<T> rootVertex, String uuid) {
		return warningOptional(
			String.format("Could not find element with uuid {%s} in class {%s}", uuid, rootVertex.getClass().getSimpleName()),
			rootVertex.findByUuid(uuid)
		);
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> EventVertexMapper<T> byUuid(RootVertex<T> rootVertex) {
		return event -> findElementByUuid(rootVertex, event.getUuid());
	}

	public CreateDocumentRequest createRequest(Group element) {
		return helper.createDocumentRequest(Group.composeIndexName(), element.getUuid(), group.transform(element));
	}

	public CreateDocumentRequest createRequest(User element) {
		return helper.createDocumentRequest(User.composeIndexName(), element.getUuid(), user.transform(element));
	}

	public CreateDocumentRequest createRequest(TagFamily element, String projectUuid) {
		return helper.createDocumentRequest(TagFamily.composeIndexName(projectUuid), element.getUuid(), tagFamily.transform(element));
	}

	public CreateDocumentRequest createRequest(Tag element, String projectUuid) {
		return helper.createDocumentRequest(Tag.composeIndexName(projectUuid), element.getUuid(), tag.transform(element));
	}
}
