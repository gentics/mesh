package com.gentics.mesh.search.verticle.entity;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.cli.ODBBootstrapInitializer;
import com.gentics.mesh.core.data.*;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.Dao;
import com.gentics.mesh.core.data.dao.DaoGlobal;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.ProjectEvent;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.tag.TagElementEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.search.verticle.eventhandler.Util.latestVersionTypes;
import static com.gentics.mesh.search.verticle.eventhandler.Util.warningOptional;

/**
 * A helper class that abstracts the common functionality shared across mesh elements
 * that are useful for the search verticle.
 */
@Singleton
public class MeshEntities {
	private static final Logger log = LoggerFactory.getLogger(MeshEntities.class);

	private final MeshHelper helper;
	private final ODBBootstrapInitializer boot;
	private final MeshOptions options;
	private final ComplianceMode complianceMode;

	public final MeshEntity<HibUser> user;
	public final MeshEntity<HibGroup> group;
	public final MeshEntity<HibRole> role;
	public final MeshEntity<HibProject> project;
	public final MeshEntity<HibTag> tag;
	public final MeshEntity<HibTagFamily> tagFamily;
	public final MeshEntity<HibSchema> schema;
	public final MeshEntity<HibMicroschema> microschema;
	public final MeshEntity<HibNodeFieldContainer> nodeContent;
	private final Map<ElementType, MeshEntity<?>> entities;

	@Inject
	public MeshEntities(MeshHelper helper, ODBBootstrapInitializer boot,
		MeshOptions options, 
		UserTransformer userTransformer, 
		RoleTransformer roleTransformer, 
		TagTransformer tagTransformer, 
		ProjectTransformer projectTransformer, 
		GroupTransformer groupTransformer, 
		TagFamilyTransformer tagFamilyTransformer, 
		SchemaTransformer schemaTransformer, 
		MicroschemaTransformer microschemaTransformer, 
		NodeContainerTransformer nodeTransformer) {
		this.helper = helper;
		this.boot = boot;
		this.options = options;
		this.complianceMode = options.getSearchOptions().getComplianceMode();

		schema = new SimpleMeshEntity<>(schemaTransformer, Schema.TYPE_INFO, byHibElementUuid(uuid -> boot.schemaDao().findByUuid(uuid)));
		microschema = new SimpleMeshEntity<>(microschemaTransformer, Microschema.TYPE_INFO, byHibElementUuid(uuid -> boot.microschemaDao().findByUuid(uuid)));
		user = new SimpleMeshEntity<>(userTransformer, User.TYPE_INFO, byHibElementUuid(uuid -> boot.userDao().findByUuid(uuid)));
		group = new SimpleMeshEntity<>(groupTransformer, Group.TYPE_INFO, byHibElementUuid(uuid -> boot.groupDao().findByUuid(uuid)));
		role = new SimpleMeshEntity<>(roleTransformer, Role.TYPE_INFO, byHibElementUuid(uuid -> boot.roleDao().findByUuid(uuid)));
		project = new SimpleMeshEntity<>(projectTransformer, Project.TYPE_INFO, byHibElementUuid(uuid -> boot.projectDao().findByUuid(uuid)));
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
	public MeshEntity<HibUser> getUser() {
		return user;
	}

	/**
	 * The Group {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<HibGroup> getGroup() {
		return group;
	}

	/**
	 * The Role {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<HibRole> getRole() {
		return role;
	}

	/**
	 * The Project {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<HibProject> getProject() {
		return project;
	}

	/**
	 * The Tag {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<HibTag> getTag() {
		return tag;
	}

	/**
	 * The TagFamily {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<HibTagFamily> getTagFamily() {
		return tagFamily;
	}

	/**
	 * The Schema {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<HibSchema> getSchema() {
		return schema;
	}

	/**
	 * The Microschema {@link MeshEntity}.
	 * @return
	 */
	public MeshEntity<HibMicroschema> getMicroschema() {
		return microschema;
	}

	private Optional<HibTagFamily> toTagFamily(MeshElementEventModel eventModel) {
		ProjectEvent event = Util.requireType(ProjectEvent.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getTagFamilyRoot(), eventModel.getUuid()));
	}

	private Optional<HibTag> toTag(MeshElementEventModel eventModel) {
		TagElementEventModel event = Util.requireType(TagElementEventModel.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getTagFamilyRoot(), event.getTagFamily().getUuid()))
			.flatMap(family -> findElementByUuid(family, eventModel.getUuid()));
	}

	private Optional<HibNodeFieldContainer> toNodeContent(MeshElementEventModel eventModel) {
		ContentDao contentDao = boot.contentDao();
		NodeMeshEventModel event = Util.requireType(NodeMeshEventModel.class, eventModel);
		return findElementByUuid(boot.projectRoot(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(project.getNodeRoot(), eventModel.getUuid()))
			.flatMap(node -> warningOptional(
				"Could not find NodeGraphFieldContainer for event " + eventModel.toJson(),
				contentDao.getGraphFieldContainer(node, event.getLanguageTag(), event.getBranchUuid(), event.getType())
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
	public static <T extends MeshCoreVertex<? extends RestModel>> Optional<T> findElementByUuid(RootVertex<T> rootVertex, String uuid) {
		return warningOptional(
			String.format("Could not find element with uuid {%s} in class {%s}", uuid, rootVertex.getClass().getSimpleName()),
			rootVertex.findByUuid(uuid)
		);
	}

	/**
	 * Finds an element in the given root vertex.
	 * If the element could not be found, a warning will be logged and an empty optional is returned.
	 * @param dao
	 * @param uuid
	 * @param <T>
	 * @return
	 */
	public static <T extends HibCoreElement> Optional<T> findElementByUuid(DaoGlobal<T> dao, String uuid) {
		return warningOptional(
			String.format("Could not find element with uuid {%s} with dao {%s}", uuid, dao.getClass().getSimpleName()),
			dao.findByUuidGlobal(uuid)
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
	public static <T extends MeshCoreVertex<? extends RestModel>> Stream<T> findElementByUuidStream(RootVertex<T> rootVertex, String uuid) {
		return findElementByUuid(rootVertex, uuid).stream();
	}

	/**
	 * Same as {@link #findElementByUuid(DaoGlobal, String)}, but as a stream.
	 *
	 * @param dao
	 * @param uuid
	 * @param <T>
	 * @return
	 */
	public static <T extends HibCoreElement> Stream<T> findElementByUuidStream(DaoGlobal<T> dao, String uuid) {
		return findElementByUuid(dao, uuid).stream();
	}

	private <T extends MeshCoreVertex<? extends RestModel>> EventVertexMapper<T> byUuid(Function<String, T> elementLoader) {
		return event -> Optional.ofNullable(elementLoader.apply(event.getUuid()));
	}

	private <T extends HibBaseElement> EventVertexMapper<T> byHibElementUuid(Function<String, T> elementLoader) {
		return event -> Optional.ofNullable(elementLoader.apply(event.getUuid()));
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(HibGroup element) {
		return helper.createDocumentRequest(Group.composeIndexName(), element.getUuid(), group.transform(element), complianceMode);
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(HibUser element) {
		return helper.createDocumentRequest(User.composeIndexName(), element.getUuid(), user.transform(element), complianceMode);
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(HibTagFamily element, String projectUuid) {
		return helper.createDocumentRequest(TagFamily.composeIndexName(projectUuid), element.getUuid(), tagFamily.transform(element), complianceMode);
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(HibTag element, String projectUuid) {
		return helper.createDocumentRequest(Tag.composeIndexName(projectUuid), element.getUuid(), tag.transform(element), complianceMode);
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
	public Stream<CreateDocumentRequest> generateNodeRequests(String nodeUuid, HibProject project, HibBranch branch) {
		NodeContainerTransformer transformer = (NodeContainerTransformer) nodeContent.getTransformer();
		return findElementByUuidStream(toGraph(project).getNodeRoot(), nodeUuid)
		.flatMap(node -> latestVersionTypes()
		.flatMap(type -> boot.contentDao().getGraphFieldContainers(node, branch, type).stream()
		.map(container -> helper.createDocumentRequest(
			ContentDao.composeIndexName(
				project.getUuid(),
				branch.getUuid(),
				container.getSchemaContainerVersion().getUuid(),
				type
			),
			ContentDao.composeDocumentId(nodeUuid, container.getLanguageTag()),
			transformer.toDocument(container, branch.getUuid(), type), complianceMode
		))));
	}
}
