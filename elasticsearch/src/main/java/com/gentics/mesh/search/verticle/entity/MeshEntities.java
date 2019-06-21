package com.gentics.mesh.search.verticle.entity;

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
import com.gentics.mesh.core.rest.event.tag.TagElementEventModel;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gentics.mesh.search.verticle.eventhandler.Util.latestVersionTypes;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static com.gentics.mesh.search.verticle.eventhandler.Util.warningOptional;

/**
 * A helper class that abstracts the common functionality shared across mesh elements
 * that are useful for the search verticle.
 */
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

	/**
	 * Returns the {@link MeshEntity} for the given {@link ElementType}.
	 * @param type
	 * @return
	 */
	public Optional<MeshEntity<?>> of(ElementType type) {
		return Optional.ofNullable(entities.get(type));
	}

	/**
	 * The User {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<User> getUser() {
		return user;
	}

	/**
	 * The Group {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<Group> getGroup() {
		return group;
	}

	/**
	 * The Role {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<Role> getRole() {
		return role;
	}

	/**
	 * The Project {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<Project> getProject() {
		return project;
	}

	/**
	 * The Tag {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<Tag> getTag() {
		return tag;
	}

	/**
	 * The TagFamily {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<TagFamily> getTagFamily() {
		return tagFamily;
	}

	/**
	 * The Schema {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<SchemaContainer> getSchema() {
		return schema;
	}

	/**
	 * The Microschema {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<MicroschemaContainer> getMicroschema() {
		return microschema;
	}

	private Optional<TagFamily> toTagFamily(MeshElementEventModel eventModel) {
		ProjectEvent event = Util.requireType(ProjectEvent.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getTagFamilyRoot(), eventModel.getUuid()));
	}

	private Optional<Tag> toTag(MeshElementEventModel eventModel) {
		TagElementEventModel event = Util.requireType(TagElementEventModel.class, eventModel);
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

	/**
	 * Finds an element in the given root vertex.
	 * If the element could not be found, a warning will be logged and an empty optional is returned.
	 * @param rootVertex
	 * @param uuid
	 * @param <T>
	 * @return
	 */
	public static <T extends MeshCoreVertex<? extends RestModel, T>> Optional<T> findElementByUuid(RootVertex<T> rootVertex, String uuid) {
		return warningOptional(
			String.format("Could not find element with uuid {%s} in class {%s}", uuid, rootVertex.getClass().getSimpleName()),
			rootVertex.findByUuid(uuid)
		);
	}

	/**
	 * Same as {@link #findElementByUuid(RootVertex, String)}, but as a stream.
	 *
	 * @param rootVertex
	 * @param uuid
	 * @param <T>
	 * @return
	 */
	public static <T extends MeshCoreVertex<? extends RestModel, T>> Stream<T> findElementByUuidStream(RootVertex<T> rootVertex, String uuid) {
		return toStream(findElementByUuid(rootVertex, uuid));
	}

	private <T extends MeshCoreVertex<? extends RestModel, T>> EventVertexMapper<T> byUuid(Supplier<RootVertex<T>> rootVertex) {
		return event -> findElementByUuid(rootVertex.get(), event.getUuid());
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(Group element) {
		return helper.createDocumentRequest(Group.composeIndexName(), element.getUuid(), group.transform(element));
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(User element) {
		return helper.createDocumentRequest(User.composeIndexName(), element.getUuid(), user.transform(element));
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(TagFamily element, String projectUuid) {
		return helper.createDocumentRequest(TagFamily.composeIndexName(projectUuid), element.getUuid(), tagFamily.transform(element));
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(Tag element, String projectUuid) {
		return helper.createDocumentRequest(Tag.composeIndexName(projectUuid), element.getUuid(), tag.transform(element));
	}

	/**
	 * Generates node requests for all latest contents of the given node.
	 * Latest contents are all draft and published versions of all languages.
	 *
	 * @param nodeUuid
	 * @param project
	 * @param branch
	 * @return
	 */
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
