package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuidBlocking;
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
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class UserRootImpl extends AbstractRootVertex<User>implements UserRoot {

	private static final Logger log = LoggerFactory.getLogger(UserRootImpl.class);

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

	// TODO unique index

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
	public void create(RoutingContext rc, Handler<AsyncResult<User>> handler) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		I18NService i18n = I18NService.getI18n();
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		UserCreateRequest requestModel = fromJson(rc, UserCreateRequest.class);
		if (requestModel == null) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_parse_request_json_error"))));
			return;
		}
		if (isEmpty(requestModel.getPassword())) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_missing_password"))));
			return;
		}
		if (isEmpty(requestModel.getUsername())) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_missing_username"))));
			return;
		}
		String groupUuid = requestModel.getGroupUuid();
		if (isEmpty(groupUuid)) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_missing_parentgroup_field"))));
			return;
		}
		try (Trx tx = db.trx()) {
			// Load the parent group for the user
			loadObjectByUuid(rc, groupUuid, CREATE_PERM, boot.groupRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Group parentGroup = rh.result();
					if (findByUsername(requestModel.getUsername()) != null) {
						String message = i18n.get(rc, "user_conflicting_username");
						handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT, message)));
						return;
					}
					MeshAuthUser requestUser = getUser(rc);
					User user;
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
										.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_incomplete_node_reference"))));
								return;
							}

							String referencedNodeUuid = requestModel.getNodeReference().getUuid();
							String projectName = requestModel.getNodeReference().getProjectName();
							// TODO decide whether we need to check perms on the project as well
							Project project = boot.projectRoot().findByName(projectName);
							if (project == null) {
								handler.handle(Future
										.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "project_not_found", projectName))));
								return;
							}
							Node node;
							try (Trx tx2 = MeshSpringConfiguration.getMeshSpringConfiguration().database().trx()) {
								node = loadObjectByUuidBlocking(rc, referencedNodeUuid, READ_PERM, project.getNodeRoot());
							}
							user.setReferencedNode(node);
						}
						txCreate.success();
					}
					handler.handle(Future.succeededFuture(user));

				}
			});
		}

	}
}