package com.gentics.mesh.plugin.comment.service;

import java.security.InvalidParameterException;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.plugin.comment.dagger.AdminClient;
import com.gentics.mesh.plugin.comment.dagger.UserClient;
import com.gentics.mesh.plugin.comment.error.AnonymousNotAllowedError;
import com.gentics.mesh.plugin.comment.error.ForbiddenException;
import com.gentics.mesh.plugin.comment.error.InvalidEmailException;
import com.gentics.mesh.plugin.comment.error.NotFoundException;
import com.gentics.mesh.plugin.comment.error.RequiredAnonymousFieldMissingError;
import com.gentics.mesh.plugin.comment.model.AnonymousCommentField;
import com.gentics.mesh.plugin.comment.model.Comment;
import com.gentics.mesh.plugin.comment.model.CommentPluginSchema;
import com.gentics.mesh.plugin.comment.model.CommentSection;
import com.gentics.mesh.plugin.comment.model.CommentStatus;
import com.gentics.mesh.plugin.comment.model.EffectiveCommentSection;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.gentics.mesh.plugin.comment.model.request.UpdateCommentRequest;
import com.gentics.mesh.plugin.comment.util.RxUtil;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

@Singleton
public class CommentService {

	private final MeshRestClient adminClient;
	private final String projectName;
	private final EffectiveCommentSectionService effectiveCommentSectionService;
	private final MailService mailService;
	private final CommentSectionService commentSectionService;
	private final SchemaService schemaService;
	private final MeshRestClient userClient;
	private final MeshService meshService;

	@Inject
	public CommentService(@AdminClient MeshRestClient adminClient,
						  JsonObject project,
						  SchemaService schemaService,
						  @UserClient MeshRestClient userClient,
						  EffectiveCommentSectionService effectiveCommentSectionService,
						  MailService mailService,
						  CommentSectionService commentSectionService, MeshService meshService) {

		this.adminClient = adminClient;
		this.projectName = project.getString("name");
		this.schemaService = schemaService;
		this.userClient = userClient;
		this.effectiveCommentSectionService = effectiveCommentSectionService;
		this.mailService = mailService;
		this.commentSectionService = commentSectionService;
		this.meshService = meshService;
	}

	/**
	 * The basic function for creating a comment.
	 * 
	 * @param request
	 * @return The created comment
	 */
	public Single<Comment> createComment(CreateCommentRequest request) {
		return commentSectionService.ensureCommentSection(request.getNode(), request.getLanguage())
		.flatMap(section -> requirePermission(section, Permission.READ_PUBLISHED)
		.andThen(createComment(section, request)
		.compose(RxUtil.waitFor(comment -> mailService.notifyCommentCreated(comment, effectiveCommentSectionService.fromSection(section))))));
	}

	/**
	 * The basic function for updating a comment.
	 * 
	 * @param uuid
	 *            Uuid of the comment which should be updated
	 * @param request
	 * @return The updated comment
	 */
	public Single<Comment> updateComment(String uuid, UpdateCommentRequest request) {
		return RxUtil.flatZip(
			fetchComment(uuid),
			fetchCommentSection(uuid),
			(comment, section) -> updateComment(section, comment, request)
		);
	}

	/**
	 * Fetches the section of the provided comment
	 */
	private Single<CommentSection> fetchCommentSection(String commentUuid) {
		return meshService.graphQl("findCommentSectionByComment", new JsonObject().put("uuid", commentUuid))
			.flatMapObservable(response -> {
				JsonObject node = response.getJsonObject("node");
				if (node == null) {
					return Observable.error(new NotFoundException());
				}
				return Observable.fromIterable(node.getJsonArray("breadcrumb"));
			})
			.map(element -> (JsonObject) element)
			.filter(node -> node.getJsonObject("schema").getString("name").equals(schemaService.getMeshSchemaName(CommentPluginSchema.SECTION)))
			.singleOrError()
			.flatMap(node -> adminClient.findNodeByUuid(projectName, node.getString("uuid")).toSingle())
			.map(CommentSection::new);
	}

	/**
	 * Fetches the section of the provided comment
	 */
	private Single<CommentSection> fetchCommentSection(Comment comment) {
		return fetchCommentSection(comment.getUuid());
	}

	/**
	 * Fetch the comment with the given uuid.
	 *
	 * @param uuid
	 * @return The created comment
	 */
	public Single<Comment> fetchComment(String uuid) {
		return fetchComment(uuid, false);
	}

	/**
	 * Fetch the comment with the given uuid.
	 *
	 * @param uuid
	 * @return The created comment
	 */
	public Single<Comment> fetchComment(String uuid, boolean checkPermission) {
		// Reject found nodes which do not match the schema of a comment
		return fetchCommentSection(uuid)
			.flatMap(section -> meshService.getUserPermissions(section.getNode()))
			.doOnSuccess(permissions -> {
				if (checkPermission) {
					meshService.requirePermission(permissions, Permission.READ);
				}
			})
			.flatMap(permissions -> adminClient.findNodeByUuid(projectName, uuid).toSingle()
			.compose(RxUtil::expectMeshError)
			.map(response -> new Comment(response, permissions)))
			.flatMap(comment -> {
				if (checkPermission) {
					return requireVisibleToUser(comment).andThen(Single.just(comment));
				} else {
					return Single.just(comment);
				}
			});
	}

	/**
	 * Makes sure that the comment is visible to the current user. (approved and visible if not an editor)
	 * If not, an error completable is returned.
	 *
	 * @param comment
	 * @return
	 */
	private Completable requireVisibleToUser(Comment comment) {
		if (comment.getStatus() == CommentStatus.APPROVED && !comment.isHidden()) {
			return Completable.complete();
		}
		return isEditor(comment)
			.flatMapCompletable(editor -> {
				if (editor) {
					return Completable.complete();
				} else {
					return Completable.error(new ForbiddenException());
				}
			});
	}

	/**
	 * Tests if the current user is an editor for the comment
	 */
	private Single<Boolean> isEditor(Comment comment) {
		// TODO: Per Request caching
		return fetchCommentSection(comment)
			.flatMap(commentSectionService::isEditor);
	}


	/**
	 * Creates a comment in the given section.
	 * 
	 * @param section
	 * @param request
	 * @return The created comment
	 */
	private Single<Comment> createComment(CommentSection section, CreateCommentRequest request) {
		EffectiveCommentSection effectiveSection = effectiveCommentSectionService.fromSection(section);
		return validateCommentRequest(request, effectiveSection)
			.andThen(meshService.getCurrentUser())
			.flatMap(currentUser -> adminClient.createNode(projectName, toRequest(request, effectiveSection, currentUser)).toSingle())
			.flatMap(response -> meshService.getUserPermissions(section.getNode())
			.map(permissions -> new Comment(response, permissions)));
	}

	/**
	 * Updates the comment in the given section.
	 *
	 * @param section
	 * @param comment
	 * @param request
	 * @return The updated comment
	 */
	private Single<Comment> updateComment(CommentSection section, Comment comment, UpdateCommentRequest request) {
		EffectiveCommentSection effectiveSection = effectiveCommentSectionService.fromSection(section);
		return validateCommentUpdateRequest(request, effectiveSection)
			.andThen(adminClient.updateNode(projectName, comment.getUuid(), toRequest(comment, request)).toSingle())
			.flatMap(response -> meshService.getUserPermissions(section.getNode())
			.map(permissions -> new Comment(response, permissions)));
	}

	/**
	 * Delete the comment with the given uuid.
	 * 
	 * @param uuid
	 *            Uuid of the comment which should be deleted.
	 */
	public Completable deleteComment(String uuid) {
		return fetchCommentSection(uuid)
			.flatMapCompletable(section -> commentSectionService.requirePermission(section, Permission.DELETE).ignoreElement())
			.andThen(adminClient.deleteNode(projectName, uuid).toCompletable());
	}

	/**
	 * Unpublish the comment with the given uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	public Completable unpublishComment(String uuid) {
		// TODO take down recursive?
		return adminClient.takeNodeOffline(projectName, uuid).toCompletable();
	}

	/**
	 * Publish the comment with the given uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	public Completable publishComment(String uuid) {

		return adminClient.publishNode(projectName, uuid).toCompletable();
	}

	public Completable hideComment(String uuid) {
		return updateHideFlagOfComment(uuid, true);
	}

	public Completable unhideComment(String uuid) {
		return updateHideFlagOfComment(uuid, false);
	}

	/**
	 * Update the hide flag of the comment.
	 * 
	 * @param uuid
	 *            Uuid of the comment
	 * @param isHidden
	 * @return
	 */
	private Completable updateHideFlagOfComment(String uuid, boolean isHidden) {
		return adminClient.findNodeByUuid(projectName, uuid).toSingle()
			.map(NodeResponse::getVersion)
			.flatMap(version -> {
				NodeUpdateRequest update = new NodeUpdateRequest();
				update.setLanguage("en");
				update.setVersion(version);
				FieldMap fieldMap = new FieldMapImpl();
				fieldMap.put("hidden", new BooleanFieldImpl().setValue(isHidden));
				update.setFields(fieldMap);
				return adminClient.updateNode(projectName, uuid, update).toSingle();
			}).ignoreElement();
	}

	/**
	 * Creates a node request for Mesh out of this request.
	 */
	public NodeCreateRequest toRequest(CreateCommentRequest request, EffectiveCommentSection section, UserResponse author) {
		String parentUuid = request.getParentComment().orElse(section.getUuid());
		NodeCreateRequest meshRequest = new NodeCreateRequest();
		meshRequest.setParentNodeUuid(parentUuid);
		// Comments themselves do not different languages.
		// We still need to provide a language for Mesh, so we default to english.
		meshRequest.setLanguage("en");
		meshRequest.setSchemaName(schemaService.getMeshSchemaName(CommentPluginSchema.COMMENT));
		meshRequest.setFields(createFields(request, section, author));
		return meshRequest;
	}

	/**
	 * Creates a node update request for Mesh out of this request.
	 * We need the previous version of the comment to update the comment.
	 */
	public NodeUpdateRequest toRequest(Comment comment, UpdateCommentRequest request) {
		NodeUpdateRequest meshRequest = new NodeUpdateRequest();
		meshRequest.setLanguage(comment.getLanguage());
		meshRequest.setVersion(comment.getVersion());
		meshRequest.setFields(request.toFieldMap());
		return meshRequest;
	}

	private FieldMap createFields(CreateCommentRequest request, EffectiveCommentSection section, UserResponse author) {
		CommentStatus status = section.isRequiresApproval() ? CommentStatus.PENDING : CommentStatus.APPROVED;
		FieldMap fieldMap = new FieldMapImpl();
		fieldMap.put("comment", new StringFieldImpl().setString(request.getComment()));
		fieldMap.put("status", new StringFieldImpl().setString(status.toFieldValue()));
		fieldMap.put("hidden", new BooleanFieldImpl().setValue(false));
		request.getAuthor().ifPresent(setStringField(fieldMap, "author"));
		request.getSubject().ifPresent(setStringField(fieldMap, "subject"));
		request.getEmail().ifPresent(setStringField(fieldMap, "email"));
		fieldMap.put("authorReference", new StringFieldImpl().setString(author.getUuid()));
		return fieldMap;
	}

	private Consumer<String> setStringField(FieldMap fieldMap, String fieldName) {
		return value -> fieldMap.put(fieldName, new StringFieldImpl().setString(value));
	}

	/**
	 * Tests if the request is valid. Returns an error Completable if the request is invalid.
	 */
	public Completable validateCommentRequest(CreateCommentRequest request, EffectiveCommentSection section) {
		return isAnonymous().doOnSuccess(anonymous -> {
			if (anonymous && !section.allowAnonymous()) {
				throw new AnonymousNotAllowedError();
			}
			if (anonymous) {
				requireAnonymousFieldsPresent(section, request);
			}
			request.getEmail().ifPresent(this::validateEmail);
		}).ignoreElement();
	}

	/**
	 * Tests if the request is valid. Returns an error Completable if the request is invalid.
	 */
	public Completable validateCommentUpdateRequest(UpdateCommentRequest request, EffectiveCommentSection section) {
		return Completable.fromAction(() -> request.getEmail().ifPresent(this::validateEmail))
			.andThen(requirePermission(section, Permission.UPDATE));
	}

	/**
	 * Checks if the current user has the permission on the node that the comments are for.
	 * Returns an error Completable if the user has no permissions.
	 */
	private Completable requirePermission(EffectiveCommentSection section, Permission permission) {
		return meshService.requirePermission(section.getNode(), permission);
	}

	/**
	 * Checks if the current user has the permission on the node that the comments are for.
	 * Returns an error Completable if the user has no permissions.
	 */
	private Completable requirePermission(CommentSection section, Permission permission) {
		return meshService.requirePermission(section.getNode(), permission);
	}

	/**
	 * Validates an email address
	 *
	 * @param email
	 * @throws InvalidEmailException
	 *             if the email is invalid
	 */
	private void validateEmail(String email) {
		if (!email.contains("@")) {
			throw new InvalidEmailException(email);
		}
	}

	/**
	 * Tests if the request has all fields set that are configured to be mandatory for an anonymous user.
	 *
	 * @param section
	 * @param request
	 * @throws RequiredAnonymousFieldMissingError
	 *             if a required field is missing
	 */
	private void requireAnonymousFieldsPresent(EffectiveCommentSection section, CreateCommentRequest request) {
		for (AnonymousCommentField field : section.getRequiredForAnonymous()) {
			if (!isPresent(field, request)) {
				throw new RequiredAnonymousFieldMissingError(field);
			}
		}
	}

	/**
	 * Tests if a single field is present in the request.
	 * 
	 * @param field
	 * @param request
	 * @return
	 */
	private boolean isPresent(AnonymousCommentField field, CreateCommentRequest request) {
		switch (field) {
		case AUTHOR:
			return request.getAuthor().isPresent();
		case EMAIL:
			return request.getEmail().isPresent();
		case SUBJECT:
				return request.getSubject().isPresent();
		default:
			throw new InvalidParameterException("Unknown field " + field.getName());
		}
	}

	/**
	 * Tests if the user for the request is anonymous.
	 * 
	 * @return
	 */
	private Single<Boolean> isAnonymous() {
		return meshService.getCurrentUser()
			.map(response -> response.getUsername().equals("anonymous"));
	}

}
