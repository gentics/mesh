package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_REFERENCE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyEventQueueBatch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HasPermissions;
import com.gentics.mesh.core.data.MeshAuthUser;
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
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

	public static final String ADMIN_FLAG_PROPERTY_KEY = "adminFlag";

	public static final String RESET_TOKEN_KEY = "resetToken";

	public static final String RESET_TOKEN_ISSUE_TIMESTAMP_KEY = "resetTokenTimestamp";

	public static final String FORCE_PASSWORD_CHANGE_KEY = "forcePasswordChange";

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(UserImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(ASSIGNED_TO_ROLE).withOut());
	}

	@Override
	protected void init(FramedGraph graph, Element element, Object id) {
		super.init(graph, element, id);
		mesh().bucketManager().store(this);
	}

	@Override
	public User disable() {
		// TODO Fixme - The #delete method will currently remove the user instead of disabling it.
		// Thus this method is not used.
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
		// TODO the #delete method will currently delete the user. It will not be deleted.
		// Boolean isEnabled = USER_STATE_CACHE.get(getUuid());
		// if (isEnabled == null) {
		// isEnabled = BooleanUtils.toBoolean(property(ENABLED_FLAG_PROPERTY_KEY).toString());
		// USER_STATE_CACHE.put(getUuid(), isEnabled);
		// }
		//
		// return isEnabled;
		return BooleanUtils.toBoolean(property(ENABLED_FLAG_PROPERTY_KEY).toString());
	}

	@Override
	public boolean isAdmin() {
		Boolean flag = property(ADMIN_FLAG_PROPERTY_KEY);
		return BooleanUtils.toBoolean(flag);
	}

	@Override
	public void setAdmin(boolean flag) {
		property(ADMIN_FLAG_PROPERTY_KEY, flag);
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
		return out(HAS_USER, GroupImpl.class);
	}

	@Override
	public String getRolesHash() {
		String indexName = "e." + ASSIGNED_TO_ROLE + "_out";
		Spliterator<Edge> itemEdges = getGraph().getEdges(indexName.toLowerCase(), id()).spliterator();
		String roles = StreamSupport.stream(itemEdges, false)
			.map(itemEdge -> itemEdge.getVertex(Direction.IN).getId().toString())
			.sorted()
			.collect(Collectors.joining());

		return ETag.hash(roles + String.valueOf(isAdmin()));
	}

	@Override
	public TraversalResult<? extends Role> getRoles() {
		return new TraversalResult<>(out(HAS_USER).in(HAS_ROLE).frameExplicit(RoleImpl.class));
	}

	@Override
	public TraversalResult<? extends Role> getRolesViaShortcut() {
		// TODO Use shortcut index.
		return out(ASSIGNED_TO_ROLE, RoleImpl.class);
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
		return out(HAS_NODE_REFERENCE, NodeImpl.class).nextOrNull();
	}

	@Override
	public User setReferencedNode(Node node) {
		setSingleLinkOutTo(node, HAS_NODE_REFERENCE);
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
		Predicate<? super GraphPermission> isValidPermission = perm -> perm != READ_PUBLISHED_PERM && perm != PUBLISH_PERM
			|| vertex.hasPublishPermissions();

		return Stream.of(GraphPermission.values())
			// Don't check for publish perms if it does not make sense for the vertex type
			.filter(isValidPermission)
			.filter(perm -> hasPermission(vertex, perm))
			.collect(Collectors.toSet());
	}

	@Override
	public boolean hasPermissionForId(Object elementId, GraphPermission permission) {
		PermissionCache permissionCache = mesh().permissionCache();
		if (permissionCache.hasPermission(id(), permission, elementId)) {
			return true;
		} else {
			// Admin users have all permissions
			if (isAdmin()) {
				for (GraphPermission perm : GraphPermission.values()) {
					permissionCache.store(id(), perm, elementId);
				}
				return true;
			}

			FramedGraph graph = getGraph();
			// Find all roles that are assigned to the user by checking the
			// shortcut edge from the index
			String idxKey = "e." + ASSIGNED_TO_ROLE + "_out";
			Iterable<Edge> roleEdges = graph.getEdges(idxKey.toLowerCase(), this.id());
			Vertex vertex = graph.getVertex(elementId);
			for (Edge roleEdge : roleEdges) {
				Vertex role = roleEdge.getVertex(Direction.IN);

				Set<String> allowedRoles = vertex.getProperty(permission.propertyKey());
				boolean hasPermission = allowedRoles != null && allowedRoles.contains(role.<String>getProperty("uuid"));
				if (hasPermission) {
					// We only store granting permissions in the store in order
					// reduce the invalidation calls.
					// This way we do not need to invalidate the cache if a role
					// is removed from a group or a role is deleted.
					permissionCache.store(id(), permission, elementId);
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
	public boolean hasReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		Node node = container.getParentNode();
		if (hasPermission(node, READ_PERM)) {
			return true;
		}
		boolean published = container.isPublished(branchUuid);
		if (published && hasPermission(node, READ_PUBLISHED_PERM)) {
			return true;
		}
		return false;
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
		if (fields.has("admin")) {
			restUser.setAdmin(isAdmin());
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
	public User addCRUDPermissionOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode) {
		addPermissionsOnRole(sourceNode, permission, targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM, PUBLISH_PERM, READ_PUBLISHED_PERM);
		return this;
	}

	@Override
	public User addPermissionsOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant) {
		// 2. Add CRUD permission to identified roles and target node
		for (Role role : sourceNode.getRolesWithPerm(permission)) {
			role.grantPermissions(targetNode, toGrant);
		}
		return this;
	}

	@Override
	public User inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {

		for (GraphPermission perm : GraphPermission.values()) {
			String key = perm.propertyKey();
			targetNode.property(key, sourceNode.property(key));
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
		mesh().permissionCache().clear();
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
		setPasswordHash(mesh().passwordEncoder().encode(password));
		return this;
	}

	@Override
	public boolean updateDry(InternalActionContext ac) {
		return update(ac, new DummyEventQueueBatch(), true);
	}
	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return update(ac, batch, false);
	}

	public boolean update(InternalActionContext ac, EventQueueBatch batch, boolean dry) {
		UserUpdateRequest requestModel = ac.fromJson(UserUpdateRequest.class);
		boolean modified = false;
		if (shouldUpdate(requestModel.getUsername(), getUsername())) {
			User conflictingUser = mesh().boot().userRoot().findByUsername(requestModel.getUsername());
			if (conflictingUser != null && !conflictingUser.getUuid().equals(getUuid())) {
				throw conflict(conflictingUser.getUuid(), requestModel.getUsername(), "user_conflicting_username");
			}
			if (!dry) {
				setUsername(requestModel.getUsername());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getAdmin(), isAdmin())) {
			if (ac.getUser().isAdmin()) {
				setAdmin(requestModel.getAdmin());
				// Permissions need to be purged
				mesh().permissionCache().clear();
			} else {
				throw error(FORBIDDEN, "user_error_admin_privilege_needed_for_admin_flag");
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getFirstname(), getFirstname())) {
			if (!dry) {
				setFirstname(requestModel.getFirstname());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getLastname(), getLastname())) {
			if (!dry) {
				setLastname(requestModel.getLastname());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getEmailAddress(), getEmailAddress())) {
			if (!dry) {
				setEmailAddress(requestModel.getEmailAddress());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getForcedPasswordChange(), isForcedPasswordChange())) {
			if (!dry) {
				setForcedPasswordChange(requestModel.getForcedPasswordChange());
			}
			modified = true;
		}

		if (!isEmpty(requestModel.getPassword())) {
			if (!dry) {
				BCryptPasswordEncoder encoder = mesh().passwordEncoder();
				setPasswordHash(encoder.encode(requestModel.getPassword()));
			}
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
				Project project = mesh().boot().projectRoot().findByName(projectName);
				if (project == null) {
					throw error(BAD_REQUEST, "project_not_found", projectName);
				}
				NodeRoot nodeRoot = project.getNodeRoot();
				Node node = nodeRoot.loadObjectByUuid(ac, referencedNodeUuid, READ_PERM);
				if (!dry) {
					setReferencedNode(node);
				}
				modified = true;
			}

		}

		if (modified && !dry) {
			setEditor(ac.getUser());
			setLastEditedTimestamp();
			batch.add(onUpdated());
		}
		return modified;
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
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
		keyBuilder.append(String.valueOf(isAdmin()));

		return keyBuilder.toString();
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/users/" + getUuid();
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
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public User getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public MeshAuthUser toAuthUser() {
		return reframeExplicit(MeshAuthUserImpl.class);
	}

}
