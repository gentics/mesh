package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

import rx.Observable;

public class UserRootImpl extends AbstractRootVertex<User> implements UserRoot {

	public static void checkIndices(Database database) {
		database.addVertexType(UserRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_USER);
	}

	@Override
	public Class<? extends User> getPersistanceClass() {
		return UserImpl.class;
	}

	@Override
	public String getSearchIndexNames() {
		return User.TYPE;
	}

	@Override
	public String getRootLabel() {
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
		Iterator<Vertex> it = db.getVertices(UserImpl.class, new String[] { "uuid" }, new Object[] { userUuid });
		if (!it.hasNext()) {
			return null;
		}
		FramedGraph graph = Database.getThreadLocalGraph();
		MeshAuthUserImpl user = graph.frameElement(it.next(), MeshAuthUserImpl.class);
		if (it.hasNext()) {
			throw new RuntimeException("Found multiple nodes with the same UUID");
		}
		Iterator<Vertex> roots = user.getElement().getVertices(Direction.IN, HAS_USER).iterator();
		Vertex root = roots.next();
		if (roots.hasNext()) {
			throw new RuntimeException("Found multiple nodes with the same UUID");
		}

		if (root.getId().equals(getId())) {
			return user;
		} else {
			throw new RuntimeException("User does not belong to the UserRoot");
		}
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException("The user root should never be deleted");
	}

	@Override
	public Observable<User> create(InternalActionContext ac) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		Database db = MeshSpringConfiguration.getInstance().database();

		try {
			UserCreateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), UserCreateRequest.class);
			if (requestModel == null) {
				throw error(BAD_REQUEST, "error_parse_request_json_error");
			}
			if (isEmpty(requestModel.getPassword())) {
				throw error(BAD_REQUEST, "user_missing_password");
			}
			if (isEmpty(requestModel.getUsername())) {
				throw error(BAD_REQUEST, "user_missing_username");
			}
			String groupUuid = requestModel.getGroupUuid();
			User createdUser = db.noTrx(() -> {
				String userName = requestModel.getUsername();
				User conflictingUser = findByUsername(userName);
				if (conflictingUser != null) {
					throw conflict(conflictingUser.getUuid(), userName, "user_conflicting_username");
				}

				Tuple<SearchQueueBatch, User> tuple = db.trx(() -> {
					MeshAuthUser requestUser = ac.getUser();
					User user = create(requestModel.getUsername(), requestUser);
					user.setFirstname(requestModel.getFirstname());
					user.setUsername(requestModel.getUsername());
					user.setLastname(requestModel.getLastname());
					user.setEmailAddress(requestModel.getEmailAddress());
					user.setPasswordHash(MeshSpringConfiguration.getInstance().passwordEncoder().encode(requestModel.getPassword()));
					requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, user);
					NodeReference reference = requestModel.getNodeReference();
					SearchQueueBatch batch = user.createIndexBatch(STORE_ACTION);

					if (!isEmpty(groupUuid)) {
						Group parentGroup = boot.groupRoot().loadObjectByUuid(ac, groupUuid, CREATE_PERM).toBlocking().first();
						parentGroup.addUser(user);
						batch.addEntry(parentGroup, STORE_ACTION);
						requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, user);
					}

					if (reference != null && reference instanceof NodeReferenceImpl) {
						NodeReferenceImpl basicReference = ((NodeReferenceImpl) reference);
						String referencedNodeUuid = basicReference.getUuid();
						String projectName = basicReference.getProjectName();

						if (isEmpty(projectName) || isEmpty(referencedNodeUuid)) {
							throw error(BAD_REQUEST, "user_incomplete_node_reference");
						}

						// TODO decide whether we need to check perms on the project as well
						Project project = boot.projectRoot().findByName(projectName).toBlocking().single();
						if (project == null) {
							throw error(BAD_REQUEST, "project_not_found", projectName);
						}
						Node node = project.getNodeRoot().loadObjectByUuid(ac, referencedNodeUuid, READ_PERM).toBlocking().first();
						user.setReferencedNode(node);
					} else if (reference != null) {
						// TODO handle user create using full node rest model.
						throw error(BAD_REQUEST, "user_creation_full_node_reference_not_implemented");
					}

					return Tuple.tuple(batch, user);
				});

				reload();
				SearchQueueBatch batch = tuple.v1();
				//				User createdUser = tuple.v2();
				return batch.process().map(done -> {
					return tuple.v2();
				}).toBlocking().first();

			});

			return Observable.just(createdUser);

		} catch (IOException e) {
			return Observable.error(e);
		}

	}
}