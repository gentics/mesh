package com.gentics.mesh.search.verticle.entity;

import static com.gentics.mesh.search.verticle.eventhandler.Util.warningOptional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.DaoGlobal;
import com.gentics.mesh.core.data.dao.RootDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
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

/**
 * A helper class that abstracts the common functionality shared across mesh elements
 * that are useful for the search verticle.
 */
@Singleton
public class MeshEntities {
	private static final Logger log = LoggerFactory.getLogger(MeshEntities.class);

	private final MeshHelper helper;
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
	public MeshEntities(MeshHelper helper, 
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
		this.complianceMode = options.getSearchOptions().getComplianceMode();

		schema = new SimpleMeshEntity<>(schemaTransformer, HibSchema.TYPE_INFO, byHibElementUuid(uuid -> Tx.get().schemaDao().findByUuid(uuid)));
		microschema = new SimpleMeshEntity<>(microschemaTransformer, HibMicroschema.TYPE_INFO, byHibElementUuid(uuid -> Tx.get().microschemaDao().findByUuid(uuid)));
		user = new SimpleMeshEntity<>(userTransformer, HibUser.TYPE_INFO, byHibElementUuid(uuid -> Tx.get().userDao().findByUuid(uuid)));
		group = new SimpleMeshEntity<>(groupTransformer, HibGroup.TYPE_INFO, byHibElementUuid(uuid -> Tx.get().groupDao().findByUuid(uuid)));
		role = new SimpleMeshEntity<>(roleTransformer, HibRole.TYPE_INFO, byHibElementUuid(uuid -> Tx.get().roleDao().findByUuid(uuid)));
		project = new SimpleMeshEntity<>(projectTransformer, HibProject.TYPE_INFO, byHibElementUuid(uuid -> Tx.get().projectDao().findByUuid(uuid)));
		tagFamily = new SimpleMeshEntity<>(tagFamilyTransformer, HibTagFamily.TYPE_INFO, this::toTagFamily);
		tag = new SimpleMeshEntity<>(tagTransformer, HibTag.TYPE_INFO, this::toTag);
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
		return findElementByUuid(Tx.get().projectDao(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(Tx.get().tagFamilyDao(), project, eventModel.getUuid()));
	}

	private Optional<HibTag> toTag(MeshElementEventModel eventModel) {
		TagElementEventModel event = Util.requireType(TagElementEventModel.class, eventModel);
		return findElementByUuid(Tx.get().projectDao(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(Tx.get().tagFamilyDao(), project, event.getTagFamily().getUuid()))
			.flatMap(family -> findElementByUuid(Tx.get().tagDao(), family, eventModel.getUuid()));
	}

	private Optional<HibNodeFieldContainer> toNodeContent(MeshElementEventModel eventModel) {
		ContentDao contentDao = Tx.get().contentDao();
		NodeMeshEventModel event = Util.requireType(NodeMeshEventModel.class, eventModel);
		return findElementByUuid(Tx.get().projectDao(), event.getProject().getUuid())
			.flatMap(project -> findElementByUuid(Tx.get().nodeDao(), project, eventModel.getUuid()))
			.flatMap(node -> warningOptional(
				"Could not find NodeGraphFieldContainer for event " + eventModel.toJson(true),
				contentDao.getFieldContainer(node, event.getLanguageTag(), event.getBranchUuid(), event.getType())
			));
	}

	/**
	 * Finds an element in the given root vertex.
	 * If the element could not be found, a warning will be logged and an empty optional is returned.
	 * @param rootVertex
	 * @param uuid
	 * @param <L>
	 * @return
	 */
	public static <R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> Optional<L> findElementByUuid(RootDao<R, L> dao, R rootVertex, String uuid) {
		return warningOptional(
			String.format("Could not find element with uuid {%s} in class {%s}", uuid, rootVertex.getClass().getSimpleName()),
			dao.findByUuid(rootVertex, uuid)
		);
	}
	
	/**
	 * Finds an element in the given root vertex.
	 * If the element could not be found, a warning will be logged and an empty optional is returned.
	 * @param dao
	 * @param uuid
	 * @param <L>
	 * @return
	 */
	public static <L extends HibCoreElement<? extends RestModel>> Optional<L> findElementByUuid(DaoGlobal<L> dao, String uuid) {
		return warningOptional(
			String.format("Could not find element with uuid {%s} in class {%s}", uuid, dao.getClass().getSimpleName()),
			dao.findByUuid(uuid)
		);
	}

	/**
	 * Same as {@link #findElementByUuid(RootVertex, String)}, but as a stream.
	 *
	 * @param dao
	 * @param uuid
	 * @param <L>
	 * @return
	 */
	public static <L extends HibCoreElement<? extends RestModel>> Stream<L> findElementByUuidStream(DaoGlobal<L> dao, String uuid) {
		return findElementByUuid(dao, uuid).stream();
	}

	/**
	 * Same as {@link #findElementByUuid(RootVertex, String)}, but as a stream.
	 *
	 * @param rootVertex
	 * @param uuid
	 * @param <L>
	 * @return
	 */
	public static <R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> Stream<L> findElementByUuidStream(RootDao<R, L> dao, R rootVertex, String uuid) {
		return findElementByUuid(dao, rootVertex, uuid).stream();
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
		return helper.createDocumentRequest(HibGroup.composeIndexName(), element.getUuid(), group.transform(element), complianceMode);
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(HibUser element) {
		return helper.createDocumentRequest(HibUser.composeIndexName(), element.getUuid(), user.transform(element), complianceMode);
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(HibTagFamily element, String projectUuid) {
		return helper.createDocumentRequest(HibTagFamily.composeIndexName(projectUuid), element.getUuid(), tagFamily.transform(element), complianceMode);
	}

	/**
	 * Creates a {@link CreateDocumentRequest} for the given element.
	 * @param element
	 * @return
	 */
	public CreateDocumentRequest createRequest(HibTag element, String projectUuid) {
		return helper.createDocumentRequest(HibTag.composeIndexName(projectUuid), element.getUuid(), tag.transform(element), complianceMode);
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
		return findElementByUuidStream(Tx.get().nodeDao(), project, nodeUuid)
		.flatMap(node -> Util.latestVersionTypes()
		.flatMap(type -> Tx.get().contentDao().getFieldContainers(node, branch, type).stream()
		.map(container -> {
			HibSchemaVersion version = container.getSchemaContainerVersion();
			List<String> indexLanguages = version.getSchema().findOverriddenSearchLanguages().collect(Collectors.toList());
			boolean languageSpecificIndex = indexLanguages.contains(container.getLanguageTag());

			return helper.createDocumentRequest(
				ContentDao.composeIndexName(
					project.getUuid(),
					branch.getUuid(),
					version.getUuid(),
					type,
					languageSpecificIndex ? container.getLanguageTag() : null,
					version.getMicroschemaVersionHash(branch)
				),
				ContentDao.composeDocumentId(nodeUuid, container.getLanguageTag()),
				transformer.toDocument(container, branch.getUuid(), type), complianceMode
			);
		})));
	}
}
