package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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

	public static final String FORCE_PASSWORD_CHANGE_KEY = "forcePasswordChange";

	public static void init(Database database) {
		database.addVertexType(UserImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(ASSIGNED_TO_ROLE, false, false, true);
	}

	@Override
	public User disable() {
		property(ENABLED_FLAG_PROPERTY_KEY, false);
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
		return property(RESET_TOKEN_ISSUE_TIMESTAMP_KEY);
	}

	@Override
	public User setResetTokenIssueTimestamp(Long timestamp) {
		property(RESET_TOKEN_ISSUE_TIMESTAMP_KEY, timestamp);
		return this;
	}

	@Override
	public User setResetToken(String token) {
		property(RESET_TOKEN_KEY, token);
		return this;
	}

	@Override
	public String getResetToken() {
		return property(RESET_TOKEN_KEY);
	}

	@Override
	public boolean isForcedPasswordChange() {
		return Optional.<Boolean>ofNullable(property(FORCE_PASSWORD_CHANGE_KEY)).orElse(false);
	}

	@Override
	public User setForcedPasswordChange(boolean force) {
		property(FORCE_PASSWORD_CHANGE_KEY, force);
		return this;
	}

	@Override
	public User enable() {
		property(ENABLED_FLAG_PROPERTY_KEY, true);
		return this;
	}

	@Override
	public boolean isEnabled() {
		return BooleanUtils.toBoolean(property(ENABLED_FLAG_PROPERTY_KEY).toString());
	}

	@Override
	public String getFirstname() {
		return property(FIRSTNAME_PROPERTY_KEY);
	}

	@Override
	public User setFirstname(String name) {
		property(FIRSTNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getLastname() {
		return property(LASTNAME_PROPERTY_KEY);
	}

	@Override
	public User setLastname(String name) {
		property(LASTNAME_PROPERTY_KEY, name);
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
		return property(USERNAME_PROPERTY_KEY);
	}

	@Override
	public User setUsername(String name) {
		property(USERNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getEmailAddress() {
		return property(EMAIL_PROPERTY_KEY);
	}

	@Override
	public User setEmailAddress(String emailAddress) {
		property(EMAIL_PROPERTY_KEY, emailAddress);
		return this;
	}

	@Override
	public Page<? extends Group> getGroups(User user, PagingParameters params) {
		VertexTraversal<?, ?, ?> traversal = out(HAS_USER);
		return new DynamicTransformablePageImpl<Group>(user, traversal, params, READ_PERM, GroupImpl.class);
	}

	@Override
	public TraversalResult<? extends Group> getGroups() {
		return new TraversalResult<>(out(HAS_USER).frameExplicit(GroupImpl.class));
	}

	@Override
	public String getRolesHash() {
		String indexName = "e." + ASSIGNED_TO_ROLE + "_out";
		Spliterator<Edge> itemEdges = getGraph().getEdges(indexName.toLowerCase(), id()).spliterator();
		String roles = StreamSupport.stream(itemEdges, false)
			.map(itemEdge -> itemEdge.getVertex(Direction.IN).getId().toString())
			.sorted()
			.collect(Collectors.joining());

		return ETag.hash(roles);
	}

	@Override
	public TraversalResult<? extends Role> getRoles() {
		return new TraversalResult<>(out(HAS_USER).in(HAS_ROLE).frameExplicit(RoleImpl.class));
	}

	@Override
	public TraversalResult<? extends Role> getRolesViaShortcut() {
		// TODO Use shortcut index.
		return new TraversalResult<>(out(ASSIGNED_TO_ROLE).frameExplicit(RoleImpl.class));
	}

	@Override
	public Page<? extends Role> getRolesViaShortcut(User user, PagingParameters params) {
		String indexName = "e." + ASSIGNED_TO_ROLE + "_out";
		return new DynamicTransformablePageImpl<>(user, indexName.toLowerCase(), id(), Direction.IN, RoleImpl.class, params, READ_PERM, null, true);
	}

	@Override
	public void updateShortcutEdges() {
		outE(ASSIGNED_TO_ROLE).removeAll();
		for (Group group : getGroups()) {
			for (Role role : group.getRoles()) {
				setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
			}
		}
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
		info.setOthers(false, vertex.hasPublishPermissions());

		return info;
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex vertex) {
		Predicate<? super GraphPermission> isValidPermission = perm ->
			perm != READ_PUBLISHED_PERM && perm != PUBLISH_PERM || vertex.hasPublishPermissions();

		return Stream.of(GraphPermission.values())
			// Don't check for publish perms if it does not make sense for the vertex type
			.filter(isValidPermission)
			.filter(perm -> hasPermission(vertex, perm))
			.collect(Collectors.toSet());
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
			FramedGraph graph = getGraph();
			// Find all roles that are assigned to the user by checking the
			// shortcut edge from the index
			String idxKey = "e." + ASSIGNED_TO_ROLE + "_out";
			Iterable<Edge> roleEdges = graph.getEdges(idxKey.toLowerCase(), this.id());
			for (Edge roleEdge : roleEdges) {
				Vertex role = roleEdge.getVertex(Direction.IN);
				// Find all permission edges between the found role and target
				// vertex with the specified label
				String roleEdgeIdx = "e." + permission.label() + "_inout";
				Iterable<Edge> edges = graph.getEdges(roleEdgeIdx.toLowerCase(),
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
			// Fall back to read and check whether the user has read perm. Read permission also includes read published.
			if (permission == READ_PUBLISHED_PERM) {
				return hasPermissionForId(elementId, READ_PERM);
			} else {
				return false;
			}
		}

	}

	@Override
	public boolean hasPermission(MeshVertex vertex, GraphPermission permission) {
		if (log.isTraceEnabled()) {
			log.debug("Checking permissions for vertex {" + vertex.getUuid() + "}");
		}
		return hasPermissionForId(vertex.id(), permission);
	}

	@Override
	public void failOnNoReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		Node node = container.getParentNode();
		if (hasPermission(node, READ_PERM)) {
			return;
		}
		boolean published = container.isPublished(branchUuid);
		if (published && hasPermission(node, READ_PUBLISHED_PERM)) {
			return;
		}
		throw error(FORBIDDEN, "error_missing_perm", node.getUuid(),
			"published".equals(requestedVersion)
				? READ_PUBLISHED_PERM.getRestPerm().getName()
				: READ_PERM.getRestPerm().getName());
	}

	@Override
	public UserReference transformToReference() {
		return new UserReference().setFirstName(getFirstname()).setLastName(getLastname()).setUuid(getUuid());
	}

	@Override
	public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		UserResponse restUser = new UserResponse();

		if (fields.has("username")) {
			restUser.setUsername(getUsername());
		}
		if (fields.has("emailAddress")) {
			restUser.setEmailAddress(getEmailAddress());
		}
		if (fields.has("firstname")) {
			restUser.setFirstname(getFirstname());
		}
		if (fields.has("lastname")) {
			restUser.setLastname(getLastname());
		}
		if (fields.has("enabled")) {
			restUser.setEnabled(isEnabled());
		}
		if (fields.has("nodeReference")) {
			setNodeReference(ac, restUser, level);
		}
		if (fields.has("groups")) {
			setGroups(ac, restUser);
		}
		if (fields.has("rolesHash")) {
			restUser.setRolesHash(getRolesHash());
		}
		if (fields.has("forcedPasswordChange")) {
			restUser.setForcedPasswordChange(isForcedPasswordChange());
		}
		fillCommonRestFields(ac, fields, restUser);
		setRolePermissions(ac, restUser);

		return restUser;
	}

	/**
	 * Set the groups to which the user belongs in the rest model.
	 *
	 * @param ac
	 * @param restUser
	 */
	private void setGroups(InternalActionContext ac, UserResponse restUser) {
		// TODO filter by permissions
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
		NodeParameters parameters = ac.getNodeParameters();

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
		return property(PASSWORD_HASH_PROPERTY_KEY);
	}

	@Override
	public User setPasswordHash(String hash) {
		property(PASSWORD_HASH_PROPERTY_KEY, hash);
		// Password has changed, the user is not forced to change their password anymore.
		setForcedPasswordChange(false);
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
			Iterable<? extends RoleImpl> rolesWithPerm = sourceNode.in(perm.label()).frameExplicit(RoleImpl.class);
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
	public void delete(BulkActionContext bac) {
		// TODO don't allow this for the admin user
		// disable();
		// TODO we should not really delete users. Instead we should remove
		// those from all groups and deactivate the access.
		// if (log.isDebugEnabled()) {
		// log.debug("Deleting user. The user will not be deleted. Instead the
		// user will be just disabled and removed from all groups.");
		// }
		// outE(HAS_USER).removeAll();
		bac.add(onDeleted());
		getElement().remove();
		bac.process();
		PermissionStore.invalidate();
	}

	/**
	 * Encode the given password and set the generated hash.
	 *
	 * @param password
	 *            Plain password to be hashed and set
	 * @return Fluent API
	 */
	@Override
	public User setPassword(String password) {
		setPasswordHash(MeshInternal.get().passwordEncoder().encode(password));
		return this;
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		UserUpdateRequest requestModel = ac.fromJson(UserUpdateRequest.class);
		boolean modified = false;
		if (shouldUpdate(requestModel.getUsername(), getUsername())) {
			User conflictingUser = MeshInternal.get().boot().userRoot().findByUsername(requestModel.getUsername());
			if (conflictingUser != null && !conflictingUser.getUuid().equals(getUuid())) {
				throw conflict(conflictingUser.getUuid(), requestModel.getUsername(), "user_conflicting_username");
			}
			setUsername(requestModel.getUsername());
			modified = true;
		}

		if (shouldUpdate(requestModel.getFirstname(), getFirstname())) {
			setFirstname(requestModel.getFirstname());
			modified = true;
		}

		if (shouldUpdate(requestModel.getLastname(), getLastname())) {
			setLastname(requestModel.getLastname());
			modified = true;
		}

		if (shouldUpdate(requestModel.getEmailAddress(), getEmailAddress())) {
			setEmailAddress(requestModel.getEmailAddress());
			modified = true;
		}

		if (shouldUpdate(requestModel.getForcedPasswordChange(), isForcedPasswordChange())) {
			setForcedPasswordChange(requestModel.getForcedPasswordChange());
			modified = true;
		}

		if (!isEmpty(requestModel.getPassword())) {
			BCryptPasswordEncoder encoder = MeshInternal.get().passwordEncoder();
			setPasswordHash(encoder.encode(requestModel.getPassword()));
			modified = true;
		}

		if (requestModel.getNodeReference() != null) {
			ExpandableNode reference = requestModel.getNodeReference();
			String referencedNodeUuid = null;
			String projectName = null;
			if (reference instanceof NodeResponse) {
				NodeResponse response = (NodeResponse) reference;
				ProjectReference project = response.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "user_incomplete_node_reference");
				}
				projectName = project.getName();
				if (isEmpty(projectName)) {
					throw error(BAD_REQUEST, "user_incomplete_node_reference");
				}
				referencedNodeUuid = response.getUuid();
			}
			if (reference instanceof NodeReference) {
				NodeReference basicReference = ((NodeReference) reference);
				if (isEmpty(basicReference.getProjectName()) || isEmpty(reference.getUuid())) {
					throw error(BAD_REQUEST, "user_incomplete_node_reference");
				}
				referencedNodeUuid = basicReference.getUuid();
				projectName = basicReference.getProjectName();
			}
			if (referencedNodeUuid != null && projectName != null) {
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
				modified = true;
			}

		}

		if (modified) {
			setEditor(ac.getUser());
			setLastEditedTimestamp();
			batch.add(onUpdated());
		}
		return modified;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(super.getETag(ac));
		keyBuilder.append(getLastEditedTimestamp());

		Node referencedNode = getReferencedNode();
		boolean expandReference = ac.getNodeParameters().getExpandedFieldnameList().contains("nodeReference")
			|| ac.getNodeParameters().getExpandAll();
		// We only need to compute the full etag if the referenced node is expanded.
		if (referencedNode != null && expandReference) {
			keyBuilder.append("-");
			keyBuilder.append(referencedNode.getETag(ac));
		} else if (referencedNode != null) {
			keyBuilder.append("-");
			keyBuilder.append(referencedNode.getUuid());
			keyBuilder.append(referencedNode.getProject().getName());
		}
		for (Group group : getGroups()) {
			keyBuilder.append(group.getUuid());
		}

		return ETag.hash(keyBuilder);
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
		return DB.get().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
