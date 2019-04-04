package com.gentics.mesh.search.verticle.entity;

import static com.gentics.mesh.search.verticle.eventhandler.Util.latestVersionTypes;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toStream;
import static com.gentics.mesh.search.verticle.eventhandler.Util.warningOptional;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Branch;
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
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
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
	public final MeshEntity<NodeGraphFieldContainer> nodeContent;
	private final Map<ElementType, MeshEntity<?>> entities;

	@Inject
	public MeshEntities(MeshHelper helper, BootstrapInitializer boot, UserTransformer userTransformer, RoleTransformer roleTransformer, TagTransformer tagTransformer, ProjectTransformer projectTransformer, GroupTransformer groupTransformer, TagFamilyTransformer tagFamilyTransformer, SchemaTransformer schemaTransformer, MicroschemaTransformer microschemaTransformer, NodeContainerTransformer nodeTransformer) {
		this.helper = helper;
		this.boot = boot;

		schema = new SimpleMeshEntity<>(schemaTransformer, SchemaContainer.TYPE_INFO, byUuid(boot::schemaContainerRoot));
		microschema = new SimpleMeshEntity<>(microschemaTransformer, MicroschemaContainer.TYPE_INFO, byUuid(boot::microschemaContainerRoot));
		user = new SimpleMeshEntity<>(userTransformer, User.TYPE_INFO, byUuid(boot::userRoot));
		group = new SimpleMeshEntity<>(groupTransformer, Group.TYPE_INFO, byUuid(boot::groupRoot));
		role = new SimpleMeshEntity<>(roleTransformer, Role.TYPE_INFO, byUuid(boot::roleRoot));
		project = new SimpleMeshEntity<>(projectTransformer, Project.TYPE_INFO, byUuid(boot::projectRoot));
		tagFamily = new SimpleMeshEntity<>(tagFamilyTransformer, TagFamily.TYPE_INFO, this::toTagFamily);
		tag = new SimpleMeshEntity<>(tagTransformer, Tag.TYPE_INFO, this::toTag);
		nodeContent = new NodeMeshEntity(nodeTransformer, this::toNodeContent);

		entities = Stream.of(schema, microschema, user, group, role, project, tagFamily, tag, nodeContent)
			.collect(Collectors.toMap(
				entity -> entity.getTypeInfo().getType(),
				Function.identity()
			));
	}

	public Optional<MeshEntity<?>> of(ElementType type) {
		return Optional.ofNullable(entities.get(type));
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

	private Optional<TagFamily> toTagFamily(MeshElementEventModel eventModel) {
		ProjectEvent event = Util.requireType(ProjectEvent.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getTagFamilyRoot(), eventModel.getUuid()));
	}

	private Optional<Tag> toTag(MeshElementEventModel eventModel) {
		TagMeshEventModel event = Util.requireType(TagMeshEventModel.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getTagFamilyRoot(), event.getTagFamily().getUuid()))
			.flatMap(family -> findElementByUuid(family, eventModel.getUuid()));
	}

	private Optional<NodeGraphFieldContainer> toNodeContent(MeshElementEventModel eventModel) {
		NodeMeshEventModel event = Util.requireType(NodeMeshEventModel.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getNodeRoot(), eventModel.getUuid()))
			.flatMap(node -> warningOptional(
				"Could not find NodeGraphFieldContainer for event " + eventModel.toJson(),
				node.getGraphFieldContainer(event.getLanguageTag(), event.getBranchUuid(), event.getType())
			));
	}

	public static <T extends MeshCoreVertex<? extends RestModel, T>> Optional<T> findElementByUuid(RootVertex<T> rootVertex, String uuid) {
		return warningOptional(
			String.format("Could not find element with uuid {%s} in class {%s}", uuid, rootVertex.getClass().getSimpleName()),
			rootVertex.findByUuid(uuid)
		);
	}

	public static <T extends MeshCoreVertex<? extends RestModel, T>> Stream<T> findElementByUuidStream(RootVertex<T> rootVertex, String uuid) {
		return toStream(findElementByUuid(rootVertex, uuid));
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> EventVertexMapper<T> byUuid(Supplier<RootVertex<T>> rootVertex) {
		return event -> findElementByUuid(rootVertex.get(), event.getUuid());
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

	public Stream<CreateDocumentRequest> generateNodeRequests(String nodeUuid, Project project, Branch branch) {
		NodeContainerTransformer transformer = (NodeContainerTransformer) nodeContent.getTransformer();
		return findElementByUuidStream(project.getNodeRoot(), nodeUuid)
		.flatMap(node -> latestVersionTypes()
		.flatMap(type -> node.getGraphFieldContainers(branch, type).stream()
		.map(container -> helper.createDocumentRequest(
			NodeGraphFieldContainer.composeIndexName(
				project.getUuid(),
				branch.getUuid(),
				container.getSchemaContainerVersion().getUuid(),
				type
			),
			NodeGraphFieldContainer.composeDocumentId(nodeUuid, container.getLanguageTag()),
			transformer.toDocument(container, branch.getUuid(), type)
		))));
	}
}
