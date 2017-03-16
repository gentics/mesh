package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_REFERENCE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

/**
 * @see User
 */
public class UserImpl extends AbstractMeshCoreVertex<UserResponse, User> implements User {

	private static final Logger log = LoggerFactory.getLogger(UserImpl.class);

	public static final String FIRSTNAME_PROPERTY_KEY = "firstname";

	public static final String LASTNAME_PROPERTY_KEY = "lastname";

	public static final String USERNAME_PROPERTY_KEY = "username";

	public static final String EMAIL_PROPERTY_KEY = "emailAddress";

	public static final String PASSWORD_HASH_PROPERTY_KEY = "passwordHash";

	public static final String ENABLED_FLAG_PROPERTY_KEY = "enabledFlag";

	public static final String RESET_TOKEN_KEY = "resetToken";

	public static final String RESET_TOKEN_ISSUE_TIMESTAMP_KEY = "resetTokenTimestamp";

	public static void init(Database database) {
		database.addVertexType(UserImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(ASSIGNED_TO_ROLE, false, false, true);
	}

	@Override
	public User disable() {
		setProperty(ENABLED_FLAG_PROPERTY_KEY, false);
		return this;
	}

	// TODO do we really need disable and deactivate and remove?!
	@Override
	public User deactivate() {
		outE(HAS_GROUP).removeAll();
		disable();
		return this;
	}

	@Override
	public Long getResetTokenIssueTimestamp() {
		return getProperty(RESET_TOKEN_ISSUE_TIMESTAMP_KEY);
	}

	@Override
	public User setResetTokenIssueTimestamp(Long timestamp) {
		setProperty(RESET_TOKEN_ISSUE_TIMESTAMP_KEY, timestamp);
		return this;
	}

	@Override
	public User setResetToken(String token) {
		setProperty(RESET_TOKEN_KEY, token);
		return this;
	}

	@Override
	public String getResetToken() {
		return getProperty(RESET_TOKEN_KEY);
	}

	@Override
	public User enable() {
		setProperty(ENABLED_FLAG_PROPERTY_KEY, true);
		return this;
	}

	@Override
	public boolean isEnabled() {
		return BooleanUtils.toBoolean(getProperty(ENABLED_FLAG_PROPERTY_KEY).toString());
	}

	@Override
	public String getFirstname() {
		return getProperty(FIRSTNAME_PROPERTY_KEY);
	}

	@Override
	public User setFirstname(String name) {
		setProperty(FIRSTNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getLastname() {
		return getProperty(LASTNAME_PROPERTY_KEY);
	}

	@Override
	public User setLastname(String name) {
		setProperty(LASTNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getName() {
		return getUsername();
	}

	@Override
	public void setName(String name) {
		setUsername(name);
	}

	@Override
	public String getUsername() {
		return getProperty(USERNAME_PROPERTY_KEY);
	}

	@Override
	public User setUsername(String name) {
		setProperty(USERNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getEmailAddress() {
		return getProperty(EMAIL_PROPERTY_KEY);
	}

	@Override
	public User setEmailAddress(String emailAddress) {
		setProperty(EMAIL_PROPERTY_KEY, emailAddress);
		return this;
	}

	@Override
	public List<? extends Group> getGroups() {
		return out(HAS_USER).toListExplicit(GroupImpl.class);
	}

	@Override
	public List<? extends Role> getRoles() {
		return out(HAS_USER).in(HAS_ROLE).toListExplicit(RoleImpl.class);
	}

	@Override
	public List<? extends Role> getRolesViaShortcut() {
		return out(ASSIGNED_TO_ROLE).toListExplicit(RoleImpl.class);
	}

	@Override
	public Node getReferencedNode() {
		return out(HAS_NODE_REFERENCE).nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public User setReferencedNode(Node node) {
		setUniqueLinkOutTo(node, HAS_NODE_REFERENCE);
		return this;
	}

	@Override
	public PermissionInfo getPermissionInfo(MeshVertex vertex) {
		PermissionInfo info = new PermissionInfo();
		Set<GraphPermission> permissions = getPermissions(vertex);
		for (GraphPermission perm : permissions) {
			info.set(perm.getRestPerm(), true);
		}
		info.setOthers(false);

		return info;
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex vertex) {
		Set<GraphPermission> graphPermissions = new HashSet<>();
		// Check all permissions one at a time and add granted permissions to the set
		for (GraphPermission perm : GraphPermission.values()) {
			if (hasPermission(vertex, perm)) {
				graphPermissions.add(perm);
			}
		}
		return graphPermissions;
	}

	@Override
	public boolean hasAdminRole() {
		for (Role role : getRolesViaShortcut()) {
			if ("admin".equals(role.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasPermissionForId(Object elementId, GraphPermission permission) {
		if (PermissionStore.hasPermission(getId(), permission, elementId)) {
			return true;
		} else {
			FramedGraph graph = Database.getThreadLocalGraph();
			// Find all roles that are assigned to the user by checking the
			// shortcut edge from the index
			Iterable<Edge> roleEdges = graph.getEdges("e." + ASSIGNED_TO_ROLE + "_out", this.getId());
			for (Edge roleEdge : roleEdges) {
				Vertex role = roleEdge.getVertex(Direction.IN);
				// Find all permission edges between the found role and target
				// vertex with the specified label
				Iterable<Edge> edges = graph.getEdges("e." + permission.label() + "_inout",
						MeshInternal.get().database().createComposedIndexKey(elementId, role.getId()));
				boolean foundPermEdge = edges.iterator().hasNext();
				if (foundPermEdge) {
					// We only store granting permissions in the store in order
					// reduce the invalidation calls.
					// This way we do not need to invalidate the cache if a role
					// is removed from a group or a role is deleted.
					PermissionStore.store(getId(), permission, elementId);
					return true;
				}
			}
			return false;
		}

	}

	@Override
	public boolean hasPermission(MeshVertex vertex, GraphPermission permission) {
		if (log.isTraceEnabled()) {
			log.debug("Checking permissions for vertex {" + vertex.getUuid() + "}");
		}
		return hasPermissionForId(vertex.getId(), permission);
	}

	@Override
	public UserReference transformToReference() {
		return new UserReference().setFirstName(getFirstname()).setLastName(getLastname()).setUuid(getUuid());
	}

	@Override
	public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		UserResponse restUser = new UserResponse();

		restUser.setUsername(getUsername());
		restUser.setEmailAddress(getEmailAddress());
		restUser.setFirstname(getFirstname());
		restUser.setLastname(getLastname());
		restUser.setEnabled(isEnabled());

		setNodeReference(ac, restUser, level);
		setGroups(ac, restUser);
		setRolePermissions(ac, restUser);
		fillCommonRestFields(ac, restUser);

		return restUser;
	}

	/**
	 * Set the groups to which the user belongs in the rest model.
	 * 
	 * @param ac
	 * @param restUser
	 */
	private void setGroups(InternalActionContext ac, UserResponse restUser) {
		for (Group group : getGroups()) {
			GroupReference reference = group.transformToReference();
			restUser.getGroups().add(reference);
		}
	}

	/**
	 * Add the node reference field to the user response (if required to).
	 * 
	 * @param ac
	 * @param restUser
	 * @param level
	 *            Current depth level of transformation
	 */
	private void setNodeReference(InternalActionContext ac, UserResponse restUser, int level) {
		NodeParametersImpl parameters = new NodeParametersImpl(ac);

		// Check whether a node reference was set.
		Node node = getReferencedNode();
		if (node == null) {
			return;
		}

		// Check whether the node reference field of the user should be expanded
		boolean expandReference = parameters.getExpandedFieldnameList().contains("nodeReference") || parameters.getExpandAll();
		if (expandReference) {
			restUser.setNodeResponse(node.transformToRestSync(ac, level));
		} else {
			NodeReference userNodeReference = node.transformToReference(ac);
			restUser.setNodeReference(userNodeReference);
		}

	}

	@Override
	public User addGroup(Group group) {
		// Redirect to group implementation
		group.addUser(this);
		return this;
	}

	@Override
	public String getPasswordHash() {
		return getProperty(PASSWORD_HASH_PROPERTY_KEY);
	}

	@Override
	public User setPasswordHash(String hash) {
		setProperty(PASSWORD_HASH_PROPERTY_KEY, hash);
		return this;
	}

	@Override
	public User addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode) {
		addPermissionsOnRole(sourceNode, permission, targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM, PUBLISH_PERM, READ_PUBLISHED_PERM);
		return this;
	}

	@Override
	public User addPermissionsOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant) {
		// 1. Determine all roles that grant given permission on the source
		// node.
		List<? extends Role> rolesThatGrantPermission = sourceNode.in(permission.label()).toListExplicit(RoleImpl.class);

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : rolesThatGrantPermission) {
			role.grantPermissions(targetNode, toGrant);
		}

		inheritRolePermissions(sourceNode, targetNode);
		return this;
	}

	@Override
	public User inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {

		for (GraphPermission perm : GraphPermission.values()) {
			List<? extends Role> rolesWithPerm = sourceNode.in(perm.label()).has(RoleImpl.class).toListExplicit(RoleImpl.class);
			for (Role role : rolesWithPerm) {
				if (log.isDebugEnabled()) {
					log.debug("Granting permission {" + perm.name() + "} to node {" + targetNode.getUuid() + "} on role {" + role.getName() + "}");
				}
				role.grantPermissions(targetNode, perm);
			}
		}
		return this;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO don't allow this for the admin user
		// disable();
		// TODO we should not really delete users. Instead we should remove
		// those from all groups and deactivate the access.
		// if (log.isDebugEnabled()) {
		// log.debug("Deleting user. The user will not be deleted. Instead the
		// user will be just disabled and removed from all groups.");
		// }
		// outE(HAS_USER).removeAll();
		batch.delete(this, false);
		getElement().remove();
		PermissionStore.invalidate();
	}

	/**
	 * Encode the given password and set the generated hash.
	 * 
	 * @param password Plain password to be hashed and set
	 * @return Fluent API
	 */
	@Override
	public User setPassword(String password) {
		setPasswordHash(MeshInternal.get().passwordEncoder().encode(password));
		return this;
	}

	@Override
	public User update(InternalActionContext ac, SearchQueueBatch batch) {
		UserUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), UserUpdateRequest.class);
		if (shouldUpdate(requestModel.getUsername(), getUsername())) {
			User conflictingUser = MeshInternal.get().boot().userRoot().findByUsername(requestModel.getUsername());
			if (conflictingUser != null && !conflictingUser.getUuid().equals(getUuid())) {
				throw conflict(conflictingUser.getUuid(), requestModel.getUsername(), "user_conflicting_username");
			}
			setUsername(requestModel.getUsername());
		}

		if (shouldUpdate(requestModel.getFirstname(), getFirstname())) {
			setFirstname(requestModel.getFirstname());
		}

		if (shouldUpdate(requestModel.getLastname(), getLastname())) {
			setLastname(requestModel.getLastname());
		}

		if (shouldUpdate(requestModel.getEmailAddress(), getEmailAddress())) {
			setEmailAddress(requestModel.getEmailAddress());
		}

		if (!isEmpty(requestModel.getPassword())) {
			BCryptPasswordEncoder encoder = MeshInternal.get().passwordEncoder();
			setPasswordHash(encoder.encode(requestModel.getPassword()));
		}

		setEditor(ac.getUser());
		setLastEditedTimestamp();

		if (requestModel.getNodeReference() != null) {
			ExpandableNode reference = requestModel.getNodeReference();
			if (reference instanceof NodeResponse) {
				// TODO also handle full node response inside node reference
				// field
				// TODO i18n
				throw error(BAD_REQUEST, "Handling node responses for user updates is not yet supported");
			}
			if (reference instanceof NodeReference) {
				NodeReference basicReference = ((NodeReference) reference);
				if (isEmpty(basicReference.getProjectName()) || isEmpty(reference.getUuid())) {
					throw error(BAD_REQUEST, "user_incomplete_node_reference");
				}
				String referencedNodeUuid = basicReference.getUuid();
				String projectName = basicReference.getProjectName();
				/*
				 * TODO decide whether we need to check perms on the project as well
				 */
				Project project = MeshInternal.get().boot().projectRoot().findByName(projectName);
				if (project == null) {
					throw error(BAD_REQUEST, "project_not_found", projectName);
				}
				NodeRoot nodeRoot = project.getNodeRoot();
				Node node = nodeRoot.loadObjectByUuid(ac, referencedNodeUuid, READ_PERM);
				setReferencedNode(node);
			}
		}
		batch.store(this, true);
		return this;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		Node referencedNode = getReferencedNode();
		keyBuilder.append(getUuid());
		keyBuilder.append("-");
		keyBuilder.append(getLastEditedTimestamp());
		boolean expandReference = ac.getNodeParameters().getExpandedFieldnameList().contains("nodeReference")
				|| ac.getNodeParameters().getExpandAll();
		// We only need to compute the full etag if the referenced node is
		// expanded.
		if (referencedNode != null && expandReference) {
			keyBuilder.append("-");
			keyBuilder.append(referencedNode.getETag(ac));
		} else if (referencedNode != null) {
			keyBuilder.append("-");
			keyBuilder.append(referencedNode.getUuid());
			keyBuilder.append(referencedNode.getProject().getName());
		}

		return ETag.hash(keyBuilder.toString());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/users/" + getUuid();
	}

	@Override
	public boolean canReadNode(InternalActionContext ac, Node node) {
		String version = ac.getVersioningParameters().getVersion();
		if (ContainerType.forVersion(version) == ContainerType.PUBLISHED) {
			return ac.getUser().hasPermission(node, GraphPermission.READ_PUBLISHED_PERM);
		} else {
			return ac.getUser().hasPermission(node, GraphPermission.READ_PERM);
		}
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public Single<UserResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return db.operateNoTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
