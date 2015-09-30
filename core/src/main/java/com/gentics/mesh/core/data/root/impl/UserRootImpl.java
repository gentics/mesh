package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuidBlocking;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class UserRootImpl extends AbstractRootVertex<User>implements UserRoot {

	@Override
	protected Class<? extends User> getPersistanceClass() {
		return UserImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_USER;
	}

	@Override
	public void addUser(User user) {
		addItem(user);
	}

	@Override
	public void removeUser(User user) {
		removeItem(user);
	}

	@Override
	public User create(String username, Group group, User creator) {
		User user = getGraph().addFramedVertex(UserImpl.class);
		user.setUsername(username);
		user.enable();
		if (group != null) {
			group.addUser(user);
		}
		if (creator != null) {
			user.setCreator(creator);
			user.setCreationTimestamp(System.currentTimeMillis());
			user.setEditor(creator);
			user.setLastEditedTimestamp(System.currentTimeMillis());
		}
		addItem(user);
		return user;
	}

	@Override
	public User findByUsername(String username) {
		return out(HAS_USER).has(UserImpl.class).has(UserImpl.USERNAME_PROPERTY_KEY, username).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return out(HAS_USER).has(UserImpl.class).has(UserImpl.USERNAME_PROPERTY_KEY, username).nextOrDefaultExplicit(MeshAuthUserImpl.class, null);
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The user root should never be deleted");
	}

	@Override
	public void create(InternalActionContext ac, Handler<AsyncResult<User>> handler) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		Database db = MeshSpringConfiguration.getInstance().database();
		UserCreateRequest requestModel;
		try {
			requestModel = JsonUtil.readNode(ac.getBodyAsString(), UserCreateRequest.class, ServerSchemaStorage.getSchemaStorage());
			if (requestModel == null) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("error_parse_request_json_error"))));
				return;
			}
			if (isEmpty(requestModel.getPassword())) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("user_missing_password"))));
				return;
			}
			if (isEmpty(requestModel.getUsername())) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("user_missing_username"))));
				return;
			}
			String groupUuid = requestModel.getGroupUuid();
			if (isEmpty(groupUuid)) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("user_missing_parentgroup_field"))));
				return;
			}
			db.noTrx(noTrx -> {
				// Load the parent group for the user
				loadObjectByUuid(ac, groupUuid, CREATE_PERM, boot.groupRoot(), rh -> {
					if (hasSucceeded(ac, rh)) {
						Group parentGroup = rh.result();
						if (findByUsername(requestModel.getUsername()) != null) {
							String message = ac.i18n("user_conflicting_username");
							handler.handle(ac.failedFuture(CONFLICT, message));
							return;
						}

						db.blockingTrx(txCreate -> {
							MeshAuthUser requestUser = ac.getUser();
							User user = create(requestModel.getUsername(), parentGroup, requestUser);
							user.setFirstname(requestModel.getFirstname());
							user.setUsername(requestModel.getUsername());
							user.setLastname(requestModel.getLastname());
							user.setEmailAddress(requestModel.getEmailAddress());
							user.setPasswordHash(MeshSpringConfiguration.getInstance().passwordEncoder().encode(requestModel.getPassword()));
							user.addGroup(parentGroup);
							requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, user);
							NodeReference reference = requestModel.getNodeReference();

							if (reference != null && reference instanceof NodeReferenceImpl) {
								NodeReferenceImpl basicReference = ((NodeReferenceImpl) reference);
								String referencedNodeUuid = basicReference.getUuid();
								String projectName = basicReference.getProjectName();

								if (isEmpty(projectName) || isEmpty(referencedNodeUuid)) {
									handler.handle(Future
											.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("user_incomplete_node_reference"))));
									return;
								}

								// TODO decide whether we need to check perms on the project as well
								Project project = boot.projectRoot().findByName(projectName);
								if (project == null) {
									handler.handle(Future
											.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("project_not_found", projectName))));
									return;
								}
								Node node = loadObjectByUuidBlocking(ac, referencedNodeUuid, READ_PERM, project.getNodeRoot());
								user.setReferencedNode(node);
							} else if (reference != null) {
								// TODO handle user create using full node rest model.
								txCreate.fail("Create of users with expanded node reference field is not yet implemented.");
								return;
							}

							SearchQueueBatch batch = user.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
							txCreate.complete(Tuple.tuple(batch, user));
						} , (AsyncResult<Tuple<SearchQueueBatch, User>> txCreated) -> {
							if (txCreated.failed()) {
								handler.handle(Future.failedFuture(txCreated.cause()));
							} else {
								processOrFail(ac, txCreated.result().v1(), handler, txCreated.result().v2());
							}
						});

					}
				});

			});
		} catch (IOException e) {
			handler.handle(Future.failedFuture(e));
		}

	}
}