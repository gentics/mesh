package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuidBlocking;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER_ROOT;

import java.io.IOException;
import java.util.Iterator;

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
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class UserRootImpl extends AbstractRootVertex<User>implements UserRoot {

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_USER);
		database.addVertexType(UserRootImpl.class);
	}

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
	public User create(String username, User creator) {
		User user = getGraph().addFramedVertex(UserImpl.class);
		user.setUsername(username);
		user.enable();

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
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		Database db = MeshSpringConfiguration.getInstance().database();
		Iterator<Vertex> it = db.getVertices(UserImpl.class, new String[] { "uuid" }, new Object[] {userUuid});
		if (!it.hasNext()) {
			return null;
		}
		FramedGraph graph = Database.getThreadLocalGraph();
		MeshAuthUserImpl user = graph.frameElement(it.next(), MeshAuthUserImpl.class);
		if (it.hasNext()) {
			throw new RuntimeException("Found multiple nodes with the same UUID");
		}
		Iterator<Vertex> roots = user.getElement().getVertices(Direction.IN, HAS_USER_ROOT).iterator();
		Vertex root = roots.next();
		if (roots.hasNext()) {
			throw new RuntimeException("Found multiple nodes with the same UUID");
		}
		
		if(root.getId().equals(getId())) {
			return user;
		} else {
			throw new RuntimeException("User does not belong to the UserRoot");
		}
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
			db.noTrx(noTrx -> {
				String userName = requestModel.getUsername();
				User conflictingUser = findByUsername(userName);
				if (conflictingUser != null) {
					HttpStatusCodeErrorException conflictError = conflict(ac, conflictingUser.getUuid(), userName, "user_conflicting_username");
					handler.handle(Future.failedFuture(conflictError));
					return;
				}

				db.trx(txCreate -> {
					MeshAuthUser requestUser = ac.getUser();
					User user = create(requestModel.getUsername(), requestUser);
					user.setFirstname(requestModel.getFirstname());
					user.setUsername(requestModel.getUsername());
					user.setLastname(requestModel.getLastname());
					user.setEmailAddress(requestModel.getEmailAddress());
					user.setPasswordHash(MeshSpringConfiguration.getInstance().passwordEncoder().encode(requestModel.getPassword()));
					requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, user);
					NodeReference reference = requestModel.getNodeReference();
					SearchQueueBatch batch = user.addIndexBatch(CREATE_ACTION);

					if (!isEmpty(groupUuid)) {
						Group parentGroup = loadObjectByUuidBlocking(ac, groupUuid, CREATE_PERM, boot.groupRoot());
						parentGroup.addUser(user);
						batch.addEntry(parentGroup, UPDATE_ACTION);
						requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, user);
					}

					if (reference != null && reference instanceof NodeReferenceImpl) {
						NodeReferenceImpl basicReference = ((NodeReferenceImpl) reference);
						String referencedNodeUuid = basicReference.getUuid();
						String projectName = basicReference.getProjectName();

						if (isEmpty(projectName) || isEmpty(referencedNodeUuid)) {
							txCreate.fail(error(ac, BAD_REQUEST, "user_incomplete_node_reference"));
							return;
						}

						// TODO decide whether we need to check perms on the project as well
						Project project = boot.projectRoot().findByName(projectName);
						if (project == null) {
							txCreate.fail(error(ac, BAD_REQUEST, "project_not_found", projectName));
							return;
						}
						Node node = loadObjectByUuidBlocking(ac, referencedNodeUuid, READ_PERM, project.getNodeRoot());
						user.setReferencedNode(node);
					} else if (reference != null) {
						// TODO handle user create using full node rest model.
						txCreate.fail("Create of users with expanded node reference field is not yet implemented.");
						return;
					}

					txCreate.complete(Tuple.tuple(batch, user));
				} , (AsyncResult<Tuple<SearchQueueBatch, User>> txCreated) -> {
					if (txCreated.failed()) {
						handler.handle(Future.failedFuture(txCreated.cause()));
					} else {
						processOrFail(ac, txCreated.result().v1(), handler, txCreated.result().v2());
					}
				});
			});
		} catch (IOException e) {
			handler.handle(Future.failedFuture(e));
		}

	}
}