package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.search.EventQueueBatch;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

/**
 * @see UserRoot
 */
public class UserRootImpl extends AbstractRootVertex<User> implements UserRoot {

	/**
	 * Initialise the type and indices for this type.
	 * 
	 * @param database
	 */
	public static void init(Database database) {
		database.addVertexType(UserRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_USER, true, false, true);
	}

	@Override
	public Class<? extends User> getPersistanceClass() {
		return UserImpl.class;
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
	public User create(String username, User creator, String uuid) {
		User user = getGraph().addFramedVertex(UserImpl.class);
		if (uuid != null) {
			user.setUuid(uuid);
		}
		user.setUsername(username);
		user.enable();

		if (creator != null) {
			user.setCreator(creator);
			user.setCreationTimestamp();
			user.setEditor(creator);
			user.setLastEditedTimestamp();
		}
		addItem(user);
		return user;
	}

	/**
	 * Redirected to {@link #findByUsername(String)}
	 */
	@Override
	public User findByName(String name) {
		return findByUsername(name);
	}

	@Override
	public User findByUsername(String username) {
		return out(HAS_USER).has(UserImpl.USERNAME_PROPERTY_KEY, username).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		// TODO use index
		return out(HAS_USER).has(UserImpl.USERNAME_PROPERTY_KEY, username).nextOrDefaultExplicit(MeshAuthUserImpl.class, null);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		Database db = MeshInternal.get().database();
		Iterator<Vertex> it = db.getVertices(UserImpl.class, new String[] { "uuid" }, new Object[] { userUuid });
		if (!it.hasNext()) {
			return null;
		}
		FramedGraph graph = getGraph();
		MeshAuthUserImpl user = graph.frameElement(it.next(), MeshAuthUserImpl.class);
		if (it.hasNext()) {
			throw new RuntimeException("Found multiple nodes with the same UUID");
		}
		Iterator<Vertex> roots = user.getElement().getVertices(Direction.IN, HAS_USER).iterator();
		Vertex root = roots.next();
		if (roots.hasNext()) {
			throw new RuntimeException("Found multiple nodes with the same UUID");
		}

		if (root.getId().equals(id())) {
			return user;
		} else {
			throw new RuntimeException("User does not belong to the UserRoot");
		}
	}

	@Override
	public void delete(BulkActionContext context) {
		throw new NotImplementedException("The user root should never be deleted");
	}

	@Override
	public User create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		BootstrapInitializer boot = MeshInternal.get().boot();
		MeshAuthUser requestUser = ac.getUser();

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
		if (!requestUser.hasPermission(this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", this.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		String groupUuid = requestModel.getGroupUuid();
		String userName = requestModel.getUsername();
		User conflictingUser = findByUsername(userName);
		if (conflictingUser != null) {
			throw conflict(conflictingUser.getUuid(), userName, "user_conflicting_username");
		}

		User user = create(requestModel.getUsername(), requestUser, uuid);
		user.setFirstname(requestModel.getFirstname());
		user.setUsername(requestModel.getUsername());
		user.setLastname(requestModel.getLastname());
		user.setEmailAddress(requestModel.getEmailAddress());
		user.setPasswordHash(MeshInternal.get().passwordEncoder().encode(requestModel.getPassword()));

		requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, user);
		ExpandableNode reference = requestModel.getNodeReference();
		batch.store(user, true);

		if (!isEmpty(groupUuid)) {
			Group parentGroup = boot.groupRoot().loadObjectByUuid(ac, groupUuid, CREATE_PERM);
			parentGroup.addUser(user);
			batch.store(parentGroup, false);
			requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, user);
		}

		if (reference != null && reference instanceof NodeReference) {
			NodeReference basicReference = ((NodeReference) reference);
			String referencedNodeUuid = basicReference.getUuid();
			String projectName = basicReference.getProjectName();

			if (isEmpty(projectName) || isEmpty(referencedNodeUuid)) {
				throw error(BAD_REQUEST, "user_incomplete_node_reference");
			}

			// TODO decide whether we need to check perms on the project as well
			Project project = boot.projectRoot().findByName(projectName);
			if (project == null) {
				throw error(BAD_REQUEST, "project_not_found", projectName);
			}
			Node node = project.getNodeRoot().loadObjectByUuid(ac, referencedNodeUuid, READ_PERM);
			user.setReferencedNode(node);
		} else if (reference != null) {
			// TODO handle user create using full node rest model.
			throw error(BAD_REQUEST, "user_creation_full_node_reference_not_implemented");
		}
		return user;
	}
}