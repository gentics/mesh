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

import org.apache.commons.lang.NotImplementedException;

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
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.NonTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;

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
	public void create(ActionContext ac, Handler<AsyncResult<User>> handler) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		UserCreateRequest requestModel = ac.fromJson(UserCreateRequest.class);
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
		try (NonTrx tx = db.nonTrx()) {
			// Load the parent group for the user
			loadObjectByUuid(ac, groupUuid, CREATE_PERM, boot.groupRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Group parentGroup = rh.result();
					if (findByUsername(requestModel.getUsername()) != null) {
						String message = ac.i18n("user_conflicting_username");
						handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT, message)));
						return;
					}
					MeshAuthUser requestUser = ac.getUser();
					User user;
					SearchQueueBatch batch;
					try (Trx txCreate = db.trx()) {
						requestUser.reload();
						user = create(requestModel.getUsername(), parentGroup, requestUser);
						user.setFirstname(requestModel.getFirstname());
						user.setUsername(requestModel.getUsername());
						user.setLastname(requestModel.getLastname());
						user.setEmailAddress(requestModel.getEmailAddress());
						user.setPasswordHash(
								MeshSpringConfiguration.getMeshSpringConfiguration().passwordEncoder().encode(requestModel.getPassword()));
						user.addGroup(parentGroup);
						requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, user);
						NodeReference reference = requestModel.getNodeReference();
						if (reference != null) {
							if (isEmpty(reference.getProjectName()) || isEmpty(reference.getUuid())) {
								handler.handle(Future
										.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("user_incomplete_node_reference"))));
								return;
							}

							String referencedNodeUuid = requestModel.getNodeReference().getUuid();
							String projectName = requestModel.getNodeReference().getProjectName();
							// TODO decide whether we need to check perms on the project as well
							Project project = boot.projectRoot().findByName(projectName);
							if (project == null) {
								handler.handle(Future
										.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("project_not_found", projectName))));
								return;
							}
							Node node;
							try (NonTrx tx2 = db.nonTrx()) {
								node = loadObjectByUuidBlocking(ac, referencedNodeUuid, READ_PERM, project.getNodeRoot());
							}
							user.setReferencedNode(node);
						}
						batch = user.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
						txCreate.success();
					}
					processOrFail(ac, batch, handler, user);

				}
			});
		}

	}
}