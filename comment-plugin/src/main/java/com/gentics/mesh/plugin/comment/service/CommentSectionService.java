package com.gentics.mesh.plugin.comment.service;

import static com.gentics.mesh.plugin.comment.model.CommentStatus.APPROVED;
import static com.gentics.mesh.plugin.comment.model.CommentStatus.DELETED;
import static com.gentics.mesh.plugin.comment.util.RxUtil.defaultSingle;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.dagger.AdminClient;
import com.gentics.mesh.plugin.comment.model.CommentChildren;
import com.gentics.mesh.plugin.comment.model.CommentPluginSchema;
import com.gentics.mesh.plugin.comment.model.CommentSection;
import com.gentics.mesh.plugin.comment.model.EffectiveCommentSection;
import com.gentics.mesh.plugin.comment.model.NestedComment;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import com.gentics.mesh.plugin.comment.model.Order;
import com.gentics.mesh.plugin.comment.util.RxUtil;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

public class CommentSectionService {

	private final SchemaService schemaService;
	private final Configuration configuration;
	private final MeshRestClient adminClient;
	private final String projectName;
	private final MeshService meshService;
	private final EffectiveCommentSectionService effectiveCommentSectionService;

	@Inject
	public CommentSectionService(SchemaService schemaService,
								 Configuration configuration,
								 @AdminClient MeshRestClient adminClient,
								 JsonObject project,
								 MeshService meshService,
								 EffectiveCommentSectionService effectiveCommentSectionService) {
		this.schemaService = schemaService;
		this.configuration = configuration;
		this.adminClient = adminClient;
		this.projectName = project.getString("name");
		this.meshService = meshService;
		this.effectiveCommentSectionService = effectiveCommentSectionService;
	}

	/**
	 * Checks if there is a node in a specific language.
	 * Returns an error Completable if the node was not found
	 *
	 * @param language language of the content
	 * @param nodeUuid uuid of the node
	 */
	private Completable checkExistence(String nodeUuid, String language) {
		return adminClient.findNodeByUuid(projectName, nodeUuid, new NodeParametersImpl().setLanguages(language))
			.toCompletable()
			.compose(RxUtil::expectMeshError);
	}

	private Maybe<CommentSection> findCommentSection(String nodeUuid, String language) {
		return getNodePath(nodeUuid, language)
			.flatMapMaybe(path -> com.gentics.mesh.plugin.common.util.RxUtil.webrootMaybe(adminClient, projectName, path))
			.map(CommentSection::new);
	}

	/**
	 * Makes sure that a comment section exists for the given node and returns it
	 */
	public Single<CommentSection> ensureCommentSection(String nodeUuid, String language) {
		return checkExistence(nodeUuid, language)
			.andThen(findCommentSection(nodeUuid, language))
			.to(defaultSingle(createDefault(nodeUuid, language)))
			.flatMap(this::transferConfiguration);
	}

	/**
	 * Create the comment section node for the given node.
	 *
	 * @param nodeUuid Node for which the section should be created
	 * @param language
	 * @return
	 */
	public Single<CommentSection> createDefault(String nodeUuid, String language) {
		return getCommentRootPath()
			.flatMap(commentRoot -> meshService.ensureFolders(commentRoot + "/" + nodeUuid))
			.flatMap(parentNode -> {
				NodeCreateRequest request = new NodeCreateRequest();
				request.setLanguage(language);
				request.setSchemaName(schemaService.getMeshSchemaName(CommentPluginSchema.SECTION));
				request.setParentNodeUuid(parentNode.getUuid());
				request.setFields(createDefaultFields(nodeUuid, language));
				return adminClient.createNode(projectName, request).toSingle();
		}).map(CommentSection::new);
	}

	private FieldMap createDefaultFields(String nodeUuid, String language) {
		FieldMap fieldMap = new FieldMapImpl();
		fieldMap.put("node", new StringFieldImpl().setString(nodeUuid));
		fieldMap.put("language", new StringFieldImpl().setString(language));
		return fieldMap;
	}

	private Single<String> getNodePath(String uuid, String language) {
		return getCommentRootPath().map(path ->
			path + String.format("/%s/%s", uuid, language)
		);
	}

	private Single<String> getCommentRootPath() {
		String root = configuration.getCommentRoot();
		if (root.startsWith("/")) {
			// Webroot
			return Single.just(root);
		} else {
			// UUID
			// TODO Cache path for uuid
			return adminClient.findNodeByUuid(projectName, root,
				// Node parameters are necessary to get the Path of the node
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)
			).toSingle()
				.map(NodeResponse::getPath);
		}
	}

	/**
	 * If configured, this will copy the configuration from a field of the referenced node
	 * and update the comment section to Mesh.
	 */
	private Single<CommentSection> transferConfiguration(CommentSection commentSection) {
		EffectiveCommentSection section = effectiveCommentSectionService.fromSection(commentSection);
		if (!section.getNodeConfigField().isPresent()) {
			return Single.just(commentSection);
		}
		String fieldName = section.getNodeConfigField().get();

		return adminClient.findNodeByUuid(projectName, section.getNode()).toSingle()
			.flatMap(node -> {
				String updateJson = node.getFields().getStringField(fieldName).getString();
				FieldMap fields = JsonUtil.readValue(updateJson, FieldMap.class);
				NodeUpdateRequest request = new NodeUpdateRequest();
				request.setFields(fields);
				request.setVersion(section.getVersion());
				request.setLanguage(section.getLanguage());
				return adminClient.updateNode(projectName, commentSection.getUuid(), request).toSingle();
			}).map(CommentSection::new);
	}

	/**
	 * Fetches comments of a section in a nested structure.
	 *  @param uuid     Uuid of the comment section
	 * @param language
	 * @param order
	 */
	public Single<NestedCommentSection> fetchComments(String uuid, String language, Order order) {
		return ensureCommentSection(uuid, language)
			.flatMap(section -> requirePermission(section, Permission.READ_PUBLISHED)
			.flatMap(permissions -> isEditor(section)
			.flatMap(isEditor -> meshService.graphQl("sectionComments", new JsonObject().put("uuid", section.getUuid()))
			.map(response -> toCommentSection(response, permissions, isEditor, order)))));
	}

	/**
	 * Tests if the current user is an editor for the comment section
	 */
	public Single<Boolean> isEditor(CommentSection section) {
		return requirePermission(section, Permission.UPDATE)
			.ignoreElement()
			.to(RxUtil::hasNoError);
	}

	/**
	 * Checks if the current user has the permission on the node that the comments are for.
	 * Returns an error Completable if the user has no permissions.
	 *
	 * Also returns the permissions that the current user has.
	 */
	public Single<UserPermissionResponse> requirePermission(CommentSection section, Permission permission) {
		// TODO this is called multiple times for the same section. It should be cached per request
		return meshService.getUserPermissions(section.getNode())
			.doOnSuccess(userPermissions -> meshService.requirePermission(userPermissions, permission));
	}

	/**
	 * Prepares the GraphQl response to have a cleaner json output
	 */
	private NestedCommentSection toCommentSection(JsonObject response, UserPermissionResponse permissions, Boolean isEditor, Order order) {
		NestedCommentSection section = NestedCommentSection.fromGraphQl(response.getJsonObject("node"), permissions);
		filterComments(section, isEditor);
		sortComments(section, order);
		return section;
	}

	private void sortComments(CommentChildren section, Order order) {
		List<NestedComment> children = section.getChildren();
		if (children != null) {
			children.sort(this.sortComments(order));
			children.forEach(child -> this.sortComments(child, order));
		}
	}

	/**
	 * Filters out comments that the user should not see (hidden and unapproved comments)
	 */
	private void filterComments(CommentChildren section, Boolean isEditor) {
		if (section.getChildren() == null) {
			return;
		}
		Iterator<NestedComment> it = section.getChildren().iterator();
		while (it.hasNext()) {
			NestedComment comment = it.next();
			if ((!isEditor && (comment.getFields().getStatus() != APPROVED || comment.getFields().getHidden())) ||
				isEditor && comment.getFields().getStatus() == DELETED) {
				it.remove();
			} else {
				filterComments(comment, isEditor);
			}
		}
	}

	/**
	 * Sorts comments by their creation date.
	 */
	private Comparator<NestedComment> sortComments(Order order) {
		Comparator<NestedComment> comp = Comparator.comparing(NestedComment::getCreated);
		if (order == Order.DESCENDING) {
			comp = comp.reversed();
		}
		return comp;
	}
}
